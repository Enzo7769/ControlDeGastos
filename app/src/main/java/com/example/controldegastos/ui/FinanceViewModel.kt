package com.example.controldegastos.ui

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.controldegastos.data.AppSettings
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.ExpenseType
import com.example.controldegastos.data.FinanceData
import com.example.controldegastos.data.FinanceRepository
import com.example.controldegastos.data.IncomeSource
import com.example.controldegastos.data.SettingsStore
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.toClp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class UpdateInfo(
    val apkUrl: String,
    val versionCode: Long,
    val versionName: String,
    val notes: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val settingsStore = SettingsStore(application)

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val repository = MutableStateFlow<FinanceRepository?>(null)

    /** Usuario con sesión abierta al iniciar (viene de la versión anterior): dueño de los datos antiguos locales. */
    private val sessionUidAtStart: String? = auth.currentUser?.uid

    val data: StateFlow<FinanceData> = repository
        .flatMapLatest { it?.data ?: flowOf(FinanceData()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinanceData())

    val syncState: StateFlow<FinanceRepository.SyncState> = repository
        .flatMapLatest { it?.syncState ?: flowOf(FinanceRepository.SyncState.OFFLINE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinanceRepository.SyncState.SYNCING)

    val settings: StateFlow<AppSettings> = settingsStore.settings

    /** Ítem a abrir al tocar una notificación de vencimiento. */
    val pendingOpenItem = MutableStateFlow<String?>(null)

    /** Mes calendario actual. */
    val today: YearMonth get() = YearMonth.now()

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        _user.value = fa.currentUser
        bindRepository(fa.currentUser)
    }

    init {
        bindRepository(auth.currentUser)
        auth.addAuthStateListener(authListener)
    }

    private fun bindRepository(user: FirebaseUser?) {
        val current = repository.value
        if (user == null) {
            current?.stop()
            repository.value = null
            return
        }
        if (current != null && current.userId == user.uid) return
        current?.stop()
        repository.value = FinanceRepository(getApplication(), user.uid, importLegacy = user.uid == sessionUidAtStart).also { it.start() }
    }


    private fun edit(transform: (FinanceData) -> FinanceData) {
        repository.value?.update(transform)
    }

    // ---------- Ítems (tarjetas, créditos, gastos fijos) ----------

    fun item(id: String): ExpenseItem? = data.value.items.firstOrNull { it.id == id }

    /** Crea o actualiza un ítem (si ya existe uno con el mismo id lo reemplaza). */
    fun saveItem(item: ExpenseItem) = edit { d -> d.copy(items = d.items.upsert(item) { it.id }) }

    /** Elimina y devuelve el ítem junto a su posición, para poder deshacer. */
    fun deleteItem(id: String): Pair<Int, ExpenseItem>? {
        val removed = data.value.items.findWithIndex { it.id == id } ?: return null
        edit { d -> d.copy(items = d.items.filterNot { it.id == id }) }
        return removed
    }

    fun restoreItem(position: Int, item: ExpenseItem) = edit { d -> d.copy(items = d.items.insertAt(position, item)) }

    private fun editItem(id: String, transform: (ExpenseItem) -> ExpenseItem) = edit { d ->
        d.copy(items = d.items.map { if (it.id == id) transform(it) else it })
    }

    fun setAmount(itemId: String, month: YearMonth, amount: Double) =
        editItem(itemId) { FinanceCalculator.setAmount(it, month, amount) }

    fun togglePaid(itemId: String, month: YearMonth) =
        editItem(itemId) { FinanceCalculator.togglePaid(it, month) }

    fun addInstallments(itemId: String, amount: Double, count: Int, start: YearMonth) =
        editItem(itemId) { FinanceCalculator.addInstallments(it, amount, count, start) }

    // ---------- Ingresos ----------

    fun saveIncome(income: IncomeSource) = edit { d -> d.copy(incomes = d.incomes.upsert(income) { it.id }) }

    fun deleteIncome(id: String): Pair<Int, IncomeSource>? {
        val removed = data.value.incomes.findWithIndex { it.id == id } ?: return null
        edit { d -> d.copy(incomes = d.incomes.filterNot { it.id == id }) }
        return removed
    }

    fun restoreIncome(position: Int, income: IncomeSource) = edit { d -> d.copy(incomes = d.incomes.insertAt(position, income)) }

    // ---------- Ajustes ----------

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)

    fun signOut() {
        updateSettings { it.copy(biometricLock = false) }
        auth.signOut()
    }

    // ---------- Exportar ----------

    fun buildShareSummary(): String {
        val d = data.value
        val includeFixed = settings.value.includeFixedExpenses
        val now = today
        val sb = StringBuilder()
        sb.appendLine("📊 Resumen de mis finanzas — ${now.longLabel()}")
        sb.appendLine()
        sb.appendLine("💰 Ingresos mensuales: ${FinanceCalculator.totalIncome(d).toClp()}")
        d.incomes.forEach { sb.appendLine("   • ${it.name}: ${it.monthlyAmount.toClp()}") }
        sb.appendLine()
        sb.appendLine("📅 Próximos meses")
        (0 until 6).forEach { i ->
            val s = FinanceCalculator.monthSummary(d, now + i, includeFixed)
            sb.appendLine("   • ${s.month.longLabel()}: gastos ${s.expenses.toClp()} · saldo ${s.balance.toClp()}")
        }
        sb.appendLine()
        sb.appendLine("💳 Deudas")
        d.items.filter { it.type != ExpenseType.FIXED }.forEach { item ->
            val extra = if (item.isCreditCard && item.creditLimit > 0) " (disponible ${item.availableCredit.toClp()})" else ""
            sb.appendLine("   • ${item.icon} ${item.name}: ${item.currentDebt.toClp()}$extra")
        }
        sb.appendLine()
        sb.append("Generado con Control de Gastos")
        return sb.toString()
    }

    // ---------- Actualizaciones (Firebase Remote Config) ----------

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build()
            )
        }
    }

    private val packageInfo = runCatching {
        application.packageManager.getPackageInfo(application.packageName, 0)
    }.getOrNull()

    val versionName: String = packageInfo?.versionName ?: "-"

    private val versionCode: Long = packageInfo?.let(PackageInfoCompat::getLongVersionCode) ?: 1L

    /** Consulta si hay una versión nueva. [onResult] recibe null si la app está al día. */
    fun checkForUpdates(onResult: (UpdateInfo?) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener onResult(null)
            val url = remoteConfig.getString("apkUrl")
            val code = remoteConfig.getLong("latestVersionCode")
            if (code > versionCode && url.isNotBlank()) {
                onResult(
                    UpdateInfo(
                        apkUrl = url,
                        versionCode = code,
                        versionName = remoteConfig.getString("latestVersionName"),
                        notes = remoteConfig.getString("updateNotes"),
                    )
                )
            } else {
                onResult(null)
            }
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        repository.value?.stop()
        super.onCleared()
    }
}

/** Reemplaza el elemento con el mismo id o lo agrega al final. */
private inline fun <T> List<T>.upsert(element: T, id: (T) -> String): List<T> =
    if (any { id(it) == id(element) }) map { if (id(it) == id(element)) element else it } else this + element

private inline fun <T> List<T>.findWithIndex(predicate: (T) -> Boolean): Pair<Int, T>? =
    indexOfFirst(predicate).takeIf { it >= 0 }?.let { it to this[it] }

private fun <T> List<T>.insertAt(position: Int, element: T): List<T> =
    toMutableList().apply { add(position.coerceIn(0, size), element) }

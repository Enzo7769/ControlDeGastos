package com.example.controldegastos.ui

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.controldegastos.data.AppSettings
import com.example.controldegastos.data.ExpenseItem
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

    val data: StateFlow<FinanceData> = repository
        .flatMapLatest { it?.data ?: flowOf(FinanceData()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinanceData())

    val syncState: StateFlow<FinanceRepository.SyncState> = repository
        .flatMapLatest { it?.syncState ?: flowOf(FinanceRepository.SyncState.OFFLINE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinanceRepository.SyncState.SYNCING)

    val settings: StateFlow<AppSettings> = settingsStore.settings

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
        repository.value = FinanceRepository(getApplication(), user.uid).also { it.start() }
    }


    private fun edit(transform: (FinanceData) -> FinanceData) {
        repository.value?.update(transform)
    }

    // ---------- Ítems (tarjetas, créditos, gastos fijos) ----------

    fun item(id: String): ExpenseItem? = data.value.items.firstOrNull { it.id == id }

    /** Crea o actualiza un ítem (si ya existe uno con el mismo id lo reemplaza). */
    fun saveItem(item: ExpenseItem) = edit { d ->
        val exists = d.items.any { it.id == item.id }
        d.copy(items = if (exists) d.items.map { if (it.id == item.id) item else it } else d.items + item)
    }

    /** Elimina y devuelve el ítem junto a su posición, para poder deshacer. */
    fun deleteItem(id: String): Pair<Int, ExpenseItem>? {
        val idx = data.value.items.indexOfFirst { it.id == id }
        if (idx < 0) return null
        val removed = data.value.items[idx]
        edit { d -> d.copy(items = d.items.filterNot { it.id == id }) }
        return idx to removed
    }

    fun restoreItem(position: Int, item: ExpenseItem) = edit { d ->
        val list = d.items.toMutableList()
        list.add(position.coerceIn(0, list.size), item)
        d.copy(items = list)
    }

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

    fun saveIncome(income: IncomeSource) = edit { d ->
        val exists = d.incomes.any { it.id == income.id }
        d.copy(incomes = if (exists) d.incomes.map { if (it.id == income.id) income else it } else d.incomes + income)
    }

    fun deleteIncome(id: String): Pair<Int, IncomeSource>? {
        val idx = data.value.incomes.indexOfFirst { it.id == id }
        if (idx < 0) return null
        val removed = data.value.incomes[idx]
        edit { d -> d.copy(incomes = d.incomes.filterNot { it.id == id }) }
        return idx to removed
    }

    fun restoreIncome(position: Int, income: IncomeSource) = edit { d ->
        val list = d.incomes.toMutableList()
        list.add(position.coerceIn(0, list.size), income)
        d.copy(incomes = list)
    }

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
        d.items.forEach { item ->
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

    val versionName: String
        get() = runCatching {
            val app = getApplication<Application>()
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull() ?: "-"

    private val versionCode: Long
        get() = runCatching {
            val app = getApplication<Application>()
            PackageInfoCompat.getLongVersionCode(app.packageManager.getPackageInfo(app.packageName, 0))
        }.getOrDefault(1L)

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

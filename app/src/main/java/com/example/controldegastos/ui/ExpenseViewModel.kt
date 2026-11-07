package com.example.controldegastos.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.example.controldegastos.data.CreditCardItem
import com.example.controldegastos.data.IncomeSource
import com.example.controldegastos.data.MonthColumn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("ControlDeGastosPrefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val _months = MutableStateFlow<List<MonthColumn>>(loadMonths())
    val months: StateFlow<List<MonthColumn>> = _months.asStateFlow()

    private val _creditCards = MutableStateFlow<List<CreditCardItem>>(loadCreditCards())
    val creditCards: StateFlow<List<CreditCardItem>> = _creditCards.asStateFlow()

    private val _incomeSources = MutableStateFlow<List<IncomeSource>>(loadIncomes())
    val incomeSources: StateFlow<List<IncomeSource>> = _incomeSources.asStateFlow()

    private val _includeFixedExpenses = MutableStateFlow(sharedPreferences.getBoolean("include_fixed", true))
    val includeFixedExpenses: StateFlow<Boolean> = _includeFixedExpenses.asStateFlow()

    private val _selectedTheme = MutableStateFlow(sharedPreferences.getString("theme_palette", "Azul Clásico") ?: "Azul Clásico")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _useBiometrics = MutableStateFlow(sharedPreferences.getBoolean("use_biometrics", false))
    val useBiometrics: StateFlow<Boolean> = _useBiometrics.asStateFlow()

    val totalMonthlyIncome: Double
        get() = _incomeSources.value.sumOf { it.monthlyAmount }

    val clpFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val todayFormattedDate: String
        get() {
            val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "CL"))
            return dateFormat.format(Calendar.getInstance().time).replaceFirstChar { it.titlecase(Locale("es", "CL")) }
        }

    val availableCategoryIcons = listOf(
        "💳" to "Tarjeta de Crédito",
        "🛒" to "Supermercado / Compras",
        "🚗" to "Vehículo / Transporte",
        "🏠" to "Casa / Vivienda",
        "✈️" to "Viajes / Vacaciones",
        "📱" to "Tecnología / Servicios",
        "📄" to "Préstamo / Crédito",
        "💼" to "Otro Gasto"
    )

    init {
        syncDataFromCloud()
    }

    private fun syncDataFromCloud() {
        if (currentUser == null) return
        val docRef = firestore.collection("users").document(currentUser.uid).collection("data").document("financial_info")
        docRef.get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                // Load from cloud into local state & storage
                val cardsJson = doc.getString("cardsJson")
                val incomesJson = doc.getString("incomesJson")
                val monthsJson = doc.getString("monthsJson")

                if (!cardsJson.isNullOrBlank()) {
                    sharedPreferences.edit().putString("saved_cards", cardsJson).apply()
                    _creditCards.value = loadCreditCards()
                }
                if (!incomesJson.isNullOrBlank()) {
                    sharedPreferences.edit().putString("saved_incomes", incomesJson).apply()
                    _incomeSources.value = loadIncomes()
                }
                if (!monthsJson.isNullOrBlank()) {
                    sharedPreferences.edit().putString("saved_months", monthsJson).apply()
                    _months.value = loadMonths()
                }
            } else {
                // Push local data to cloud for the first time
                syncDataToCloud()
            }
        }
    }

    private fun syncDataToCloud() {
        if (currentUser == null) return
        val cardsJson = sharedPreferences.getString("saved_cards", "[]") ?: "[]"
        val incomesJson = sharedPreferences.getString("saved_incomes", "[]") ?: "[]"
        val monthsJson = sharedPreferences.getString("saved_months", "[]") ?: "[]"

        val data = hashMapOf(
            "cardsJson" to cardsJson,
            "incomesJson" to incomesJson,
            "monthsJson" to monthsJson,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(currentUser.uid).collection("data").document("financial_info")
            .set(data)
    }

    // Firebase Remote Config for In-App Updates
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build()
        )
    }

    data class UpdateInfo(
        val apkUrl: String,
        val latestVersionCode: Long,
        val latestVersionName: String,
        val updateNotes: String
    )

    fun checkForUpdates(onUpdateAvailable: (UpdateInfo) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val apkUrl = remoteConfig.getString("apkUrl")
                val versionCode = remoteConfig.getLong("latestVersionCode")
                val versionName = remoteConfig.getString("latestVersionName")
                val notes = remoteConfig.getString("updateNotes")

                val currentVersionCode = getCurrentVersionCode()
                if (versionCode > currentVersionCode && apkUrl.isNotBlank()) {
                    onUpdateAvailable(UpdateInfo(apkUrl, versionCode, versionName, notes))
                }
            }
        }
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val app = getApplication<Application>()
            val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        } catch (_: Exception) {
            1L
        }
    }

    fun setUseBiometrics(enabled: Boolean) {
        _useBiometrics.value = enabled
        sharedPreferences.edit().putBoolean("use_biometrics", enabled).apply()
    }

    fun setThemePalette(themeName: String) {
        _selectedTheme.value = themeName
        sharedPreferences.edit().putString("theme_palette", themeName).apply()
    }

    fun toggleIncludeFixedExpenses(include: Boolean) {
        _includeFixedExpenses.value = include
        sharedPreferences.edit().putBoolean("include_fixed", include).apply()
    }

    fun addMonth(monthName: String) {
        val newIndex = (_months.value.maxOfOrNull { it.index } ?: -1) + 1
        val updated = _months.value + MonthColumn(newIndex, monthName)
        _months.value = updated
        saveMonths(updated)
    }

    fun togglePaidMonth(cardId: String, monthIndex: Int) {
        _creditCards.update { list ->
            list.map { card ->
                if (card.id == cardId) {
                    val updatedPaid = card.paidMonths.toMutableSet()
                    if (updatedPaid.contains(monthIndex)) {
                        updatedPaid.remove(monthIndex)
                    } else {
                        updatedPaid.add(monthIndex)
                    }
                    val updatedCard = card.copy(paidMonths = updatedPaid)
                    saveCreditCards(list.map { if (it.id == cardId) updatedCard else it })
                    updatedCard
                } else {
                    card
                }
            }
        }
    }

    fun addCardItem(name: String, categoryIcon: String, isCredit: Boolean, limit: Double, usedQuota: Double, initialAmounts: Map<Int, Double>) {
        val newItem = CreditCardItem(
            name = name,
            categoryIcon = categoryIcon,
            isCreditCard = isCredit,
            creditLimit = limit,
            manualUsedQuota = usedQuota,
            monthlyAmounts = initialAmounts.toMutableMap()
        )
        _creditCards.update {
            val updated = it + newItem
            saveCreditCards(updated)
            updated
        }
    }

    fun updateCardInfo(cardId: String, name: String, categoryIcon: String, isCredit: Boolean, creditLimit: Double, manualUsedQuota: Double) {
        _creditCards.update { list ->
            val updated = list.map { card ->
                if (card.id == cardId) {
                    card.copy(
                        name = name,
                        categoryIcon = categoryIcon,
                        isCreditCard = isCredit,
                        creditLimit = creditLimit,
                        manualUsedQuota = manualUsedQuota
                    )
                } else {
                    card
                }
            }
            saveCreditCards(updated)
            updated
        }
    }

    fun deleteCardItem(cardId: String) {
        _creditCards.update { list ->
            val updated = list.filter { it.id != cardId }
            saveCreditCards(updated)
            updated
        }
    }

    fun updateMonthlyAmount(cardId: String, monthIndex: Int, amount: Double) {
        _creditCards.update { list ->
            val updated = list.map { card ->
                if (card.id == cardId) {
                    val updatedAmounts = card.monthlyAmounts.toMutableMap()
                    if (amount <= 0.0) {
                        updatedAmounts.remove(monthIndex)
                    } else {
                        updatedAmounts[monthIndex] = amount
                    }
                    card.copy(monthlyAmounts = updatedAmounts)
                } else {
                    card
                }
            }
            saveCreditCards(updated)
            updated
        }
    }

    fun addIncomeSource(name: String, amount: Double) {
        _incomeSources.update {
            val updated = it + IncomeSource(name = name, monthlyAmount = amount)
            saveIncomes(updated)
            updated
        }
    }

    fun deleteIncomeSource(incomeId: String) {
        _incomeSources.update {
            val updated = it.filter { inc -> inc.id != incomeId }
            saveIncomes(updated)
            updated
        }
    }

    private fun isFixedExpense(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("casa") || lower.contains("apoyo") || lower.contains("gasto fijo")
    }

    fun getTotalExpenseForMonth(monthIndex: Int): Double {
        val includeFixed = _includeFixedExpenses.value
        return _creditCards.value.sumOf { card ->
            if (!includeFixed && isFixedExpense(card.name)) {
                0.0
            } else {
                card.monthlyAmounts[monthIndex] ?: 0.0
            }
        }
    }

    fun getCupoQueSobra(monthIndex: Int): Double {
        return totalMonthlyIncome - getTotalExpenseForMonth(monthIndex)
    }

    fun getUsageColor(usageRatio: Float): Color {
        return when {
            usageRatio < 0.5f -> Color(0xFF2E7D32)
            usageRatio < 0.8f -> Color(0xFFEF6C00)
            else -> Color(0xFFC62828)
        }
    }

    private fun generateRealCurrentMonths(): List<MonthColumn> {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM-yy", Locale("es", "CL"))
        val list = mutableListOf<MonthColumn>()
        for (i in 0 until 12) {
            val monthName = monthFormat.format(calendar.time).replaceFirstChar { it.titlecase(Locale("es", "CL")) }
            list.add(MonthColumn(i, monthName))
            calendar.add(Calendar.MONTH, 1)
        }
        return list
    }

    // --- Persistence & Cloud Sync Helpers ---

    private fun saveMonths(list: List<MonthColumn>) {
        val jsonArray = JSONArray()
        list.forEach { m ->
            val obj = JSONObject().apply {
                put("index", m.index)
                put("name", m.name)
            }
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString("saved_months", jsonArray.toString()).apply()
        syncDataToCloud()
    }

    private fun loadMonths(): List<MonthColumn> {
        val str = sharedPreferences.getString("saved_months", null) ?: return generateRealCurrentMonths()
        try {
            val arr = JSONArray(str)
            val result = mutableListOf<MonthColumn>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(MonthColumn(obj.getInt("index"), obj.getString("name")))
            }
            return if (result.isNotEmpty()) result else generateRealCurrentMonths()
        } catch (_: Exception) {
            return generateRealCurrentMonths()
        }
    }

    private fun saveCreditCards(list: List<CreditCardItem>) {
        val jsonArray = JSONArray()
        list.forEach { card ->
            val obj = JSONObject().apply {
                put("id", card.id)
                put("name", card.name)
                put("categoryIcon", card.categoryIcon)
                put("isCreditCard", card.isCreditCard)
                put("creditLimit", card.creditLimit)
                put("manualUsedQuota", card.manualUsedQuota)

                val amountsObj = JSONObject()
                card.monthlyAmounts.forEach { (k, v) -> amountsObj.put(k.toString(), v) }
                put("monthlyAmounts", amountsObj)

                val paidArr = JSONArray()
                card.paidMonths.forEach { paidArr.put(it) }
                put("paidMonths", paidArr)
            }
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString("saved_cards", jsonArray.toString()).apply()
        syncDataToCloud()
    }

    private fun loadCreditCards(): List<CreditCardItem> {
        val str = sharedPreferences.getString("saved_cards", null) ?: return listOf(
            CreditCardItem(id = "c1", name = "Ripley", categoryIcon = "💳", isCreditCard = true, creditLimit = 1500000.0, manualUsedQuota = 1200000.0, monthlyAmounts = mutableMapOf(0 to 220467.0, 1 to 470085.0, 2 to 288706.0, 3 to 285582.0, 4 to 277073.0, 5 to 213063.0)),
            CreditCardItem(id = "c2", name = "Falabella", categoryIcon = "💳", isCreditCard = true, creditLimit = 1200000.0, manualUsedQuota = 350000.0, monthlyAmounts = mutableMapOf(0 to 108890.0, 1 to 40000.0, 2 to 40375.0, 3 to 40577.0, 4 to 40577.0, 5 to 36610.0)),
            CreditCardItem(id = "c3", name = "match", categoryIcon = "📱", isCreditCard = false, creditLimit = 500000.0, manualUsedQuota = 0.0, monthlyAmounts = mutableMapOf(0 to 129757.0, 1 to 90315.0, 2 to 100000.0, 3 to 100000.0, 4 to 40000.0)),
            CreditCardItem(id = "c4", name = "match Cre", categoryIcon = "💳", isCreditCard = true, creditLimit = 800000.0, manualUsedQuota = 570000.0, monthlyAmounts = mutableMapOf(1 to 124454.0, 2 to 129590.0, 3 to 129587.0, 4 to 96284.0, 5 to 92296.0)),
            CreditCardItem(id = "c5", name = "APOYO CASA", categoryIcon = "🏠", isCreditCard = false, creditLimit = 0.0, manualUsedQuota = 0.0, monthlyAmounts = mutableMapOf(2 to 60000.0, 3 to 60000.0, 4 to 60000.0, 5 to 60000.0)),
            CreditCardItem(id = "c6", name = "CASA (Gasto Fijo)", categoryIcon = "🏠", isCreditCard = false, creditLimit = 0.0, manualUsedQuota = 0.0, monthlyAmounts = mutableMapOf(2 to 300000.0, 3 to 300000.0, 4 to 300000.0, 5 to 300000.0))
        )
        try {
            val arr = JSONArray(str)
            val result = mutableListOf<CreditCardItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val amountsMap = mutableMapOf<Int, Double>()
                val amountsObj = obj.optJSONObject("monthlyAmounts")
                if (amountsObj != null) {
                    val keys = amountsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        amountsMap[k.toInt()] = amountsObj.getDouble(k)
                    }
                }

                val paidSet = mutableSetOf<Int>()
                val paidArr = obj.optJSONArray("paidMonths")
                if (paidArr != null) {
                    for (p in 0 until paidArr.length()) {
                        paidSet.add(paidArr.getInt(p))
                    }
                }

                result.add(
                    CreditCardItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        categoryIcon = obj.optString("categoryIcon", "💳"),
                        isCreditCard = obj.getBoolean("isCreditCard"),
                        creditLimit = obj.getDouble("creditLimit"),
                        manualUsedQuota = obj.optDouble("manualUsedQuota", 0.0),
                        monthlyAmounts = amountsMap,
                        paidMonths = paidSet
                    )
                )
            }
            return result
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun saveIncomes(list: List<IncomeSource>) {
        val jsonArray = JSONArray()
        list.forEach { inc ->
            val obj = JSONObject().apply {
                put("id", inc.id)
                put("name", inc.name)
                put("monthlyAmount", inc.monthlyAmount)
            }
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString("saved_incomes", jsonArray.toString()).apply()
        syncDataToCloud()
    }

    private fun loadIncomes(): List<IncomeSource> {
        val str = sharedPreferences.getString("saved_incomes", null) ?: return listOf(
            IncomeSource(id = "i1", name = "Trabajo 1", monthlyAmount = 1050000.0),
            IncomeSource(id = "i2", name = "Trabajo 2", monthlyAmount = 940000.0)
        )
        try {
            val arr = JSONArray(str)
            val result = mutableListOf<IncomeSource>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(IncomeSource(obj.getString("id"), obj.getString("name"), obj.getDouble("monthlyAmount")))
            }
            return result
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun generateExportSummary(): String {
        val sb = StringBuilder()
        sb.append("📊 *RESUMEN DE CONTROL DE GASTOS Y CUOTAS*\n")
        sb.append("Fecha: $todayFormattedDate\n")
        sb.append("====================================\n\n")

        sb.append("💰 *INGRESOS MENSUALES TOTALES:* ${clpFormat.format(totalMonthlyIncome)}\n")
        _incomeSources.value.forEach { inc ->
            sb.append("  - ${inc.name}: ${clpFormat.format(inc.monthlyAmount)}\n")
        }
        sb.append("\n")

        sb.append("📈 *PROYECCIÓN MES A MES (GASTOS VS SOBRANTE)*\n")
        sb.append("------------------------------------\n")
        months.value.forEach { month ->
            val totalExp = getTotalExpenseForMonth(month.index)
            val sobra = getCupoQueSobra(month.index)
            sb.append("• *${month.name}*:\n")
            sb.append("  Gastos: ${clpFormat.format(totalExp)} | Sobra: ${clpFormat.format(sobra)}\n")
        }
        sb.append("\n")

        sb.append("💳 *DETALLE DE TARJETAS Y GASTOS*\n")
        sb.append("------------------------------------\n")
        _creditCards.value.forEach { card ->
            sb.append("• ${card.categoryIcon} *${card.name}* (${if (card.isCreditCard) "Tarjeta de Crédito" else "Gasto Fijo"})\n")
            if (card.isCreditCard) {
                sb.append("  Cupo Total: ${clpFormat.format(card.creditLimit)}\n")
                sb.append("  Cupo Usado (Banco): ${clpFormat.format(card.totalDebt)}\n")
                sb.append("  Cupo Disponible: ${clpFormat.format(card.availableLimit)}\n")
            } else {
                sb.append("  Deuda Acumulada: ${clpFormat.format(card.totalDebt)}\n")
            }
            sb.append("  Montos por mes:\n")
            card.monthlyAmounts.forEach { (mIdx, amt) ->
                val mName = months.value.find { it.index == mIdx }?.name ?: "Mes $mIdx"
                val paidStatus = if (card.paidMonths.contains(mIdx)) " [PAGADO ✓]" else ""
                sb.append("    - $mName: ${clpFormat.format(amt)}$paidStatus\n")
            }
            sb.append("\n")
        }

        sb.append("Generado desde ControlDeGastos App\n")
        return sb.toString()
    }
}

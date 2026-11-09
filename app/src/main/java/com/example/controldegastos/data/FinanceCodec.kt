package com.example.controldegastos.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialización JSON de [FinanceData] y migración desde el formato antiguo
 * (montos indexados por posición de columna 0, 1, 2… y lista de meses aparte).
 */
object FinanceCodec {

    const val SCHEMA_VERSION = 2

    // ---------- Formato actual (v2) ----------

    fun encode(data: FinanceData): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("updatedAt", data.updatedAt)
        root.put("items", JSONArray().apply { data.items.forEach { put(encodeItem(it)) } })
        root.put("incomes", JSONArray().apply {
            data.incomes.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("monthlyAmount", it.monthlyAmount))
            }
        })
        return root.toString()
    }

    private fun encodeItem(item: ExpenseItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("icon", item.icon)
        put("type", item.type.name)
        put("creditLimit", item.creditLimit)
        put("usedQuota", item.usedQuota)
        put("amounts", JSONObject().apply {
            item.amounts.toSortedMap().forEach { (ym, v) -> put(ym.key(), v) }
        })
        put("paid", JSONArray().apply { item.paidMonths.sorted().forEach { put(it.key()) } })
        item.dueDay?.let { put("dueDay", it) }
    }

    /** Devuelve null si el texto no es un documento v2 válido. */
    fun decode(json: String?): FinanceData? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            if (root.optInt("schemaVersion", 0) < 2) return null
            val items = root.optJSONArray("items").objects().map(::decodeItem)
            val incomes = root.optJSONArray("incomes").objects().map {
                IncomeSource(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    monthlyAmount = it.optDouble("monthlyAmount", 0.0),
                )
            }
            FinanceData(items, incomes, root.optLong("updatedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeItem(obj: JSONObject): ExpenseItem {
        val amounts = mutableMapOf<YearMonth, Double>()
        obj.optJSONObject("amounts")?.let { a ->
            a.keys().forEach { k -> YearMonth.parseKey(k)?.let { amounts[it] = a.optDouble(k, 0.0) } }
        }
        val paid = mutableSetOf<YearMonth>()
        obj.optJSONArray("paid")?.let { arr ->
            for (i in 0 until arr.length()) YearMonth.parseKey(arr.optString(i))?.let(paid::add)
        }
        return ExpenseItem(
            id = obj.getString("id"),
            name = obj.getString("name"),
            icon = obj.optString("icon", "💳"),
            type = ExpenseType.fromName(obj.optString("type")),
            creditLimit = obj.optDouble("creditLimit", 0.0),
            usedQuota = obj.optDouble("usedQuota", 0.0),
            amounts = amounts.filterValues { it > 0.0 },
            paidMonths = paid,
            dueDay = if (obj.has("dueDay")) obj.optInt("dueDay").takeIf { it in 1..31 } else null,
        )
    }

    // ---------- Migración desde la versión 1 ----------

    /**
     * Convierte los datos de la versión anterior de la app.
     * @param cardsJson lista de tarjetas con "monthlyAmounts" por índice y "paidMonths".
     * @param incomesJson lista de ingresos.
     * @param monthsJson lista de columnas {index, name} (ej. "Nov-26").
     * @param fallbackStart mes que se asume para el índice 0 si no se puede deducir de los nombres.
     */
    fun migrateLegacy(
        cardsJson: String?,
        incomesJson: String?,
        monthsJson: String?,
        fallbackStart: YearMonth,
        updatedAt: Long = 0L,
    ): FinanceData? {
        if (cardsJson.isNullOrBlank() && incomesJson.isNullOrBlank()) return null
        return try {
            val monthForIndex = buildLegacyMonthResolver(monthsJson, fallbackStart)

            val items = JSONArray(cardsJson ?: "[]").objects().map { obj ->
                val name = obj.optString("name", "Sin nombre")
                val isCredit = obj.optBoolean("isCreditCard", true)
                val type = when {
                    isLegacyFixed(name) -> ExpenseType.FIXED
                    isCredit -> ExpenseType.CREDIT_CARD
                    else -> ExpenseType.LOAN
                }
                val amounts = mutableMapOf<YearMonth, Double>()
                obj.optJSONObject("monthlyAmounts")?.let { a ->
                    a.keys().forEach { k ->
                        val idx = k.toIntOrNull() ?: return@forEach
                        val v = a.optDouble(k, 0.0)
                        if (v > 0.0) amounts[monthForIndex(idx)] = v
                    }
                }
                val paid = mutableSetOf<YearMonth>()
                obj.optJSONArray("paidMonths")?.let { arr ->
                    for (i in 0 until arr.length()) paid += monthForIndex(arr.optInt(i))
                }
                ExpenseItem(
                    id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    name = name,
                    icon = obj.optString("categoryIcon", "💳"),
                    type = type,
                    creditLimit = if (type == ExpenseType.CREDIT_CARD) obj.optDouble("creditLimit", 0.0) else 0.0,
                    usedQuota = if (type == ExpenseType.CREDIT_CARD) obj.optDouble("manualUsedQuota", 0.0) else 0.0,
                    amounts = amounts,
                    paidMonths = paid,
                )
            }
            val incomes = JSONArray(incomesJson ?: "[]").objects().map {
                IncomeSource(
                    id = it.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    name = it.optString("name", "Ingreso"),
                    monthlyAmount = it.optDouble("monthlyAmount", 0.0),
                )
            }
            FinanceData(items, incomes, updatedAt)
        } catch (_: Exception) {
            null
        }
    }

    private fun isLegacyFixed(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("casa") || lower.contains("apoyo") || lower.contains("gasto fijo")
    }

    /**
     * La app antigua guardaba los meses como columnas con nombre ("Nov-26", "dic.-26", "Abr-27").
     * Se intenta leer el nombre de cada columna; si no se puede, se deduce a partir de otra columna
     * reconocida (índice consecutivo = mes consecutivo) o del mes de respaldo.
     */
    private fun buildLegacyMonthResolver(monthsJson: String?, fallbackStart: YearMonth): (Int) -> YearMonth {
        val parsed = mutableMapOf<Int, YearMonth>()
        if (!monthsJson.isNullOrBlank()) {
            runCatching {
                JSONArray(monthsJson).objects().forEach { o ->
                    val idx = o.optInt("index", -1)
                    parseLegacyMonthName(o.optString("name"))?.let { if (idx >= 0) parsed[idx] = it }
                }
            }
        }
        // Base: mes que corresponde al índice 0, tomada de la primera columna reconocible.
        val base = parsed.entries.minByOrNull { it.key }?.let { it.value - it.key } ?: fallbackStart
        return { idx -> parsed[idx] ?: (base + idx) }
    }

    private val monthPrefixes = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** "Nov-26" → 2026-11, "sept.-27" → 2027-09, "Agosto 2026" → 2026-08. Null si no se reconoce. */
    fun parseLegacyMonthName(name: String?): YearMonth? {
        if (name.isNullOrBlank()) return null
        val lower = name.lowercase().trim()
        val letters = lower.takeWhile { it.isLetter() }
        if (letters.length < 3) return null
        val monthIdx = monthPrefixes.indexOf(letters.take(3))
        if (monthIdx < 0) return null
        val digits = Regex("(\\d{2,4})").findAll(lower).lastOrNull()?.value ?: return null
        val year = when (digits.length) {
            2 -> 2000 + digits.toInt()
            4 -> digits.toInt()
            else -> return null
        }
        return YearMonth(year, monthIdx + 1)
    }

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }
}

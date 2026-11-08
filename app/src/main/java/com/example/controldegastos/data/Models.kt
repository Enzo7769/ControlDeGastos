package com.example.controldegastos.data

import java.util.Calendar
import java.util.UUID

/**
 * Mes calendario real (año + mes 1..12). Se usa como clave de los montos mensuales,
 * así los datos no dependen de "cuándo se abrió la app por primera vez".
 */
data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    init {
        require(month in 1..12) { "Mes inválido: $month" }
    }

    /** Número absoluto de meses, útil para restar y sumar. */
    val ordinal: Int get() = year * 12 + (month - 1)

    operator fun plus(months: Int): YearMonth = fromOrdinal(ordinal + months)
    operator fun minus(months: Int): YearMonth = fromOrdinal(ordinal - months)

    /** Meses que van desde [other] hasta este mes (positivo si este es posterior). */
    fun monthsFrom(other: YearMonth): Int = ordinal - other.ordinal

    val lengthOfMonth: Int
        get() = when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

    /** Clave estable para guardar en JSON: "2026-11". */
    fun key(): String = "%04d-%02d".format(year, month)

    override fun compareTo(other: YearMonth): Int = ordinal.compareTo(other.ordinal)

    override fun toString(): String = key()

    companion object {
        fun fromOrdinal(ordinal: Int): YearMonth =
            YearMonth(Math.floorDiv(ordinal, 12), Math.floorMod(ordinal, 12) + 1)

        fun parseKey(key: String): YearMonth? {
            val parts = key.split("-")
            if (parts.size != 2) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return if (m in 1..12) YearMonth(y, m) else null
        }

        fun now(calendar: Calendar = Calendar.getInstance()): YearMonth =
            YearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
}

/** Fecha simple (sin hora), compatible con todas las versiones de Android. */
data class CalendarDate(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDate> {
    val yearMonth: YearMonth get() = YearMonth(year, month)

    /** Días desde 1970-01-01 (algoritmo civil de Howard Hinnant). */
    val epochDay: Long
        get() {
            val y = if (month <= 2) year - 1 else year
            val era = Math.floorDiv(y, 400)
            val yoe = y - era * 400
            val mp = (month + 9) % 12
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era.toLong() * 146097 + doe - 719468
        }

    fun daysUntil(other: CalendarDate): Int = (other.epochDay - epochDay).toInt()

    override fun compareTo(other: CalendarDate): Int = epochDay.compareTo(other.epochDay)

    companion object {
        fun today(calendar: Calendar = Calendar.getInstance()): CalendarDate = CalendarDate(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
        )
    }
}

/** Tipo de compromiso. Define cómo se muestra y si cuenta como "gasto fijo". */
enum class ExpenseType(val label: String, val description: String) {
    CREDIT_CARD("Tarjeta de crédito", "Con cupo y cuotas mensuales"),
    LOAN("Crédito / Préstamo", "Cuotas fijas por un tiempo"),
    FIXED("Gasto fijo", "Arriendo, apoyo en casa, servicios…");

    companion object {
        fun fromName(name: String?): ExpenseType =
            entries.firstOrNull { it.name == name } ?: CREDIT_CARD
    }
}

/** Una tarjeta, crédito o gasto fijo con sus montos por mes. */
data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "💳",
    val type: ExpenseType = ExpenseType.CREDIT_CARD,
    /** Cupo total de la tarjeta (solo tarjetas de crédito). */
    val creditLimit: Double = 0.0,
    /** Cupo utilizado informado por el banco (0 = calcular desde las cuotas). */
    val usedQuota: Double = 0.0,
    /** Monto a pagar por mes. */
    val amounts: Map<YearMonth, Double> = emptyMap(),
    /** Meses marcados como pagados. */
    val paidMonths: Set<YearMonth> = emptySet(),
    /** Día del mes en que vence el pago (1–31), o null si no se definió. */
    val dueDay: Int? = null,
) {
    /** Fecha de vencimiento en [month] (si el mes es más corto, el último día del mes). */
    fun dueDate(month: YearMonth): CalendarDate? =
        dueDay?.let { CalendarDate(month.year, month.month, it.coerceIn(1, month.lengthOfMonth)) }

    val isCreditCard: Boolean get() = type == ExpenseType.CREDIT_CARD

    fun amountFor(month: YearMonth): Double = amounts[month] ?: 0.0

    fun isPaid(month: YearMonth): Boolean = month in paidMonths

    /** Suma de las cuotas que aún no están marcadas como pagadas. */
    val pendingInstallments: Double
        get() = amounts.filterKeys { it !in paidMonths }.values.sum()

    /**
     * Deuda vigente: para tarjetas con cupo usado informado se usa ese valor (lo que dice el banco);
     * en otro caso, la suma de cuotas pendientes.
     */
    val currentDebt: Double
        get() = if (isCreditCard && usedQuota > 0.0) usedQuota else pendingInstallments

    /**
     * Cifra principal para mostrar: los gastos fijos no son deuda, se muestra su pago mensual;
     * tarjetas y créditos muestran la deuda vigente.
     */
    fun headline(today: YearMonth): Pair<String, Double> =
        if (type == ExpenseType.FIXED) "Pago mensual" to (amountFor(today).takeIf { it > 0 } ?: amounts.values.lastOrNull() ?: 0.0)
        else "Deuda" to currentDebt

    val availableCredit: Double
        get() = (creditLimit - currentDebt).coerceAtLeast(0.0)

    /** Porcentaje de uso del cupo entre 0 y 1 (0 si no tiene cupo). */
    val usageRatio: Float
        get() = if (isCreditCard && creditLimit > 0.0) (currentDebt / creditLimit).coerceIn(0.0, 1.0).toFloat() else 0f
}

data class IncomeSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val monthlyAmount: Double,
)

/** Todo lo que el usuario guarda. Se persiste local y en la nube como un solo documento. */
data class FinanceData(
    val items: List<ExpenseItem> = emptyList(),
    val incomes: List<IncomeSource> = emptyList(),
    /** Marca de tiempo de la última modificación (ms). Sirve para resolver conflictos con la nube. */
    val updatedAt: Long = 0L,
) {
    val isEmpty: Boolean get() = items.isEmpty() && incomes.isEmpty()
}

package com.example.controldegastos.domain

import com.example.controldegastos.data.CalendarDate
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.ExpenseType
import com.example.controldegastos.data.FinanceData
import com.example.controldegastos.data.YearMonth

/** Resumen de un mes: cuánto entra, cuánto sale y qué falta pagar. */
data class MonthSummary(
    val month: YearMonth,
    val income: Double,
    val expenses: Double,
    val paid: Double,
    val pending: Double,
    val paymentsCount: Int,
    val paidCount: Int,
) {
    /** Lo que sobra (o falta, si es negativo) después de pagar todo. */
    val balance: Double get() = income - expenses
    val isDeficit: Boolean get() = balance < 0
    /** Proporción de los ingresos comprometida en gastos (puede superar 1). */
    val spendRatio: Float get() = if (income > 0) (expenses / income).toFloat() else if (expenses > 0) 1f else 0f
}

/** Un pago con fecha de vencimiento que aún no se marca como pagado. */
data class DuePayment(val item: ExpenseItem, val date: CalendarDate, val amount: Double, val daysLeft: Int) {
    val isOverdue: Boolean get() = daysLeft < 0
}

data class ItemShare(val item: ExpenseItem, val amount: Double, val fraction: Float)

/** Cálculos puros sobre los datos financieros (sin Android, fáciles de probar). */
object FinanceCalculator {

    fun totalIncome(data: FinanceData): Double = data.incomes.sumOf { it.monthlyAmount }

    fun countsFor(item: ExpenseItem, includeFixed: Boolean): Boolean =
        includeFixed || item.type != ExpenseType.FIXED

    fun monthSummary(data: FinanceData, month: YearMonth, includeFixed: Boolean): MonthSummary {
        val relevant = data.items.filter { countsFor(it, includeFixed) && it.amountFor(month) > 0.0 }
        val total = relevant.sumOf { it.amountFor(month) }
        val paidItems = relevant.filter { it.isPaid(month) }
        val paid = paidItems.sumOf { it.amountFor(month) }
        return MonthSummary(
            month = month,
            income = totalIncome(data),
            expenses = total,
            paid = paid,
            pending = total - paid,
            paymentsCount = relevant.size,
            paidCount = paidItems.size,
        )
    }

    /** Participación de cada ítem en los gastos del mes, de mayor a menor. */
    fun breakdown(data: FinanceData, month: YearMonth, includeFixed: Boolean): List<ItemShare> {
        val rows = data.items
            .filter { countsFor(it, includeFixed) }
            .map { it to it.amountFor(month) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
        val total = rows.sumOf { it.second }
        if (total <= 0.0) return emptyList()
        return rows.map { (item, amount) -> ItemShare(item, amount, (amount / total).toFloat()) }
    }

    /**
     * Meses a mostrar: desde el primer mes con datos (o el actual) hasta el último con datos,
     * asegurando al menos [minFutureMonths] meses hacia adelante desde hoy.
     */
    fun timeline(items: List<ExpenseItem>, today: YearMonth, minFutureMonths: Int = 11): List<YearMonth> {
        val allMonths = items.flatMap { it.amounts.keys }
        val start = minOf(allMonths.minOrNull() ?: today, today)
        val end = maxOf(allMonths.maxOrNull() ?: today, today + minFutureMonths)
        return (0..end.monthsFrom(start)).map { start + it }
    }

    /** Meses desde hoy en adelante que terminan en déficit. */
    fun upcomingDeficits(data: FinanceData, today: YearMonth, includeFixed: Boolean, horizon: Int = 12): List<MonthSummary> =
        (0 until horizon).map { monthSummary(data, today + it, includeFixed) }.filter { it.isDeficit }

    /** Deuda total de tarjetas y créditos (los gastos fijos no son deuda). */
    fun totalDebt(items: List<ExpenseItem>): Double =
        items.filter { it.type != ExpenseType.FIXED }.sumOf { it.currentDebt }

    fun totalAvailableCredit(items: List<ExpenseItem>): Double =
        items.filter { it.isCreditCard && it.creditLimit > 0 }.sumOf { it.availableCredit }

    /**
     * Pagos no pagados que vencen entre hace [overdueDays] días y dentro de [withinDays] días,
     * ordenados por fecha. Revisa el mes anterior, el actual y el siguiente.
     */
    fun duePayments(
        data: FinanceData,
        today: CalendarDate,
        withinDays: Int,
        overdueDays: Int = 0,
    ): List<DuePayment> {
        val months = listOf(today.yearMonth - 1, today.yearMonth, today.yearMonth + 1)
        return data.items.flatMap { item ->
            months.mapNotNull { m ->
                val amount = item.amountFor(m)
                val date = item.dueDate(m) ?: return@mapNotNull null
                if (amount <= 0.0 || item.isPaid(m)) return@mapNotNull null
                val days = today.daysUntil(date)
                if (days in -overdueDays..withinDays) DuePayment(item, date, amount, days) else null
            }
        }.sortedBy { it.date }
    }

    /** Agrega [count] cuotas de [amount] desde [start], sumándolas a lo que ya exista en cada mes. */
    fun addInstallments(item: ExpenseItem, amount: Double, count: Int, start: YearMonth): ExpenseItem {
        if (amount <= 0.0 || count <= 0) return item
        val updated = item.amounts.toMutableMap()
        repeat(count) { i ->
            val m = start + i
            updated[m] = (updated[m] ?: 0.0) + amount
        }
        return item.copy(amounts = updated)
    }

    fun setAmount(item: ExpenseItem, month: YearMonth, amount: Double): ExpenseItem {
        val updated = item.amounts.toMutableMap()
        if (amount <= 0.0) updated.remove(month) else updated[month] = amount
        val paid = if (amount <= 0.0) item.paidMonths - month else item.paidMonths
        return item.copy(amounts = updated, paidMonths = paid)
    }

    fun togglePaid(item: ExpenseItem, month: YearMonth): ExpenseItem =
        item.copy(paidMonths = if (month in item.paidMonths) item.paidMonths - month else item.paidMonths + month)
}

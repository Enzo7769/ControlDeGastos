package com.example.controldegastos

import com.example.controldegastos.data.CalendarDate
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.FinanceCodec
import com.example.controldegastos.data.FinanceData
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuePaymentsTest {
    private val nov = YearMonth(2026, 11)
    private val dic = YearMonth(2026, 12)

    @Test fun `dias entre fechas y meses cortos`() {
        assertEquals(0L, CalendarDate(1970, 1, 1).epochDay)
        assertEquals(1, CalendarDate(2026, 12, 31).daysUntil(CalendarDate(2027, 1, 1)))
        assertEquals(29, YearMonth(2028, 2).lengthOfMonth)
        assertEquals(28, YearMonth(2027, 2).lengthOfMonth)
        val item = ExpenseItem(name = "Visa", dueDay = 31)
        assertEquals(CalendarDate(2027, 2, 28), item.dueDate(YearMonth(2027, 2)))
        assertNull(ExpenseItem(name = "Sin día").dueDate(nov))
    }

    @Test fun `vencimientos proximos excluyen pagados y fuera de rango`() {
        val visa = ExpenseItem(id = "v", name = "Visa", dueDay = 9, amounts = mapOf(nov to 100.0, dic to 50.0))
        val luz = ExpenseItem(id = "l", name = "Luz", dueDay = 20, amounts = mapOf(nov to 30.0))
        val pagada = ExpenseItem(id = "p", name = "Pagada", dueDay = 8, amounts = mapOf(nov to 10.0), paidMonths = setOf(nov))
        val data = FinanceData(items = listOf(visa, luz, pagada))
        val today = CalendarDate(2026, 11, 8)

        val soon = FinanceCalculator.duePayments(data, today, withinDays = 1)
        assertEquals(listOf("v"), soon.map { it.item.id })
        assertEquals(1, soon.single().daysLeft)

        val week = FinanceCalculator.duePayments(data, CalendarDate(2026, 11, 15), withinDays = 7, overdueDays = 7)
        assertEquals(listOf("v", "l"), week.map { it.item.id })
        assertTrue(week.first().isOverdue)
    }

    @Test fun `vencimiento cruza al mes siguiente`() {
        val visa = ExpenseItem(id = "v", name = "Visa", dueDay = 2, amounts = mapOf(dic to 50.0))
        val due = FinanceCalculator.duePayments(FinanceData(items = listOf(visa)), CalendarDate(2026, 11, 30), withinDays = 3)
        assertEquals(CalendarDate(2026, 12, 2), due.single().date)
    }

    @Test fun `dia de pago se guarda en json`() {
        val data = FinanceData(items = listOf(ExpenseItem(id = "a", name = "A", dueDay = 15)), updatedAt = 1)
        assertEquals(15, FinanceCodec.decode(FinanceCodec.encode(data))!!.items.single().dueDay)
    }
}

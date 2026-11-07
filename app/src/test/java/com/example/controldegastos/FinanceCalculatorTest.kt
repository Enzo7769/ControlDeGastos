package com.example.controldegastos

import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.ExpenseType
import com.example.controldegastos.data.FinanceData
import com.example.controldegastos.data.IncomeSource
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorTest {
    private val nov = YearMonth(2026, 11)
    private val dic = YearMonth(2026, 12)

    private val card = ExpenseItem(
        id = "card", name = "Visa", type = ExpenseType.CREDIT_CARD, creditLimit = 1_000_000.0,
        amounts = mapOf(nov to 200_000.0, dic to 150_000.0), paidMonths = setOf(nov),
    )
    private val rent = ExpenseItem(id = "rent", name = "Arriendo", type = ExpenseType.FIXED, amounts = mapOf(nov to 400_000.0, dic to 400_000.0))
    private val data = FinanceData(items = listOf(card, rent), incomes = listOf(IncomeSource("i", "Sueldo", 700_000.0)))

    @Test fun `resumen del mes con y sin gastos fijos`() {
        val withFixed = FinanceCalculator.monthSummary(data, nov, includeFixed = true)
        assertEquals(600_000.0, withFixed.expenses, 0.0)
        assertEquals(100_000.0, withFixed.balance, 0.0)
        assertEquals(200_000.0, withFixed.paid, 0.0)
        assertEquals(400_000.0, withFixed.pending, 0.0)
        assertEquals(2, withFixed.paymentsCount)
        assertEquals(1, withFixed.paidCount)

        val withoutFixed = FinanceCalculator.monthSummary(data, nov, includeFixed = false)
        assertEquals(200_000.0, withoutFixed.expenses, 0.0)
        assertEquals(1, withoutFixed.paymentsCount)
    }

    @Test fun `deficit detectado`() {
        val tight = data.copy(incomes = listOf(IncomeSource("i", "Sueldo", 500_000.0)))
        val deficits = FinanceCalculator.upcomingDeficits(tight, nov, includeFixed = true, horizon = 3)
        assertEquals(listOf(nov, dic), deficits.map { it.month })
        assertTrue(deficits.all { it.isDeficit })
    }

    @Test fun `deuda usa cupo del banco o cuotas pendientes`() {
        assertEquals(150_000.0, card.currentDebt, 0.0) // nov pagado → queda dic
        assertEquals(150_000.0, FinanceCalculator.totalDebt(listOf(card, rent)), 0.0) // el arriendo no es deuda
        assertEquals("Pago mensual" to 400_000.0, rent.headline(nov))
        assertEquals(850_000.0, card.availableCredit, 0.0)
        val withBank = card.copy(usedQuota = 600_000.0)
        assertEquals(600_000.0, withBank.currentDebt, 0.0)
        assertEquals(0.6f, withBank.usageRatio, 0.001f)
    }

    @Test fun `agregar cuotas suma sobre montos existentes`() {
        val updated = FinanceCalculator.addInstallments(card, 50_000.0, 3, dic)
        assertEquals(200_000.0, updated.amountFor(dic), 0.0)
        assertEquals(50_000.0, updated.amountFor(YearMonth(2027, 1)), 0.0)
        assertEquals(50_000.0, updated.amountFor(YearMonth(2027, 2)), 0.0)
        assertEquals(0.0, updated.amountFor(YearMonth(2027, 3)), 0.0)
    }

    @Test fun `monto cero borra el mes y su estado pagado`() {
        val cleared = FinanceCalculator.setAmount(card, nov, 0.0)
        assertFalse(nov in cleared.amounts)
        assertFalse(cleared.isPaid(nov))
    }

    @Test fun `alternar pagado`() {
        assertFalse(FinanceCalculator.togglePaid(card, nov).isPaid(nov))
        assertTrue(FinanceCalculator.togglePaid(card, dic).isPaid(dic))
    }

    @Test fun `linea de tiempo cubre datos y al menos un año`() {
        val t = FinanceCalculator.timeline(listOf(card.copy(amounts = mapOf(YearMonth(2026, 9) to 1.0))), nov)
        assertEquals(YearMonth(2026, 9), t.first())
        assertEquals(nov + 11, t.last())
        assertEquals(t.size, t.distinct().size)
    }

    @Test fun `distribucion ordenada y suma 1`() {
        val shares = FinanceCalculator.breakdown(data, nov, includeFixed = true)
        assertEquals(listOf("rent", "card"), shares.map { it.item.id })
        assertEquals(1f, shares.sumOf { it.fraction.toDouble() }.toFloat(), 0.001f)
    }
}

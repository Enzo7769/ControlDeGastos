package com.example.controldegastos

import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.ExpenseType
import com.example.controldegastos.data.FinanceCodec
import com.example.controldegastos.data.FinanceData
import com.example.controldegastos.data.IncomeSource
import com.example.controldegastos.data.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCodecTest {

    @Test fun `codificar y decodificar conserva los datos`() {
        val data = FinanceData(
            items = listOf(
                ExpenseItem(
                    id = "a", name = "Ripley", icon = "💳", type = ExpenseType.CREDIT_CARD,
                    creditLimit = 1_500_000.0, usedQuota = 1_200_000.0,
                    amounts = mapOf(YearMonth(2026, 11) to 220_467.0, YearMonth(2026, 12) to 470_085.0),
                    paidMonths = setOf(YearMonth(2026, 11)),
                ),
                ExpenseItem(id = "b", name = "Arriendo", icon = "🏠", type = ExpenseType.FIXED, amounts = mapOf(YearMonth(2027, 1) to 300_000.0)),
            ),
            incomes = listOf(IncomeSource("i", "Sueldo", 1_050_000.0)),
            updatedAt = 123L,
        )
        assertEquals(data, FinanceCodec.decode(FinanceCodec.encode(data)))
    }

    @Test fun `decode rechaza formato antiguo o inválido`() {
        assertNull(FinanceCodec.decode(null))
        assertNull(FinanceCodec.decode("[]"))
        assertNull(FinanceCodec.decode("{\"items\":[]}"))
    }

    @Test fun `nombres de meses antiguos`() {
        assertEquals(YearMonth(2026, 11), FinanceCodec.parseLegacyMonthName("Nov-26"))
        assertEquals(YearMonth(2026, 12), FinanceCodec.parseLegacyMonthName("dic.-26"))
        assertEquals(YearMonth(2027, 9), FinanceCodec.parseLegacyMonthName("Sept-27"))
        assertEquals(YearMonth(2026, 8), FinanceCodec.parseLegacyMonthName("Agosto 2026"))
        assertNull(FinanceCodec.parseLegacyMonthName("Mes 5"))
        assertNull(FinanceCodec.parseLegacyMonthName("Octubre"))
    }

    @Test fun `migra datos de la version anterior`() {
        val cards = """
            [
              {"id":"c1","name":"Ripley","categoryIcon":"💳","isCreditCard":true,"creditLimit":1500000,"manualUsedQuota":1200000,
               "monthlyAmounts":{"0":220467,"1":470085,"13":1000},"paidMonths":[0]},
              {"id":"c3","name":"match","categoryIcon":"📱","isCreditCard":false,"creditLimit":500000,"manualUsedQuota":0,
               "monthlyAmounts":{"2":100000}},
              {"id":"c6","name":"CASA (Gasto Fijo)","categoryIcon":"🏠","isCreditCard":false,"creditLimit":0,
               "monthlyAmounts":{"2":300000}}
            ]
        """.trimIndent()
        val incomes = """[{"id":"i1","name":"Trabajo 1","monthlyAmount":1050000}]"""
        // Columna 0 = "Nov-26"; la 13 no existe en la lista y se deduce como Dic-27.
        val months = """[{"index":0,"name":"Nov-26"},{"index":1,"name":"Dic-26"},{"index":2,"name":"Ene-27"},{"index":12,"name":"Abr-27"}]"""

        val data = FinanceCodec.migrateLegacy(cards, incomes, months, fallbackStart = YearMonth(2020, 1), updatedAt = 5L)
        assertNotNull(data)
        data!!
        assertEquals(5L, data.updatedAt)
        assertEquals(1, data.incomes.size)

        val ripley = data.items.first { it.id == "c1" }
        assertEquals(ExpenseType.CREDIT_CARD, ripley.type)
        assertEquals(220_467.0, ripley.amountFor(YearMonth(2026, 11)), 0.0)
        assertEquals(470_085.0, ripley.amountFor(YearMonth(2026, 12)), 0.0)
        assertEquals(1_000.0, ripley.amountFor(YearMonth(2027, 12)), 0.0)
        assertTrue(ripley.isPaid(YearMonth(2026, 11)))
        // Columna con nombre propio ("Abr-27" en índice 12) se respeta aunque no sea consecutiva.
        assertEquals(1_200_000.0, ripley.usedQuota, 0.0)

        val match = data.items.first { it.id == "c3" }
        assertEquals(ExpenseType.LOAN, match.type)
        assertEquals(0.0, match.creditLimit, 0.0)
        assertEquals(100_000.0, match.amountFor(YearMonth(2027, 1)), 0.0)

        val casa = data.items.first { it.id == "c6" }
        assertEquals(ExpenseType.FIXED, casa.type)
    }

    @Test fun `migracion sin nombres reconocibles usa el mes de respaldo`() {
        val cards = """[{"id":"x","name":"Tarjeta","isCreditCard":true,"creditLimit":0,"monthlyAmounts":{"0":10,"3":20}}]"""
        val data = FinanceCodec.migrateLegacy(cards, null, """[{"index":0,"name":"Primero"}]""", YearMonth(2026, 5))!!
        val item = data.items.single()
        assertEquals(10.0, item.amountFor(YearMonth(2026, 5)), 0.0)
        assertEquals(20.0, item.amountFor(YearMonth(2026, 8)), 0.0)
    }

    @Test fun `migracion sin datos devuelve null`() {
        assertNull(FinanceCodec.migrateLegacy(null, null, null, YearMonth(2026, 1)))
    }
}

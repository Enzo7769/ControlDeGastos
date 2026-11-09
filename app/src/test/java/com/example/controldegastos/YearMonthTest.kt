package com.example.controldegastos

import com.example.controldegastos.data.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YearMonthTest {
    @Test fun `suma y resta cruzando años`() {
        assertEquals(YearMonth(2027, 1), YearMonth(2026, 12) + 1)
        assertEquals(YearMonth(2026, 12), YearMonth(2027, 1) - 1)
        assertEquals(YearMonth(2028, 3), YearMonth(2026, 11) + 16)
    }

    @Test fun `diferencia en meses`() {
        assertEquals(14, YearMonth(2028, 1).monthsFrom(YearMonth(2026, 11)))
        assertEquals(-2, YearMonth(2026, 9).monthsFrom(YearMonth(2026, 11)))
    }

    @Test fun `clave ida y vuelta`() {
        val ym = YearMonth(2026, 3)
        assertEquals("2026-03", ym.key())
        assertEquals(ym, YearMonth.parseKey("2026-03"))
        assertNull(YearMonth.parseKey("2026-13"))
        assertNull(YearMonth.parseKey("basura"))
    }
}

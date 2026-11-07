package com.example.controldegastos

import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.util.groupDigits
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.relativeTo
import com.example.controldegastos.util.shortLabel
import com.example.controldegastos.util.toClp
import com.example.controldegastos.util.toClpCompact
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun `pesos chilenos`() {
        assertEquals("$1.050.000", 1_050_000.0.toClp())
        assertEquals("-$25.000", (-25_000.0).toClp())
        assertEquals("$0", 0.0.toClp())
    }

    @Test fun `formato compacto`() {
        assertEquals("$1,3M", 1_250_000.0.toClpCompact())
        assertEquals("$2M", 2_000_000.0.toClpCompact())
        assertEquals("$85K", 85_000.0.toClpCompact())
    }

    @Test fun `separador de miles`() {
        assertEquals("", groupDigits(""))
        assertEquals("999", groupDigits("999"))
        assertEquals("1.000", groupDigits("1000"))
        assertEquals("12.345.678", groupDigits("12345678"))
    }

    @Test fun `etiquetas de mes`() {
        val nov = YearMonth(2026, 11)
        assertEquals("Noviembre 2026", nov.longLabel())
        assertEquals("Nov 26", nov.shortLabel())
        assertEquals("Este mes", nov.relativeTo(nov))
        assertEquals("En 3 meses", (nov + 3).relativeTo(nov))
        assertEquals("Hace 2 meses", (nov - 2).relativeTo(nov))
    }
}

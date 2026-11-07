package com.example.controldegastos.util

import com.example.controldegastos.data.YearMonth
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val chileLocale = Locale("es", "CL")

private val clp: NumberFormat = NumberFormat.getNumberInstance(chileLocale).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
    isGroupingUsed = true
}

/** 1050000.0 → "$1.050.000"  ·  -25000.0 → "-$25.000" */
fun Double.toClp(): String {
    val rounded = Math.round(this)
    val formatted = "$" + clp.format(abs(rounded))
    return if (rounded < 0) "-$formatted" else formatted
}

/** Formato corto para gráficos: 1250000 → "$1,3M", 85000 → "$85K". */
fun Double.toClpCompact(): String {
    val a = abs(this)
    val sign = if (this < 0) "-" else ""
    return when {
        a >= 1_000_000 -> sign + "$" + "%.1f".format(chileLocale, a / 1_000_000).removeSuffix(",0") + "M"
        a >= 1_000 -> sign + "$" + Math.round(a / 1_000) + "K"
        else -> toClp()
    }
}

/** Separa miles con puntos: "1050000" → "1.050.000". */
fun groupDigits(digits: String): String {
    if (digits.isEmpty()) return ""
    val sb = StringBuilder()
    digits.reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return sb.reverse().toString()
}

private val monthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

/** "Noviembre 2026" */
fun YearMonth.longLabel(): String = "${monthNames[month - 1]} $year"

/** "Nov 26" */
fun YearMonth.shortLabel(): String = "${monthNames[month - 1].take(3)} ${(year % 100).toString().padStart(2, '0')}"

/** "Noviembre" */
fun YearMonth.monthName(): String = monthNames[month - 1]

/** Texto relativo al mes actual: "Este mes", "Próximo mes", "Hace 2 meses", "En 3 meses". */
fun YearMonth.relativeTo(today: YearMonth): String = when (val d = monthsFrom(today)) {
    0 -> "Este mes"
    1 -> "Próximo mes"
    -1 -> "Mes pasado"
    in 2..Int.MAX_VALUE -> "En $d meses"
    else -> "Hace ${-d} meses"
}

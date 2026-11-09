package com.example.controldegastos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.controldegastos.data.AccentColor

/** Tonos de cada color de acento para modo claro y oscuro. */
internal data class AccentTones(
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
)

internal fun AccentColor.tones(): AccentTones = when (this) {
    AccentColor.DYNAMIC, AccentColor.BLUE -> AccentTones(
        Color(0xFF1565C0), Color(0xFFD6E3FF), Color(0xFF001B3E),
        Color(0xFFA8C8FF), Color(0xFF003062), Color(0xFF1A4A8A), Color(0xFFD6E3FF),
    )
    AccentColor.EMERALD -> AccentTones(
        Color(0xFF2E7D32), Color(0xFFB7F1B4), Color(0xFF002106),
        Color(0xFF8FD98B), Color(0xFF00390A), Color(0xFF1B5E20), Color(0xFFB7F1B4),
    )
    AccentColor.TEAL -> AccentTones(
        Color(0xFF00737D), Color(0xFF9EF0FA), Color(0xFF001F23),
        Color(0xFF80D4DE), Color(0xFF00363C), Color(0xFF004F57), Color(0xFF9EF0FA),
    )
    AccentColor.VIOLET -> AccentTones(
        Color(0xFF6A1B9A), Color(0xFFF3DAFF), Color(0xFF2C0049),
        Color(0xFFE2B6FF), Color(0xFF4A0072), Color(0xFF6B2E8F), Color(0xFFF3DAFF),
    )
    AccentColor.ORANGE -> AccentTones(
        Color(0xFFB54700), Color(0xFFFFDBCA), Color(0xFF341100),
        Color(0xFFFFB68F), Color(0xFF552000), Color(0xFF7A3000), Color(0xFFFFDBCA),
    )
    AccentColor.RUBY -> AccentTones(
        Color(0xFFB3261E), Color(0xFFFFDAD5), Color(0xFF410001),
        Color(0xFFFFB4A9), Color(0xFF690003), Color(0xFF8C1D18), Color(0xFFFFDAD5),
    )
}

/** Colores con significado financiero (positivo / negativo / advertencia). */
@Immutable
data class FinanceColors(
    val positive: Color,
    val positiveContainer: Color,
    val onPositiveContainer: Color,
    val negative: Color,
    val negativeContainer: Color,
    val onNegativeContainer: Color,
    val warning: Color,
    /** Paleta para gráficos (distinguible en claro y oscuro). */
    val chart: List<Color>,
)

internal val LightFinanceColors = FinanceColors(
    positive = Color(0xFF1B7F3B),
    positiveContainer = Color(0xFFD3F5DC),
    onPositiveContainer = Color(0xFF00210C),
    negative = Color(0xFFC62828),
    negativeContainer = Color(0xFFFFDAD6),
    onNegativeContainer = Color(0xFF410002),
    warning = Color(0xFFE07A00),
    chart = listOf(
        Color(0xFF2F6FDE), Color(0xFFE8833A), Color(0xFF2EA37A), Color(0xFFD64E6F),
        Color(0xFF8A5CD6), Color(0xFF1FA4C4), Color(0xFFC9A227), Color(0xFF6B7A8F),
    ),
)

internal val DarkFinanceColors = FinanceColors(
    positive = Color(0xFF7EDB9A),
    positiveContainer = Color(0xFF0F5228),
    onPositiveContainer = Color(0xFFD3F5DC),
    negative = Color(0xFFFFB4AB),
    negativeContainer = Color(0xFF93000A),
    onNegativeContainer = Color(0xFFFFDAD6),
    warning = Color(0xFFFFB960),
    chart = listOf(
        Color(0xFF7FA8F5), Color(0xFFF2A46C), Color(0xFF5FCCA4), Color(0xFFF08AA3),
        Color(0xFFB596F0), Color(0xFF63CBE3), Color(0xFFE3C55E), Color(0xFF9AA8BA),
    ),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

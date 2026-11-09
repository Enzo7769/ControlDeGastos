package com.example.controldegastos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.controldegastos.data.AccentColor
import com.example.controldegastos.data.ThemeMode

val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun lightScheme(accent: AccentColor): ColorScheme {
    val t = accent.tones()
    return lightColorScheme(
        primary = t.lightPrimary,
        onPrimary = Color.White,
        primaryContainer = t.lightPrimaryContainer,
        onPrimaryContainer = t.lightOnPrimaryContainer,
        secondary = Color(0xFF535F70),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE3F0),
        onSecondaryContainer = Color(0xFF101C2B),
        tertiary = Color(0xFF1B7F3B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD3F5DC),
        onTertiaryContainer = Color(0xFF00210C),
        background = Color(0xFFF8F9FC),
        onBackground = Color(0xFF191C20),
        surface = Color(0xFFF8F9FC),
        onSurface = Color(0xFF191C20),
        surfaceVariant = Color(0xFFE0E2EA),
        onSurfaceVariant = Color(0xFF44474F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF2F3F7),
        surfaceContainer = Color(0xFFECEEF2),
        surfaceContainerHigh = Color(0xFFE6E8EC),
        surfaceContainerHighest = Color(0xFFE0E2E6),
        outline = Color(0xFF747780),
        outlineVariant = Color(0xFFC4C6D0),
    )
}

private fun darkScheme(accent: AccentColor): ColorScheme {
    val t = accent.tones()
    return darkColorScheme(
        primary = t.darkPrimary,
        onPrimary = t.darkOnPrimary,
        primaryContainer = t.darkPrimaryContainer,
        onPrimaryContainer = t.darkOnPrimaryContainer,
        secondary = Color(0xFFBCC7DB),
        onSecondary = Color(0xFF263141),
        secondaryContainer = Color(0xFF3C4758),
        onSecondaryContainer = Color(0xFFD8E3F8),
        tertiary = Color(0xFF7EDB9A),
        onTertiary = Color(0xFF003919),
        tertiaryContainer = Color(0xFF0F5228),
        onTertiaryContainer = Color(0xFFD3F5DC),
        background = Color(0xFF111318),
        onBackground = Color(0xFFE2E2E9),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFE2E2E9),
        surfaceVariant = Color(0xFF44474F),
        onSurfaceVariant = Color(0xFFC4C6D0),
        surfaceContainerLowest = Color(0xFF0C0E13),
        surfaceContainerLow = Color(0xFF191C20),
        surfaceContainer = Color(0xFF1D2024),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474F),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ControlDeGastosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        accent == AccentColor.DYNAMIC && supportsDynamicColor ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent)
        else -> lightScheme(accent)
    }
    CompositionLocalProvider(LocalFinanceColors provides if (dark) DarkFinanceColors else LightFinanceColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Acceso rápido: `FinanceTheme.colors.positive`. */
object FinanceTheme {
    val colors: FinanceColors
        @Composable @ReadOnlyComposable get() = LocalFinanceColors.current
}

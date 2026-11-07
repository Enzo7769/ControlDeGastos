package com.example.controldegastos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ControlDeGastosTheme(
    themePalette: String = "Azul Clásico",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePalette) {
        "Esmeralda Finanzas" -> lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF003300),
            secondaryContainer = Color(0xFFA5D6A7),
            onSecondaryContainer = Color(0xFF002200),
            tertiaryContainer = Color(0xFFE8F5E9),
            surfaceVariant = Color(0xFFF1F8E9)
        )
        "Elegante Oscuro" -> darkColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF1565C0),
            onPrimaryContainer = Color.White,
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2C),
            onSurfaceVariant = Color(0xFFE0E0E0),
            background = Color(0xFF121212),
            onBackground = Color.White,
            secondaryContainer = Color(0xFF263238),
            onSecondaryContainer = Color.White,
            tertiaryContainer = Color(0xFF37474F)
        )
        "Rubí Intenso" -> lightColorScheme(
            primary = Color(0xFFC62828),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFCDD2),
            onPrimaryContainer = Color(0xFF400000),
            secondaryContainer = Color(0xFFEF9A9A),
            onSecondaryContainer = Color(0xFF300000),
            tertiaryContainer = Color(0xFFFFEBEE),
            surfaceVariant = Color(0xFFFFEBEE)
        )
        "Atardecer Naranja" -> lightColorScheme(
            primary = Color(0xFFE65100),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE0B2),
            onPrimaryContainer = Color(0xFF3E1000),
            secondaryContainer = Color(0xFFFFCC80),
            onSecondaryContainer = Color(0xFF3B1200),
            tertiaryContainer = Color(0xFFFFF3E0),
            surfaceVariant = Color(0xFFFFF3E0)
        )
        "Océano Turquesa" -> lightColorScheme(
            primary = Color(0xFF00838F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB2EBF2),
            onPrimaryContainer = Color(0xFF002025),
            secondaryContainer = Color(0xFF80DEEA),
            onSecondaryContainer = Color(0xFF001F24),
            tertiaryContainer = Color(0xFFE0F7FA),
            surfaceVariant = Color(0xFFE0F7FA)
        )
        "Violeta Neón" -> lightColorScheme(
            primary = Color(0xFF6A1B9A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE1BEE7),
            onPrimaryContainer = Color(0xFF28003A),
            secondaryContainer = Color(0xFFCE93D8),
            onSecondaryContainer = Color(0xFF210030),
            tertiaryContainer = Color(0xFFF3E5F5),
            surfaceVariant = Color(0xFFF3E5F5)
        )
        "Moka Elegante" -> lightColorScheme(
            primary = Color(0xFF4E342E),
            onPrimary = Color.White,
            primaryContainer = Color(0xD7D0C7B9),
            onPrimaryContainer = Color(0xFF1F0D09),
            secondaryContainer = Color(0xFFBCAAA4),
            onSecondaryContainer = Color(0xFF1F0D0B),
            tertiaryContainer = Color(0xFFEFEBE9),
            surfaceVariant = Color(0xFFEFEBE9)
        )
        "Midnight Noche" -> darkColorScheme(
            primary = Color(0xFF80CBC4),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF004D40),
            onPrimaryContainer = Color.White,
            surface = Color(0xFF0D1117),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF161B22),
            onSurfaceVariant = Color(0xFFC9D1D9),
            background = Color(0xFF010409),
            onBackground = Color.White,
            secondaryContainer = Color(0xFF1F2937),
            onSecondaryContainer = Color.White
        )
        "Dorado & Lujo" -> lightColorScheme(
            primary = Color(0xFF8D6E63),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFECB3),
            onPrimaryContainer = Color(0xFF3E2723),
            secondaryContainer = Color(0xFFFFD54F),
            onSecondaryContainer = Color(0xFF2C1C00),
            tertiaryContainer = Color(0xFFFFF8E1),
            surfaceVariant = Color(0xFFFFF8E1)
        )
        "Monocromático Slate" -> lightColorScheme(
            primary = Color(0xFF37474F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCFD8DC),
            onPrimaryContainer = Color(0xFF102027),
            secondaryContainer = Color(0xFF90A4AE),
            onSecondaryContainer = Color(0xFF0A1318),
            tertiaryContainer = Color(0xFFECEFF1),
            surfaceVariant = Color(0xFFECEFF1)
        )
        "Menta Fresca" -> lightColorScheme(
            primary = Color(0xFF00695C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB2DFDB),
            onPrimaryContainer = Color(0xFF00251A),
            secondaryContainer = Color(0xFF80CBC4),
            onSecondaryContainer = Color(0xFF001D14),
            tertiaryContainer = Color(0xFFE0F2F1),
            surfaceVariant = Color(0xFFE0F2F1)
        )
        else -> lightColorScheme( // Azul Clásico / Estándar
            primary = Color(0xFF1976D2),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFBBDEFB),
            onPrimaryContainer = Color(0xFF002171),
            secondaryContainer = Color(0xFF90CAF9),
            onSecondaryContainer = Color(0xFF001540),
            tertiaryContainer = Color(0xFFE3F2FD),
            surfaceVariant = Color(0xFFE3F2FD)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

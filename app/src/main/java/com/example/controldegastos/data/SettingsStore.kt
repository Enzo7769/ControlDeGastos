package com.example.controldegastos.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Oscuro") }

enum class AccentColor(val label: String, val seed: Long) {
    DYNAMIC("Del sistema", 0xFF1565C0),
    BLUE("Azul", 0xFF1565C0),
    EMERALD("Esmeralda", 0xFF2E7D32),
    TEAL("Turquesa", 0xFF00838F),
    VIOLET("Violeta", 0xFF6A1B9A),
    ORANGE("Naranja", 0xFFE65100),
    RUBY("Rubí", 0xFFC62828),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.BLUE,
    val biometricLock: Boolean = false,
    val includeFixedExpenses: Boolean = true,
    /** Avisar un día antes y el día del vencimiento de cada pago. */
    val paymentReminders: Boolean = true,
)

/** Preferencias del dispositivo (no dependen del usuario). */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load(context))
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit {
            putString("theme_mode", next.themeMode.name)
            putString("accent", next.accent.name)
            putBoolean("biometric_lock", next.biometricLock)
            putBoolean("include_fixed", next.includeFixedExpenses)
            putBoolean("payment_reminders", next.paymentReminders)
        }
    }

    private fun load(context: Context): AppSettings {
        if (prefs.contains("theme_mode")) {
            return AppSettings(
                themeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", null) } ?: ThemeMode.SYSTEM,
                accent = AccentColor.entries.firstOrNull { it.name == prefs.getString("accent", null) } ?: AccentColor.BLUE,
                biometricLock = prefs.getBoolean("biometric_lock", false),
                includeFixedExpenses = prefs.getBoolean("include_fixed", true),
                paymentReminders = prefs.getBoolean("payment_reminders", true),
            )
        }
        // Migrar preferencias de la versión anterior.
        val legacy = context.getSharedPreferences(FinanceRepository.LEGACY_PREFS, Context.MODE_PRIVATE)
        val (mode, accent) = legacyTheme(legacy.getString("theme_palette", null))
        return AppSettings(
            themeMode = mode,
            accent = accent,
            biometricLock = legacy.getBoolean("use_biometrics", false),
            includeFixedExpenses = legacy.getBoolean("include_fixed", true),
        )
    }

    private fun legacyTheme(name: String?): Pair<ThemeMode, AccentColor> = when (name) {
        "Esmeralda Finanzas", "Menta Fresca" -> ThemeMode.SYSTEM to AccentColor.EMERALD
        "Elegante Oscuro" -> ThemeMode.DARK to AccentColor.BLUE
        "Midnight Noche" -> ThemeMode.DARK to AccentColor.TEAL
        "Rubí Intenso" -> ThemeMode.SYSTEM to AccentColor.RUBY
        "Atardecer Naranja", "Dorado & Lujo", "Moka Elegante" -> ThemeMode.SYSTEM to AccentColor.ORANGE
        "Océano Turquesa" -> ThemeMode.SYSTEM to AccentColor.TEAL
        "Violeta Neón" -> ThemeMode.SYSTEM to AccentColor.VIOLET
        else -> ThemeMode.SYSTEM to AccentColor.BLUE
    }
}

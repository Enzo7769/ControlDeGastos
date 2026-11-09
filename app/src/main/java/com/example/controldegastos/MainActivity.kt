package com.example.controldegastos

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import com.example.controldegastos.data.ThemeMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.reminders.PaymentReminders
import com.example.controldegastos.ui.AppRoot
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.screens.LockScreen
import com.example.controldegastos.ui.theme.ControlDeGastosTheme
import com.example.controldegastos.util.BiometricLock

class MainActivity : AppCompatActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    /** La app está bloqueada esperando huella/PIN. */
    private var locked by mutableStateOf(false)
    private var lockError by mutableStateOf<String?>(null)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        applyNightMode(viewModel.settings.value.themeMode)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Tras rotar la pantalla se conserva el estado; si el sistema cerró la app en segundo plano
        // (o pasó más de un minuto) se vuelve a bloquear.
        val savedAt = savedInstanceState?.getLong(KEY_SAVED_AT) ?: 0L
        val wasLocked = savedInstanceState?.getBoolean(KEY_LOCKED) ?: true
        val longAgo = System.currentTimeMillis() - savedAt > RELOCK_AFTER_MS
        locked = shouldLock() && (wasLocked || longAgo)
        if (savedInstanceState == null) handleIntent(intent)
        PaymentReminders.sync(this, viewModel.settings.value.paymentReminders)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val user by viewModel.user.collectAsStateWithLifecycle()
            LaunchedEffect(settings.themeMode) { applyNightMode(settings.themeMode) }

            ControlDeGastosTheme(themeMode = settings.themeMode, accent = settings.accent) {
                if (locked && user != null && settings.biometricLock) {
                    LockScreen(
                        errorMessage = lockError,
                        onUnlock = ::unlock,
                        onSignOut = {
                            viewModel.signOut()
                            locked = false
                        },
                    )
                } else {
                    AppRoot(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Al tocar un recordatorio se abre directamente el detalle de ese pago. */
    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(PaymentReminders.EXTRA_OPEN_ITEM)?.let { viewModel.pendingOpenItem.value = it }
    }

    override fun onStart() {
        super.onStart()
        // Volver a bloquear si la app estuvo en segundo plano más de un minuto.
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS && shouldLock()) {
            locked = true
        }
        if (locked) unlock()
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LOCKED, locked)
        outState.putLong(KEY_SAVED_AT, System.currentTimeMillis())
    }

    /** Hace que recursos, barras del sistema y fondo de ventana sigan el tema elegido en la app. */
    private fun applyNightMode(mode: ThemeMode) {
        val target = when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        if (AppCompatDelegate.getDefaultNightMode() != target) AppCompatDelegate.setDefaultNightMode(target)
    }

    private fun shouldLock(): Boolean =
        viewModel.settings.value.biometricLock && viewModel.user.value != null

    private fun unlock() {
        if (!BiometricLock.isAvailable(this)) {
            // El teléfono ya no tiene huella/PIN configurado: no hay forma de verificar, se desactiva.
            viewModel.updateSettings { it.copy(biometricLock = false) }
            locked = false
            return
        }
        lockError = null
        BiometricLock.authenticate(
            activity = this,
            onSuccess = { locked = false },
            onError = { lockError = it },
        )
    }

    private companion object {
        const val KEY_LOCKED = "locked"
        const val KEY_SAVED_AT = "saved_at"
        const val RELOCK_AFTER_MS = 60_000L
    }
}

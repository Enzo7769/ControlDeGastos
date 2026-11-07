package com.example.controldegastos

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        locked = savedInstanceState?.getBoolean(KEY_LOCKED) ?: shouldLock()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val user by viewModel.user.collectAsStateWithLifecycle()

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
        const val RELOCK_AFTER_MS = 60_000L
    }
}

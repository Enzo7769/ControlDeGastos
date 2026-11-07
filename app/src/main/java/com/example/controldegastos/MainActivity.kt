package com.example.controldegastos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.controldegastos.ui.ExpenseViewModel
import com.example.controldegastos.ui.LoginScreen
import com.example.controldegastos.ui.MainExpenseApp
import com.example.controldegastos.ui.SplashScreen
import com.example.controldegastos.ui.theme.ControlDeGastosTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val expenseViewModel: ExpenseViewModel = viewModel()
            val currentTheme by expenseViewModel.selectedTheme.collectAsState()
            val useBiometrics by expenseViewModel.useBiometrics.collectAsState()

            var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
            var showSplash by remember { mutableStateOf(true) }
            var isUnlocked by remember { mutableStateOf(!useBiometrics) }

            // Trigger biometric prompt if enabled and logged in
            LaunchedEffect(useBiometrics, currentUser) {
                if (currentUser != null && useBiometrics) {
                    val executor = ContextCompat.getMainExecutor(this@MainActivity)
                    val biometricPrompt = BiometricPrompt(
                        this@MainActivity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isUnlocked = true
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "Autenticación: $errString", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Desbloquear Control de Gastos")
                        .setSubtitle("Confirma tu huella dactilar para acceder a tus finanzas")
                        .setNegativeButtonText("Cancelar")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                } else {
                    isUnlocked = true
                }
            }

            ControlDeGastosTheme(themePalette = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (currentUser == null) {
                        LoginScreen {
                            currentUser = FirebaseAuth.getInstance().currentUser
                        }
                    } else if (showSplash) {
                        SplashScreen {
                            showSplash = false
                        }
                    } else if (isUnlocked) {
                        MainExpenseApp(expenseViewModel) {
                            currentUser = null // Triggered on Logout
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {}
                    }
                }
            }
        }
    }
}

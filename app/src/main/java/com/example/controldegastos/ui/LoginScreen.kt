package com.example.controldegastos.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                isLoading = true
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "¡Sesión iniciada con Google!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Error Firebase: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Error Google Sign-In (${e.statusCode}): Configura la huella SHA-1 en Firebase Console.", Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Control de Gastos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRegistering) "Crea una cuenta para guardar tus datos en la nube" else "Inicia sesión para sincronizar tus finanzas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim()

                        if (cleanEmail.isNotBlank() && cleanPassword.isNotBlank()) {
                            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                Toast.makeText(context, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            if (isRegistering) {
                                auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        val msg = when {
                                            e.localizedMessage?.contains("CONFIGURATION_NOT_FOUND") == true ->
                                                "Error: Habilita 'Correo electrónico/contraseña' en Firebase Console > Authentication."
                                            e.localizedMessage?.contains("badly formatted") == true ->
                                                "El formato del correo electrónico no es válido."
                                            else -> "Error: ${e.localizedMessage}"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                auth.signInWithEmailAndPassword(cleanEmail, cleanPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        val msg = when {
                                            e.localizedMessage?.contains("CONFIGURATION_NOT_FOUND") == true ->
                                                "Error: Habilita 'Correo electrónico/contraseña' en Firebase Console > Authentication."
                                            e.localizedMessage?.contains("badly formatted") == true ->
                                                "El formato del correo electrónico no es válido."
                                            else -> "Error: ${e.localizedMessage}"
                                        }
                                        Toast.makeText(context, "Error al iniciar sesión: $msg", Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegistering) "Registrarse" else "Iniciar Sesión")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        try {
                            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                            val webClientId = if (resId != 0) context.getString(resId) else ""
                            if (webClientId.isBlank()) {
                                Toast.makeText(context, "Error: Configura google-services.json correctamente para Google Sign-In", Toast.LENGTH_LONG).show()
                                return@OutlinedButton
                            }
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(webClientId)
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error Google Sign-In: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🌐", style = MaterialTheme.typography.titleMedium)
                        Text("Continuar con Google", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { isRegistering = !isRegistering }
                ) {
                    Text(if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate aquí")
                }
            }
        }
    }
}

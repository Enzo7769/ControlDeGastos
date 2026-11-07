package com.example.controldegastos.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.controldegastos.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider

private enum class AuthMode { LOGIN, REGISTER, RESET }

/** Inicio de sesión. Al autenticarse, el ViewModel detecta el cambio de usuario y muestra la app. */
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }

    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val token = account?.idToken
            if (token == null) {
                loading = false
                return@rememberLauncherForActivityResult
            }
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                .addOnFailureListener { loading = false; error = authErrorMessage(it) }
        } catch (e: ApiException) {
            loading = false
            // 12501 = el usuario canceló la selección de cuenta.
            if (e.statusCode != 12501) error = "No se pudo iniciar sesión con Google (código ${e.statusCode})."
        }
    }

    fun submit() {
        focus.clearFocus()
        error = null
        info = null
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            error = "Ingresa un correo electrónico válido."
            return
        }
        if (mode != AuthMode.RESET && password.length < 6) {
            error = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        loading = true
        when (mode) {
            AuthMode.LOGIN -> auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnFailureListener { loading = false; error = authErrorMessage(it) }
            AuthMode.REGISTER -> auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnFailureListener { loading = false; error = authErrorMessage(it) }
            AuthMode.RESET -> auth.sendPasswordResetEmail(cleanEmail)
                .addOnSuccessListener {
                    loading = false
                    info = "Te enviamos un correo para restablecer tu contraseña."
                    mode = AuthMode.LOGIN
                }
                .addOnFailureListener { loading = false; error = authErrorMessage(it) }
        }
    }

    fun googleSignIn() {
        error = null
        val clientId = webClientId(context)
        if (clientId.isBlank()) {
            error = "Google no está configurado en este proyecto (google-services.json)."
            return
        }
        val client = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(clientId).requestEmail().build(),
        )
        loading = true
        client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_wallet),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Text("Control de Gastos", style = MaterialTheme.typography.headlineMedium)
                Text(
                    when (mode) {
                        AuthMode.LOGIN -> "Tus tarjetas, cuotas e ingresos en un solo lugar."
                        AuthMode.REGISTER -> "Crea tu cuenta para guardar tus datos en la nube."
                        AuthMode.RESET -> "Ingresa tu correo y te enviaremos un enlace para crear una nueva contraseña."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                    singleLine = true,
                    enabled = !loading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (mode == AuthMode.RESET) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (mode != AuthMode.RESET) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    painterResource(if (showPassword) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                    contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !loading,
                        supportingText = if (mode == AuthMode.REGISTER) ({ Text("Mínimo 6 caracteres") }) else null,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
                info?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }

                if (mode == AuthMode.LOGIN) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { mode = AuthMode.RESET; error = null; info = null }, enabled = !loading) {
                            Text("¿Olvidaste tu contraseña?")
                        }
                    }
                }

                Button(
                    onClick = ::submit,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            when (mode) {
                                AuthMode.LOGIN -> "Iniciar sesión"
                                AuthMode.REGISTER -> "Crear cuenta"
                                AuthMode.RESET -> "Enviar enlace"
                            }
                        )
                    }
                }

                if (mode != AuthMode.RESET) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("  o  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = ::googleSignIn,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("G", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Text("Continuar con Google")
                    }
                }

                TextButton(
                    onClick = {
                        error = null
                        info = null
                        mode = when (mode) {
                            AuthMode.LOGIN -> AuthMode.REGISTER
                            else -> AuthMode.LOGIN
                        }
                    },
                    enabled = !loading,
                ) {
                    Text(
                        when (mode) {
                            AuthMode.LOGIN -> "¿No tienes cuenta? Regístrate"
                            AuthMode.REGISTER -> "¿Ya tienes cuenta? Inicia sesión"
                            AuthMode.RESET -> "Volver a iniciar sesión"
                        }
                    )
                }
            }
        }
    }
}

/**
 * El ID de cliente web lo genera el plugin google-services solo si el proyecto de Firebase
 * tiene Google habilitado; por eso se busca por nombre en vez de usar R.string directamente.
 */
@SuppressLint("DiscouragedApi")
private fun webClientId(context: Context): String {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    return if (resId != 0) context.getString(resId) else ""
}

/** Traduce los errores de Firebase a mensajes claros en español. */
private fun authErrorMessage(e: Exception): String = when (e) {
    is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos e inténtalo otra vez."
    is FirebaseAuthException -> when (e.errorCode) {
        "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" -> "Correo o contraseña incorrectos."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo. Inicia sesión."
        "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil. Usa al menos 6 caracteres."
        "ERROR_USER_DISABLED" -> "Esta cuenta fue deshabilitada."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Este método de inicio de sesión no está habilitado en Firebase."
        else -> e.localizedMessage ?: "No se pudo completar la operación."
    }
    else -> if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true) {
        "El inicio de sesión con correo no está habilitado en Firebase Authentication."
    } else {
        e.localizedMessage ?: "No se pudo completar la operación."
    }
}

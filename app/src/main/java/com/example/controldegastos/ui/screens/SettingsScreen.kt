package com.example.controldegastos.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.R
import com.example.controldegastos.data.AccentColor
import com.example.controldegastos.data.FinanceRepository
import com.example.controldegastos.data.ThemeMode
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.LocalAppSnackbar
import com.example.controldegastos.ui.Navigator
import com.example.controldegastos.ui.openUpdate
import com.example.controldegastos.ui.components.ConfirmDialog
import com.example.controldegastos.ui.theme.supportsDynamicColor
import com.example.controldegastos.util.BiometricLock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: FinanceViewModel, navigator: Navigator) {
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ----- Cuenta -----
            SettingsGroup("Cuenta") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.AccountCircle, null) },
                    headlineContent = { Text(user?.displayName?.takeIf { it.isNotBlank() } ?: "Mi cuenta") },
                    supportingContent = {
                        val status = when (sync) {
                            FinanceRepository.SyncState.SYNCED -> "Sincronizado"
                            FinanceRepository.SyncState.SYNCING -> "Sincronizando…"
                            FinanceRepository.SyncState.OFFLINE -> "Sin conexión (se guarda en el teléfono)"
                        }
                        Text(listOfNotNull(user?.email?.takeIf { it.isNotBlank() }, status).joinToString(" · "))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    modifier = Modifier.clickable { confirmSignOut = true },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                    headlineContent = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // ----- Seguridad -----
            SettingsGroup("Seguridad") {
                ListItem(
                    modifier = Modifier.clickable {
                        toggleLock(!settings.biometricLock, context, viewModel) { snackbar.show(it) }
                    },
                    leadingContent = { Icon(Icons.Outlined.Lock, null) },
                    headlineContent = { Text("Bloquear con huella o PIN") },
                    supportingContent = { Text("Pide verificación al abrir la app") },
                    trailingContent = {
                        Switch(
                            checked = settings.biometricLock,
                            onCheckedChange = { toggleLock(it, context, viewModel) { msg -> snackbar.show(msg) } },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // ----- Cálculos -----
            SettingsGroup("Cálculos") {
                ListItem(
                    modifier = Modifier.clickable {
                        viewModel.updateSettings { it.copy(includeFixedExpenses = !it.includeFixedExpenses) }
                    },
                    leadingContent = { Icon(painterResource(R.drawable.ic_trending_up), null) },
                    headlineContent = { Text("Incluir gastos fijos") },
                    supportingContent = { Text("Suma arriendo, apoyo en casa y servicios a los totales mensuales") },
                    trailingContent = {
                        Switch(
                            checked = settings.includeFixedExpenses,
                            onCheckedChange = { v -> viewModel.updateSettings { it.copy(includeFixedExpenses = v) } },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // ----- Apariencia -----
            SettingsGroup("Apariencia") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Tema", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { i, mode ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateSettings { it.copy(themeMode = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            ) { Text(mode.label) }
                        }
                    }
                    Text("Color principal", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentColor.entries
                            .filter { it != AccentColor.DYNAMIC || supportsDynamicColor }
                            .forEach { accent ->
                                AccentSwatch(
                                    accent = accent,
                                    selected = settings.accent == accent,
                                    onClick = { viewModel.updateSettings { it.copy(accent = accent) } },
                                )
                            }
                    }
                }
            }

            // ----- Datos -----
            SettingsGroup("Datos") {
                ListItem(
                    modifier = Modifier.clickable {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.buildShareSummary())
                        }
                        context.startActivity(Intent.createChooser(send, "Compartir resumen"))
                    },
                    leadingContent = { Icon(Icons.Outlined.Share, null) },
                    headlineContent = { Text("Compartir resumen") },
                    supportingContent = { Text("Envía un resumen por WhatsApp, correo, etc.") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // ----- Acerca de -----
            SettingsGroup("Acerca de") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, null) },
                    headlineContent = { Text("Versión") },
                    supportingContent = { Text(viewModel.versionName) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    modifier = Modifier.clickable(enabled = !checkingUpdate) {
                        checkingUpdate = true
                        viewModel.checkForUpdates { info ->
                            checkingUpdate = false
                            if (info == null) {
                                Toast.makeText(context, "Tienes la última versión", Toast.LENGTH_SHORT).show()
                            } else {
                                snackbar.show("Versión ${info.versionName} disponible", "Descargar") { openUpdate(context, info) }
                            }
                        }
                    },
                    leadingContent = { Icon(Icons.Outlined.Refresh, null) },
                    headlineContent = { Text(if (checkingUpdate) "Buscando…" else "Buscar actualizaciones") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "¿Cerrar sesión?",
            message = "Tus datos quedan guardados en tu cuenta y los verás al volver a entrar.",
            confirmLabel = "Cerrar sesión",
            destructive = true,
            onConfirm = viewModel::signOut,
            onDismiss = { confirmSignOut = false },
        )
    }
}

private fun toggleLock(
    enable: Boolean,
    context: android.content.Context,
    viewModel: FinanceViewModel,
    onMessage: (String) -> Unit,
) {
    if (enable && !BiometricLock.isAvailable(context)) {
        onMessage("Configura una huella o bloqueo de pantalla en tu teléfono primero")
        return
    }
    viewModel.updateSettings { it.copy(biometricLock = enable) }
    onMessage(if (enable) "Bloqueo activado" else "Bloqueo desactivado")
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    // "Del sistema" usa los colores del fondo de pantalla (Android 12+): se representa con un arcoíris.
    val brush = if (accent == AccentColor.DYNAMIC) {
        Brush.sweepGradient(listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335), Color(0xFF4285F4)))
    } else SolidColor(Color(accent.seed))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(brush)
                .then(
                    if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                    else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = Color.White)
        }
        Text(accent.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

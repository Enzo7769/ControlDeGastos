package com.example.controldegastos.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val title: String, val iconText: String) {
    object Dashboard : Screen("dashboard", "Resumen", "📊")
    object Cards : Screen("cards", "Tarjetas", "💳")
    object Income : Screen("income", "Ingresos", "💰")
    object Projections : Screen("projections", "Proyección", "📈")
}

@Composable
fun MainExpenseApp(
    viewModel: ExpenseViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<ExpenseViewModel.UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates { info ->
            updateInfo = info
        }
    }

    val items = listOf(
        Screen.Dashboard,
        Screen.Cards,
        Screen.Income,
        Screen.Projections
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(screen.iconText, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel, onLogout) {
                    selectedScreen = Screen.Projections
                }
                Screen.Cards -> CardsScreen(viewModel)
                Screen.Income -> IncomeScreen(viewModel)
                Screen.Projections -> ProjectionsScreen(viewModel)
            }
        }
    }

    // Update Dialog powered by Remote Config & opening Drive link
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¡Nueva Actualización (${info.latestVersionName})!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(info.updateNotes)
                    Text("Hay una nueva versión de Control de Gastos disponible.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Abriendo página de descarga...", Toast.LENGTH_SHORT).show()
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "No se pudo abrir el enlace de descarga", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Descargar e Instalar")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("Más tarde")
                }
            }
        )
    }
}

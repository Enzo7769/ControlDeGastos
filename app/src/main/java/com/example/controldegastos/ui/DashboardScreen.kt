package com.example.controldegastos.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.controldegastos.data.CreditCardItem
import com.example.controldegastos.data.MonthColumn
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onLogout: () -> Unit = {},
    onNavigateToProjections: () -> Unit
) {
    val cards by viewModel.creditCards.collectAsState()
    val months by viewModel.months.collectAsState()
    val includeFixed by viewModel.includeFixedExpenses.collectAsState()
    val currentTheme by viewModel.selectedTheme.collectAsState()
    val useBiometrics by viewModel.useBiometrics.collectAsState()
    val clpFormat = viewModel.clpFormat
    val todayFormattedDate = viewModel.todayFormattedDate

    // Interactive Month Selector State (0 = Current Month, 1 = Next Month, etc.)
    var selectedMonthIndex by rememberSaveable { mutableIntStateOf(0) }

    val selectedMonthObj = months.find { month: MonthColumn -> month.index == selectedMonthIndex } ?: months.firstOrNull()
    val selectedMonthName = selectedMonthObj?.name ?: "Mes Seleccionado"

    val selectedMonthExpenses = viewModel.getTotalExpenseForMonth(selectedMonthIndex)
    val selectedMonthSobrante = viewModel.getCupoQueSobra(selectedMonthIndex)
    val totalIncome = viewModel.totalMonthlyIncome
    val totalDebtAllCards = cards.sumOf { card: CreditCardItem -> card.totalDebt }

    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val exportSummaryText = remember { mutableStateOf("") }

    val themeOptions = listOf(
        "Azul Clásico",
        "Esmeralda Finanzas",
        "Elegante Oscuro",
        "Rubí Intenso",
        "Atardecer Naranja",
        "Océano Turquesa",
        "Violeta Neón",
        "Moka Elegante",
        "Midnight Noche",
        "Dorado & Lujo",
        "Monocromático Slate",
        "Menta Fresca"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Gastos y Cuotas", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showThemeDialog = true }) {
                        Text("🎨", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = {
                        exportSummaryText.value = viewModel.generateExportSummary()
                        showExportDialog = true
                    }) {
                        Text("📤", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real Today Date Badge
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📅", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text(text = "Fecha de hoy:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = todayFormattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Toggle for Fixed Expenses
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Contabilizar Gastos Fijos (Casa/Apoyo)", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (includeFixed) "Incluidos en los totales" else "Excluidos (Solo Tarjetas/Cuotas)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeFixed,
                            onCheckedChange = { checked: Boolean -> viewModel.toggleIncludeFixedExpenses(checked) }
                        )
                    }
                }
            }

            // Interactive Month Selector & Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Month Navigator Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (selectedMonthIndex > 0) selectedMonthIndex-- },
                                enabled = selectedMonthIndex > 0,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Text("◀", style = MaterialTheme.typography.titleLarge)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = selectedMonthName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        selectedMonthIndex == 0 -> "Mes Actual"
                                        selectedMonthIndex > 0 -> "Mes Futuro (+${selectedMonthIndex})"
                                        else -> "Mes Pasado"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }

                            IconButton(
                                onClick = { if (selectedMonthIndex < months.size - 1) selectedMonthIndex++ },
                                enabled = selectedMonthIndex < months.size - 1,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Text("▶", style = MaterialTheme.typography.titleLarge)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))

                        // Selected Month Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Ingresos Totales", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = clpFormat.format(totalIncome),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Gastos en $selectedMonthName", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = clpFormat.format(selectedMonthExpenses),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Cupo que Sobra ($selectedMonthName)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = clpFormat.format(selectedMonthSobrante),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedMonthSobrante >= 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.errorContainer
                                )
                            }
                            Button(
                                onClick = onNavigateToProjections,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Ver Tabla Excel")
                            }
                        }
                    }
                }
            }

            // Visual Expense Distribution Chart
            if (cards.isNotEmpty() && totalDebtAllCards > 0.0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "📊 Distribución de Deuda por Tarjeta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = MaterialTheme.shapes.small
                                    )
                            ) {
                                val chartColors: List<Color> = listOf(
                                    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047),
                                    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1)
                                )
                                cards.forEachIndexed { idx: Int, card: CreditCardItem ->
                                    val fraction = (card.totalDebt / totalDebtAllCards).toFloat()
                                    if (fraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(fraction)
                                                .background(chartColors[idx % chartColors.size])
                                        )
                                    }
                                }
                            }

                            val chartColors: List<Color> = listOf(
                                Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047),
                                Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1)
                            )
                            cards.forEachIndexed { idx: Int, card: CreditCardItem ->
                                val pct = (card.totalDebt / totalDebtAllCards * 100).toInt()
                                if (pct > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = card.categoryIcon, style = MaterialTheme.typography.bodyMedium)
                                            Text(text = card.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = "$pct% (${clpFormat.format(card.totalDebt)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cards Breakdown for Selected Month
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Gastos en $selectedMonthName por Tarjeta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🟢 < 50% uso", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        Text("🟠 50-80% uso", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF6C00))
                        Text("🔴 > 80% uso", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                    }
                }
            }

            // Cards Summary List for Selected Month
            items(cards) { card: CreditCardItem ->
                val ratio = if (card.creditLimit > 0) (card.totalDebt / card.creditLimit).toFloat() else 0f
                val statusColor = viewModel.getUsageColor(ratio)

                val monthAmount = card.monthlyAmounts[selectedMonthIndex] ?: 0.0
                val isPaid = card.paidMonths.contains(selectedMonthIndex)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = card.categoryIcon, style = MaterialTheme.typography.titleMedium)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(statusColor, shape = MaterialTheme.shapes.extraSmall)
                                )
                                Text(
                                    text = card.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cuota en $selectedMonthName: ${clpFormat.format(monthAmount)} ${if (isPaid) "(Pagado ✓)" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Deuda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = clpFormat.format(card.totalDebt),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Theme & Settings Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Ajustes de Cuenta y Apariencia") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🔒 Bloqueo con Huella Dactilar", fontWeight = FontWeight.Bold)
                                    Text("Pedir huella al abrir la app", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = useBiometrics,
                                    onCheckedChange = { checked: Boolean -> viewModel.setUseBiometrics(checked) }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                showThemeDialog = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🚪 Cerrar Sesión")
                        }
                    }

                    item {
                        Text("Temas de Colores:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }

                    items(themeOptions) { themeName: String ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemePalette(themeName)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = currentTheme == themeName,
                                onClick = {
                                    viewModel.setThemePalette(themeName)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(themeName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showThemeDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Export Preview Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Resumen para Exportar / Compartir") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    item {
                        Text(
                            text = exportSummaryText.value,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportSummaryText.value)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartir Resumen")
                            context.startActivity(shareIntent)
                        }
                    ) {
                        Text("📤 Compartir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

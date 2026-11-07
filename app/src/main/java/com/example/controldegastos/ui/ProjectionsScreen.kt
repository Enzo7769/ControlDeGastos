package com.example.controldegastos.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.controldegastos.data.MonthColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionsScreen(viewModel: ExpenseViewModel) {
    val cards by viewModel.creditCards.collectAsState()
    val months by viewModel.months.collectAsState()
    val includeFixed by viewModel.includeFixedExpenses.collectAsState()
    val totalIncome = viewModel.totalMonthlyIncome
    val clpFormat = viewModel.clpFormat
    val horizontalScrollState = rememberScrollState()

    var showAddMonthDialog by remember { mutableStateOf(false) }
    var newMonthName by remember { mutableStateOf("") }

    // Check for months with negative balance (Deficit)
    val deficitMonths = remember(months, cards, totalIncome, includeFixed) {
        months.mapNotNull { month: MonthColumn ->
            val sobra = viewModel.getCupoQueSobra(month.index)
            if (sobra < 0) Pair(month, sobra) else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabla de Gastos (Estilo Excel)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMonthDialog = true }) {
                Text("+ Agregar Mes")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Semáforo / Alert Banner for Deficit Months
            if (deficitMonths.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚠️ Alerta de Salud Financiera (Déficit)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            deficitMonths.forEach { pair: Pair<MonthColumn, Double> ->
                                val m = pair.first
                                val sobra = pair.second
                                Text(
                                    text = "• En ${m.name} tus gastos superan tus ingresos por ${clpFormat.format(-sobra)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
                                text = if (includeFixed) "Incluidos en los totales y sobrante" else "Excluidos (Solo Tarjetas/Cuotas)",
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

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Ingresos Totales (Suma Trabajos)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = clpFormat.format(totalIncome),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Desglose por Mes y por Tarjeta / Gasto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Desliza horizontalmente para ver todos los meses. Usa '+ Agregar Mes' para añadir meses futuros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Table View with Horizontal Scroll
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Header Row (Months)
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Item / Tarjeta",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(130.dp)
                            )
                            months.forEach { month ->
                                Text(
                                    text = month.name,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        // Card Rows
                        cards.forEach { card ->
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = card.name,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(130.dp)
                                )
                                months.forEach { month ->
                                    val amount = card.monthlyAmounts[month.index] ?: 0.0
                                    val isPaid = card.paidMonths.contains(month.index)
                                    val displayText = if (amount > 0.0) clpFormat.format(amount) else "-"
                                    Text(
                                        text = if (isPaid) "$displayText (✓)" else displayText,
                                        modifier = Modifier.width(90.dp),
                                        color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Total Expenses Row
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL GASTOS",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(130.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                            months.forEach { month ->
                                val totalExp = viewModel.getTotalExpenseForMonth(month.index)
                                Text(
                                    text = if (totalExp > 0.0) clpFormat.format(totalExp) else "$0",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Cupo que sobra (Remaining Balance) Row
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CUPO QUE SOBRA",
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(130.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            months.forEach { month ->
                                val sobra = viewModel.getCupoQueSobra(month.index)
                                Text(
                                    text = clpFormat.format(sobra),
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(90.dp),
                                    color = if (sobra >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Month
    if (showAddMonthDialog) {
        AlertDialog(
            onDismissRequest = { showAddMonthDialog = false },
            title = { Text("Agregar Nuevo Mes a la Tabla") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newMonthName,
                        onValueChange = { newMonthName = it },
                        label = { Text("Nombre del Mes (ej. Abr-27, May-27)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMonthName.isNotBlank()) {
                            viewModel.addMonth(newMonthName)
                            newMonthName = ""
                            showAddMonthDialog = false
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMonthDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

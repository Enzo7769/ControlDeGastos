package com.example.controldegastos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.controldegastos.data.CreditCardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(viewModel: ExpenseViewModel) {
    val cards by viewModel.creditCards.collectAsState()
    val months by viewModel.months.collectAsState()
    val clpFormat = viewModel.clpFormat
    val categoryIcons = viewModel.availableCategoryIcons

    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<CreditCardItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) {
            cards
        } else {
            cards.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tarjetas y Créditos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+ Nuevo Gasto / Tarjeta")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("🔍 Buscar tarjeta o gasto...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Toca cualquier tarjeta para ingresar el Cupo Usado del banco, ver el cupo disponible, editar cuotas mes a mes o marcar pagos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filteredCards) { card ->
                val totalUsed = card.totalDebt
                val totalLimit = card.creditLimit
                val available = card.availableLimit
                val usageRatio = if (totalLimit > 0.0) (totalUsed / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f
                val paidCount = card.paidMonths.size
                val totalMonthsWithDebt = card.monthlyAmounts.size
                val statusColor = viewModel.getUsageColor(usageRatio)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedItemForDetail = card },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = card.categoryIcon, style = MaterialTheme.typography.titleLarge)
                                Column {
                                    Text(
                                        text = card.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (card.isCreditCard) "Tarjeta de Crédito / Cuotas" else "Gasto Fijo / Cuenta",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteCardItem(card.id) }) {
                                Text("🗑️")
                            }
                        }

                        HorizontalDivider()

                        if (card.isCreditCard && totalLimit > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Cupo Usado (Ingresado/Banco)", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = clpFormat.format(totalUsed),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Cupo Disponible", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = clpFormat.format(available),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Límite Total: ${clpFormat.format(totalLimit)}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "${(usageRatio * 100).toInt()}% usado", style = MaterialTheme.typography.bodySmall)
                                }
                                LinearProgressIndicator(
                                    progress = { usageRatio },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = statusColor
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Deuda Total:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = clpFormat.format(totalUsed),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meses pagados: $paidCount de $totalMonthsWithDebt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "✏️ Toca para editar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail & Edit Dialog for Card Name, Category Icon, Limit, Manual Used Quota, and Monthly Amounts
    selectedItemForDetail?.let { card ->
        var editName by remember { mutableStateOf(card.name) }
        var editCategoryIcon by remember { mutableStateOf(card.categoryIcon) }
        var editLimit by remember { mutableStateOf(card.creditLimit.toInt().toString()) }
        var editUsedQuota by remember { mutableStateOf(card.manualUsedQuota.toInt().toString()) }
        var editIsCredit by remember { mutableStateOf(card.isCreditCard) }
        var showAddMonthDialogInCard by remember { mutableStateOf(false) }
        var newMonthNameInCard by remember { mutableStateOf("") }

        val amountStates = remember(card) {
            mutableStateMapOf<Int, String>().apply {
                months.forEach { m ->
                    this[m.index] = (card.monthlyAmounts[m.index] ?: 0.0).let { if (it > 0) it.toInt().toString() else "" }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            title = { Text("Editar: ${card.name}") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nombre de Tarjeta / Gasto") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Selecciona Categoría e Icono:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryIcons.forEach { (icon, desc) ->
                                    FilterChip(
                                        selected = editCategoryIcon == icon,
                                        onClick = { editCategoryIcon = icon },
                                        label = { Text(icon) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = editLimit,
                                onValueChange = { editLimit = it },
                                label = { Text("Cupo Total / Límite del Banco ($)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editUsedQuota,
                                onValueChange = { editUsedQuota = it },
                                label = { Text("Cupo Usado Real (Banco/App) ($)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "💡 Ingresa el cupo usado total que te muestra tu banco (incluyendo cuotas futuras que aún no cargan en la tabla).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editIsCredit, onCheckedChange = { editIsCredit = it })
                                Text("Es Tarjeta de Crédito (Muestra Cupo)")
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cuotas y Control por Mes:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { showAddMonthDialogInCard = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ Agregar Mes", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    items(months) { month ->
                        val currentText = amountStates[month.index] ?: ""
                        val isPaid = card.paidMonths.contains(month.index)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPaid)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = month.name, fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = { viewModel.togglePaidMonth(card.id, month.index) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(if (isPaid) "✅ Pagado" else "⏳ Pendiente", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                OutlinedTextField(
                                    value = currentText,
                                    onValueChange = { newStr ->
                                        amountStates[month.index] = newStr
                                        val newVal = newStr.toDoubleOrNull() ?: 0.0
                                        viewModel.updateMonthlyAmount(card.id, month.index, newVal)
                                    },
                                    label = { Text("Monto cuota de este mes ($)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLim = editLimit.toDoubleOrNull() ?: card.creditLimit
                        val newUsed = editUsedQuota.toDoubleOrNull() ?: 0.0
                        viewModel.updateCardInfo(card.id, editName, editCategoryIcon, editIsCredit, newLim, newUsed)
                        selectedItemForDetail = null
                    }
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDetail = null }) {
                    Text("Cerrar")
                }
            }
        )

        // Sub-dialog to add a new month inside the card edit view
        if (showAddMonthDialogInCard) {
            AlertDialog(
                onDismissRequest = { showAddMonthDialogInCard = false },
                title = { Text("Agregar Nuevo Mes") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newMonthNameInCard,
                            onValueChange = { newMonthNameInCard = it },
                            label = { Text("Nombre del Mes (ej. Abr-27)") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newMonthNameInCard.isNotBlank()) {
                                viewModel.addMonth(newMonthNameInCard)
                                newMonthNameInCard = ""
                                showAddMonthDialogInCard = false
                            }
                        }
                    ) {
                        Text("Agregar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMonthDialogInCard = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    // Dialog: Add Card or Expense Item
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var selectedIcon by remember { mutableStateOf("💳") }
        var isCredit by remember { mutableStateOf(true) }
        var limit by remember { mutableStateOf("1000000") }
        var usedQuota by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar Nueva Tarjeta o Gasto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre (ej. Bci, Cencosud, Préstamo)") }
                    )

                    Text("Categoría e Icono:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryIcons.forEach { (icon, desc) ->
                            FilterChip(
                                selected = selectedIcon == icon,
                                onClick = { selectedIcon = icon },
                                label = { Text(icon) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                        Text("Es Tarjeta de Crédito (Muestra Cupo)")
                    }
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Cupo Total / Límite ($)") }
                    )
                    OutlinedTextField(
                        value = usedQuota,
                        onValueChange = { usedQuota = it },
                        label = { Text("Cupo Usado Real (Banco) ($)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = limit.toDoubleOrNull() ?: 1000000.0
                        val usedVal = usedQuota.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            viewModel.addCardItem(name, selectedIcon, isCredit, limitVal, usedVal, emptyMap())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Guardar / Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

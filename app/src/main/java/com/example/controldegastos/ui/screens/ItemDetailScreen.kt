package com.example.controldegastos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.LocalAppSnackbar
import com.example.controldegastos.ui.Navigator
import com.example.controldegastos.ui.Route
import com.example.controldegastos.ui.components.ConfirmDialog
import com.example.controldegastos.ui.components.IconBubble
import com.example.controldegastos.ui.components.MoneyField
import com.example.controldegastos.ui.components.MonthSwitcher
import com.example.controldegastos.ui.components.SectionHeader
import com.example.controldegastos.ui.components.UsageBar
import com.example.controldegastos.ui.components.digitsToAmount
import com.example.controldegastos.ui.components.toDigits
import com.example.controldegastos.ui.theme.FinanceTheme
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.toClp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(viewModel: FinanceViewModel, itemId: String, navigator: Navigator) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val expense = data.items.firstOrNull { it.id == itemId }
    val snackbar = LocalAppSnackbar.current
    var leaving by remember { mutableStateOf(false) }

    LaunchedEffect(expense == null) {
        if (expense == null && !leaving) {
            leaving = true
            navigator.back()
        }
    }
    if (expense == null) return

    val today = viewModel.today
    var showDelete by remember { mutableStateOf(false) }
    var showInstallments by rememberSaveable { mutableStateOf(false) }
    var editingMonth by rememberSaveable { mutableStateOf<String?>(null) }
    var showPast by rememberSaveable { mutableStateOf(false) }

    val timeline = remember(expense, today) { FinanceCalculator.timeline(listOf(expense), today) }
    val cutoff = maxOf(timeline.first(), today - 2)
    val visibleMonths = if (showPast) timeline else timeline.filter { it >= cutoff }
    val hasPast = timeline.first() < cutoff

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(expense.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.open(Route.ItemForm(expense.id)) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") { HeaderCard(expense) }
            item(key = "action") {
                Button(
                    onClick = { showInstallments = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(
                        if (expense.isCreditCard) "Agregar compra en cuotas" else "Repetir monto mensual",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            item(key = "calendar-title") {
                SectionHeader(
                    title = "Calendario de pagos",
                    actionLabel = if (hasPast) (if (showPast) "Ocultar anteriores" else "Mostrar meses anteriores") else null,
                    onAction = { showPast = !showPast },
                )
            }
            items(visibleMonths, key = { it.key() }) { month ->
                MonthRow(
                    month = month,
                    amount = expense.amountFor(month),
                    paid = expense.isPaid(month),
                    isToday = month == today,
                    onClick = { editingMonth = month.key() },
                    onTogglePaid = { viewModel.togglePaid(expense.id, month) },
                )
            }
            item(key = "footer") {
                val paidTotal = expense.amounts.filterKeys { it in expense.paidMonths }.values.sum()
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "Total pendiente ${expense.pendingInstallments.toClp()} · Pagado ${paidTotal.toClp()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Eliminar \"${expense.name}\"",
            message = "Se borrarán también sus cuotas y pagos registrados.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                val removed = viewModel.deleteItem(expense.id)
                if (removed != null) {
                    leaving = true
                    navigator.back()
                    val (pos, deleted) = removed
                    snackbar.show("\"${deleted.name}\" eliminado", "Deshacer") { viewModel.restoreItem(pos, deleted) }
                }
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showInstallments) {
        InstallmentsDialog(
            isCreditCard = expense.isCreditCard,
            today = today,
            onConfirm = { amount, count, start -> viewModel.addInstallments(expense.id, amount, count, start) },
            onDismiss = { showInstallments = false },
        )
    }

    editingMonth?.let { key ->
        val month = YearMonth.parseKey(key) ?: return@let
        AmountDialog(
            month = month,
            initial = expense.amountFor(month),
            onSave = { viewModel.setAmount(expense.id, month, it) },
            onDismiss = { editingMonth = null },
        )
    }
}

@Composable
private fun HeaderCard(item: ExpenseItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBubble(item.icon, size = 56.dp)
                Column {
                    Text(item.type.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val (headLabel, headValue) = item.headline(YearMonth.now())
                    Text(if (headLabel == "Deuda") "Deuda actual" else headLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(headValue.toClp(), style = MaterialTheme.typography.headlineMedium)
                }
            }
            if (item.isCreditCard && item.creditLimit > 0.0) {
                Row(Modifier.fillMaxWidth()) {
                    LabeledValue("Cupo total", item.creditLimit.toClp(), Modifier.weight(1f))
                    LabeledValue("Usado", item.currentDebt.toClp(), Modifier.weight(1f))
                    LabeledValue("Disponible", item.availableCredit.toClp(), Modifier.weight(1f), alignEnd = true)
                }
                UsageBar(item.usageRatio)
            }
            if (item.isCreditCard && item.usedQuota > 0.0) {
                Text(
                    "El cupo usado corresponde al informado según el banco.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun MonthRow(
    month: YearMonth,
    amount: Double,
    paid: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    onTogglePaid: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(month.longLabel(), style = MaterialTheme.typography.bodyLarge)
                    if (isToday) {
                        Box(
                            Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text("Hoy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Text(
                    when {
                        amount <= 0.0 -> "Sin pago · toca para agregar"
                        paid -> "${amount.toClp()} · Pagado"
                        else -> amount.toClp()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        amount <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        paid -> FinanceTheme.colors.positive
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Checkbox(checked = paid, onCheckedChange = { onTogglePaid() }, enabled = amount > 0.0)
        }
    }
}

@Composable
private fun InstallmentsDialog(
    isCreditCard: Boolean,
    today: YearMonth,
    onConfirm: (amount: Double, count: Int, start: YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableIntStateOf(if (isCreditCard) 3 else 12) }
    var startKey by rememberSaveable { mutableStateOf(today.key()) }
    val start = YearMonth.parseKey(startKey) ?: today
    val amount = amountText.digitsToAmount()
    val end = start + (count - 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreditCard) "Compra en cuotas" else "Repetir monto mensual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = if (isCreditCard) "Valor de cada cuota" else "Monto mensual",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isCreditCard) "Cuotas" else "Meses", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { if (count > 1) count-- }, enabled = count > 1) { Text("−") }
                    Text("$count", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
                    FilledTonalIconButton(onClick = { if (count < 72) count++ }, enabled = count < 72) { Text("+") }
                }
                Text("Desde", style = MaterialTheme.typography.labelLarge)
                MonthSwitcher(month = start, today = today, onChange = { startKey = it.key() })
                if (amount > 0.0) {
                    Text(
                        "Se agregarán $count ${if (count == 1) "cuota" else "cuotas"} de ${amount.toClp()} " +
                            "desde ${start.longLabel()} hasta ${end.longLabel()} (total ${(amount * count).toClp()}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount, count, start); onDismiss() }, enabled = amount > 0.0) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun AmountDialog(
    month: YearMonth,
    initial: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(month.key()) { mutableStateOf(initial.toDigits()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(month.longLabel()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Monto a pagar",
                    supportingText = "Déjalo vacío para quitar el pago de este mes.",
                )
                if (initial > 0.0) {
                    OutlinedButton(onClick = { onSave(0.0); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Quitar pago de este mes")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(text.digitsToAmount()); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

package com.example.controldegastos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.R
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.FinanceData
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.domain.MonthSummary
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.Navigator
import com.example.controldegastos.ui.Route
import com.example.controldegastos.ui.components.EmptyState
import com.example.controldegastos.ui.components.UsageBar
import com.example.controldegastos.ui.theme.FinanceTheme
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.relativeTo
import com.example.controldegastos.util.shortLabel
import com.example.controldegastos.util.toClp
import com.example.controldegastos.util.toClpCompact
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen(viewModel: FinanceViewModel, navigator: Navigator) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today = viewModel.today
    val includeFixed = settings.includeFixedExpenses

    var showTable by rememberSaveable { mutableStateOf(false) }
    var showPast by rememberSaveable { mutableStateOf(false) }

    val months = remember(data, today, showPast) {
        val timeline = FinanceCalculator.timeline(data.items, today)
        if (showPast) timeline else timeline.filter { it >= today }
    }
    val summaries = remember(data, months, includeFixed) {
        months.map { FinanceCalculator.monthSummary(data, it, includeFixed) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Proyección") }) }) { padding ->
        if (data.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_trending_up),
                    title = "Aún no hay nada que proyectar",
                    message = "Agrega tus tarjetas, créditos o gastos fijos para ver cómo se verán los próximos meses.",
                    actionLabel = "Agregar tarjeta o deuda",
                    onAction = { navigator.open(Route.ItemForm()) },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Resumen", "Tabla").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = showTable == (i == 1),
                            onClick = { showTable = i == 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                        ) { Text(label) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip("Incluir gastos fijos", includeFixed) {
                        viewModel.updateSettings { it.copy(includeFixedExpenses = !includeFixed) }
                    }
                    ToggleChip("Ver meses anteriores", showPast) { showPast = !showPast }
                }
            }
            if (showTable) {
                item {
                    ProjectionTable(data, months, summaries, today, includeFixed) { navigator.open(Route.ItemDetail(it)) }
                }
            } else {
                item { BalanceBarChart(summaries, today) }
                items(summaries, key = { it.month.ordinal }) { MonthCard(it, today) }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
    )
}

@Composable
private fun BalanceBarChart(summaries: List<MonthSummary>, today: YearMonth) {
    val positive = FinanceTheme.colors.positive
    val negative = FinanceTheme.colors.negative
    val maxPos = summaries.maxOfOrNull { it.balance }?.coerceAtLeast(0.0) ?: 0.0
    val maxNeg = summaries.minOfOrNull { it.balance }?.coerceAtMost(0.0)?.let { -it } ?: 0.0
    val range = (maxPos + maxNeg).takeIf { it > 0 } ?: 1.0
    val baseline = (maxPos / range).toFloat()
    val axis = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Saldo por mes", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                summaries.forEach { s ->
                    val color = if (s.balance >= 0) positive else negative
                    Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            s.balance.toClpCompact(),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            maxLines = 1,
                        )
                        Canvas(Modifier.width(28.dp).height(110.dp)) {
                            val baseY = size.height * baseline
                            drawLine(axis, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.dp.toPx())
                            val h = (abs(s.balance) / range * size.height).toFloat()
                            val top = if (s.balance >= 0) baseY - h else baseY
                            if (h > 0f) {
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(0f, top),
                                    size = Size(size.width, h),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                )
                            }
                        }
                        Text(
                            s.month.shortLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (s.month == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (s.month == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCard(s: MonthSummary, today: YearMonth) {
    val isCurrent = s.month == today
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.month.longLabel(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(s.month.relativeTo(today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabeledAmount("Ingresos", s.income.toClp())
                LabeledAmount("Gastos", s.expenses.toClp())
                LabeledAmount(
                    "Saldo",
                    s.balance.toClp(),
                    color = if (s.isDeficit) FinanceTheme.colors.negative else FinanceTheme.colors.positive,
                    alignEnd = true,
                )
            }
            UsageBar(s.spendRatio)
        }
    }
}

@Composable
private fun LabeledAmount(label: String, value: String, color: Color = Color.Unspecified, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

private val NameColumnWidth = 120.dp
private val MonthColumnWidth = 96.dp
private val RowHeight = 44.dp

@Composable
private fun ProjectionTable(
    data: FinanceData,
    months: List<YearMonth>,
    summaries: List<MonthSummary>,
    today: YearMonth,
    includeFixed: Boolean,
    onOpenItem: (String) -> Unit,
) {
    val scroll = rememberScrollState()
    val items = data.items.filter { FinanceCalculator.countsFor(it, includeFixed) }
    val highlight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val positive = FinanceTheme.colors.positive
    val negative = FinanceTheme.colors.negative

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            // Encabezado
            TableRow(
                name = { Text("Ítem", style = MaterialTheme.typography.labelLarge) },
                months = months,
                scroll = scroll,
                today = today,
                highlight = highlight,
            ) { m ->
                Text(m.shortLabel(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            HorizontalDivider()
            items.forEach { item ->
                TableRow(
                    name = {
                        Text(
                            "${item.icon} ${item.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onOpenItem(item.id) },
                        )
                    },
                    months = months,
                    scroll = scroll,
                    today = today,
                    highlight = highlight,
                ) { m -> AmountCell(item, m, positive) }
            }
            HorizontalDivider()
            TableRow(
                name = { Text("TOTAL GASTOS", style = MaterialTheme.typography.labelLarge) },
                months = months,
                scroll = scroll,
                today = today,
                highlight = highlight,
            ) { m ->
                val s = summaries.firstOrNull { it.month == m }
                Text(
                    (s?.expenses ?: 0.0).toClp(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TableRow(
                name = { Text("SALDO", style = MaterialTheme.typography.labelLarge) },
                months = months,
                scroll = scroll,
                today = today,
                highlight = highlight,
            ) { m ->
                val balance = summaries.firstOrNull { it.month == m }?.balance ?: 0.0
                Text(
                    balance.toClp(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) negative else positive,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AmountCell(item: ExpenseItem, month: YearMonth, positive: Color) {
    val amount = item.amountFor(month)
    val paid = item.isPaid(month)
    Text(
        when {
            amount <= 0.0 -> "—"
            paid -> "✓ ${amount.toClp()}"
            else -> amount.toClp()
        },
        style = MaterialTheme.typography.bodyMedium,
        color = when {
            amount <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
            paid -> positive
            else -> MaterialTheme.colorScheme.onSurface
        },
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Fila con la primera columna fija y las columnas de meses en un área con scroll compartido. */
@Composable
private fun TableRow(
    name: @Composable () -> Unit,
    months: List<YearMonth>,
    scroll: ScrollState,
    today: YearMonth,
    highlight: Color,
    cellWidth: Dp = MonthColumnWidth,
    cell: @Composable (YearMonth) -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(NameColumnWidth).padding(start = 12.dp, end = 4.dp)) { name() }
        Row(Modifier.weight(1f).horizontalScroll(scroll)) {
            months.forEach { m ->
                Box(
                    Modifier
                        .width(cellWidth)
                        .height(RowHeight)
                        .then(if (m == today) Modifier.background(highlight) else Modifier)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) { cell(m) }
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

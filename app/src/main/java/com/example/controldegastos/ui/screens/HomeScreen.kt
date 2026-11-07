package com.example.controldegastos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.R
import com.example.controldegastos.data.FinanceRepository
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.domain.MonthSummary
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.Navigator
import com.example.controldegastos.ui.Route
import com.example.controldegastos.ui.Tab
import com.example.controldegastos.ui.components.DonutChart
import com.example.controldegastos.ui.components.EmptyState
import com.example.controldegastos.ui.components.IconBubble
import com.example.controldegastos.ui.components.LegendDot
import com.example.controldegastos.ui.components.MonthSwitcher
import com.example.controldegastos.ui.components.SectionHeader
import com.example.controldegastos.ui.components.UsageBar
import com.example.controldegastos.ui.theme.FinanceTheme
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.monthName
import com.example.controldegastos.util.toClp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: FinanceViewModel, navigator: Navigator) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    val today = viewModel.today

    var monthOrdinal by rememberSaveable { mutableIntStateOf(today.ordinal) }
    val month = YearMonth.fromOrdinal(monthOrdinal)
    val includeFixed = settings.includeFixedExpenses

    val summary = remember(data, month, includeFixed) { FinanceCalculator.monthSummary(data, month, includeFixed) }
    val breakdown = remember(data, month, includeFixed) { FinanceCalculator.breakdown(data, month, includeFixed) }
    val deficits = remember(data, today, includeFixed) { FinanceCalculator.upcomingDeficits(data, today, includeFixed) }
    val payments = remember(data, month, includeFixed) {
        data.items.filter { it.amountFor(month) > 0.0 && FinanceCalculator.countsFor(it, includeFixed) }
    }

    val name = user?.displayName?.takeIf { it.isNotBlank() }?.substringBefore(' ')
        ?: user?.email?.substringBefore('@')
        ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (name.isNotBlank()) "Hola, $name" else "Hola", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when (sync) {
                                FinanceRepository.SyncState.SYNCED -> "Datos sincronizados"
                                FinanceRepository.SyncState.SYNCING -> "Sincronizando…"
                                FinanceRepository.SyncState.OFFLINE -> "Sin conexión · guardado en el teléfono"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.open(Route.Settings) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
    ) { padding ->
        if (data.isEmpty) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_wallet),
                    title = "Bienvenido a Control de Gastos",
                    message = "Registra tus ingresos y tus tarjetas, créditos o gastos fijos para saber cuánto te queda cada mes.",
                )
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = { navigator.goToTab(Tab.INCOME) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Agregar ingreso")
                    }
                    OutlinedButton(onClick = { navigator.open(Route.ItemForm()) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Agregar tarjeta o deuda")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeroCard(summary, month, today, onMonthChange = { monthOrdinal = it.ordinal })
            }
            item {
                FilterChip(
                    selected = includeFixed,
                    onClick = { viewModel.updateSettings { it.copy(includeFixedExpenses = !includeFixed) } },
                    label = { Text("Incluir gastos fijos") },
                    leadingIcon = if (includeFixed) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
            }
            if (deficits.isNotEmpty()) {
                item { DeficitCard(deficits, onClick = { navigator.goToTab(Tab.PROJECTION) }) }
            }
            if (breakdown.isNotEmpty()) {
                item { BreakdownCard(summary.expenses, breakdown) }
            }
            item { SectionHeader("Pagos de ${month.monthName()}") }
            if (payments.isEmpty()) {
                item {
                    Text(
                        "No tienes pagos registrados para este mes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(payments, key = { it.id }) { item ->
                val paid = item.isPaid(month)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navigator.open(Route.ItemDetail(item.id)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBubble(item.icon, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (paid) TextDecoration.LineThrough else null,
                                color = if (paid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (paid) "Pagado" else item.type.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (paid) FinanceTheme.colors.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            item.amountFor(month).toClp(),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (paid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        Checkbox(checked = paid, onCheckedChange = { viewModel.togglePaid(item.id, month) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(summary: MonthSummary, month: YearMonth, today: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = onContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthSwitcher(month = month, today = today, onChange = onMonthChange, contentColor = onContainer)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (summary.isDeficit) "Te faltan" else "Te queda",
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer.copy(alpha = 0.8f),
                )
                Text(
                    (if (summary.isDeficit) -summary.balance else summary.balance).toClp(),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (summary.isDeficit) FinanceTheme.colors.negative else onContainer,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Ingresos", style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.8f))
                    Text(summary.income.toClp(), style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gastos", style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.8f))
                    Text(summary.expenses.toClp(), style = MaterialTheme.typography.titleMedium)
                }
            }
            UsageBar(summary.spendRatio)
            Text(
                if (summary.paymentsCount == 0) "Sin pagos este mes"
                else "Pagado ${summary.paidCount} de ${summary.paymentsCount} · Falta pagar ${summary.pending.toClp()}",
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun DeficitCard(deficits: List<MonthSummary>, onClick: () -> Unit) {
    val colors = FinanceTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.negativeContainer, contentColor = colors.onNegativeContainer),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Meses con déficit", style = MaterialTheme.typography.titleSmall)
                deficits.take(4).forEach {
                    Text("${it.month.longLabel()}: te faltarán ${(-it.balance).toClp()}", style = MaterialTheme.typography.bodySmall)
                }
                if (deficits.size > 4) {
                    Text("y ${deficits.size - 4} más…", style = MaterialTheme.typography.bodySmall)
                }
                Text("Ver proyección", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BreakdownCard(total: Double, shares: List<com.example.controldegastos.domain.ItemShare>) {
    val palette = FinanceTheme.colors.chart
    val maxRows = 6
    val shown = shares.take(maxRows)
    val rest = shares.drop(maxRows)
    val rows: List<Triple<String, Double, Float>> = shown.map { Triple(it.item.name, it.amount, it.fraction) } +
        if (rest.isNotEmpty()) listOf(Triple("Otros", rest.sumOf { it.amount }, rest.sumOf { it.fraction.toDouble() }.toFloat())) else emptyList()
    val colorFor: (Int) -> Color = { palette[it % palette.size] }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("¿En qué se va tu plata?", style = MaterialTheme.typography.titleMedium)
            DonutChart(
                slices = rows.mapIndexed { i, r -> r.third to colorFor(i) },
                modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(total.toClp(), style = MaterialTheme.typography.titleMedium)
                }
            }
            HorizontalDivider()
            rows.forEachIndexed { i, (label, amount, fraction) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(colorFor(i))
                    Spacer(Modifier.width(10.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${(fraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Text(amount.toClp(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}

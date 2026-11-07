package com.example.controldegastos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.R
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.ExpenseType
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.Navigator
import com.example.controldegastos.ui.Route
import com.example.controldegastos.ui.components.EmptyState
import com.example.controldegastos.ui.components.IconBubble
import com.example.controldegastos.ui.components.UsageBar
import com.example.controldegastos.ui.theme.FinanceTheme
import com.example.controldegastos.util.toClp

private enum class TypeFilter(val label: String, val type: ExpenseType?) {
    ALL("Todos", null),
    CARDS("Tarjetas", ExpenseType.CREDIT_CARD),
    LOANS("Créditos", ExpenseType.LOAN),
    FIXED("Gastos fijos", ExpenseType.FIXED),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: FinanceViewModel, navigator: Navigator) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val today = viewModel.today
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(TypeFilter.ALL) }

    val items = data.items
    val filtered = remember(items, query, filter) {
        items.filter { item ->
            (filter.type == null || item.type == filter.type) &&
                (query.isBlank() || item.name.contains(query.trim(), ignoreCase = true))
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Deudas y gastos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navigator.open(Route.ItemForm()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar") },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_credit_card),
                    title = "Aún no tienes deudas registradas",
                    message = "Agrega tus tarjetas, créditos y gastos fijos para ver cuánto pagas cada mes.",
                    actionLabel = "Agregar la primera",
                    onAction = { navigator.open(Route.ItemForm()) },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Deuda total", style = MaterialTheme.typography.labelLarge)
                            Text(FinanceCalculator.totalDebt(items).toClp(), style = MaterialTheme.typography.titleLarge)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Cupo disponible", style = MaterialTheme.typography.labelLarge)
                            Text(FinanceCalculator.totalAvailableCredit(items).toClp(), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                )
            }
            item(key = "filters") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TypeFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        "Sin resultados",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(filtered, key = { it.id }) { item ->
                ItemCard(item, today) { navigator.open(Route.ItemDetail(item.id)) }
            }
        }
    }
}

@Composable
private fun ItemCard(item: ExpenseItem, today: YearMonth, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(item.icon)
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val monthAmount = item.amountFor(today)
                    if (monthAmount > 0.0) {
                        val paid = item.isPaid(today)
                        Text(
                            "Este mes: ${monthAmount.toClp()}" + if (paid) " · Pagado ✓" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (paid) FinanceTheme.colors.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Deuda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.currentDebt.toClp(), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (item.isCreditCard && item.creditLimit > 0.0) {
                UsageBar(item.usageRatio)
                Text(
                    "Disponible ${item.availableCredit.toClp()} · ${(item.usageRatio * 100).toInt()}% usado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package com.example.controldegastos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.R
import com.example.controldegastos.data.IncomeSource
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.LocalAppSnackbar
import com.example.controldegastos.ui.components.EmptyState
import com.example.controldegastos.ui.components.IconBubble
import com.example.controldegastos.ui.components.MoneyField
import com.example.controldegastos.ui.components.digitsToAmount
import com.example.controldegastos.ui.components.toDigits
import com.example.controldegastos.ui.theme.FinanceTheme
import com.example.controldegastos.util.toClp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(viewModel: FinanceViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    val incomes = data.incomes
    val total = FinanceCalculator.totalIncome(data)

    // null = cerrado, "" = nuevo, id = editar
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ingresos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingId = "" },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar ingreso") },
            )
        },
    ) { padding ->
        if (incomes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_money),
                    title = "Sin ingresos registrados",
                    message = "Agrega tu sueldo u otras fuentes de ingreso para calcular cuánto te queda cada mes.",
                    actionLabel = "Agregar ingreso",
                    onAction = { editingId = "" },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val colors = FinanceTheme.colors
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.positiveContainer,
                            contentColor = colors.onPositiveContainer,
                        ),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Ingreso mensual total", style = MaterialTheme.typography.labelLarge)
                            Text(total.toClp(), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                if (incomes.size == 1) "1 fuente de ingreso" else "${incomes.size} fuentes de ingreso",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                items(incomes, key = { it.id }) { income ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editingId = income.id },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBubble("💼", size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(income.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    income.monthlyAmount.toClp() + " al mes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinanceTheme.colors.positive,
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deleteIncome(income.id)?.let { (pos, removed) ->
                                    snackbar.show("Ingreso eliminado", "Deshacer") { viewModel.restoreIncome(pos, removed) }
                                }
                            }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${income.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    editingId?.let { id ->
        val existing = incomes.firstOrNull { it.id == id }
        IncomeDialog(
            existing = existing,
            onDismiss = { editingId = null },
            onSave = { name, amount ->
                viewModel.saveIncome(
                    IncomeSource(id = existing?.id ?: UUID.randomUUID().toString(), name = name, monthlyAmount = amount)
                )
                editingId = null
            },
        )
    }
}

@Composable
private fun IncomeDialog(
    existing: IncomeSource?,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var amount by rememberSaveable(existing?.id) { mutableStateOf(existing?.monthlyAmount?.toDigits().orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameError = submitted && name.isBlank()
    val amountError = submitted && amount.digitsToAmount() <= 0.0

    fun trySave() {
        submitted = true
        if (name.isNotBlank() && amount.digitsToAmount() > 0.0) onSave(name.trim(), amount.digitsToAmount())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuevo ingreso" else "Editar ingreso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej. Sueldo, Freelance)") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Ingresa un nombre") } } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Monto mensual",
                    isError = amountError,
                    supportingText = if (amountError) "El monto debe ser mayor a 0" else null,
                    imeAction = ImeAction.Done,
                    onImeAction = ::trySave,
                )
            }
        },
        confirmButton = { Button(onClick = ::trySave) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

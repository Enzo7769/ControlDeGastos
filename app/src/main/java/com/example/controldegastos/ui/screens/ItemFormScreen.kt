package com.example.controldegastos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.controldegastos.data.ExpenseItem
import com.example.controldegastos.data.ExpenseType
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.ui.FinanceViewModel
import com.example.controldegastos.ui.LocalAppSnackbar
import com.example.controldegastos.ui.Navigator
import com.example.controldegastos.ui.Route
import com.example.controldegastos.ui.components.CategoryIcons
import com.example.controldegastos.ui.components.IconBubble
import com.example.controldegastos.ui.components.MoneyField
import com.example.controldegastos.ui.components.SectionHeader
import com.example.controldegastos.ui.components.digitsToAmount
import com.example.controldegastos.ui.components.toDigits
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.toClp

private fun defaultMonths(type: ExpenseType): Int = if (type == ExpenseType.CREDIT_CARD) 1 else 12

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemFormScreen(viewModel: FinanceViewModel, itemId: String?, navigator: Navigator) {
    val existing = remember(itemId) { itemId?.let(viewModel::item) }
    val isEditing = existing != null
    val snackbar = LocalAppSnackbar.current
    val today = viewModel.today

    var type by rememberSaveable(itemId) { mutableStateOf(existing?.type ?: ExpenseType.CREDIT_CARD) }
    var name by rememberSaveable(itemId) { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable(itemId) { mutableStateOf(existing?.icon ?: CategoryIcons.first().first) }
    var limitText by rememberSaveable(itemId) { mutableStateOf(existing?.creditLimit?.toDigits().orEmpty()) }
    var usedText by rememberSaveable(itemId) { mutableStateOf(existing?.usedQuota?.toDigits().orEmpty()) }
    var firstAmountText by rememberSaveable(itemId) { mutableStateOf("") }
    var months by rememberSaveable(itemId) { mutableIntStateOf(defaultMonths(type)) }
    var monthsTouched by rememberSaveable(itemId) { mutableStateOf(false) }
    var showErrors by rememberSaveable(itemId) { mutableStateOf(false) }

    val limit = limitText.digitsToAmount()
    val used = usedText.digitsToAmount()
    val nameError = name.isBlank()
    val quotaError = type == ExpenseType.CREDIT_CARD && limit > 0.0 && used > limit

    fun save() {
        if (nameError || quotaError) {
            showErrors = true
            return
        }
        val isCard = type == ExpenseType.CREDIT_CARD
        var item = (existing ?: ExpenseItem(name = name.trim())).copy(
            name = name.trim(),
            icon = icon,
            type = type,
            creditLimit = if (isCard) limit else 0.0,
            usedQuota = if (isCard) used else 0.0,
        )
        if (!isEditing) {
            item = FinanceCalculator.addInstallments(item, firstAmountText.digitsToAmount(), months, today)
        }
        viewModel.saveItem(item)
        if (isEditing) navigator.back() else navigator.replace(Route.ItemDetail(item.id))
        snackbar.show("Guardado")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar" else "Nueva deuda o gasto") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                },
                actions = { TextButton(onClick = ::save) { Text("Guardar") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Tipo")
            ExpenseType.entries.forEach { t ->
                val selected = t == type
                Card(
                    onClick = {
                        type = t
                        if (!monthsTouched) months = defaultMonths(t)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(t.label, style = MaterialTheme.typography.titleSmall)
                            Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            SectionHeader("Datos")
            val nameSupport: (@Composable () -> Unit)? =
                if (showErrors && nameError) { { Text("Ingresa un nombre") } } else null
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Nombre") },
                placeholder = { Text("Ej: Tarjeta Banco, Arriendo") },
                singleLine = true,
                isError = showErrors && nameError,
                supportingText = nameSupport,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Ícono", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcons.forEach { (emoji, _) ->
                    val selected = emoji == icon
                    Card(
                        onClick = { icon = emoji },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        IconBubble(
                            emoji,
                            modifier = Modifier.padding(4.dp),
                            container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                }
            }

            if (type == ExpenseType.CREDIT_CARD) {
                SectionHeader("Cupo")
                MoneyField(value = limitText, onValueChange = { limitText = it }, label = "Cupo total")
                MoneyField(
                    value = usedText,
                    onValueChange = { usedText = it },
                    label = "Cupo utilizado (según tu banco)",
                    isError = showErrors && quotaError,
                    supportingText = if (showErrors && quotaError) "No puede ser mayor al cupo total"
                    else "Opcional. Si lo dejas vacío, la deuda se calcula con las cuotas pendientes.",
                )
            }

            if (!isEditing) {
                SectionHeader("Primer pago (opcional)")
                MoneyField(
                    value = firstAmountText,
                    onValueChange = { firstAmountText = it },
                    label = if (type == ExpenseType.CREDIT_CARD) "Monto a pagar este mes" else "Monto mensual",
                    imeAction = ImeAction.Done,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meses", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { months--; monthsTouched = true }, enabled = months > 1) { Text("−") }
                    Text("$months", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
                    FilledTonalIconButton(onClick = { months++; monthsTouched = true }, enabled = months < 72) { Text("+") }
                }
                val amount = firstAmountText.digitsToAmount()
                if (amount > 0.0) {
                    val end = today + (months - 1)
                    Text(
                        if (months == 1) "Se registrará ${amount.toClp()} en ${today.longLabel()}."
                        else "Se registrarán $months pagos de ${amount.toClp()} desde ${today.longLabel()} hasta ${end.longLabel()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Después podrás agregar más cuotas desde el detalle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

package com.example.controldegastos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controldegastos.data.YearMonth
import com.example.controldegastos.ui.theme.FinanceTheme
import com.example.controldegastos.util.groupDigits
import com.example.controldegastos.util.longLabel
import com.example.controldegastos.util.relativeTo

// ---------------------------------------------------------------------------
// Campo de dinero: solo dígitos, muestra separador de miles y prefijo "$".
// El valor se maneja como String de dígitos ("1050000").
// ---------------------------------------------------------------------------

/** Convierte un monto a String de dígitos para usar en [MoneyField] ("" si es 0). */
fun Double.toDigits(): String = if (this <= 0.0) "" else Math.round(this).toString()

/** Convierte el texto de [MoneyField] a monto (0.0 si está vacío). */
fun String.digitsToAmount(): Double = filter(Char::isDigit).toLongOrNull()?.toDouble() ?: 0.0

private object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = groupDigits(raw)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Cantidad de puntos insertados antes de la posición "offset".
                val digitsAfter = raw.length - offset
                val totalDots = if (raw.isEmpty()) 0 else (raw.length - 1) / 3
                val dotsAfter = if (digitsAfter <= 0) 0 else (digitsAfter - 1) / 3
                return (offset + totalDots - dotsAfter).coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int =
                out.take(offset.coerceIn(0, out.length)).count { it.isDigit() }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter(Char::isDigit).take(12).trimStart('0')) },
        label = { Text(label) },
        prefix = { Text("$ ") },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = ThousandsTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Selector de mes con flechas.
// ---------------------------------------------------------------------------

@Composable
fun MonthSwitcher(
    month: YearMonth,
    today: YearMonth,
    onChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    min: YearMonth? = null,
    max: YearMonth? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onChange(month - 1) }, enabled = min == null || month > min) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior", tint = contentColor)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(month.longLabel(), style = MaterialTheme.typography.titleMedium, color = contentColor)
            TextButton(
                onClick = { onChange(today) },
                enabled = month != today,
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                modifier = Modifier.height(28.dp),
            ) {
                Text(
                    if (month == today) "Este mes" else "${month.relativeTo(today)} · Volver a hoy",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.75f),
                )
            }
        }
        IconButton(onClick = { onChange(month + 1) }, enabled = max == null || month < max) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente", tint = contentColor)
        }
    }
}

// ---------------------------------------------------------------------------
// Ícono (emoji) dentro de un círculo de color suave.
// ---------------------------------------------------------------------------

@Composable
fun IconBubble(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = (size.value * 0.45f).sp, textAlign = TextAlign.Center)
    }
}

/** Íconos disponibles para tarjetas y gastos. */
val CategoryIcons: List<Pair<String, String>> = listOf(
    "💳" to "Tarjeta",
    "🏦" to "Banco",
    "📄" to "Crédito",
    "🏠" to "Hogar",
    "🛒" to "Supermercado",
    "🚗" to "Auto",
    "📱" to "Teléfono",
    "💡" to "Servicios",
    "🎓" to "Estudios",
    "🩺" to "Salud",
    "✈️" to "Viajes",
    "🎁" to "Otros",
)

// ---------------------------------------------------------------------------
// Encabezados, estados vacíos y diálogos.
// ---------------------------------------------------------------------------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = if (destructive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) else ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------------------------------------------------------------------------
// Indicadores y gráficos.
// ---------------------------------------------------------------------------

/** Verde < 50 %, naranjo < 80 %, rojo ≥ 80 %. */
@Composable
@ReadOnlyComposable
fun usageColor(ratio: Float): Color = when {
    ratio < 0.5f -> FinanceTheme.colors.positive
    ratio < 0.8f -> FinanceTheme.colors.warning
    else -> FinanceTheme.colors.negative
}

@Composable
fun UsageBar(ratio: Float, modifier: Modifier = Modifier, color: Color = usageColor(ratio)) {
    LinearProgressIndicator(
        progress = { ratio.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Gráfico de dona. [slices] son fracciones (que suman ~1) con su color. */
@Composable
fun DonutChart(
    slices: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    thickness: Dp = 18.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    center: @Composable () -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = thickness.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val gap = if (slices.size > 1) 2f else 0f
            var start = -90f
            slices.forEach { (fraction, color) ->
                val sweep = 360f * fraction
                if (sweep > gap) {
                    drawArc(color, start + gap / 2, sweep - gap, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                }
                start += sweep
            }
        }
        center()
    }
}

/** Punto de color para leyendas. */
@Composable
fun LegendDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(color))
}

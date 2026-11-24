package com.example.recordatorios.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.theme.PrioridadAlta
import com.example.recordatorios.ui.theme.PrioridadBaja
import com.example.recordatorios.ui.theme.PrioridadMedia
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import java.time.format.DateTimeFormatter

val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/**
 * Componente reutilizable que representa UN recordatorio en la lista.
 * No sabe nada de navegación: solo avisa con onClick que lo tocaron.
 */
@Composable
fun TarjetaRecordatorio(
    recordatorio: Recordatorio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estilo = RepositorioSimulado.estiloDe(recordatorio.categoria)
    val colorCategoria = Color(estilo.colorArgb)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja con el color de la categoría (sale del Map de la Actividad 1)
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(colorCategoria)
            )
            Column(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                recordatorio.descripcion?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                FilaEtiquetas(recordatorio)
                textoFecha(recordatorio)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Categoría, prioridad y estado como "pastillas". Se usa en la tarjeta y en el detalle. */
@Composable
fun FilaEtiquetas(recordatorio: Recordatorio, modifier: Modifier = Modifier) {
    val estilo = RepositorioSimulado.estiloDe(recordatorio.categoria)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Etiqueta("${estilo.etiqueta} · ${recordatorio.categoria.nombre}", Color(estilo.colorArgb))
        Etiqueta(recordatorio.prioridad.etiqueta, colorPrioridad(recordatorio.prioridad))
        Etiqueta(recordatorio.estado.etiqueta, MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun Etiqueta(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> PrioridadAlta
    Prioridad.Media -> PrioridadMedia
    Prioridad.Baja -> PrioridadBaja
}

/** Texto corto según el estado (when exhaustivo sobre la sealed class). */
private fun textoFecha(r: Recordatorio): String? = when (val estado = r.estado) {
    is EstadoRecordatorio.Completado -> "Completado el ${estado.fechaCompletado.format(formatoFecha)}"
    is EstadoRecordatorio.Pospuesto -> "Pospuesto hasta ${estado.hasta.format(formatoFecha)}"
    EstadoRecordatorio.Pendiente, EstadoRecordatorio.Vencido ->
        r.fechaLimite?.let { "Vence: ${it.format(formatoFecha)}" }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaRecordatorioPreview() {
    RecordatoriosTheme {
        TarjetaRecordatorio(
            recordatorio = RepositorioSimulado.recordatorios().first(),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

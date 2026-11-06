package com.example.recordatorios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.cantidadPorEstado
import com.example.recordatorios.data.filtrarPorEstado
import com.example.recordatorios.data.filtrarPorPrioridad
import com.example.recordatorios.data.ordenados
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstadoUi
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.theme.PrioridadAlta
import com.example.recordatorios.ui.theme.PrioridadBaja
import com.example.recordatorios.ui.theme.PrioridadMedia
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/** Opciones del filtro de estado: null = todos. */
private val opcionesEstado: List<Pair<KClass<out EstadoRecordatorio>?, String>> = listOf(
    null to "Todos",
    EstadoRecordatorio.Pendiente::class to "Pendiente",
    EstadoRecordatorio.Completado::class to "Completado",
    EstadoRecordatorio.Pospuesto::class to "Pospuesto",
    EstadoRecordatorio.Vencido::class to "Vencido"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecordatorios(estadoUi: EstadoUi, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (estadoUi) {
                is EstadoUi.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is EstadoUi.Vacio -> MensajeCentrado(stringResource(R.string.sin_resultados))
                is EstadoUi.Error -> MensajeCentrado("Error: ${estadoUi.mensaje}")
                is EstadoUi.Exito -> ContenidoRecordatorios(estadoUi.recordatorios)
            }
        }
    }
}

@Composable
private fun ContenidoRecordatorios(recordatorios: List<Recordatorio>) {
    var filtroEstado by remember { mutableStateOf<KClass<out EstadoRecordatorio>?>(null) }
    var filtroPrioridad by remember { mutableStateOf<Prioridad?>(null) }

    val visibles = recordatorios
        .filtrarPorEstado(filtroEstado)
        .filtrarPorPrioridad(filtroPrioridad)
        .ordenados()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Resumen(recordatorios.cantidadPorEstado(), recordatorios.size) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.filtro_estado), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(opcionesEstado) { (clase, etiqueta) ->
                        FilterChip(
                            selected = filtroEstado == clase,
                            onClick = { filtroEstado = clase },
                            label = { Text(etiqueta) }
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.filtro_prioridad), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filtroPrioridad == null,
                            onClick = { filtroPrioridad = null },
                            label = { Text(stringResource(R.string.todos)) }
                        )
                    }
                    items(Prioridad.todas) { prioridad ->
                        FilterChip(
                            selected = filtroPrioridad == prioridad,
                            onClick = { filtroPrioridad = prioridad },
                            label = { Text(prioridad.etiqueta) }
                        )
                    }
                }
            }
        }

        if (visibles.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.sin_resultados),
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(visibles, key = { it.id }) { TarjetaRecordatorio(it) }
        }
    }
}

@Composable
private fun Resumen(conteo: Map<String, Int>, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total: $total recordatorios", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                conteo.entries.joinToString("  ·  ") { "${it.key}: ${it.value}" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TarjetaRecordatorio(recordatorio: Recordatorio, modifier: Modifier = Modifier) {
    val estilo = RepositorioSimulado.estiloDe(recordatorio.categoria)
    val colorCategoria = Color(estilo.colorArgb)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral con el color de la categoría (sale del Map)
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
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etiqueta("${estilo.etiqueta} · ${recordatorio.categoria.nombre}", colorCategoria)
                    Etiqueta(recordatorio.prioridad.etiqueta, colorPrioridad(recordatorio.prioridad))
                    Etiqueta(
                        recordatorio.estado.etiqueta,
                        MaterialTheme.colorScheme.secondary
                    )
                }
                detalleEstado(recordatorio)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(
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

@Composable
private fun MensajeCentrado(texto: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(texto, modifier = Modifier.padding(24.dp))
    }
}

private fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> PrioridadAlta
    Prioridad.Media -> PrioridadMedia
    Prioridad.Baja -> PrioridadBaja
}

/** Texto extra según el estado y los datos opcionales (when exhaustivo sobre sealed class). */
private fun detalleEstado(r: Recordatorio): String? {
    val partes = mutableListOf<String>()
    when (val estado = r.estado) {
        is EstadoRecordatorio.Completado -> partes += "Completado el ${estado.fechaCompletado.format(formatoFecha)}"
        is EstadoRecordatorio.Pospuesto -> partes += "Pospuesto hasta ${estado.hasta.format(formatoFecha)}"
        EstadoRecordatorio.Pendiente, EstadoRecordatorio.Vencido ->
            r.fechaLimite?.let { partes += "Vence: ${it.format(formatoFecha)}" }
    }
    r.ubicacion?.let { partes += "📍 ${it.nombre}" }
    r.minutosAvisoPrevio?.let { partes += "🔔 $it min antes" }
    return partes.takeIf { it.isNotEmpty() }?.joinToString("   ")
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun VistaPreviaPantalla() {
    RecordatoriosTheme {
        PantallaRecordatorios(EstadoUi.Exito(RepositorioSimulado.recordatorios()))
    }
}

@Preview(showBackground = true)
@Composable
private fun VistaPreviaVacia() {
    RecordatoriosTheme { PantallaRecordatorios(EstadoUi.Vacio) }
}

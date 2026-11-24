package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.components.FilaEtiquetas
import com.example.recordatorios.ui.components.IndicadorEstado
import com.example.recordatorios.ui.components.TipoIndicador
import com.example.recordatorios.ui.components.formatoFecha
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun DetalleRoute(
    onVolver: () -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetalleScreen(
        uiState = uiState,
        onVolver = onVolver,
        onReintentar = viewModel::cargar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState) {
                DetalleUiState.Cargando -> IndicadorEstado(
                    tipo = TipoIndicador.CARGA,
                    mensaje = "Cargando recordatorio..."
                )

                is DetalleUiState.Encontrado -> DetalleContenido(uiState.recordatorio)

                is DetalleUiState.NoEncontrado -> IndicadorEstado(
                    tipo = TipoIndicador.VACIO,
                    titulo = "No encontrado",
                    mensaje = "No existe un recordatorio con el id ${uiState.id}.",
                    textoAccion = "Volver al listado",
                    onAccion = onVolver
                )

                is DetalleUiState.Error -> IndicadorEstado(
                    tipo = TipoIndicador.ERROR,
                    titulo = "Algo salió mal",
                    mensaje = uiState.mensaje,
                    textoAccion = "Reintentar",
                    onAccion = onReintentar
                )
            }
        }
    }
}

@Composable
private fun DetalleContenido(recordatorio: Recordatorio, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(recordatorio.titulo, style = MaterialTheme.typography.headlineSmall)
        FilaEtiquetas(recordatorio)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilaDato("Descripción", recordatorio.descripcion ?: "Sin descripción")
                HorizontalDivider()
                FilaDato("Categoría", recordatorio.categoria.nombre)
                FilaDato("Prioridad", recordatorio.prioridad.etiqueta)
                FilaDato("Estado", textoEstado(recordatorio.estado))
                HorizontalDivider()
                FilaDato("Fecha límite", recordatorio.fechaLimite?.format(formatoFecha) ?: "Sin fecha")
                FilaDato(
                    "Ubicación",
                    recordatorio.ubicacion?.let { "${it.nombre} (${it.latitud}, ${it.longitud})" }
                        ?: "Sin ubicación"
                )
                FilaDato(
                    "Aviso",
                    recordatorio.minutosAvisoPrevio?.let { "$it minutos antes" } ?: "Sin aviso"
                )
                HorizontalDivider()
                FilaDato("ID", recordatorio.id.toString())
            }
        }
    }
}

/** Una fila "etiqueta: valor" del detalle. */
@Composable
private fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    EstadoRecordatorio.Vencido -> "Vencido"
    is EstadoRecordatorio.Completado -> "Completado el ${estado.fechaCompletado.format(formatoFecha)}"
    is EstadoRecordatorio.Pospuesto -> "Pospuesto hasta ${estado.hasta.format(formatoFecha)}"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DetallePreview() {
    RecordatoriosTheme {
        DetalleScreen(
            uiState = DetalleUiState.Encontrado(RepositorioSimulado.recordatorios()[1]),
            onVolver = {},
            onReintentar = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalleNoEncontradoPreview() {
    RecordatoriosTheme {
        DetalleScreen(uiState = DetalleUiState.NoEncontrado(99), onVolver = {}, onReintentar = {})
    }
}

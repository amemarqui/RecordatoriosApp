package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.components.FilaEtiquetas
import com.example.recordatorios.ui.components.IndicadorEstado
import com.example.recordatorios.ui.components.TipoIndicador
import com.example.recordatorios.ui.components.formatoFecha
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun DetalleRoute(
    onVolver: () -> Unit,
    onEditar: (String) -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Después de eliminar, se vuelve al listado una sola vez
    LaunchedEffect(uiState) {
        if (uiState is DetalleUiState.Eliminado) onVolver()
    }

    DetalleScreen(
        uiState = uiState,
        onVolver = onVolver,
        onEditar = { onEditar(viewModel.idRecordatorio) },
        onEliminar = viewModel::onEliminar,
        onCambiarCompletado = viewModel::onCambiarCompletado,
        onReintentarSincronizacion = viewModel::onReintentarSincronizacion,
        onReintentar = viewModel::onReintentar,
        onMensajeMostrado = viewModel::onMensajeMostrado
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCambiarCompletado: (Boolean) -> Unit,
    onReintentarSincronizacion: () -> Unit,
    onReintentar: () -> Unit,
    onMensajeMostrado: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarEliminacion by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val encontrado = uiState as? DetalleUiState.Encontrado

    LaunchedEffect(encontrado?.mensaje) {
        encontrado?.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (encontrado != null) {
                        IconButton(onClick = onEditar, enabled = !encontrado.procesando) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { confirmarEliminacion = true }, enabled = !encontrado.procesando) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
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
                DetalleUiState.Cargando, DetalleUiState.Eliminado -> IndicadorEstado(
                    tipo = TipoIndicador.CARGA,
                    mensaje = "Cargando recordatorio..."
                )

                is DetalleUiState.Encontrado -> DetalleContenido(
                    estado = uiState,
                    onCambiarCompletado = onCambiarCompletado,
                    onReintentarSincronizacion = onReintentarSincronizacion
                )

                is DetalleUiState.NoEncontrado -> IndicadorEstado(
                    tipo = TipoIndicador.VACIO,
                    titulo = "No encontrado",
                    mensaje = "Este recordatorio ya no existe.",
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

    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("Se borra del teléfono ahora y del servidor en la próxima sincronización.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminacion = false
                    onEliminar()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminacion = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DetalleContenido(
    estado: DetalleUiState.Encontrado,
    onCambiarCompletado: (Boolean) -> Unit,
    onReintentarSincronizacion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recordatorio = estado.recordatorio
    val completado = recordatorio.estado is EstadoRecordatorio.Completado

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
                FilaDato("Creado", recordatorio.fechaCreacion?.format(formatoFecha) ?: "-")
                FilaDato(
                    "Ubicación",
                    recordatorio.ubicacion?.let { "${it.nombre} (${it.latitud}, ${it.longitud})" }
                        ?: "Sin ubicación"
                )
                FilaDato(
                    "Aviso",
                    recordatorio.minutosAvisoPrevio?.let { "$it minutos antes" } ?: "Sin aviso"
                )
            }
        }

        TarjetaSincronizacion(recordatorio, onReintentarSincronizacion, habilitado = !estado.procesando)

        if (completado) {
            OutlinedButton(
                onClick = { onCambiarCompletado(false) },
                enabled = !estado.procesando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Marcar como pendiente") }
        } else {
            Button(
                onClick = { onCambiarCompletado(true) },
                enabled = !estado.procesando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Marcar como completado")
            }
        }
    }
}

/** Muestra si el registro ya está confirmado por el servidor, pendiente o con error. */
@Composable
private fun TarjetaSincronizacion(
    recordatorio: Recordatorio,
    onReintentar: () -> Unit,
    habilitado: Boolean,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (recordatorio.sincronizacion) {
        EstadoSincronizacion.SINCRONIZADO -> colores.surfaceVariant to "Confirmado por el servidor."
        EstadoSincronizacion.PENDIENTE -> colores.tertiaryContainer to
            "Guardado en el teléfono. Pendiente de enviar al servidor."
        EstadoSincronizacion.ERROR -> colores.errorContainer to
            "El servidor rechazó este cambio. Revisalo y reintentá."
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sincronización: ${recordatorio.sincronizacion.etiqueta}", style = MaterialTheme.typography.titleSmall)
            Text(texto, style = MaterialTheme.typography.bodySmall)
            recordatorio.errorSincronizacion?.let {
                Text("Último intento: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (recordatorio.sincronizacion == EstadoSincronizacion.ERROR) {
                TextButton(onClick = onReintentar, enabled = habilitado) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar sincronización")
                }
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
            uiState = DetalleUiState.Encontrado(
                RepositorioSimulado.recordatorios()[1].copy(sincronizacion = EstadoSincronizacion.PENDIENTE)
            ),
            onVolver = {}, onEditar = {}, onEliminar = {}, onCambiarCompletado = {},
            onReintentarSincronizacion = {}, onReintentar = {}, onMensajeMostrado = {}
        )
    }
}

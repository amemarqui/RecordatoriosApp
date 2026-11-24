package com.example.recordatorios.ui.crear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.ErroresFormulario
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.components.CampoTextoValidado
import com.example.recordatorios.ui.components.SelectorCategoria
import com.example.recordatorios.ui.components.SelectorPrioridad
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun CrearRecordatorioRoute(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit,
    viewModel: CrearRecordatorioViewModel = viewModel(factory = CrearRecordatorioViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando el guardado termina bien, se avisa UNA vez para volver al listado
    LaunchedEffect(uiState.envio) {
        if (uiState.envio is EstadoEnvio.Exitoso) onGuardado()
    }

    CrearRecordatorioScreen(
        uiState = uiState,
        onTituloChange = viewModel::onTituloChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onCategoriaSeleccionada = viewModel::onCategoriaSeleccionada,
        onPrioridadSeleccionada = viewModel::onPrioridadSeleccionada,
        onGuardar = viewModel::onGuardar,
        onCancelar = onCancelar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearRecordatorioScreen(
    uiState: CrearUiState,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaSeleccionada: (Categoria) -> Unit,
    onPrioridadSeleccionada: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Nuevo recordatorio") },
                navigationIcon = {
                    IconButton(onClick = onCancelar, enabled = !uiState.enviando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        FormularioRecordatorio(
            estado = uiState,
            onTituloChange = onTituloChange,
            onDescripcionChange = onDescripcionChange,
            onCategoriaSeleccionada = onCategoriaSeleccionada,
            onPrioridadSeleccionada = onPrioridadSeleccionada,
            onGuardar = onGuardar,
            onCancelar = onCancelar,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/** El formulario en sí. Todos los campos son controlados (valor + lambda). */
@Composable
fun FormularioRecordatorio(
    estado: CrearUiState,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaSeleccionada: (Categoria) -> Unit,
    onPrioridadSeleccionada: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habilitado = !estado.enviando

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()                       // que el teclado no tape los botones
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CampoTextoValidado(
            valor = estado.titulo,
            onValorChange = onTituloChange,
            etiqueta = "Título *",
            mensajeError = estado.errores.titulo,
            habilitado = habilitado
        )

        CampoTextoValidado(
            valor = estado.descripcion,
            onValorChange = onDescripcionChange,
            etiqueta = "Descripción (opcional)",
            mensajeError = estado.errores.descripcion,
            maxCaracteres = Recordatorio.MAX_DESCRIPCION,
            unaLinea = false,
            minLineas = 3,
            habilitado = habilitado
        )

        SelectorCategoria(
            categorias = estado.categoriasDisponibles,
            seleccionada = estado.categoria,
            onSeleccionar = onCategoriaSeleccionada,
            mensajeError = estado.errores.categoria,
            habilitado = habilitado
        )

        SelectorPrioridad(
            seleccionada = estado.prioridad,
            onSeleccionar = onPrioridadSeleccionada,
            mensajeError = estado.errores.prioridad,
            habilitado = habilitado
        )

        // Error general de guardado (no es de un campo en particular)
        val envio = estado.envio
        if (envio is EstadoEnvio.Error) {
            ErrorGuardado(envio.mensaje)
        }

        Text(
            "* Campos obligatorios",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            OutlinedButton(onClick = onCancelar, enabled = habilitado) {
                Text("Cancelar")
            }
            Button(onClick = onGuardar, enabled = estado.puedeGuardar) {
                if (estado.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun ErrorGuardado(mensaje: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Error al guardar", style = MaterialTheme.typography.titleSmall)
                Text("$mensaje Podés tocar Guardar para reintentar.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/* ---------- Vistas previas ---------- */

@Preview(showBackground = true, showSystemUi = true, name = "Formulario con errores")
@Composable
private fun FormularioConErroresPreview() {
    RecordatoriosTheme {
        CrearRecordatorioScreen(
            uiState = CrearUiState(
                titulo = "Ab",
                categoriasDisponibles = RepositorioSimulado.categorias.toList(),
                errores = ErroresFormulario(
                    titulo = "El título debe tener al menos 4 caracteres",
                    categoria = "Seleccioná una categoría",
                    prioridad = "Seleccioná una prioridad"
                )
            ),
            onTituloChange = {}, onDescripcionChange = {}, onCategoriaSeleccionada = {},
            onPrioridadSeleccionada = {}, onGuardar = {}, onCancelar = {}
        )
    }
}

@Preview(showBackground = true, name = "Guardando")
@Composable
private fun FormularioGuardandoPreview() {
    RecordatoriosTheme {
        CrearRecordatorioScreen(
            uiState = CrearUiState(
                titulo = "Estudiar para el parcial",
                descripcion = "Repasar Navigation Compose y ViewModel",
                categoria = RepositorioSimulado.ESTUDIO,
                prioridad = Prioridad.Alta,
                categoriasDisponibles = RepositorioSimulado.categorias.toList(),
                formularioValido = true,
                envio = EstadoEnvio.Enviando
            ),
            onTituloChange = {}, onDescripcionChange = {}, onCategoriaSeleccionada = {},
            onPrioridadSeleccionada = {}, onGuardar = {}, onCancelar = {}
        )
    }
}

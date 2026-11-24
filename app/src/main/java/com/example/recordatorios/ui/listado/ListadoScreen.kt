package com.example.recordatorios.ui.listado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.R
import com.example.recordatorios.data.Escenario
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.cantidadPorEstado
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.components.IndicadorEstado
import com.example.recordatorios.ui.components.TarjetaRecordatorio
import com.example.recordatorios.ui.components.TipoIndicador
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import kotlin.reflect.KClass

/** Opciones del filtro de estado: null = todos. */
private val opcionesEstado: List<Pair<KClass<out EstadoRecordatorio>?, String>> = listOf(
    null to "Todos",
    EstadoRecordatorio.Pendiente::class to "Pendiente",
    EstadoRecordatorio.Completado::class to "Completado",
    EstadoRecordatorio.Pospuesto::class to "Pospuesto",
    EstadoRecordatorio.Vencido::class to "Vencido"
)

/**
 * "Route": conecta el ViewModel con la pantalla. Acá se recolecta el StateFlow
 * con collectAsStateWithLifecycle (deja de recolectar cuando la app está en segundo plano).
 */
@Composable
fun ListadoRoute(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Int) -> Unit,
    viewModel: ListadoViewModel = viewModel(factory = ListadoViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ListadoScreen(
        uiState = uiState,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::onReintentar,
        onFiltroEstadoChange = viewModel::onFiltroEstadoChange,
        onFiltroPrioridadChange = viewModel::onFiltroPrioridadChange,
        onSimularEscenario = viewModel::onSimularEscenario
    )
}

/** Pantalla "sin estado": recibe el UiState y avisa todo con lambdas. Se puede previsualizar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    uiState: ListadoUiState,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Int) -> Unit,
    onReintentar: () -> Unit,
    onFiltroEstadoChange: (KClass<out EstadoRecordatorio>?) -> Unit,
    onFiltroPrioridadChange: (Prioridad?) -> Unit,
    onSimularEscenario: (Escenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                actions = { MenuSimulacion(onSimularEscenario) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Icon(Icons.Filled.Add, contentDescription = "Crear recordatorio")
            }
        }
    ) { innerPadding ->
        // innerPadding del Scaffold: evita que el contenido quede debajo de la TopAppBar
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // when exhaustivo: se muestra UNA sola cosa por vez
            when (uiState) {
                ListadoUiState.Cargando -> IndicadorEstado(
                    tipo = TipoIndicador.CARGA,
                    mensaje = "Cargando recordatorios..."
                )

                ListadoUiState.Vacio -> IndicadorEstado(
                    tipo = TipoIndicador.VACIO,
                    titulo = "Todavía no hay recordatorios",
                    mensaje = "Cuando crees uno va a aparecer en esta lista.",
                    textoAccion = "Crear recordatorio",
                    onAccion = onCrearRecordatorio
                )

                is ListadoUiState.Error -> IndicadorEstado(
                    tipo = TipoIndicador.ERROR,
                    titulo = "Algo salió mal",
                    mensaje = uiState.mensaje,
                    textoAccion = "Reintentar",
                    onAccion = onReintentar
                )

                is ListadoUiState.ConDatos -> ListaRecordatorios(
                    estado = uiState,
                    onRecordatorioClick = onRecordatorioClick,
                    onFiltroEstadoChange = onFiltroEstadoChange,
                    onFiltroPrioridadChange = onFiltroPrioridadChange
                )
            }
        }
    }
}

@Composable
private fun ListaRecordatorios(
    estado: ListadoUiState.ConDatos,
    onRecordatorioClick: (Int) -> Unit,
    onFiltroEstadoChange: (KClass<out EstadoRecordatorio>?) -> Unit,
    onFiltroPrioridadChange: (Prioridad?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // abajo dejo más espacio para que el FAB no tape la última tarjeta
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Resumen(estado.recordatorios) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.filtro_estado), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(opcionesEstado) { (clase, etiqueta) ->
                        FilterChip(
                            selected = estado.filtroEstado == clase,
                            onClick = { onFiltroEstadoChange(clase) },
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
                            selected = estado.filtroPrioridad == null,
                            onClick = { onFiltroPrioridadChange(null) },
                            label = { Text(stringResource(R.string.todos)) }
                        )
                    }
                    items(Prioridad.todas) { prioridad ->
                        FilterChip(
                            selected = estado.filtroPrioridad == prioridad,
                            onClick = { onFiltroPrioridadChange(prioridad) },
                            label = { Text(prioridad.etiqueta) }
                        )
                    }
                }
            }
        }

        if (estado.visibles.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.sin_resultados),
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(estado.visibles, key = { it.id }) { recordatorio ->
                TarjetaRecordatorio(
                    recordatorio = recordatorio,
                    onClick = { onRecordatorioClick(recordatorio.id) }
                )
            }
        }
    }
}

@Composable
private fun Resumen(recordatorios: List<Recordatorio>, modifier: Modifier = Modifier) {
    val conteo = recordatorios.cantidadPorEstado()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total: ${recordatorios.size} recordatorios", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                conteo.entries.joinToString("  ·  ") { "${it.key}: ${it.value}" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Menú ⋮ para probar los estados (consigna 3: mecanismo de simulación). */
@Composable
private fun MenuSimulacion(onSimularEscenario: (Escenario) -> Unit) {
    // Esto es estado del propio elemento visual (menú abierto/cerrado), por eso queda en la UI
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Simular escenarios")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Text(
                "Simular escenario",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Escenario.entries.forEach { escenario ->
                DropdownMenuItem(
                    text = { Text(escenario.descripcion) },
                    onClick = {
                        abierto = false
                        onSimularEscenario(escenario)
                    }
                )
            }
        }
    }
}

/* ---------- Vistas previas de cada estado ---------- */

@Composable
private fun PreviewListado(estado: ListadoUiState) {
    RecordatoriosTheme {
        ListadoScreen(
            uiState = estado,
            onCrearRecordatorio = {},
            onRecordatorioClick = {},
            onReintentar = {},
            onFiltroEstadoChange = {},
            onFiltroPrioridadChange = {},
            onSimularEscenario = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Con datos")
@Composable
private fun ListadoConDatosPreview() {
    val datos = RepositorioSimulado.recordatorios()
    PreviewListado(ListadoUiState.ConDatos(recordatorios = datos, visibles = datos))
}

@Preview(showBackground = true, name = "Vacío")
@Composable
private fun ListadoVacioPreview() = PreviewListado(ListadoUiState.Vacio)

@Preview(showBackground = true, name = "Error")
@Composable
private fun ListadoErrorPreview() =
    PreviewListado(ListadoUiState.Error("No se pudieron cargar los recordatorios."))

@Preview(showBackground = true, name = "Cargando")
@Composable
private fun ListadoCargandoPreview() = PreviewListado(ListadoUiState.Cargando)

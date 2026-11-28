package com.example.recordatorios.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.preferencias.NombresPreferencia
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.data.remote.simulado.ModoServidor
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun AjustesRoute(
    onVolver: () -> Unit,
    viewModel: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory)
) {
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    val modoServidor by viewModel.modoServidor.collectAsStateWithLifecycle()
    val cuenta by viewModel.cuenta.collectAsStateWithLifecycle()

    AjustesScreen(
        preferencias = preferencias,
        modoServidor = modoServidor,
        cuenta = cuenta,
        accionesCuenta = AccionesCuenta(
            onPedirCierre = viewModel::onPedirCierreSesion,
            onCancelarCierre = viewModel::onCancelarCierreSesion,
            onConfirmarCierre = viewModel::onConfirmarCierreSesion,
            onUidPruebaChange = viewModel::onUidPruebaChange,
            onProbarAcceso = viewModel::onProbarAccesoAjeno
        ),
        onVolver = onVolver,
        onTemaChange = viewModel::onTemaChange,
        onMostrarCompletadosChange = viewModel::onMostrarCompletadosChange,
        onOrdenChange = viewModel::onOrdenChange,
        onRestablecer = viewModel::onRestablecer,
        onGuardarValorInvalido = viewModel::onGuardarValorInvalido,
        onModoServidorChange = viewModel::onModoServidorChange
    )
}

/** Lambdas de la sección Cuenta agrupadas, para no alargar más la firma de la pantalla. */
data class AccionesCuenta(
    val onPedirCierre: () -> Unit = {},
    val onCancelarCierre: () -> Unit = {},
    val onConfirmarCierre: () -> Unit = {},
    val onUidPruebaChange: (String) -> Unit = {},
    val onProbarAcceso: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    preferencias: PreferenciasUsuario,
    modoServidor: ModoServidor?,
    cuenta: CuentaUiState,
    accionesCuenta: AccionesCuenta,
    onVolver: () -> Unit,
    onTemaChange: (TemaApp) -> Unit,
    onMostrarCompletadosChange: (Boolean) -> Unit,
    onOrdenChange: (OrdenRecordatorios) -> Unit,
    onRestablecer: () -> Unit,
    onGuardarValorInvalido: () -> Unit,
    onModoServidorChange: (ModoServidor) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Estas opciones se guardan en el teléfono con DataStore y se mantienen al cerrar la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AvisosPreferencias(preferencias)

            SeccionCuenta(cuenta, accionesCuenta)

            HorizontalDivider()
            Seccion("Apariencia")
            GrupoOpciones(
                titulo = "Tema" + marcaPredeterminado(preferencias, NombresPreferencia.TEMA),
                opciones = TemaApp.entries,
                seleccionada = preferencias.tema,
                texto = { if (it == TemaApp.SISTEMA) "Sistema" else it.etiqueta },
                onSeleccionar = onTemaChange
            )

            HorizontalDivider()
            Seccion("Listado")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferencias.mostrarCompletados,
                        role = Role.Switch,
                        onValueChange = onMostrarCompletadosChange
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mostrar completados" +
                        marcaPredeterminado(preferencias, NombresPreferencia.MOSTRAR_COMPLETADOS),
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = preferencias.mostrarCompletados, onCheckedChange = null)
            }
            GrupoOpciones(
                titulo = "Ordenar por" + marcaPredeterminado(preferencias, NombresPreferencia.ORDEN),
                opciones = OrdenRecordatorios.entries,
                seleccionada = preferencias.orden,
                texto = {
                    when (it) {
                        OrdenRecordatorios.PRIORIDAD -> "Prioridad"
                        OrdenRecordatorios.FECHA_LIMITE -> "Fecha límite"
                        OrdenRecordatorios.TITULO -> "Título A-Z"
                        OrdenRecordatorios.RECIENTES -> "Más recientes"
                    }
                },
                onSeleccionar = onOrdenChange
            )

            HorizontalDivider()
            Seccion("Pruebas de preferencias")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onGuardarValorInvalido, modifier = Modifier.weight(1f)) {
                    Text("Valor inválido", maxLines = 1)
                }
                OutlinedButton(onClick = onRestablecer, modifier = Modifier.weight(1f)) {
                    Text("Restablecer", maxLines = 1)
                }
            }

            if (modoServidor != null) {
                HorizontalDivider()
                Seccion("Servidor simulado")
                Text(
                    "Para probar errores de red. Vuelve a \"Normal\" al cerrar la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GrupoOpciones(
                    titulo = "Cómo responde el servidor",
                    opciones = ModoServidor.entries,
                    seleccionada = modoServidor,
                    texto = {
                        when (it) {
                            ModoServidor.NORMAL -> "Normal"
                            ModoServidor.SIN_CONEXION -> "Sin conexión"
                            ModoServidor.TIMEOUT -> "Timeout"
                            ModoServidor.ERROR_500 -> "Error 500"
                            ModoServidor.SOLICITUD_INVALIDA -> "Rechaza (400)"
                        }
                    },
                    onSeleccionar = onModoServidorChange
                )
            }
        }
    }
}

/** Avisa si alguna preferencia usó el valor predeterminado por un dato inválido o un error de lectura. */
@Composable
private fun AvisosPreferencias(preferencias: PreferenciasUsuario) {
    val avisos = buildList {
        if (preferencias.invalidas.isNotEmpty()) {
            add("Se encontró un valor guardado no válido en: ${preferencias.invalidas.joinToString()}. Se usó el valor predeterminado.")
        }
        if (preferencias.errorLectura) {
            add("No se pudieron leer las preferencias guardadas. Se usan los valores predeterminados.")
        }
    }
    if (avisos.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column { avisos.forEach { Text(it, style = MaterialTheme.typography.bodySmall) } }
        }
    }
}

private fun marcaPredeterminado(preferencias: PreferenciasUsuario, nombre: String): String = when (nombre) {
    in preferencias.invalidas -> " (predeterminado: dato inválido)"
    in preferencias.porDefecto -> " (predeterminado)"
    else -> ""
}

@Composable
private fun Seccion(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

/**
 * Opciones como chips (se elige una). Ocupan poco espacio y toda la opción es tocable.
 * El chip elegido muestra un tilde, así no depende solo del color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> GrupoOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T,
    texto: (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.selectableGroup()) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                FilterChip(
                    selected = elegida,
                    onClick = { onSeleccionar(opcion) },
                    label = { Text(texto(opcion)) },
                    leadingIcon = if (elegida) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.semantics { role = Role.RadioButton }
                )
            }
        }
    }
}

/** Actividad 4: cuenta activa, cierre de sesión y prueba de acceso a otra cuenta. */
@Composable
private fun SeccionCuenta(cuenta: CuentaUiState, acciones: AccionesCuenta) {
    Seccion("Cuenta")
    SelectionContainer {
        Column {
            Text(cuenta.email ?: "Sin email", style = MaterialTheme.typography.bodyLarge)
            Text(
                "uid: ${cuenta.uid ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (cuenta.errorCierre != null) {
        Text("No se pudo cerrar la sesión: ${cuenta.errorCierre}", color = MaterialTheme.colorScheme.error)
    }
    OutlinedButton(
        onClick = acciones.onPedirCierre,
        enabled = !cuenta.cerrandoSesion,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (cuenta.cerrandoSesion) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Cerrando sesión…")
        } else {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }

    if (cuenta.confirmandoCierre) {
        AlertDialog(
            onDismissRequest = { if (!cuenta.cerrandoSesion) acciones.onCancelarCierre() },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    if (cuenta.cambiosPendientes > 0) {
                        "Tenés ${cuenta.cambiosPendientes} cambio(s) sin subir. Antes de salir se intenta " +
                            "sincronizarlos; si no hay conexión quedan guardados en este teléfono y solo " +
                            "se suben cuando vuelvas a entrar con esta cuenta."
                    } else {
                        "Tus recordatorios ya están en la nube. Se borran de este teléfono y vuelven " +
                            "a aparecer cuando inicies sesión de nuevo."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = acciones.onConfirmarCierre, enabled = !cuenta.cerrandoSesion) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = acciones.onCancelarCierre, enabled = !cuenta.cerrandoSesion) {
                    Text("Cancelar")
                }
            }
        )
    }

    HorizontalDivider()
    Seccion("Prueba de seguridad")
    Text(
        "Pegá el uid de OTRA cuenta. La app intenta leer y escribir sus recordatorios " +
            "directamente en Firestore: las reglas del servidor tienen que rechazarlo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = cuenta.uidPrueba,
        onValueChange = acciones.onUidPruebaChange,
        label = { Text("uid de otra cuenta") },
        singleLine = true,
        enabled = !cuenta.probando,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedButton(
        onClick = acciones.onProbarAcceso,
        enabled = !cuenta.probando && cuenta.uidPrueba.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (cuenta.probando) "Probando…" else "Intentar acceder")
    }
    if (cuenta.resultadoPrueba != null) {
        Card(Modifier.fillMaxWidth()) {
            Text(cuenta.resultadoPrueba, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AjustesPreview() {
    RecordatoriosTheme {
        AjustesScreen(
            preferencias = PreferenciasUsuario(
                porDefecto = setOf(NombresPreferencia.ORDEN),
                invalidas = setOf(NombresPreferencia.TEMA)
            ),
            modoServidor = ModoServidor.NORMAL,
            cuenta = CuentaUiState(email = "ana@mail.com", uid = "Xy12abc"),
            accionesCuenta = AccionesCuenta(),
            onVolver = {}, onTemaChange = {}, onMostrarCompletadosChange = {}, onOrdenChange = {},
            onRestablecer = {}, onGuardarValorInvalido = {}, onModoServidorChange = {}
        )
    }
}

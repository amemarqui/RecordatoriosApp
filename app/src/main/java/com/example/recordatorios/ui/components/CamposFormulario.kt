package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad

/*
 * Componentes CONTROLADOS del formulario: no guardan estado propio.
 * Reciben el valor actual y avisan los cambios con una lambda;
 * el estado vive en el FormularioViewModel.
 */

/**
 * Campo de texto con validación. El error se comunica de tres formas
 * (no solo con el color rojo): isError, un mensaje de texto con ícono
 * y la propiedad semántica error() para lectores de pantalla.
 */
@Composable
fun CampoTextoValidado(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    mensajeError: String?,
    modifier: Modifier = Modifier,
    maxCaracteres: Int? = null,
    unaLinea: Boolean = true,
    minLineas: Int = 1,
    habilitado: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = mensajeError != null,
        singleLine = unaLinea,
        minLines = minLineas,
        enabled = habilitado,
        trailingIcon = if (mensajeError != null) {
            { Icon(Icons.Filled.Warning, contentDescription = null) }
        } else null,
        supportingText = {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = mensajeError ?: "",
                    modifier = Modifier.weight(1f)
                )
                if (maxCaracteres != null) {
                    Text("${valor.length}/$maxCaracteres")
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (mensajeError != null) error(mensajeError) }
    )
}

/**
 * Grupo de chips para elegir UNA opción. Es genérico (<T>) para usarlo
 * tanto con categorías como con prioridades.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SelectorOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T?,
    onSeleccionar: (T) -> Unit,
    textoOpcion: (T) -> String,
    mensajeError: String?,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    colorOpcion: (T) -> Color? = { null }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (mensajeError != null) error(mensajeError) }
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = if (mensajeError != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                val color = colorOpcion(opcion)
                FilterChip(
                    selected = elegida,
                    onClick = { onSeleccionar(opcion) },
                    enabled = habilitado,
                    label = { Text(textoOpcion(opcion)) },
                    leadingIcon = if (elegida) {
                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null,
                    colors = if (color != null) {
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f)
                        )
                    } else FilterChipDefaults.filterChipColors()
                )
            }
        }
        if (mensajeError != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionada: Categoria?,
    onSeleccionar: (Categoria) -> Unit,
    mensajeError: String?,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    SelectorOpciones(
        titulo = "Categoría *",
        opciones = categorias,
        seleccionada = seleccionada,
        onSeleccionar = onSeleccionar,
        textoOpcion = { it.nombre },
        mensajeError = mensajeError,
        modifier = modifier,
        habilitado = habilitado,
        colorOpcion = { Color(RepositorioSimulado.estiloDe(it).colorArgb) }
    )
}

@Composable
fun SelectorPrioridad(
    seleccionada: Prioridad?,
    onSeleccionar: (Prioridad) -> Unit,
    mensajeError: String?,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    SelectorOpciones(
        titulo = "Prioridad *",
        opciones = Prioridad.todas,
        seleccionada = seleccionada,
        onSeleccionar = onSeleccionar,
        textoOpcion = { it.etiqueta },
        mensajeError = mensajeError,
        modifier = modifier,
        habilitado = habilitado,
        colorOpcion = { colorPrioridad(it) }
    )
}

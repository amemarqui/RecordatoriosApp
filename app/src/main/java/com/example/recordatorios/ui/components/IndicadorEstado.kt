package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

enum class TipoIndicador { CARGA, VACIO, ERROR }

/**
 * Componente para comunicar carga, vacío o error (consigna 2).
 * Lo usan el listado y el detalle. Si se pasa textoAccion + onAccion
 * muestra un botón (ej: "Crear recordatorio" o "Reintentar").
 */
@Composable
fun IndicadorEstado(
    tipo: TipoIndicador,
    mensaje: String,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (tipo) {
            TipoIndicador.CARGA -> CircularProgressIndicator()
            TipoIndicador.VACIO -> Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null, // el texto de abajo ya lo explica
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            TipoIndicador.ERROR -> Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        titulo?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }

        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            // TalkBack lee el mensaje cuando aparece (útil para los errores)
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )

        if (textoAccion != null && onAccion != null) {
            Button(onClick = onAccion) { Text(textoAccion) }
        }
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun IndicadorErrorPreview() {
    RecordatoriosTheme {
        IndicadorEstado(
            tipo = TipoIndicador.ERROR,
            titulo = "Algo salió mal",
            mensaje = "No se pudieron cargar los recordatorios.",
            textoAccion = "Reintentar",
            onAccion = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun IndicadorVacioPreview() {
    RecordatoriosTheme {
        IndicadorEstado(
            tipo = TipoIndicador.VACIO,
            titulo = "Todavía no hay recordatorios",
            mensaje = "Creá el primero con el botón de abajo.",
            textoAccion = "Crear recordatorio",
            onAccion = {}
        )
    }
}

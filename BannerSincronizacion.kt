package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.data.InfoSincronizacion
import com.example.recordatorios.data.mapeo.aLocalDateTime
import com.example.recordatorios.data.sync.ResultadoSincronizacion
import com.example.recordatorios.data.sync.mensaje
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import java.time.format.DateTimeFormatter

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

private data class Aviso(
    val texto: String,
    val icono: ImageVector?,
    val fondo: Color,
    val textoBoton: String? = null,
    val cargando: Boolean = false
)

/**
 * Franja que informa el estado de la sincronización: sin conexión, sincronizando,
 * cambios pendientes, error o todo al día. Siempre con texto, no solo con color.
 */
@Composable
fun BannerSincronizacion(
    info: InfoSincronizacion,
    onSincronizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val pendientes = info.cambiosSinSincronizar
    val ultimo = info.ultimoResultado

    val aviso = when {
        !info.conectado -> Aviso(
            texto = if (pendientes > 0) {
                "Sin conexión. $pendientes ${if (pendientes == 1) "cambio guardado" else "cambios guardados"} en el teléfono: se enviarán cuando vuelva la red."
            } else {
                "Sin conexión. Podés seguir usando la app normalmente."
            },
            icono = Icons.Filled.Warning,
            fondo = colores.errorContainer
        )
        info.sincronizando -> Aviso("Sincronizando con el servidor...", null, colores.secondaryContainer, cargando = true)
        ultimo != null && ultimo !is ResultadoSincronizacion.Exito && ultimo !is ResultadoSincronizacion.YaEnCurso ->
            Aviso(ultimo.mensaje(), Icons.Filled.Warning, colores.errorContainer, textoBoton = "Reintentar")
        pendientes > 0 -> Aviso(
            "$pendientes ${if (pendientes == 1) "cambio" else "cambios"} sin sincronizar",
            Icons.Filled.Refresh,
            colores.tertiaryContainer,
            textoBoton = "Sincronizar"
        )
        else -> Aviso(
            texto = "Todo sincronizado" +
                (info.ultimaVezMs?.let { " · ${it.aLocalDateTime().format(formatoHora)}" } ?: ""),
            icono = Icons.Filled.CheckCircle,
            fondo = colores.surfaceVariant
        )
    }

    Surface(
        color = aviso.fondo,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                aviso.cargando -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                aviso.icono != null -> Icon(aviso.icono, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(aviso.texto, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (aviso.textoBoton != null) {
                TextButton(onClick = onSincronizar) { Text(aviso.textoBoton) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BannerSinConexionPreview() {
    RecordatoriosTheme {
        BannerSincronizacion(InfoSincronizacion(conectado = false, cambiosSinSincronizar = 2), onSincronizar = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun BannerErrorPreview() {
    RecordatoriosTheme {
        BannerSincronizacion(
            InfoSincronizacion(ultimoResultado = ResultadoSincronizacion.Timeout, cambiosSinSincronizar = 1),
            onSincronizar = {}
        )
    }
}

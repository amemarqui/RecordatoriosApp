package com.example.recordatorios

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.agruparPorCategoria
import com.example.recordatorios.data.categoriasUsadas
import com.example.recordatorios.model.EstadoUi
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ResultadoValidacion
import com.example.recordatorios.model.ValidadorRecordatorio
import com.example.recordatorios.ui.screens.PantallaRecordatorios
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val recordatorios = RepositorioSimulado.recordatorios()
        registrarDiagnostico(recordatorios.size)

        val estadoUi = if (recordatorios.isEmpty()) EstadoUi.Vacio else EstadoUi.Exito(recordatorios)

        setContent {
            RecordatoriosTheme {
                PantallaRecordatorios(estadoUi)
            }
        }
    }

    /** Escribe en Logcat (filtro: tag "Recordatorios") el resultado de las colecciones y validaciones. */
    private fun registrarDiagnostico(total: Int) {
        val recordatorios = RepositorioSimulado.recordatorios()
        Log.d(TAG, "Recordatorios simulados cargados: $total")
        Log.d(TAG, "Categorías únicas (Set): ${RepositorioSimulado.categorias.map { it.nombre }}")
        Log.d(TAG, "Categorías usadas: ${recordatorios.categoriasUsadas().map { it.nombre }}")
        Log.d(TAG, "Agrupados por categoría (Map): " +
            recordatorios.agruparPorCategoria().mapKeys { it.key.nombre }.mapValues { it.value.size })

        // Prueba de la regla de validación con datos inválidos a propósito
        val invalido = ValidadorRecordatorio.validar(
            id = 99,
            titulo = "   ",
            categoria = null,
            prioridad = Prioridad.Alta,
            minutosAvisoPrevio = 10
        )
        when (invalido) {
            is ResultadoValidacion.Valido -> Log.w(TAG, "Se esperaba un recordatorio inválido")
            is ResultadoValidacion.Invalido -> Log.d(TAG, "Validación rechazó datos inválidos: ${invalido.errores}")
        }
    }

    companion object {
        private const val TAG = "Recordatorios"
    }
}

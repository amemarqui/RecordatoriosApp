package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.io.IOException

/**
 * Implementación en memoria del repositorio. Los datos se pierden al cerrar
 * la app (la persistencia con Room llega en el módulo 3).
 *
 * Los delay() simulan la demora de una base de datos o de la red,
 * para poder ver el estado de carga y el botón de guardado deshabilitado.
 */
class RepositorioEnMemoria(
    private val demoraCargaMs: Long = 1_200,
    private val demoraGuardadoMs: Long = 1_500
) : RecordatoriosRepository, SimuladorEscenarios {

    private val datos = MutableStateFlow(RepositorioSimulado.recordatorios())

    // Banderas del simulador: cada error ocurre una sola vez,
    // así se puede probar que "Reintentar" funciona.
    @Volatile private var fallarProximaCarga = false
    @Volatile private var fallarProximoGuardado = false

    override fun observarRecordatorios(): Flow<List<Recordatorio>> = flow {
        delay(demoraCargaMs)
        if (fallarProximaCarga) {
            fallarProximaCarga = false
            throw IOException("No se pudieron cargar los recordatorios. Revisá la conexión e intentá de nuevo.")
        }
        emitAll(datos)
    }

    override suspend fun obtenerPorId(id: Int): Recordatorio? {
        delay(demoraCargaMs / 2)
        return datos.value.find { it.id == id }
    }

    override suspend fun guardar(nuevo: NuevoRecordatorio): Recordatorio {
        delay(demoraGuardadoMs)
        if (fallarProximoGuardado) {
            fallarProximoGuardado = false
            throw IOException("No se pudo guardar el recordatorio. Intentá nuevamente.")
        }
        val recordatorio = Recordatorio(
            id = (datos.value.maxOfOrNull { it.id } ?: 0) + 1,
            titulo = nuevo.titulo.trim(),
            descripcion = nuevo.descripcion?.trim()?.takeIf { it.isNotEmpty() },
            categoria = nuevo.categoria,
            prioridad = nuevo.prioridad
        )
        datos.update { lista -> lista + recordatorio }
        return recordatorio
    }

    override fun categoriasDisponibles(): Set<Categoria> = RepositorioSimulado.categorias

    override fun aplicarEscenario(escenario: Escenario) {
        when (escenario) {
            Escenario.NORMAL -> datos.value = RepositorioSimulado.recordatorios()
            Escenario.VACIO -> datos.value = emptyList()
            Escenario.ERROR_CARGA -> fallarProximaCarga = true
            Escenario.ERROR_GUARDADO -> fallarProximoGuardado = true
        }
    }
}

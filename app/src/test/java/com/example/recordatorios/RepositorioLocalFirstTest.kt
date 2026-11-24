package com.example.recordatorios

import com.example.recordatorios.data.NuevoRecordatorio
import com.example.recordatorios.data.RepositorioRecordatoriosLocalFirst
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.ResultadoOperacion
import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.example.recordatorios.fakes.FakePreferencias
import com.example.recordatorios.fakes.FakeRecordatorioDao
import com.example.recordatorios.fakes.FakeRecordatoriosApi
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.Prioridad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioLocalFirstTest {

    private val dao = FakeRecordatorioDao()
    private val api = FakeRecordatoriosApi()
    private val preferencias = FakePreferencias()
    private var hayRed = false   // arranca sin conexión
    private var contadorIds = 0

    private fun nuevo(titulo: String, prioridad: Prioridad = Prioridad.Media) = NuevoRecordatorio(
        titulo = titulo, descripcion = null, categoria = RepositorioSimulado.ESTUDIO, prioridad = prioridad
    )

    private fun crearRepositorio(alcance: kotlinx.coroutines.CoroutineScope) = RepositorioRecordatoriosLocalFirst(
        dao = dao,
        preferencias = preferencias,
        sincronizador = SincronizadorRecordatorios(dao, api, hayConexion = { hayRed }),
        conectado = MutableStateFlow(hayRed),
        alcanceApp = alcance,
        generarId = { "id-${++contadorIds}" }
    )

    @Test
    fun crearSinConexionGuardaEnRoomYQuedaPendiente() = runBlocking {
        val repo = crearRepositorio(this)
        val resultado = repo.crear(nuevo("Estudiar Room"))

        assertTrue(resultado is ResultadoOperacion.Exito)
        val lista = repo.observarRecordatorios().first()
        assertEquals(1, lista.size)
        assertEquals(EstadoSincronizacion.PENDIENTE, lista.first().sincronizacion)
        assertEquals(0, api.llamadasGuardar)
    }

    @Test
    fun eliminarLoOcultaEnseguidaAunqueNoHayaRed() = runBlocking {
        val repo = crearRepositorio(this)
        repo.crear(nuevo("Borrar esto"))

        repo.eliminar("id-1")

        assertTrue(repo.observarRecordatorios().first().isEmpty())
        assertEquals(EstadoSyncEntity.PENDIENTE_ELIMINAR, dao.obtenerPorId("id-1")!!.estadoSync)
    }

    @Test
    fun ocultarCompletadosYOrdenarSegunPreferencias() = runBlocking {
        val repo = crearRepositorio(this)
        repo.crear(nuevo("Banana", Prioridad.Baja))
        repo.crear(nuevo("Ananá", Prioridad.Alta))
        repo.crear(nuevo("Cereza", Prioridad.Media))
        repo.cambiarCompletado("id-3", completado = true)

        preferencias.cambiarOrden(OrdenRecordatorios.TITULO)
        assertEquals(listOf("Ananá", "Banana", "Cereza"), repo.observarRecordatorios().first().map { it.titulo })

        preferencias.cambiarMostrarCompletados(false)
        assertEquals(listOf("Ananá", "Banana"), repo.observarRecordatorios().first().map { it.titulo })
    }

    @Test
    fun editarCambiaLosDatosSinCrearOtroRegistro() = runBlocking {
        val repo = crearRepositorio(this)
        repo.crear(nuevo("Título viejo"))
        repo.actualizar("id-1", nuevo("Título nuevo", Prioridad.Alta))

        val lista = repo.observarRecordatorios().first()
        assertEquals(1, lista.size)
        assertEquals("Título nuevo", lista.first().titulo)
        assertEquals(Prioridad.Alta, lista.first().prioridad)
    }
}

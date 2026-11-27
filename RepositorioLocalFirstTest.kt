package com.example.recordatorios

import com.example.recordatorios.data.NuevoRecordatorio
import com.example.recordatorios.data.RepositorioRecordatoriosLocalFirst
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.ResultadoOperacion
import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.example.recordatorios.fakes.FakeFuenteRemota
import com.example.recordatorios.fakes.FakePreferencias
import com.example.recordatorios.fakes.FakeRecordatorioDao
import com.example.recordatorios.fakes.entidadDePrueba
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.Prioridad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioLocalFirstTest {

    private val dao = FakeRecordatorioDao()
    private val preferencias = FakePreferencias()
    private var hayRed = false   // arranca sin conexión
    private val sesion = MutableStateFlow<String?>("usuario-a")
    private val nube = FakeFuenteRemota(uidAutenticado = { sesion.value })
    private var contadorIds = 0

    private fun nuevo(titulo: String, prioridad: Prioridad = Prioridad.Media) = NuevoRecordatorio(
        titulo = titulo, descripcion = null, categoria = RepositorioSimulado.ESTUDIO, prioridad = prioridad
    )

    private fun crearRepositorio(alcance: CoroutineScope) = RepositorioRecordatoriosLocalFirst(
        dao = dao,
        preferencias = preferencias,
        sincronizador = SincronizadorRecordatorios(dao, nube, hayConexion = { hayRed }, uidActual = { sesion.value }),
        remota = nube,
        conectado = MutableStateFlow(hayRed),
        uidSesion = sesion,
        uidActual = { sesion.value },
        alcanceApp = alcance,
        generarId = { "id-${++contadorIds}" }
    )

    @Test
    fun crearSinConexionGuardaEnRoomConElUidYQuedaPendiente() = runBlocking {
        val repo = crearRepositorio(this)
        assertTrue(repo.crear(nuevo("Estudiar Room")) is ResultadoOperacion.Exito)

        val lista = repo.observarRecordatorios().first()
        assertEquals(1, lista.size)
        assertEquals(EstadoSincronizacion.PENDIENTE, lista.first().sincronizacion)
        assertEquals("usuario-a", dao.obtenerPorId("usuario-a", "id-1")!!.uid)
        assertEquals(0, nube.llamadasGuardar)
    }

    @Test
    fun otraCuentaEnElMismoTelefonoNoVeLosDatosDeLaAnterior() = runBlocking {
        val repo = crearRepositorio(this)
        repo.crear(nuevo("Dato privado de A"))

        sesion.value = "usuario-b"   // cambia la cuenta

        assertTrue(repo.observarRecordatorios().first().isEmpty())
        assertTrue(repo.eliminar("id-1") is ResultadoOperacion.Error)   // tampoco puede tocarlo
    }

    @Test
    fun sinSesionNoSeMuestraNiSeGuardaNada() = runBlocking {
        sesion.value = null
        val repo = crearRepositorio(this)
        assertTrue(repo.crear(nuevo("Sin dueño")) is ResultadoOperacion.Error)
        assertTrue(repo.observarRecordatorios().first().isEmpty())
    }

    @Test
    fun cerrarSesionBorraLoSincronizadoYConservaLoPendiente() = runBlocking {
        dao.insertar(entidadDePrueba("ya-en-la-nube", estadoSync = EstadoSyncEntity.SINCRONIZADO))
        dao.insertar(entidadDePrueba("sin-enviar", estadoSync = EstadoSyncEntity.PENDIENTE))
        val repo = crearRepositorio(this)   // sin red: el pendiente no se puede subir

        val resultado = repo.prepararCierreDeSesion()

        assertEquals(1, (resultado as ResultadoOperacion.Exito).valor)        // 1 cambio sin sincronizar
        assertEquals(null, dao.obtenerPorId("usuario-a", "ya-en-la-nube"))   // borrado del teléfono
        assertTrue(dao.obtenerPorId("usuario-a", "sin-enviar") != null)       // se conserva
    }

    @Test
    fun laPruebaDeSeguridadEsDenegadaPorLasReglas() = runBlocking {
        val repo = crearRepositorio(this)
        val informe = repo.probarAccesoAOtraCuenta("usuario-b")
        assertTrue(informe, informe.contains("Lectura") && informe.contains("DENEGADA"))
        assertTrue(nube.documentosDe("usuario-b").isEmpty())
    }

    @Test
    fun eliminarLoOcultaEnseguidaAunqueNoHayaRed() = runBlocking {
        val repo = crearRepositorio(this)
        repo.crear(nuevo("Borrar esto"))

        repo.eliminar("id-1")

        assertTrue(repo.observarRecordatorios().first().isEmpty())
        assertEquals(EstadoSyncEntity.PENDIENTE_ELIMINAR, dao.obtenerPorId("usuario-a", "id-1")!!.estadoSync)
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
    }
}

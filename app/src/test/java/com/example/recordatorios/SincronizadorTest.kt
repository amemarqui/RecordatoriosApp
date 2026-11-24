package com.example.recordatorios

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.sync.ResultadoSincronizacion
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.example.recordatorios.fakes.FakeRecordatorioDao
import com.example.recordatorios.fakes.FakeRecordatoriosApi
import com.example.recordatorios.fakes.entidadDePrueba
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorTest {

    private val dao = FakeRecordatorioDao()
    private val api = FakeRecordatoriosApi()
    private var hayRed = true
    private val sincronizador = SincronizadorRecordatorios(dao, api, hayConexion = { hayRed })

    @Test
    fun creadoSinConexionQuedaPendienteYDespuesSeSincronizaSinDuplicar() = runBlocking {
        dao.insertar(entidadDePrueba("a"))

        hayRed = false
        assertEquals(ResultadoSincronizacion.SinConexion, sincronizador.sincronizar())
        assertEquals(EstadoSyncEntity.PENDIENTE, dao.obtenerPorId("a")!!.estadoSync)
        assertEquals(0, api.llamadasGuardar)

        hayRed = true
        assertTrue(sincronizador.sincronizar() is ResultadoSincronizacion.Exito)
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("a")!!.estadoSync)

        // Una segunda sincronización no vuelve a enviarlo ni lo duplica
        sincronizador.sincronizar()
        assertEquals(1, api.llamadasGuardar)
        assertEquals(1, api.servidor.size)
        assertEquals(1, dao.tabla.value.size)
    }

    @Test
    fun dosPedidosAlMismoTiempoEnvianUnaSolaVez() = runBlocking {
        dao.insertar(entidadDePrueba("a"))
        api.demoraMs = 100

        val primero = async { sincronizador.sincronizar() }
        val segundo = async { sincronizador.sincronizar() }

        assertTrue(primero.await() is ResultadoSincronizacion.Exito)
        assertEquals(ResultadoSincronizacion.YaEnCurso, segundo.await())
        assertEquals(1, api.llamadasGuardar)
        assertEquals(1, api.servidor.size)
    }

    @Test
    fun timeoutDejaPendienteYElReintentoNoDuplica() = runBlocking {
        dao.insertar(entidadDePrueba("a"))

        api.falla = FakeRecordatoriosApi.Falla.TIMEOUT
        assertEquals(ResultadoSincronizacion.Timeout, sincronizador.sincronizar())
        val pendiente = dao.obtenerPorId("a")!!
        assertEquals(EstadoSyncEntity.PENDIENTE, pendiente.estadoSync)
        assertEquals(1, pendiente.intentosSync)
        assertNotNull(pendiente.ultimoErrorSync)

        api.falla = FakeRecordatoriosApi.Falla.NINGUNA
        sincronizador.sincronizar()
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("a")!!.estadoSync)
        assertEquals(1, api.servidor.size)
    }

    @Test
    fun error500SeTrataComoTemporal() = runBlocking {
        dao.insertar(entidadDePrueba("a"))
        api.falla = FakeRecordatoriosApi.Falla.ERROR_500
        assertEquals(ResultadoSincronizacion.ErrorServidor(500), sincronizador.sincronizar())
        assertEquals(EstadoSyncEntity.PENDIENTE, dao.obtenerPorId("a")!!.estadoSync)
    }

    @Test
    fun error400MarcaErrorYSePuedeReintentar() = runBlocking {
        dao.insertar(entidadDePrueba("a"))
        api.falla = FakeRecordatoriosApi.Falla.ERROR_400

        val resultado = sincronizador.sincronizar()
        assertTrue(resultado is ResultadoSincronizacion.Error || resultado is ResultadoSincronizacion.Exito)
        assertEquals(EstadoSyncEntity.ERROR, dao.obtenerPorId("a")!!.estadoSync)

        // Mientras está en ERROR no se reenvía solo
        api.falla = FakeRecordatoriosApi.Falla.NINGUNA
        sincronizador.sincronizar()
        assertEquals(1, api.llamadasGuardar)

        // El usuario toca "Reintentar"
        dao.volverAPendiente("a")
        sincronizador.sincronizar()
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("a")!!.estadoSync)
    }

    @Test
    fun eliminacionPendienteSeEnviaYDespuesSeBorraLocal() = runBlocking {
        dao.insertar(entidadDePrueba("a", estadoSync = EstadoSyncEntity.PENDIENTE_ELIMINAR))
        api.servidor["a"] = RecordatorioDto(id = "a", titulo = "Viejo", categoriaId = 1, prioridad = "BAJA")

        sincronizador.sincronizar()

        assertNull(dao.obtenerPorId("a"))
        assertTrue(api.servidor.isEmpty())
    }

    @Test
    fun eliminarAlgoQueElServidorNoTeniaNoEsError() = runBlocking {
        dao.insertar(entidadDePrueba("a", estadoSync = EstadoSyncEntity.PENDIENTE_ELIMINAR))
        assertTrue(sincronizador.sincronizar() is ResultadoSincronizacion.Exito)   // el 404 se acepta
        assertNull(dao.obtenerPorId("a"))
    }

    @Test
    fun descargaDelServidorYDescartaDatosInvalidos() = runBlocking {
        api.servidor["r1"] = RecordatorioDto(id = "r1", titulo = "Del servidor", categoriaId = 2, prioridad = "ALTA")
        api.servidor["r2"] = RecordatorioDto(id = "r2", titulo = "Prioridad rara", categoriaId = 2, prioridad = "URGENTE")

        val resultado = sincronizador.sincronizar() as ResultadoSincronizacion.Exito

        assertEquals(1, resultado.recibidos)
        assertEquals(1, resultado.descartados)
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("r1")!!.estadoSync)
        assertNull(dao.obtenerPorId("r2"))
    }

    @Test
    fun unCambioLocalPendienteNoSePisaConElDelServidor() = runBlocking {
        dao.insertar(entidadDePrueba("a", titulo = "Versión local", actualizadoEn = 5_000))
        api.falla = FakeRecordatoriosApi.Falla.ERROR_400   // el envío falla y queda en ERROR (no sincronizado)
        api.servidor["a"] = RecordatorioDto(id = "a", titulo = "Versión servidor", categoriaId = 1, prioridad = "BAJA")

        sincronizador.sincronizar()
        api.falla = FakeRecordatoriosApi.Falla.NINGUNA
        sincronizador.sincronizar()

        assertEquals("Versión local", dao.obtenerPorId("a")!!.titulo)
    }
}

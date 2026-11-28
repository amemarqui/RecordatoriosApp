package com.example.recordatorios

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.sync.ResultadoSincronizacion
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.example.recordatorios.fakes.FakeFuenteRemota
import com.example.recordatorios.fakes.FakeRecordatorioDao
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
    private var hayRed = true
    private var uid: String? = "usuario-a"
    private val nube = FakeFuenteRemota(uidAutenticado = { uid })
    private val sincronizador = SincronizadorRecordatorios(dao, nube, hayConexion = { hayRed }, uidActual = { uid })

    @Test
    fun modificacionSinConexionQuedaPendienteYDespuesSeSincronizaSinDuplicar() = runBlocking {
        dao.insertar(entidadDePrueba("a"))

        hayRed = false
        assertEquals(ResultadoSincronizacion.SinConexion, sincronizador.sincronizar())
        assertEquals(EstadoSyncEntity.PENDIENTE, dao.obtenerPorId("usuario-a", "a")!!.estadoSync)
        assertEquals(0, nube.llamadasGuardar)

        hayRed = true   // vuelve la conectividad
        assertTrue(sincronizador.sincronizar() is ResultadoSincronizacion.Exito)
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("usuario-a", "a")!!.estadoSync)

        sincronizador.sincronizar()
        assertEquals(1, nube.llamadasGuardar)
        assertEquals(1, nube.documentosDe("usuario-a").size)
    }

    @Test
    fun sinSesionNoSeSincronizaNada() = runBlocking {
        uid = null
        assertEquals(ResultadoSincronizacion.SinSesion, sincronizador.sincronizar())
        assertEquals(0, nube.llamadasGuardar)
    }

    @Test
    fun dosPedidosAlMismoTiempoEnvianUnaSolaVez() = runBlocking {
        dao.insertar(entidadDePrueba("a"))
        nube.demoraMs = 100

        val primero = async { sincronizador.sincronizar() }
        val segundo = async { sincronizador.sincronizar() }

        assertTrue(primero.await() is ResultadoSincronizacion.Exito)
        assertEquals(ResultadoSincronizacion.YaEnCurso, segundo.await())
        assertEquals(1, nube.llamadasGuardar)
        assertEquals(1, nube.documentosDe("usuario-a").size)
    }

    @Test
    fun timeoutDejaPendienteYElReintentoNoDuplica() = runBlocking {
        dao.insertar(entidadDePrueba("a"))

        nube.falla = FakeFuenteRemota.Falla.TIMEOUT
        assertEquals(ResultadoSincronizacion.Timeout, sincronizador.sincronizar())
        val pendiente = dao.obtenerPorId("usuario-a", "a")!!
        assertEquals(EstadoSyncEntity.PENDIENTE, pendiente.estadoSync)
        assertEquals(1, pendiente.intentosSync)
        assertNotNull(pendiente.ultimoErrorSync)

        nube.falla = FakeFuenteRemota.Falla.NINGUNA
        sincronizador.sincronizar()
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("usuario-a", "a")!!.estadoSync)
        assertEquals(1, nube.documentosDe("usuario-a").size)
    }

    @Test
    fun servicioNoDisponibleSeTrataComoTemporal() = runBlocking {
        dao.insertar(entidadDePrueba("a"))
        nube.falla = FakeFuenteRemota.Falla.NO_DISPONIBLE
        assertEquals(ResultadoSincronizacion.ErrorServidor(503), sincronizador.sincronizar())
        assertEquals(EstadoSyncEntity.PENDIENTE, dao.obtenerPorId("usuario-a", "a")!!.estadoSync)
    }

    @Test
    fun rechazoMarcaErrorYSePuedeReintentar() = runBlocking {
        dao.insertar(entidadDePrueba("a"))
        nube.falla = FakeFuenteRemota.Falla.RECHAZO
        sincronizador.sincronizar()
        assertEquals(EstadoSyncEntity.ERROR, dao.obtenerPorId("usuario-a", "a")!!.estadoSync)

        nube.falla = FakeFuenteRemota.Falla.NINGUNA
        sincronizador.sincronizar()
        assertEquals(1, nube.llamadasGuardar)     // en ERROR no se reenvía solo

        dao.volverAPendiente("usuario-a", "a")    // el usuario toca "Reintentar"
        sincronizador.sincronizar()
        assertEquals(EstadoSyncEntity.SINCRONIZADO, dao.obtenerPorId("usuario-a", "a")!!.estadoSync)
    }

    @Test
    fun eliminacionPendienteSeEnviaYDespuesSeBorraLocal() = runBlocking {
        dao.insertar(entidadDePrueba("a", estadoSync = EstadoSyncEntity.PENDIENTE_ELIMINAR))
        nube.documentosDe("usuario-a")["a"] = RecordatorioDto(id = "a", titulo = "Viejo", categoriaId = 1, prioridad = "BAJA")

        sincronizador.sincronizar()

        assertNull(dao.obtenerPorId("usuario-a", "a"))
        assertTrue(nube.documentosDe("usuario-a").isEmpty())
    }

    @Test
    fun recuperaEnOtroDispositivoLoCreadoEnElPrimero() = runBlocking {
        // Dispositivo 1 crea y sincroniza
        dao.insertar(entidadDePrueba("a", titulo = "Creado en el teléfono 1"))
        sincronizador.sincronizar()

        // Dispositivo 2: base vacía, misma cuenta, misma nube
        val dao2 = FakeRecordatorioDao()
        val sincronizador2 = SincronizadorRecordatorios(dao2, nube, hayConexion = { true }, uidActual = { uid })
        val resultado = sincronizador2.sincronizar() as ResultadoSincronizacion.Exito

        assertEquals(1, resultado.recibidos)
        assertEquals("Creado en el teléfono 1", dao2.obtenerPorId("usuario-a", "a")!!.titulo)
    }

    @Test
    fun descargaDeLaNubeYDescartaDatosInvalidos() = runBlocking {
        nube.documentosDe("usuario-a")["r1"] = RecordatorioDto(id = "r1", titulo = "De la nube", categoriaId = 2, prioridad = "ALTA")
        nube.documentosDe("usuario-a")["r2"] = RecordatorioDto(id = "r2", titulo = "Prioridad rara", categoriaId = 2, prioridad = "URGENTE")

        val resultado = sincronizador.sincronizar() as ResultadoSincronizacion.Exito

        assertEquals(1, resultado.recibidos)
        assertEquals(1, resultado.descartados)
        assertNull(dao.obtenerPorId("usuario-a", "r2"))
    }

    @Test
    fun unCambioLocalPendienteNoSePisaConElDeLaNube() = runBlocking {
        dao.insertar(entidadDePrueba("a", titulo = "Versión local", actualizadoEn = 5_000))
        nube.falla = FakeFuenteRemota.Falla.RECHAZO
        nube.documentosDe("usuario-a")["a"] = RecordatorioDto(id = "a", titulo = "Versión nube", categoriaId = 1, prioridad = "BAJA")

        sincronizador.sincronizar()
        nube.falla = FakeFuenteRemota.Falla.NINGUNA
        sincronizador.sincronizar()

        assertEquals("Versión local", dao.obtenerPorId("usuario-a", "a")!!.titulo)
    }
}

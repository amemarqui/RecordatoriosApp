package com.example.recordatorios

import com.example.recordatorios.data.Escenario
import com.example.recordatorios.data.NuevoRecordatorio
import com.example.recordatorios.data.RepositorioEnMemoria
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.Prioridad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RepositorioEnMemoriaTest {

    // Sin demoras para que los tests sean rápidos
    private val repo = RepositorioEnMemoria(demoraCargaMs = 0, demoraGuardadoMs = 0)

    private val nuevo = NuevoRecordatorio(
        titulo = "  Llamar al técnico  ",
        descripcion = "",
        categoria = RepositorioSimulado.HOGAR,
        prioridad = Prioridad.Media
    )

    @Test
    fun guardarAsignaIdYApareceEnLaLista() = runBlocking {
        val antes = repo.observarRecordatorios().first().size
        val guardado = repo.guardar(nuevo)

        assertEquals(antes + 1, repo.observarRecordatorios().first().size)
        assertEquals("Llamar al técnico", guardado.titulo)   // se guarda con trim
        assertNull(guardado.descripcion)                     // vacía => null
        assertNotNull(repo.obtenerPorId(guardado.id))
    }

    @Test
    fun obtenerPorIdInexistenteDevuelveNull() = runBlocking {
        assertNull(repo.obtenerPorId(999))
    }

    @Test
    fun escenarioVacioDejaLaListaSinElementos() = runBlocking {
        repo.aplicarEscenario(Escenario.VACIO)
        assertTrue(repo.observarRecordatorios().first().isEmpty())
    }

    @Test
    fun errorDeCargaOcurreUnaSolaVez() = runBlocking {
        repo.aplicarEscenario(Escenario.ERROR_CARGA)
        try {
            repo.observarRecordatorios().first()
            fail("Se esperaba un error")
        } catch (e: IOException) {
            // ok
        }
        // El reintento funciona
        assertTrue(repo.observarRecordatorios().first().isNotEmpty())
    }

    @Test(expected = IOException::class)
    fun errorAlGuardarSimulado() {
        runBlocking {
            repo.aplicarEscenario(Escenario.ERROR_GUARDADO)
            repo.guardar(nuevo)
        }
    }
}

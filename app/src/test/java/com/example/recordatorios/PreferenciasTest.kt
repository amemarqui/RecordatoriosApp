package com.example.recordatorios

import com.example.recordatorios.data.preferencias.NombresPreferencia
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.data.preferencias.interpretarPreferencias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenciasTest {

    @Test
    fun sinDatosGuardadosUsaTodosLosPredeterminados() {
        val p = interpretarPreferencias(null, null, null)
        assertEquals(TemaApp.SISTEMA, p.tema)
        assertTrue(p.mostrarCompletados)
        assertEquals(OrdenRecordatorios.PRIORIDAD, p.orden)
        assertEquals(3, p.porDefecto.size)
        assertTrue(p.invalidas.isEmpty())
    }

    @Test
    fun valoresGuardadosSeRespetan() {
        val p = interpretarPreferencias("OSCURO", false, "TITULO")
        assertEquals(TemaApp.OSCURO, p.tema)
        assertFalse(p.mostrarCompletados)
        assertEquals(OrdenRecordatorios.TITULO, p.orden)
        assertTrue(p.porDefecto.isEmpty())
    }

    @Test
    fun valorDesconocidoUsaElPredeterminadoYSeInforma() {
        val p = interpretarPreferencias("VIOLETA", true, "ALEATORIO")
        assertEquals(TemaApp.SISTEMA, p.tema)
        assertEquals(OrdenRecordatorios.PRIORIDAD, p.orden)
        assertEquals(setOf(NombresPreferencia.TEMA, NombresPreferencia.ORDEN), p.invalidas)
    }

    @Test
    fun errorDeLecturaSeMarca() {
        assertTrue(interpretarPreferencias(null, null, null, errorLectura = true).errorLectura)
    }
}

package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.Ubicacion
import java.time.LocalDateTime
import kotlin.reflect.KClass

/** Etiqueta corta y color (ARGB) con que se muestra cada categoría. */
data class EstiloCategoria(val etiqueta: String, val colorArgb: Long)

/**
 * CONSIGNA 3 (Act. 1) — Datos simulados con List, Set y Map.
 * En la Actividad 2 estos datos son el contenido inicial del RepositorioEnMemoria.
 */
object RepositorioSimulado {

    // Categorías base
    val TRABAJO = Categoria(1, "Trabajo")
    val ESTUDIO = Categoria(2, "Estudio")
    val SALUD = Categoria(3, "Salud")
    val HOGAR = Categoria(4, "Hogar")
    val TRAMITES = Categoria(5, "Trámites")

    /**
     * Lista "cruda" de categorías tal como podría llegar de distintas fuentes:
     * contiene repetidas a propósito.
     */
    private val categoriasConRepetidas: List<Categoria> = listOf(
        TRABAJO, ESTUDIO, SALUD, TRABAJO, HOGAR, ESTUDIO, TRAMITES, SALUD
    )

    /** SET: elimina las categorías repetidas (data class => igualdad por valor). */
    val categorias: Set<Categoria> = categoriasConRepetidas.toSet()

    /** MAP: asocia cada categoría con su etiqueta y color. */
    val estilosPorCategoria: Map<Categoria, EstiloCategoria> = mapOf(
        TRABAJO to EstiloCategoria("TRB", 0xFF1E88E5),
        ESTUDIO to EstiloCategoria("EST", 0xFF8E24AA),
        SALUD to EstiloCategoria("SAL", 0xFF43A047),
        HOGAR to EstiloCategoria("HOG", 0xFFFB8C00),
        TRAMITES to EstiloCategoria("TRM", 0xFF6D4C41)
    )

    private val estiloPorDefecto = EstiloCategoria("GEN", 0xFF757575)

    fun estiloDe(categoria: Categoria): EstiloCategoria =
        estilosPorCategoria[categoria] ?: estiloPorDefecto

    /** LIST: recordatorios simulados (inmutable). */
    fun recordatorios(ahora: LocalDateTime = LocalDateTime.now()): List<Recordatorio> = listOf(
        Recordatorio(
            id = 1,
            titulo = "Entregar Actividad 2 de Programación Mobile",
            categoria = ESTUDIO,
            prioridad = Prioridad.Alta,
            descripcion = "Subir el PDF con las capturas y el enlace al repositorio",
            fechaLimite = ahora.plusDays(2),
            minutosAvisoPrevio = 120
        ),
        Recordatorio(
            id = 2,
            titulo = "Reunión de seguimiento del sprint",
            categoria = TRABAJO,
            prioridad = Prioridad.Media,
            fechaLimite = ahora.plusDays(1).withHour(10).withMinute(0),
            ubicacion = Ubicacion("Oficina central", -34.6037, -58.3816),
            minutosAvisoPrevio = 15
        ),
        Recordatorio(
            id = 3,
            titulo = "Turno con el médico clínico",
            categoria = SALUD,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Completado(ahora.minusDays(1)),
            fechaLimite = ahora.minusDays(1)
        ),
        Recordatorio(
            id = 4,
            titulo = "Comprar lamparitas LED",
            categoria = HOGAR,
            prioridad = Prioridad.Baja
        ),
        Recordatorio(
            id = 5,
            titulo = "Renovar la licencia de conducir",
            categoria = TRAMITES,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Vencido,
            fechaLimite = ahora.minusDays(3)
        ),
        Recordatorio(
            id = 6,
            titulo = "Leer el material del Módulo 2",
            categoria = ESTUDIO,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Pospuesto(ahora.plusDays(4))
        ),
        Recordatorio(
            id = 7,
            titulo = "Revisar casos de prueba automatizados",
            categoria = TRABAJO,
            prioridad = Prioridad.Alta,
            descripcion = "Actualizar los tests que fallaron en la última ejecución",
            fechaLimite = ahora.plusHours(6)
        ),
        Recordatorio(
            id = 8,
            titulo = "Pagar la factura de luz",
            categoria = HOGAR,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Completado(ahora.minusHours(5))
        )
    )
}

/* ---------- Operaciones sobre colecciones ---------- */

/** Filtra por estado. Se compara por clase para que todos los Completado/Pospuesto coincidan. */
fun List<Recordatorio>.filtrarPorEstado(estado: KClass<out EstadoRecordatorio>?): List<Recordatorio> =
    if (estado == null) this else filter { it.estado::class == estado }

fun List<Recordatorio>.filtrarPorPrioridad(prioridad: Prioridad?): List<Recordatorio> =
    if (prioridad == null) this else filter { it.prioridad == prioridad }

/** Ordena: primero mayor prioridad, luego por fecha límite (las sin fecha al final). */
fun List<Recordatorio>.ordenados(): List<Recordatorio> =
    sortedWith(
        compareByDescending<Recordatorio> { it.prioridad.peso }
            .thenBy { it.fechaLimite ?: LocalDateTime.MAX }
    )

/** MAP: cantidad de recordatorios por estado (clave = etiqueta del estado). */
fun List<Recordatorio>.cantidadPorEstado(): Map<String, Int> =
    groupingBy { it.estado.etiqueta }.eachCount()

/** MAP: recordatorios agrupados por categoría. */
fun List<Recordatorio>.agruparPorCategoria(): Map<Categoria, List<Recordatorio>> =
    groupBy { it.categoria }

/** SET: categorías efectivamente usadas por los recordatorios (sin repetir). */
fun List<Recordatorio>.categoriasUsadas(): Set<Categoria> =
    mapTo(mutableSetOf()) { it.categoria }

package com.example.recordatorios.data

import android.content.Context
import com.example.recordatorios.BuildConfig
import com.example.recordatorios.data.local.RecordatoriosDatabase
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.preferencias.PreferenciasDataStore
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.dataStorePreferencias
import com.example.recordatorios.data.remote.ClienteRed
import com.example.recordatorios.data.remote.simulado.AlmacenServidorSimulado
import com.example.recordatorios.data.remote.simulado.ControlServidorSimulado
import com.example.recordatorios.data.remote.simulado.ServidorSimuladoInterceptor
import com.example.recordatorios.data.sync.MonitorConectividad
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Contenedor de dependencias "a mano". Se crea UNA vez en RecordatoriosApplication,
 * así hay una sola base de datos, un solo DataStore y un solo sincronizador.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Alcance de toda la app: sigue vivo aunque se cierre una pantalla. */
    val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val gson = Gson()
    private val monitor = MonitorConectividad(appContext)

    val conectado: StateFlow<Boolean> =
        monitor.conectado.stateIn(alcanceApp, SharingStarted.Eagerly, monitor.hayConexion())

    private val database = RecordatoriosDatabase.obtener(appContext)

    val preferencias: PreferenciasRepository = PreferenciasDataStore(appContext.dataStorePreferencias)

    private val servidorSimulado: ServidorSimuladoInterceptor? =
        if (BuildConfig.USAR_SERVIDOR_SIMULADO) {
            ServidorSimuladoInterceptor(
                almacen = AlmacenServidorSimulado(
                    archivo = File(appContext.filesDir, "servidor_simulado.json"),
                    gson = gson,
                    datosIniciales = { RepositorioSimulado.recordatorios().map { it.aDto() } }
                ),
                gson = gson,
                hayConexionReal = { monitor.hayConexion() }
            )
        } else null

    /** null cuando se usa un servidor real (no hay nada que simular). */
    val controlServidor: ControlServidorSimulado? = servidorSimulado

    private val api = ClienteRed.crearApi(gson = gson, interceptorExtra = servidorSimulado)

    private val sincronizador = SincronizadorRecordatorios(
        dao = database.recordatorioDao(),
        api = api,
        hayConexion = { monitor.hayConexion() }
    )

    val repositorio: RecordatoriosRepository = RepositorioRecordatoriosLocalFirst(
        dao = database.recordatorioDao(),
        preferencias = preferencias,
        sincronizador = sincronizador,
        conectado = conectado,
        alcanceApp = alcanceApp
    )

    /** Sincroniza al abrir la app y cada vez que vuelve la conexión. */
    fun iniciarSincronizacionAutomatica() {
        alcanceApp.launch {
            conectado.filter { it }.collect { sincronizador.sincronizar() }
        }
    }
}

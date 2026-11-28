package com.example.recordatorios.data

import android.content.Context
import com.example.recordatorios.BuildConfig
import com.example.recordatorios.data.auth.AutenticacionFirebase
import com.example.recordatorios.data.auth.AutenticacionRepository
import com.example.recordatorios.data.auth.EstadoSesion
import com.example.recordatorios.data.firebase.FuenteRemotaFirestore
import com.example.recordatorios.data.local.RecordatoriosDatabase
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.preferencias.PreferenciasDataStore
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.dataStorePreferencias
import com.example.recordatorios.data.remote.ClienteRed
import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.FuenteRemotaRest
import com.example.recordatorios.data.remote.simulado.AlmacenServidorSimulado
import com.example.recordatorios.data.remote.simulado.ControlServidorSimulado
import com.example.recordatorios.data.remote.simulado.ServidorSimuladoInterceptor
import com.example.recordatorios.data.sync.MonitorConectividad
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Contenedor de dependencias "a mano". Se crea UNA vez en RecordatoriosApplication:
 * una sola base de datos, un solo DataStore, una sola instancia de Firebase y un sincronizador.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Alcance de toda la app: sigue vivo aunque se cierre una pantalla. */
    val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val monitor = MonitorConectividad(appContext)

    val conectado: StateFlow<Boolean> =
        monitor.conectado.stateIn(alcanceApp, SharingStarted.Eagerly, monitor.hayConexion())

    private val database = RecordatoriosDatabase.obtener(appContext)

    val preferencias: PreferenciasRepository = PreferenciasDataStore(appContext.dataStorePreferencias)

    /* ---------- Firebase (Actividad 4) ---------- */

    val autenticacion: AutenticacionRepository = AutenticacionFirebase(
        FirebaseAuth.getInstance(),
        // Solo un booleano ("había sesión"), nunca credenciales
        appContext.getSharedPreferences("estado_sesion", Context.MODE_PRIVATE)
    )

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Caché solo en memoria: Room ya es la copia local, así no se duplican en disco
        // datos de la nube (tampoco quedan datos de otra sesión guardados por el SDK).
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }

    /** uid de la sesión activa (null = sin sesión). */
    val uidSesion: Flow<String?> = autenticacion.sesion
        .map { (it as? EstadoSesion.Activa)?.uid }
        .distinctUntilChanged()

    /* ---------- Fuente remota: Firestore (o REST de la Actividad 3) ---------- */

    private var servidorSimulado: ServidorSimuladoInterceptor? = null

    private val fuenteRemota: FuenteRemotaRecordatorios =
        if (BuildConfig.FUENTE_REMOTA == "rest") crearFuenteRest() else FuenteRemotaFirestore(firestore)

    /** Solo existe si se usa el servidor REST simulado. Con Firestore es null. */
    val controlServidor: ControlServidorSimulado? get() = servidorSimulado

    private val sincronizador = SincronizadorRecordatorios(
        dao = database.recordatorioDao(),
        remota = fuenteRemota,
        hayConexion = { monitor.hayConexion() },
        uidActual = { autenticacion.uidActual }
    )

    val repositorio: RecordatoriosRepository = RepositorioRecordatoriosLocalFirst(
        dao = database.recordatorioDao(),
        preferencias = preferencias,
        sincronizador = sincronizador,
        remota = fuenteRemota,
        conectado = conectado,
        uidSesion = uidSesion,
        uidActual = { autenticacion.uidActual },
        alcanceApp = alcanceApp
    )

    /**
     * Al abrir la app: verifica la sesión guardada, limpia registros sin dueño
     * (de versiones anteriores) y sincroniza cada vez que hay red Y sesión
     * (al iniciar sesión, al volver la conexión).
     */
    fun iniciar() {
        alcanceApp.launch {
            runCatching { database.recordatorioDao().eliminarSinDuenio() }
            autenticacion.verificarSesionGuardada()
        }
        alcanceApp.launch {
            combine(conectado, uidSesion) { hayRed, uid -> hayRed to uid }
                .distinctUntilChanged()
                .collect { (hayRed, uid) ->
                    if (uid == null) sincronizador.reiniciarEstado()
                    else if (hayRed) sincronizador.sincronizar()
                }
        }
    }

    private fun crearFuenteRest(): FuenteRemotaRecordatorios {
        val gson = Gson()
        val interceptor = if (BuildConfig.USAR_SERVIDOR_SIMULADO) {
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
        servidorSimulado = interceptor
        return FuenteRemotaRest(ClienteRed.crearApi(gson = gson, interceptorExtra = interceptor))
    }
}

package com.example.recordatorios.data.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * ÚNICA instancia de DataStore de la app: el delegado preferencesDataStore
 * garantiza que haya una sola por archivo. Solo guarda configuraciones simples,
 * nunca recordatorios ni datos sensibles (eso va en Room).
 */
val Context.dataStorePreferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias_usuario")

class PreferenciasDataStore(
    private val dataStore: DataStore<Preferences>
) : PreferenciasRepository {

    // Claves tipadas
    private object Claves {
        val TEMA = stringPreferencesKey(NombresPreferencia.TEMA)
        val MOSTRAR_COMPLETADOS = booleanPreferencesKey(NombresPreferencia.MOSTRAR_COMPLETADOS)
        val ORDEN = stringPreferencesKey(NombresPreferencia.ORDEN)
    }

    override val preferencias: Flow<PreferenciasUsuario> = dataStore.data
        .map { prefs ->
            interpretarPreferencias(
                temaGuardado = prefs[Claves.TEMA],
                mostrarCompletadosGuardado = prefs[Claves.MOSTRAR_COMPLETADOS],
                ordenGuardado = prefs[Claves.ORDEN]
            )
        }
        .catch { e ->
            // Error de lectura recuperable (archivo dañado, disco): se usan los valores
            // predeterminados y se avisa en Ajustes. Otros errores sí se propagan.
            if (e is IOException) {
                emit(interpretarPreferencias(null, null, null, errorLectura = true))
            } else {
                throw e
            }
        }

    // edit() es suspendida y corre fuera del hilo principal
    override suspend fun cambiarTema(tema: TemaApp) {
        dataStore.edit { it[Claves.TEMA] = tema.name }
    }

    override suspend fun cambiarMostrarCompletados(mostrar: Boolean) {
        dataStore.edit { it[Claves.MOSTRAR_COMPLETADOS] = mostrar }
    }

    override suspend fun cambiarOrden(orden: OrdenRecordatorios) {
        dataStore.edit { it[Claves.ORDEN] = orden.name }
    }

    override suspend fun restablecer() {
        dataStore.edit { it.clear() }
    }

    override suspend fun guardarValorInvalidoDePrueba() {
        dataStore.edit { it[Claves.TEMA] = "VIOLETA" }
    }
}

package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecordatorioEntity::class], version = 1, exportSchema = false)
abstract class RecordatoriosDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        private const val NOMBRE = "recordatorios.db"

        @Volatile
        private var instancia: RecordatoriosDatabase? = null

        /** Una única instancia para toda la app (patrón singleton con doble chequeo). */
        fun obtener(context: Context): RecordatoriosDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecordatoriosDatabase::class.java,
                    NOMBRE
                ).build().also { instancia = it }
            }
    }
}

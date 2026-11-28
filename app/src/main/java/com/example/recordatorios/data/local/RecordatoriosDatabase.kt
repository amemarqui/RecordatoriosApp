package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecordatorioEntity::class], version = 2, exportSchema = false)
abstract class RecordatoriosDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        private const val NOMBRE = "recordatorios.db"

        /**
         * Versión 1 (Actividad 3) -> 2 (Actividad 4): agrega la columna uid.
         * Se usa una migración real en lugar de una destructiva para no perder
         * cambios pendientes que todavía no se sincronizaron.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordatorios ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_uid ON recordatorios (uid)")
            }
        }

        @Volatile
        private var instancia: RecordatoriosDatabase? = null

        /** Una única instancia para toda la app. */
        fun obtener(context: Context): RecordatoriosDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecordatoriosDatabase::class.java,
                    NOMBRE
                )
                    .addMigrations(MIGRACION_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}

package com.example.halifaxtransit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.halifaxtransit.models.Route
import com.example.halifaxtransit.models.FavouriteLocation

@Database(
    entities = [
        Route::class,
        FavouriteLocation::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routesDao(): RoutesDao
    abstract fun favouriteLocationDao(): FavouriteLocationDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // MIGRATION: add favourite column to Routes
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE Routes ADD COLUMN favourite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // MIGRATION: add FavouriteLocations table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS FavouriteLocations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        lat REAL NOT NULL,
                        lon REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "HalifaxTransit.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }

    }
}

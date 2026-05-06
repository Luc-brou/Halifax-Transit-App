package com.example.halifaxtransit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.halifaxtransit.models.Route

@Database(entities = [Route::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routesDao(): RoutesDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // MIGRATION: add favourite column
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE Routes ADD COLUMN favourite INTEGER NOT NULL DEFAULT 0"
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
                    .addMigrations(MIGRATION_1_2)
                    .createFromAsset("RoutesData.db")
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

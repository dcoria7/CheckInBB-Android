package com.dc.checkinbb.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BabyEntity::class, FeedingRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedingDao(): FeedingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Usado por el widget (fuera del grafo de Hilt) */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "checkinbb_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

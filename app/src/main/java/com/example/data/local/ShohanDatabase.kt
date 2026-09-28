package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChatMessageEntity::class, ProactiveReminderEntity::class], version = 2, exportSchema = false)
abstract class ShohanDatabase : RoomDatabase() {
    abstract fun shohanDao(): ShohanDao

    companion object {
        @Volatile
        private var INSTANCE: ShohanDatabase? = null

        fun getInstance(context: Context): ShohanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShohanDatabase::class.java,
                    "shohan_ai.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

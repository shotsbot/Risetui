package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: BrowserDatabase? = null

    fun getDatabase(context: Context): BrowserDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                BrowserDatabase::class.java,
                "browser_database"
            ).fallbackToDestructiveMigration()
            .build()
            INSTANCE = instance
            instance
        }
    }
}

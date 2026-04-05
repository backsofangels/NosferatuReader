package com.nosferatu.launcher.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nosferatu.launcher.data.BookDao
import com.nosferatu.launcher.data.EbookEntity
import com.nosferatu.launcher.data.TocDao
import com.nosferatu.launcher.data.TocEntryEntity

@Database(entities = [EbookEntity::class, TocEntryEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun tocDao(): TocDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nosferatu_library.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
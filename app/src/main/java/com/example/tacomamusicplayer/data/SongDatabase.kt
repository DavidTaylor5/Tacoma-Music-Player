package com.example.wrappedmp3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SongEntry::class], version = 1, exportSchema = false)
abstract class SongDatabase: RoomDatabase() {

    abstract fun songEntryDao(): SongEntryDao

    companion object {
        @Volatile
        private var INSTANCE: SongDatabase? = null

        fun getDatabase(context: Context): SongDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, SongDatabase::class.java, "song_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }





}
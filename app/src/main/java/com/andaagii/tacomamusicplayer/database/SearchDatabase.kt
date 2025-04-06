package com.andaagii.tacomamusicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.andaagii.tacomamusicplayer.data.SearchData

@Database(entities = [SearchData::class], version = 9, exportSchema = false)
abstract class SearchDatabase : RoomDatabase() {

    abstract fun playlistDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: SearchDatabase? = null

        fun getDatabase(context: Context): SearchDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, SearchDatabase::class.java, "search_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

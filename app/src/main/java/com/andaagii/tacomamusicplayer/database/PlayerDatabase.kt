package com.andaagii.tacomamusicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.data.SearchData

@Database(entities = [Playlist::class, SearchData::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PlayerDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: PlayerDatabase? = null

        fun getDatabase(context: Context): PlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, PlayerDatabase::class.java, "player_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

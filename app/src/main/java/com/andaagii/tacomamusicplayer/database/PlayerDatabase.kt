package com.andaagii.tacomamusicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.entity.*

@Database(
    entities = [
        SongEntity::class,
        SongGroupEntity::class,
        SongGroupCrossRefEntity::class],
    version = 14,
    exportSchema = false
)
abstract class PlayerDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun songGroupDao(): SongGroupDao

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

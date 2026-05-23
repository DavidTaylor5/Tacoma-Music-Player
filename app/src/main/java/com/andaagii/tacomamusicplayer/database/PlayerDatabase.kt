package com.andaagii.tacomamusicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.entity.*

/**
 * Room database singleton for the Tacoma Music Player.
 *
 * Declares three entities: [SongEntity] (individual tracks), [SongGroupEntity] (albums and
 * playlists), and [SongGroupCrossRefEntity] (the junction table linking playlists to tracks).
 *
 * **Schema version:** 23. `fallbackToDestructiveMigration()` is configured — any schema
 * change wipes and rebuilds all tables. Bump the version and plan for data loss accordingly.
 *
 * Obtain the singleton via [getDatabase]. In application code, prefer injecting [SongDao]
 * or [SongGroupDao] directly through Hilt ([DatabaseModule]) rather than accessing this
 * class directly.
 */
@Database(
    entities = [
        SongEntity::class,
        SongGroupEntity::class,
        SongGroupCrossRefEntity::class],
    version = 23,
    exportSchema = false
)
abstract class PlayerDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun songGroupDao(): SongGroupDao

    companion object {
        @Volatile
        private var INSTANCE: PlayerDatabase? = null

        /**
         * Returns the application-wide [PlayerDatabase] singleton, creating it on first call.
         *
         * Uses double-checked locking with a `@Volatile` backing field to guarantee only one
         * instance is created even when multiple threads call this simultaneously.
         *
         * @param context Used to locate the database file on disk. Pass the application context
         *   to avoid leaking an Activity or Service reference.
         */
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

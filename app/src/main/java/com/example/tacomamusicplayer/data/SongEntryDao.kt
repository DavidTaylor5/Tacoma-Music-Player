package com.example.tacomamusicplayer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntry)

    @Delete
    suspend fun delete(song:SongEntry)

    @Update
    suspend fun update(song: SongEntry)

    @Query("SELECT * from songs WHERE id = :id")
    fun getSong(id: Int): Flow<SongEntry>

    @Query("SELECT * from songs ORDER BY name ASC")
    fun getAllSongs(): Flow<List<SongEntry>>
}
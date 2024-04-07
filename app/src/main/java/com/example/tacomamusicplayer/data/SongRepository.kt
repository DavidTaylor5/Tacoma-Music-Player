package com.example.tacomamusicplayer.data

import kotlinx.coroutines.flow.Flow

interface SongRepository {

    /**
     * Retrieve all the songs from the given data source.
     */
    fun getAllSongsStream(): Flow<List<SongEntry>>

    /**
     * Retrieve an song from the given data source that matches with the [id].
     */
    fun getSongStream(id: Int): Flow<SongEntry>

    /**
     * Insert song in the data source
     */
    suspend fun insertSong(song: SongEntry)

    /**
     * Delete song from the data source
     */
    suspend fun deleteSong(song: SongEntry)

    /**
     * Update song in the data source
     */
    suspend fun updateSong(song: SongEntry)
}
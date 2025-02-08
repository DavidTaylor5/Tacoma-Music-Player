package com.example.tacomamusicplayer.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tacomamusicplayer.data.Playlist

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlist")
    fun getAllPlaylists(): LiveData<List<Playlist>> //For now I'll use this one...

    @Query("SELECT * FROM playlist WHERE uid IN (:playlistIds)")
    fun loadAllByIds(playlistIds: IntArray): LiveData<List<Playlist>>

    @Query("SELECT * FROM playlist WHERE playlist_title LIKE :title LIMIT 1")
    fun findPlaylistByName(title: String): Playlist

    @Update
    fun updatePlaylists(vararg playlist: Playlist)

    @Insert
    fun insertPlaylists(vararg playlists: Playlist)

    @Delete
    fun deletePlaylists(playlist: Playlist)
}

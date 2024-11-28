package com.example.tacomamusicplayer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {

    //TODO I need to rename / add / some functions so that this is more clear
    @Query("SELECT * FROM playlist")
    fun getAll(): LiveData<List<Playlist>> //For now I'll use this one...

    @Query("SELECT * FROM playlist WHERE uid IN (:playlistIds)")
    fun loadAllByIds(playlistIds: IntArray): LiveData<List<Playlist>>

    @Query("SELECT * FROM playlist WHERE playlist_title LIKE :title LIMIT 1")
    fun findByName(title: String): Playlist

    @Insert
    fun insertAll(vararg playlists: Playlist) //this one...

    @Delete
    fun delete(playlist: Playlist) //this one...
}

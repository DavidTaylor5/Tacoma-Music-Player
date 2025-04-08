package com.andaagii.tacomamusicplayer.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.data.SearchData

@Dao
interface SearchDao {
    @Query("SELECT * FROM searchdata")
    fun getAllPlaylists(): LiveData<List<SearchData>>

//    @Query("SELECT * FROM searchdata WHERE title LIKE :title LIMIT 1")
//    fun findItemFromTitle(title: String): SearchData
//
//    fun findItemFromSongTitle(): SearchData
//
//    fun findItemFromAlbumTitle(): SearchData
//
//    fun findItemFromArtist(): SearchData

    @Update
    fun updateItems(vararg item: SearchData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(vararg item: SearchData)

    @Delete
    fun deleteItems(vararg item: SearchData)
}

package com.andaagii.tacomamusicplayer.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andaagii.tacomamusicplayer.database.entity.SongEntity

//TODO return values as FLOW


/**
 * Store information on Songs.
 */
@Dao
interface SongDao {
    @Query("SELECT * FROM song_table")
    fun getAllSongs(): LiveData<List<SongEntity>>

    @Update
    fun updateItems(vararg item: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(vararg item: SongEntity)

    @Delete
    fun deleteItems(vararg item: SongEntity)

    /**
     * Search through search data descriptions to determine return results, case insensitive.
     */
    @Query("SELECT * FROM song_table WHERE LOWER(search_description) LIKE '%' || LOWER(:search) || '%'")
    fun findDescriptionFromSearchStr(search: String): List<SongEntity>
}
package com.andaagii.tacomamusicplayer.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

//TODO return values as FLOW

/**
 * Store information on SongGroups. A SongGroup can be either an Album or a Playlist.
 */
@Dao
interface SongGroupDao {

    @Query("SELECT * FROM song_group_table")
    fun getAllSongGroups(): LiveData<List<SongGroupEntity>>

    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    fun getSongGroupsByType(type: SongGroupType): LiveData<List<SongGroupEntity>>

    @Query("SELECT * FROM song_group_table WHERE group_title LIKE :title LIMIT 1")
    fun findSongGroupByName(title: String): SongGroupEntity?

    @Query("SELECT * FROM song_group_table WHERE search_description = :description LIMIT 1")
    fun findSongGroupByDescription(description: String): SongGroupEntity?

    @Update
    fun updateSongGroups(vararg songGroup: SongGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSongGroups(vararg songGroup: SongGroupEntity)

    @Delete
    fun deleteSongGroups(vararg songGroup: SongGroupEntity)

    /**
     * Search through search data descriptions to determine return results, case insensitive.
     */
    @Query("SELECT * FROM song_group_table WHERE LOWER(search_description) LIKE '%' || LOWER(:search) || '%'")
    fun findDescriptionFromSearchStr(search: String): List<SongGroupEntity>
}

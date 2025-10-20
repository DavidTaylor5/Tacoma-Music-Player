package com.andaagii.tacomamusicplayer.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupCrossRefEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import kotlinx.coroutines.flow.Flow

//TODO return values as FLOW

/**
 * Store information on SongGroups. A SongGroup can be either an Album or a Playlist.
 */
@Dao
interface SongGroupDao {

    @Query("SELECT * FROM song_group_table")
    fun getAllSongGroups(): LiveData<List<SongGroupEntity>>

    //TODO I can mirror functions, having one function for one-shot, one function for flow of data.

    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    fun getSongGroupsByType(type: SongGroupType): List<SongGroupEntity>

    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    fun getSongGroupsByTypeFlow(type: SongGroupType): Flow<List<SongGroupEntity>>

    @Query("SELECT * FROM song_group_table WHERE group_title LIKE :title LIMIT 1")
    fun findSongGroupByName(title: String): SongGroupEntity?

    @Query("SELECT * FROM song_group_table WHERE search_description = :description LIMIT 1")
    fun findSongGroupByDescription(description: String): SongGroupEntity?

    @Query("DELETE FROM song_ref_table WHERE groupTitle = :playlistName AND searchDescription = :songDescription")
    fun deleteSongFromPlaylist(playlistName: String, songDescription: String)

    @Delete
    fun deleteSongsFromPlaylist(vararg songGroupRef: SongGroupCrossRefEntity)

    @Query("DELETE FROM song_ref_table WHERE groupTitle = :playlistName")
    fun deleteAllSongsFromPlaylist(playlistName: String)

    @Query("SELECT * FROM song_group_table WHERE group_artist = :artist")
    fun findAllSongGroupsByArtist(artist: String): List<SongGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRef(vararg songGroupRef: SongGroupCrossRefEntity)

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

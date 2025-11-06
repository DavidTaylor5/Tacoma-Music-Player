package com.andaagii.tacomamusicplayer.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    suspend fun getSongGroupsByType(type: SongGroupType): List<SongGroupEntity>

    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    fun getSongGroupsByTypeFlow(type: SongGroupType): Flow<List<SongGroupEntity>>

    @Query("SELECT * FROM song_group_table WHERE group_title LIKE :title LIMIT 1")
    suspend fun findSongGroupByName(title: String): SongGroupEntity?

    @Query("SELECT * FROM song_group_table WHERE search_description = :description LIMIT 1")
    suspend fun findSongGroupByDescription(description: String): SongGroupEntity?

    @Delete
    suspend fun deleteSongsFromPlaylist(vararg songGroupRef: SongGroupCrossRefEntity)

    @Query("SELECT * FROM song_group_table WHERE group_artist = :artist")
    suspend fun findAllSongGroupsByArtist(artist: String): List<SongGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRef(vararg songGroupRef: SongGroupCrossRefEntity)

    @Update
    suspend fun updateSongGroups(vararg songGroup: SongGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongGroups(vararg songGroup: SongGroupEntity)

    @Delete
    suspend fun deleteSongGroups(vararg songGroup: SongGroupEntity)

    /**
     * Search through search data descriptions to determine return results, case insensitive.
     */
    @Query("""
        SELECT * FROM song_group_table 
        WHERE LOWER(search_description) LIKE '%' || LOWER(:search) || '%'
        ORDER BY INSTR( LOWER(search_description), LOWER(:search))
        LIMIT 25
    """)
    suspend fun findDescriptionFromSearchStr(search: String): List<SongGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(vararg songGroupRef: SongGroupCrossRefEntity)

    @Update
    suspend fun updatePlaylistSong(vararg songGroupRef: SongGroupCrossRefEntity)

    @Query("SELECT * FROM song_ref_table WHERE groupId = :groupId ORDER BY position ASC")
    suspend fun selectSongsFromPlaylist(groupId: Int): List<SongGroupCrossRefEntity>

    @Query("DELETE FROM song_ref_table WHERE groupId = :groupId AND searchDescription = :songDescription AND position = :position")
    suspend fun deleteSongFromPlaylist(groupId: Int, songDescription: String, position: Int)

    @Query("DELETE FROM song_ref_table WHERE groupId = :groupId")
    suspend fun deleteAllSongsFromPlaylist(groupId: Int)
}
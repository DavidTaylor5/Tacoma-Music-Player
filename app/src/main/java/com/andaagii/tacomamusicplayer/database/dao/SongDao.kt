package com.andaagii.tacomamusicplayer.database.dao

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
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM song_table WHERE album_title = :albumTitle")
    suspend fun getAllSongsFromAlbum(albumTitle: String): List<SongEntity>

    @Query("SELECT * FROM song_table WHERE album_title = :artist")
    suspend fun getAllSongsFromArtist(artist: String): List<SongEntity>

    @Query("""
        SELECT s.* FROM song_table AS s
        INNER JOIN song_ref_table AS p
        ON s.search_description = p.searchDescription
        WHERE p.groupId = :groupId
    """)
    suspend fun selectAllSongsFromPlaylist(groupId: Int): List<SongEntity>

    @Query("SELECT * FROM song_table WHERE song_name = :songName")
    suspend fun queryAllSongsWithSongName(songName: String): List<SongEntity>

    @Update
    suspend fun updateItems(vararg item: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(vararg item: SongEntity)

    @Delete
    suspend fun deleteItems(vararg item: SongEntity)

    @Query("SELECT * FROM song_table WHERE search_description = :searchDescription")
    suspend fun findSongFromSearchDescription(searchDescription: String): List<SongEntity>

    /**
     * Search through search data descriptions to determine return results, case insensitive.
     */
    @Query("""
        SELECT * FROM song_table 
        WHERE LOWER(search_description) LIKE '%' || LOWER(:search) || '%'
        ORDER BY INSTR( LOWER(search_description), LOWER(:search))
        LIMIT 25
    """)
    suspend fun findDescriptionFromSearchStr(search: String): List<SongEntity>
}
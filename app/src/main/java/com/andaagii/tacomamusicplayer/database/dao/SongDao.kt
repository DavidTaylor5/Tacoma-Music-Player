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
    fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM song_table WHERE album_title = :albumTitle")
    fun getAllSongsFromAlbum(albumTitle: String): List<SongEntity>

    @Query("SELECT * FROM song_table WHERE album_title = :artist")
    fun getAllSongsFromArtist(artist: String): List<SongEntity>

    @Query("SELECT DISTINCT song_artist FROM song_table")
    fun getAllArtists(): List<String>

    @Query("""
        SELECT s.* FROM song_table AS s
        INNER JOIN song_ref_table AS p
        ON s.search_description = p.searchDescription
        WHERE p.groupTitle = :playlistName
    """)
    fun selectAllSongsFromPlaylist(playlistName: String): List<SongEntity>

    @Query("SELECT * FROM song_table WHERE song_name = :songName")
    fun queryAllSongsWithSongName(songName: String): List<SongEntity>

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
package com.andaagii.tacomamusicplayer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andaagii.tacomamusicplayer.database.entity.SongEntity

/**
 * DAO for [SongEntity] operations against `song_table`.
 *
 * All functions are `suspend` and intended to be called from a coroutine running on
 * `Dispatchers.IO`. None expose reactive [kotlinx.coroutines.flow.Flow] or
 * [androidx.lifecycle.LiveData] — callers are responsible for re-querying when the
 * library changes.
 */
@Dao
interface SongDao {

    /** Returns every track in the library, in undefined order. */
    @Query("SELECT * FROM song_table")
    suspend fun getAllSongs(): List<SongEntity>

    /**
     * Returns all tracks whose album title exactly matches [albumTitle].
     *
     * @param albumTitle The album title to filter by (case-sensitive).
     */
    @Query("SELECT * FROM song_table WHERE album_title = :albumTitle")
    suspend fun getAllSongsFromAlbum(albumTitle: String): List<SongEntity>

    /**
     * Intended to return all tracks by a given artist, but currently filters by
     * `album_title` instead of `song_artist` due to a column mismatch in the query.
     *
     * **Known bug:** the SQL reads `WHERE album_title = :artist`, so results will be
     * empty unless an album title happens to equal the artist string.
     *
     * @param artist The artist name to search for (currently matched against album_title).
     */
    @Query("SELECT * FROM song_table WHERE album_title = :artist")
    suspend fun getAllSongsFromArtist(artist: String): List<SongEntity>

    /**
     * Returns all tracks that belong to the playlist identified by [groupId], by joining
     * `song_table` with `song_ref_table` on `search_description`.
     *
     * Note: results are not ordered by `position` in this query. Use
     * [SongGroupDao.selectSongsFromPlaylist] first to get the ordered cross-references,
     * then look up tracks individually if order must be preserved.
     *
     * @param groupId The [com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity.groupId]
     *   of the playlist.
     */
    @Query("""
        SELECT s.* FROM song_table AS s
        INNER JOIN song_ref_table AS p
        ON s.search_description = p.searchDescription
        WHERE p.groupId = :groupId
    """)
    suspend fun selectAllSongsFromPlaylist(groupId: Int): List<SongEntity>

    /**
     * Returns all tracks whose display name exactly matches [songName].
     *
     * May return multiple results when different albums contain a track with the same title.
     *
     * @param songName The track name to match (case-sensitive).
     */
    @Query("SELECT * FROM song_table WHERE song_name = :songName")
    suspend fun queryAllSongsWithSongName(songName: String): List<SongEntity>

    /** Updates the given track rows in place. Fails silently if a row no longer exists. */
    @Update
    suspend fun updateItems(vararg item: SongEntity)

    /**
     * Inserts one or more tracks, replacing any existing row with the same
     * [SongEntity.searchDescription] primary key.
     *
     * `REPLACE` is chosen over `IGNORE` so that re-cataloging the library always refreshes
     * stale metadata (artwork paths, duration) without requiring a separate update call.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(vararg item: SongEntity)

    /** Deletes the given track rows from the table. */
    @Delete
    suspend fun deleteItems(vararg item: SongEntity)

    /**
     * Returns all tracks whose [SongEntity.searchDescription] exactly matches [searchDescription].
     *
     * Returns a list rather than a single item because the primary key is unique, but the
     * caller receives a list for a consistent return type across query functions.
     *
     * @param searchDescription The composite `"songName_albumTitle_artist"` key to look up.
     */
    @Query("SELECT * FROM song_table WHERE search_description = :searchDescription")
    suspend fun findSongFromSearchDescription(searchDescription: String): List<SongEntity>

    /**
     * Full-text search over [SongEntity.searchDescription], case-insensitive.
     *
     * Results are ordered by match position within the string so that prefix matches
     * (e.g., a title that *starts* with the query) rank above mid-string matches.
     * Limited to 25 results to keep response times predictable on large libraries.
     *
     * @param search The search term to match against `search_description`.
     * @return Up to 25 [SongEntity] rows ordered by match proximity.
     */
    @Query("""
        SELECT * FROM song_table
        WHERE LOWER(search_description) LIKE '%' || LOWER(:search) || '%'
        ORDER BY INSTR( LOWER(search_description), LOWER(:search))
        LIMIT 25
    """)
    suspend fun findDescriptionFromSearchStr(search: String): List<SongEntity>
}

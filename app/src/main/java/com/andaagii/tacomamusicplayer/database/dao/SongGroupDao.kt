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

/**
 * DAO for [SongGroupEntity] (albums and playlists) and [SongGroupCrossRefEntity] (playlist
 * track membership) operations against `song_group_table` and `song_ref_table`.
 *
 * Reactive queries are available in two forms: [getAllSongGroups] returns a [LiveData] for
 * legacy observers, while [getSongGroupsByTypeFlow] returns a [Flow] for coroutine-based
 * collectors. One-shot reads use `suspend` functions.
 */
@Dao
interface SongGroupDao {

    /**
     * Returns a [LiveData] that emits the full list of all groups (albums and playlists)
     * whenever the table changes.
     *
     * Observed by legacy `LiveData` observers; prefer [getSongGroupsByTypeFlow] for new
     * coroutine-based callers.
     */
    @Query("SELECT * FROM song_group_table")
    fun getAllSongGroups(): LiveData<List<SongGroupEntity>>

    /**
     * Returns a deduplicated list of artist names for groups of the given [type].
     *
     * Defaults to [SongGroupType.ALBUM] since artist browsing applies to albums. Playlist
     * groups are skipped by default because playlists have no single attributed artist.
     *
     * @param type The group type to filter by; defaults to [SongGroupType.ALBUM].
     */
    @Query("SELECT DISTINCT group_artist FROM song_group_table WHERE song_group_type = :type")
    suspend fun getAllArtists(type: SongGroupType = SongGroupType.ALBUM): List<String>

    /**
     * One-shot fetch of all groups matching [type].
     *
     * Use [getSongGroupsByTypeFlow] if the caller needs to react to future table changes.
     *
     * @param type The [SongGroupType] to filter by (e.g., `ALBUM` or `PLAYLIST`).
     */
    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    suspend fun getSongGroupsByType(type: SongGroupType): List<SongGroupEntity>

    /**
     * Returns a [Flow] that re-emits all groups of [type] whenever the table changes.
     *
     * Prefer this over [getSongGroupsByType] when the caller is a ViewModel that needs to
     * stay in sync with library updates without polling.
     *
     * @param type The [SongGroupType] to filter by.
     */
    @Query("SELECT * FROM song_group_table WHERE song_group_type = :type")
    fun getSongGroupsByTypeFlow(type: SongGroupType): Flow<List<SongGroupEntity>>

    /**
     * Finds a single group whose title matches [title] via a `LIKE` pattern.
     *
     * Returns `null` when no match is found. Because `LIKE` is used without wildcards,
     * this performs a case-insensitive exact match on `group_title`.
     *
     * @param title The exact group title to search for.
     * @return The first matching [SongGroupEntity], or `null`.
     */
    @Query("SELECT * FROM song_group_table WHERE group_title LIKE :title LIMIT 1")
    suspend fun findSongGroupByName(title: String): SongGroupEntity?

    /**
     * Finds a single group whose [SongGroupEntity.searchDescription] exactly matches
     * [description].
     *
     * @param description The composite search description to look up.
     * @return The matching [SongGroupEntity], or `null` if none exists.
     */
    @Query("SELECT * FROM song_group_table WHERE search_description = :description LIMIT 1")
    suspend fun findSongGroupByDescription(description: String): SongGroupEntity?

    /**
     * Deletes the given [SongGroupCrossRefEntity] rows from `song_ref_table`.
     *
     * Removes specific track memberships from a playlist without deleting the playlist
     * itself or the underlying [SongGroupEntity]. Pass the cross-reference objects
     * retrieved from [selectSongsFromPlaylist] to target precise rows.
     */
    @Delete
    suspend fun deleteSongsFromPlaylist(vararg songGroupRef: SongGroupCrossRefEntity)

    /**
     * Returns all groups whose [SongGroupEntity.groupArtist] exactly matches [artist].
     *
     * @param artist The artist name to match (case-sensitive).
     */
    @Query("SELECT * FROM song_group_table WHERE group_artist = :artist")
    suspend fun findAllSongGroupsByArtist(artist: String): List<SongGroupEntity>

    /**
     * Inserts one or more [SongGroupCrossRefEntity] rows, replacing any existing row with
     * the same primary key.
     *
     * `REPLACE` is used so that re-adding a song that already exists in a playlist updates
     * its position rather than causing a conflict error. Functionally equivalent to
     * [insertPlaylistSongs]; prefer one consistently within a call site.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRef(vararg songGroupRef: SongGroupCrossRefEntity)

    /** Updates the given [SongGroupEntity] rows in place. */
    @Update
    suspend fun updateSongGroups(vararg songGroup: SongGroupEntity)

    /**
     * Inserts one or more [SongGroupEntity] rows, replacing any existing row with the same
     * [SongGroupEntity.groupId].
     *
     * `REPLACE` is chosen so that re-cataloging from MediaStore refreshes stale album
     * metadata without requiring a separate update call.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongGroups(vararg songGroup: SongGroupEntity)

    /**
     * Deletes the given [SongGroupEntity] rows from `song_group_table`.
     *
     * Because [SongGroupCrossRefEntity] declares a `CASCADE` foreign key on `groupId`,
     * deleting a group automatically removes all of its cross-reference rows from
     * `song_ref_table` without a separate call.
     */
    @Delete
    suspend fun deleteSongGroups(vararg songGroup: SongGroupEntity)

    /**
     * Full-text search over [SongGroupEntity.searchDescription], case-insensitive.
     *
     * Results are ordered by match position within the string so that prefix matches
     * rank above mid-string matches. Limited to 25 results.
     *
     * @param search The search term to match against `search_description`.
     * @return Up to 25 [SongGroupEntity] rows ordered by match proximity.
     */
    @Query("""
        SELECT * FROM song_group_table
        WHERE LOWER(search_description) LIKE '%' || LOWER(:search) || '%'
        ORDER BY INSTR( LOWER(search_description), LOWER(:search))
        LIMIT 25
    """)
    suspend fun findDescriptionFromSearchStr(search: String): List<SongGroupEntity>

    /**
     * Inserts one or more [SongGroupCrossRefEntity] rows, replacing any existing row with
     * the same primary key.
     *
     * Functionally identical to [insertRef]. Both functions exist for call-site clarity;
     * [insertRef] is used for general cross-reference inserts while this function is used
     * specifically when building a playlist's initial track list.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(vararg songGroupRef: SongGroupCrossRefEntity)

    /**
     * Updates one or more [SongGroupCrossRefEntity] rows in place.
     *
     * Used when reordering tracks within a playlist — the [SongGroupCrossRefEntity.position]
     * field is updated without changing the group or song references.
     */
    @Update
    suspend fun updatePlaylistSong(vararg songGroupRef: SongGroupCrossRefEntity)

    /**
     * Returns all [SongGroupCrossRefEntity] rows for [groupId], sorted by
     * [SongGroupCrossRefEntity.position] ascending.
     *
     * The ordered list of cross-references defines the playback order for the playlist.
     * Pass the [SongGroupCrossRefEntity.searchDescription] values to [SongDao] to
     * hydrate the full [com.andaagii.tacomamusicplayer.database.entity.SongEntity] objects.
     *
     * @param groupId The playlist's [SongGroupEntity.groupId].
     */
    @Query("SELECT * FROM song_ref_table WHERE groupId = :groupId ORDER BY position ASC")
    suspend fun selectSongsFromPlaylist(groupId: Int): List<SongGroupCrossRefEntity>

    /**
     * Deletes a single track from a playlist, identified by the combination of [groupId],
     * [songDescription], and [position].
     *
     * All three parameters are required to avoid accidentally removing a song that appears
     * more than once in the same playlist at different positions.
     *
     * @param groupId The playlist's [SongGroupEntity.groupId].
     * @param songDescription The [SongGroupCrossRefEntity.searchDescription] of the track to remove.
     * @param position The exact [SongGroupCrossRefEntity.position] of the row to delete.
     */
    @Query("DELETE FROM song_ref_table WHERE groupId = :groupId AND searchDescription = :songDescription AND position = :position")
    suspend fun deleteSongFromPlaylist(groupId: Int, songDescription: String, position: Int)

    /**
     * Deletes all cross-reference rows for [groupId], effectively clearing a playlist's
     * track list without deleting the playlist [SongGroupEntity] itself.
     *
     * @param groupId The playlist's [SongGroupEntity.groupId].
     */
    @Query("DELETE FROM song_ref_table WHERE groupId = :groupId")
    suspend fun deleteAllSongsFromPlaylist(groupId: Int)
}

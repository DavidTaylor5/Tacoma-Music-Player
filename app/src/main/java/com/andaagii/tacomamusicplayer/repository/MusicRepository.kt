package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Full read-write repository interface for all library and playlist operations.
 *
 * Extends [MusicProviderRepository] with write operations and reactive [Flow]-based queries.
 * The single concrete implementation is [MusicRepositoryImpl], bound as a singleton by Hilt.
 * ViewModels should inject this interface; [com.andaagii.tacomamusicplayer.service.MusicService]
 * injects the narrower [MusicProviderRepository] instead.
 *
 * All `suspend` functions switch to [kotlinx.coroutines.Dispatchers.IO] internally;
 * callers do not need to specify a dispatcher.
 */
interface MusicRepository: MusicProviderRepository {

    /**
     * Creates a new empty playlist with the given name and persists it to the database.
     *
     * @param playlistName Display name for the new playlist. Duplicate names are not currently
     *   enforced at this layer — callers should validate uniqueness before invoking.
     */
    suspend fun createPlaylist(playlistName: String)

    /**
     * Removes [songs] from the playlist identified by [playlistTitle].
     *
     * @param playlistTitle Exact title of the target playlist.
     * @param songs The tracks to remove, identified by their [SongEntity] records.
     */
    suspend fun removeSongsFromPlaylist(playlistTitle: String, songs: List<SongEntity>)

    /**
     * Returns a [Flow] that emits the full album list whenever the database changes.
     *
     * Suitable for observing in a ViewModel via `viewModelScope`; each emission reflects the
     * current state of all [com.andaagii.tacomamusicplayer.enumtype.SongGroupType.ALBUM] entries.
     */
    fun getAllAvailableAlbumsFlow(): Flow<List<MediaItem>>

    /**
     * Returns a [Flow] that emits the full playlist list whenever the database changes.
     *
     * Mirrors [getAllAvailableAlbumsFlow] for
     * [com.andaagii.tacomamusicplayer.enumtype.SongGroupType.PLAYLIST] entries.
     */
    fun getAllAvailablePlaylistFlow(): Flow<List<MediaItem>>

    /**
     * Sets the custom artwork for the song group (album or playlist) identified by [title].
     *
     * Updates the group's `artFileCustom` field and sets `useCustomArt = true`.
     *
     * @param title Exact title of the target album or playlist.
     * @param artFileName File name of the new custom artwork image.
     */
    suspend fun updateSongGroupImage(title: String, artFileName: String)

    /**
     * Propagates a custom artwork file to every individual track in [title].
     *
     * Sets `artFileCustom` and `useCustomArt = true` on each [SongEntity] that belongs to the
     * album, so per-song artwork lookups reflect the album-level override.
     *
     * @param title Exact album title whose tracks should be updated.
     * @param artFileName File name of the custom artwork to apply to all tracks.
     */
    suspend fun updateAlbumSongsWithCustomImage(title: String, artFileName: String)

    /**
     * Appends [songDescriptions] to the playlist identified by [playlistTitle].
     *
     * Each description is a `searchDescription` primary key from [SongEntity]. New songs are
     * appended after the existing last position using increments of 100 to leave room for future
     * insertions without full re-ordering.
     *
     * @param playlistTitle Exact title of the target playlist.
     * @param songDescriptions Ordered list of `searchDescription` keys for the songs to add.
     */
    suspend fun addSongsToPlaylist(playlistTitle: String, songDescriptions: List<String>)

    /**
     * Creates the persistent queue entry in the database if it does not already exist.
     *
     * The queue is stored as a special [com.andaagii.tacomamusicplayer.enumtype.SongGroupType.QUEUE]
     * group identified by [title]. This is called once on app startup to ensure the queue group
     * row is always present before any save/restore operations run.
     *
     * @param title The unique identifier for the queue group (typically [Const.PLAYLIST_QUEUE_TITLE]).
     */
    suspend fun createInitialQueueIfEmpty(title: String)

    /**
     * Replaces the track order of [playlistTitle] with the positions defined by [songDescriptions].
     *
     * Deletes all existing cross-reference rows for the playlist and re-inserts them with new
     * sequential positions, effectively performing a full reorder.
     *
     * @param playlistTitle Exact title of the playlist to reorder.
     * @param songDescriptions New ordered list of `searchDescription` keys; index 0 becomes position 0.
     */
    suspend fun updatePlaylistSongOrder(playlistTitle: String, songDescriptions: List<String>)

    /**
     * Renames the playlist currently titled [originalTitle] to [newTitle].
     *
     * No-ops silently if no playlist with [originalTitle] exists.
     *
     * @param originalTitle Current exact title of the playlist.
     * @param newTitle The replacement title to store.
     */
    suspend fun updatePlaylistTitle(originalTitle: String, newTitle: String)
}
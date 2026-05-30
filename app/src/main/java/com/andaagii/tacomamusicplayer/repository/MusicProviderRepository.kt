package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem

/**
 * Read-only repository interface used by [com.andaagii.tacomamusicplayer.service.MusicService]
 * to serve the Android Auto browsing hierarchy and voice-search results.
 *
 * All functions are `suspend` and run on the caller's coroutine context; implementations are
 * expected to switch to [kotlinx.coroutines.Dispatchers.IO] internally for any database or
 * file-system work.
 *
 * For write operations and reactive [kotlinx.coroutines.flow.Flow]-based queries, see
 * [MusicRepository].
 */
interface MusicProviderRepository {

    /**
     * Returns all albums in the library as [MediaItem]s, suitable for display or Auto browsing.
     *
     * @param useFileProviderUri When `true`, artwork URIs are wrapped as `content://` FileProvider
     *   URIs so that Android Auto (which cannot read raw `file://` paths) can load them.
     */
    suspend fun getAllAlbums(useFileProviderUri: Boolean = false): List<MediaItem>

    /**
     * Returns all distinct artists in the library as [MediaItem]s.
     */
    suspend fun getAllArtists(): List<MediaItem>

    /**
     * Returns all user-created playlists as [MediaItem]s.
     *
     * @param useFileProviderUri When `true`, artwork URIs are wrapped as `content://` FileProvider
     *   URIs required by Android Auto.
     */
    suspend fun getAllPlaylists(useFileProviderUri: Boolean = false): List<MediaItem>

    /**
     * Returns all albums whose primary artist matches [artist].
     *
     * @param artist The artist name to filter by (exact match).
     */
    suspend fun getAlbumsFromArtist(artist: String): List<MediaItem>

    /**
     * Returns all tracks in [albumTitle] in track-list order.
     *
     * Returns an empty list if no album with that title exists in the database.
     *
     * @param albumTitle Exact album title to look up.
     * @param useFileProviderUri When `true`, artwork URIs use the FileProvider scheme.
     */
    suspend fun getSongsFromAlbum(
        albumTitle: String,
        useFileProviderUri: Boolean = false
    ): List<MediaItem>

    /**
     * Returns all tracks in [playlistTitle] in their saved position order.
     *
     * Returns an empty list if no playlist with that title exists.
     *
     * @param playlistTitle Exact playlist title to look up.
     * @param useFileProviderUri When `true`, artwork URIs use the FileProvider scheme.
     */
    suspend fun getSongsFromPlaylist(
        playlistTitle: String,
        useFileProviderUri: Boolean = false
    ): List<MediaItem>

    /**
     * Returns all tracks whose song name matches [songTitle] (exact match).
     *
     * @param songTitle The track name to search for.
     */
    suspend fun getSongFromName(songTitle: String): List<MediaItem>

    /**
     * Full-text search across songs, albums, and playlists.
     *
     * Results are sorted by the position of [search] within the item's subtitle field so that
     * closer matches (e.g., a title that starts with the query) rank higher than partial matches.
     * Returns at most ~25 combined results across all three categories.
     *
     * @param search The query string; matching is case-insensitive.
     * @param useFileProviderUri When `true`, artwork URIs use the FileProvider scheme.
     */
    suspend fun searchMusic(
        search: String,
        useFileProviderUri: Boolean = false
    ): List<MediaItem>
}
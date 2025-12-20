package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem

interface MusicProviderRepository {

    suspend fun getAllAlbums(useFileProviderUri: Boolean = false): List<MediaItem>

    suspend fun getAllArtists(): List<MediaItem>

    suspend fun getAllPlaylists(useFileProviderUri: Boolean = false): List<MediaItem>

    suspend fun getAlbumsFromArtist(artist: String): List<MediaItem>

    suspend fun getSongsFromAlbum(
        albumTitle: String,
        useFileProviderUri: Boolean = false
    ): List<MediaItem>

    suspend fun getSongsFromPlaylist(
        playlistTitle: String,
        useFileProviderUri: Boolean = false
    ): List<MediaItem>

    suspend fun getSongFromName(songTitle: String): List<MediaItem>

    /**
     * Searches music database to return 25 relevant songs/albums/playlists
     */
    suspend fun searchMusic(
        search: String,
        useFileProviderUri: Boolean = false
    ): List<MediaItem>
}
package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import kotlinx.coroutines.flow.Flow

interface MusicProviderRepository {

    suspend fun getAllAlbums(useFileProviderUri: Boolean = false): List<MediaItem>

    suspend fun getAllArtists(): List<MediaItem>

    suspend fun getAllPlaylists(): List<MediaItem>

    suspend fun getAlbumsFromArtist(artist: String): List<MediaItem>

    suspend fun getSongsFromAlbum(albumTitle: String): List<MediaItem>

    suspend fun getSongsFromPlaylist(playlistTitle: String): List<MediaItem>

    suspend fun getSongFromName(songTitle: String): List<MediaItem>

    /**
     * Searches music database to return 25 relevant songs/albums/playlists
     */
    suspend fun searchMusic(search: String): List<MediaItem>
}
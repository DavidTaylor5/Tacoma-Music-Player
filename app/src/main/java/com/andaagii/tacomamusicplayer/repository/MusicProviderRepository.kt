package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity

interface MusicProviderRepository {

    suspend fun getAllAlbums(): List<MediaItem>

    suspend fun getAllArtists(): List<MediaItem>

    suspend fun getAllPlaylists(): List<MediaItem>

    suspend fun getAlbumsFromArtist(artist: String): List<MediaItem>

    suspend fun getSongsFromAlbum(albumTitle: String): List<MediaItem>

    suspend fun getSongsFromPlaylist(playlistTitle: String): List<MediaItem>

    suspend fun getSongFromName(songTitle: String): List<MediaItem>
}
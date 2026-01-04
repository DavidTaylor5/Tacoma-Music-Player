package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * I want my app to follow the repository pattern...
 * I will inject the database into this class,
 * then I will inject the repository into the viewmodel.
 * This will also allow be to work on testing my app.
 */
interface MusicRepository: MusicProviderRepository {

    /**
     * Creates a playlist in memory.
     * @param playlistName Name of a new playlist. TODO Don't allow two albums of the same name.
     */
    suspend fun createPlaylist(playlistName: String)

    /**
     * Remove a songs from a specific playlist
     * @param playlistTitle Title of a playlist.
     * @param songs Songs to be deleted.
     */
    suspend fun removeSongsFromPlaylist(playlistTitle: String, songs: List<SongEntity>)


    /**
     * Returns a continuous flow of Album data, will update if more albums are added.
     */
    fun getAllAvailableAlbumsFlow(): Flow<List<MediaItem>>

    /**
     * Returns a continuous flow of Playlist data, will update if more playlists are added.
     */
    fun getAllAvailablePlaylistFlow(): Flow<List<MediaItem>>

    suspend fun updateSongGroupImage(title: String, artFileName: String)

    suspend fun addSongsToPlaylist(playlistTitle: String, songDescriptions: List<String>)

    suspend fun createInitialQueueIfEmpty(title: String)

    /**
     * Update the songs in a playlist.
     */
    suspend fun updatePlaylistSongOrder(playlistTitle: String, songDescriptions: List<String>)

    suspend fun updatePlaylistTitle(originalTitle: String, newTitle: String)
}
package com.andaagii.tacomamusicplayer.repository

import androidx.media3.common.MediaItem

/**
 * I want my app to follow the repository pattern...
 * I will inject the database into this class,
 * then I will inject the repository into the viewmodel.
 * This will also allow be to work on testing my app.
 */
interface MusicRepository {

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
    suspend fun removeSongsFromPlaylist(playlistTitle: String, songs: List<MediaItem>)

}
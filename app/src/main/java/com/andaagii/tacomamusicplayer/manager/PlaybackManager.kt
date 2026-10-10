package com.andaagii.tacomamusicplayer.manager

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.manager.state.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Manage all of the Music State, and Music Controls.
 * Internally handles the mediaController and mediaBrowser.
 */
interface PlaybackManager {
    val playbackStateFlow: Flow<PlaybackState>

    fun initialize()
    fun release()

    fun saveState()

    fun moveInQueue(from: Int, to: Int)

    fun clearQueue()

    suspend fun addToQueue(songs: List<MediaItem>, clear: Boolean)

    suspend fun addToQueue(song: MediaItem, clear: Boolean)

    fun removeFromQueue(song: MediaItem, position: Int)

    fun removeFromQueue(songs: List<MediaItem>, positions: List<Int>)

    suspend fun flipShuffleState()

    suspend fun flipLoopMode()

    suspend fun flipPlayingState()

    suspend fun play(queuePosition: Int = 0, songPosition: Long = 0)

    suspend fun pause()

    suspend fun playQueueAtPosition(position: Int)

    suspend fun seekNextSong()

    suspend fun seekPreviousSong()

    suspend fun seekForward()

    suspend fun seekBackward()
}
package com.andaagii.tacomamusicplayer.manager

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.andaagii.tacomamusicplayer.manager.state.ControlState
import com.andaagii.tacomamusicplayer.manager.state.PlaybackState
import com.andaagii.tacomamusicplayer.manager.state.PositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Manage all of the Music State, and Music Controls.
 * Internally handles the mediaController and mediaBrowser.
 */
interface PlaybackManager {
    val playbackStateFlow: Flow<PlaybackState>

    val positionStateFlow: Flow<PositionState>
    val controlStateFlow: Flow<ControlState>

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

    /**
     * Only use this for populating player timeline!
     * Don't use any commands on the controller!
     */
    fun getController(): MediaController?
}
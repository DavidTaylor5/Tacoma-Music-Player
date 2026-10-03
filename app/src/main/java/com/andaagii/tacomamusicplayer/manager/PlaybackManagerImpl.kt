package com.andaagii.tacomamusicplayer.manager

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import com.andaagii.tacomamusicplayer.manager.state.PlayerControlState
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class PlaybackManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionToken: SessionToken
): PlaybackManager {

    /**
     * Reference to the app's mediaController.
     */
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    private val loopingFlow: Flow<Int> = DataStoreUtil.getLoopingPreference(context)
    private val shuffleFlow: Flow<ShuffleType> =
        DataStoreUtil.getShufflePreference(context).map { shuffleStr ->
            ShuffleType.determineShuffleTypeFromString(shuffleStr)
        }
    private val isPlayingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val playerControlFlow: Flow<PlayerControlState> //TODO This should actually be handled here...
        get() = _playerControlFlow
    private val _playerControlFlow: Flow<PlayerControlState> = combine(
        loopingFlow, shuffleFlow, isPlayingFlow
    ) { looping, shuffle, isPlaying ->
        PlayerControlState(
            isPlaying = isPlaying,
            loopMode = looping,
            shuffleMode = shuffle
        )
    }

    override val queue: Flow<List<MediaItem>>
        get() = _queue
    private val _queue: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())

    // Unsorted Queue is like a save point, allowing user to unshuffle their queue
    private var unshuffledQueue: List<MediaItem>? = null

    override val songPosition: Flow<Int>
        get() = _songPosition
    private val _songPosition: MutableStateFlow<Int> = MutableStateFlow(0)

    val currentPlayingSongInfo: Flow<SongData?>
        get() = _currentPlayingSongInfo
    private val _currentPlayingSongInfo: MutableStateFlow<SongData?> = MutableStateFlow(null)

    // New property for selectedIndex
    val selectedIndex: Flow<Int>
        get() = _selectedIndex
    private val _selectedIndex: MutableStateFlow<Int> = MutableStateFlow(-1)

    override fun initialize() {
        Timber.d("initialize: ")
        setupMediaController(sessionToken)
    }

    override fun release() {
        Timber.d("release: ")
        _mediaController.value?.removeListener(playerListener)
        _mediaController.value?.release()
    }

    override fun moveInQueue(from: Int, to: Int) {
        TODO("Not yet implemented")
    }

    override fun clearQueue() {
        _queue.value = listOf()
        unshuffledQueue = listOf()
    }

    override fun addToQueue(song: MediaItem) {
        val tempQ = _queue.value.toMutableList()
        tempQ.add(song)
        _queue.value = tempQ
    }

    override fun removeFromQueue(position: Int) {
        val tempQ = _queue.value.toMutableList()
        tempQ.removeAt(position)
        _queue.value = tempQ
    }

    override fun play() {
        _mediaController.value?.play()
    }

    override fun pause() {
        _mediaController.value?.pause()
    }

    override fun updateShuffleState() {
        TODO("Not yet implemented")
    }

    override fun updateLoopingState() {
        TODO("Not yet implemented")
    }

    /**
     * Returns a mediaController, used to interact with the music session.
     * @param session The session token associated with this app. [Should only be one]
     */
    private fun setupMediaController(session: SessionToken) {
        Timber.d("setupMediaController: session=$session")
        val controllerFuture = MediaController.Builder(context, session).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()

            _mediaController.value = controller

            //TODO Figure out this Logic
//            //Add old queue to the mediaController
//            restoreQueue()
//
//            //Restore the original ordering for current songs in mediaController
//            restoreQueueOrder()

            controller.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object: Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {

            Timber.d("onMediaMetadataChanged: artist=${mediaMetadata.artist}, title=${mediaMetadata.title}, albumTitle=${mediaMetadata.albumTitle}")
            _currentPlayingSongInfo.value = (
                    SongData(
                        songUri = "UNKNOWN",
                        songTitle = mediaMetadata.title.toString(),
                        albumTitle = mediaMetadata.albumTitle.toString(),
                        artist = mediaMetadata.artist.toString(),
                        artworkUri = mediaMetadata.artworkUri.toString(),
                        duration = mediaMetadata.description.toString()
                    ))
            _selectedIndex.value = queue.value.indexOfFirst { it.mediaId == mediaMetadata.mediaId }
            super.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            isPlayingFlow.value = isPlaying
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Timber.d("onRepeatModeChanged: ")
            super.onRepeatModeChanged(repeatMode)

            saveLoopingPref(context, _mediaController.value?.repeatMode ?: Player.REPEAT_MODE_ONE)
        }
    }

    private fun saveLoopingPref(context: Context, loopInt: Int) {
        Timber.d("saveLoopingPref: loopInt=$loopInt")
        // AppScope TODO needed
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setLoopingPreference(context, loopInt)
        }
    }

    private fun saveShufflePref(context: Context, shuffleType: ShuffleType) {
        Timber.d("saveShufflePref: shuffleType=$shuffleType")
        // AppScope TODO needed
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setShufflePreference(context, shuffleType)
        }
    }
}

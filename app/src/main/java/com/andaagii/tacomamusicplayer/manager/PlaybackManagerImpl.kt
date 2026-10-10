package com.andaagii.tacomamusicplayer.manager

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.di.ApplicationScope
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import com.andaagii.tacomamusicplayer.manager.state.ControlState
import com.andaagii.tacomamusicplayer.manager.state.PlaybackState
import com.andaagii.tacomamusicplayer.manager.state.PositionState
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import kotlin.collections.map

class PlaybackManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val musicRepo: MusicRepository,
    private val mediaItemUtil: MediaItemUtil,
    private val sessionToken: SessionToken
): PlaybackManager {
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    private val _loopingFlow: StateFlow<Int> = DataStoreUtil.getLoopingPreference(context)
        .stateIn(
            appScope,
            SharingStarted.Lazily,
            Player.REPEAT_MODE_OFF
        )

    private val _shuffleFlow: StateFlow<ShuffleType> =
        DataStoreUtil.getShufflePreference(context).map { shuffleStr ->
            ShuffleType.determineShuffleTypeFromString(shuffleStr)
        }.stateIn(
            appScope,
            SharingStarted.Lazily,
            ShuffleType.NOT_SHUFFLED
        )

    private val _isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val controlStateFlow: Flow<ControlState>
        get() = _controlFlow
    private val _controlFlow: Flow<ControlState> = combine(
        _loopingFlow, _shuffleFlow, _isPlaying
    ) { looping, shuffle, playing ->
        ControlState(
            loopMode = looping,
            shuffleMode = shuffle,
            isPlaying = playing
        )
    }

    private val _currentPlayingSong: MutableStateFlow<SongData?> = MutableStateFlow(null)

    private val _songPosition: MutableStateFlow<Int> = MutableStateFlow(0)

    private val _queue: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())
    private var _unshuffledQueue: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())

    override val positionStateFlow: Flow<PositionState>
        get() = _positionFlow
    private val _positionFlow: Flow<PositionState> = combine(
        _queue, _unshuffledQueue, _currentPlayingSong, _songPosition
    ) { queue, unshuffled, currSong, position ->
        PositionState(
            queue = queue,
            unShuffledQueue = unshuffled,
            currentlyPlayingSong = currSong,
            songPosition = position
        )
    }

    override val playbackStateFlow: Flow<PlaybackState> //TODO This should actually be handled here...
        get() = _playerControlFlow
    private val _playerControlFlow: Flow<PlaybackState> = combine(
        _controlFlow, _positionFlow
    ) { control, position ->
        PlaybackState(
            controlState = control,
            positionState = position
        )
    }

    override fun initialize() {
        Timber.d("initialize: ")
        setupMediaController(sessionToken)
    }

    override fun release() {
        Timber.d("release: ")
        _mediaController.value?.removeListener(playerListener)
        _mediaController.value?.release()
    }

    override fun saveState() {
        saveQueueState()
        savePlayerState()
    }

    override suspend fun playQueueAtPosition(position: Int) = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.seekTo(position, 0L)
    }

    private fun saveQueueState() {
        Timber.d("saveQueueState: ")
        appScope.launch {
            musicRepo.createInitialQueueIfEmpty(Const.PLAYLIST_QUEUE_TITLE)
            musicRepo.updatePlaylistSongOrder(
                Const.PLAYLIST_QUEUE_TITLE,
                _queue.value.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
            )

            musicRepo.createInitialQueueIfEmpty(Const.ORIGINAL_QUEUE_ORDER)

            musicRepo.updatePlaylistSongOrder(
                Const.ORIGINAL_QUEUE_ORDER,
                _unshuffledQueue.value.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
            )
        }
    }

    fun restorePlaybackState() {
        Timber.d("restorePlaybackState: ")
        // No need to restore playback state if it's already playing
        if(_isPlaying.value) return

        appScope.launch(Dispatchers.IO) {
            val playbackPosition = DataStoreUtil.getPlaybackPosition(context).firstOrNull()
            val songPosition = DataStoreUtil.getSongPosition(context).firstOrNull()

            _queue.value = musicRepo.getSongsFromPlaylist(Const.PLAYLIST_QUEUE_TITLE)
            _unshuffledQueue.value = musicRepo.getSongsFromPlaylist(Const.ORIGINAL_QUEUE_ORDER)
            Timber.d("restoreQueue: queue=${ _queue.value.map { it.mediaMetadata.title }}")

            withContext(Dispatchers.Main) {
                // Restore Playback State
                _mediaController.value?.let { controller ->
                    if(_shuffleFlow.value == ShuffleType.NOT_SHUFFLED) {
                        _mediaController.value?.addMediaItems(_queue.value)
                    } else {
                        _mediaController.value?.addMediaItems(_unshuffledQueue.value)
                    }

                    if(songPosition != null && songPosition < controller.mediaItemCount) {
                        if(playbackPosition != null) {
                            controller.seekTo(songPosition, playbackPosition)
                        } else {
                            controller.seekTo(songPosition, 0)
                        }
                    }
                }
            }
        }
    }

    private fun savePlayerState() {
        val playbackPosition = _mediaController.value?.currentPosition ?: 0
        val songPosition = _mediaController.value?.currentMediaItemIndex ?: 0
        Timber.d("savePlayerState: playbackPosition=$playbackPosition, songPosition=$songPosition")

        appScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaybackPosition(context, playbackPosition)
            DataStoreUtil.setSongPosition(context, songPosition)
        }
    }

    override fun moveInQueue(from: Int, to: Int) {
        val tempQ = _queue.value.toMutableList()
        Collections.swap(tempQ, from, to)
        _queue.value = tempQ
        if(_shuffleFlow.value == ShuffleType.NOT_SHUFFLED) {
            _unshuffledQueue.value = tempQ
        }
    }

    override fun clearQueue() {
        _queue.value = listOf()
        _unshuffledQueue.value = listOf()
    }

    override suspend fun addToQueue(song: MediaItem, clear: Boolean) {
        addToQueue(listOf(song), clear)
    }

    override suspend fun addToQueue(songs: List<MediaItem>, clear: Boolean) {
        var tempQ = if(!clear) _queue.value.toMutableList() else listOf<MediaItem>()
        tempQ = tempQ + songs
        _queue.value = tempQ
        if(_shuffleFlow.value == ShuffleType.NOT_SHUFFLED) {
            _unshuffledQueue.value = tempQ
        } else {
           tempQ = if(!clear) _unshuffledQueue.value.toMutableList() else listOf()
           tempQ + listOf(songs)
           _unshuffledQueue.value = tempQ
        }

        // After queue is updated, player must reflect queue
        updatePlayerWithQueue(
            if(clear) null else songs
        )
    }

    private suspend fun updatePlayerWithQueue(appendedSongs: List<MediaItem>?) = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        if(appendedSongs == null) { // queue was cleared
            controller.clearMediaItems()
            controller.addMediaItems(_queue.value)
        } else {
            controller.addMediaItems(appendedSongs)
        }
    }

    override fun removeFromQueue(song: MediaItem, position: Int) {
        removeFromQueue(listOf(song), listOf(position))
    }

    override fun removeFromQueue(songs: List<MediaItem>, positions: List<Int>) {
        val tempQ = _queue.value.toMutableList()
        tempQ.filterIndexed { index, song ->
            !positions.contains(index)
        }
        _queue.value = tempQ
        if(_shuffleFlow.value == ShuffleType.NOT_SHUFFLED) {
            _unshuffledQueue.value = tempQ
        } else {
            val tempQ = _unshuffledQueue.value.toMutableList()
            for(song in songs) {
                tempQ.remove(song)
            }
            _unshuffledQueue.value = tempQ
        }
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
            controller.addListener(playerListener)

            restorePlaybackState()
        }, MoreExecutors.directExecutor())
    }

    /**
     * Changes between songs being shuffled and songs being in original order.
     */
    override suspend fun flipShuffleState() {
        if(_shuffleFlow.value == ShuffleType.SHUFFLED) {
            saveShufflePref(context, ShuffleType.NOT_SHUFFLED)
            unshuffleSongs()
            Timber.d("flipShuffleState: ${ShuffleType.NOT_SHUFFLED}")
        } else {
            shuffleSongs()
            saveShufflePref(context, ShuffleType.SHUFFLED)
            Timber.d("flipShuffleState: ${ShuffleType.SHUFFLED}")
        }
    }

    private suspend fun shuffleSongs() = withContext(Dispatchers.Main) {
        Timber.d("shuffleSongsInMediaController: ")
        val shuffledSongs = shuffleSongs(_queue.value)

        if(shuffledSongs.isNotEmpty()) {
            _queue.value = shuffledSongs
            _mediaController.value?.addMediaItems(_queue.value)
        }
    }

    private suspend fun unshuffleSongs() = withContext(Dispatchers.Main) {
        _queue.value = _unshuffledQueue.value
        _mediaController.value?.addMediaItems(_queue.value)
    }

    override suspend fun flipPlayingState() = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    /**
     * Shuffle the given songs, if startingSongPosition is given, that song will be the first in queue.
     */
    private fun shuffleSongs(mediaItems: List<MediaItem>, startingSongPosition: Int? = null): List<MediaItem> {
        Timber.d("shuffleSongs: mediaItems=${mediaItems.map { it.mediaMetadata.title }} startingSongPosition=$startingSongPosition")
        if(startingSongPosition == null) {
            return mediaItems.shuffled()
        } else {

            val songOrder = mutableListOf<MediaItem>()
            songOrder.add(mediaItems[startingSongPosition])

            val songsMinusFirstSong = mediaItems.toMutableList()
            songsMinusFirstSong.removeAt(startingSongPosition)
            songsMinusFirstSong.shuffle()

            songOrder.addAll(songsMinusFirstSong)
            return songOrder
        }
    }

    override suspend fun flipLoopMode() = withContext(Dispatchers.IO) {
        if(_loopingFlow.value == Player.REPEAT_MODE_OFF) {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_ONE}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_ONE
        } else if(_loopingFlow.value == Player.REPEAT_MODE_ONE) {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_ALL}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_ALL
        } else {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_OFF}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_OFF
        }

        saveLoopingPref(context, _mediaController.value?.repeatMode ?: Player.REPEAT_MODE_ONE)
    }

    private fun saveLoopingPref(context: Context, loopInt: Int) {
        Timber.d("saveLoopingPref: loopInt=$loopInt")
        appScope.launch(Dispatchers.IO) {
            DataStoreUtil.setLoopingPreference(context, loopInt)
        }
    }

    private fun saveShufflePref(context: Context, shuffleType: ShuffleType) {
        Timber.d("saveShufflePref: shuffleType=$shuffleType")
        appScope.launch(Dispatchers.IO) {
            DataStoreUtil.setShufflePreference(context, shuffleType)
        }
    }

    private val playerListener = object: Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d("onMediaMetadataChanged: artist=${mediaMetadata.artist}, title=${mediaMetadata.title}, albumTitle=${mediaMetadata.albumTitle}")
            _currentPlayingSong.value =
                SongData(
                    songUri = "UNKNOWN",
                    songTitle = mediaMetadata.title.toString(),
                    albumTitle = mediaMetadata.albumTitle.toString(),
                    artist = mediaMetadata.artist.toString(),
                    artworkUri = mediaMetadata.artworkUri.toString(),
                    duration = mediaMetadata.description.toString()
                )

            _songPosition.value = _mediaController.value?.currentMediaItemIndex ?: 0

            super.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _isPlaying.value = isPlaying
        }
    }

    override suspend fun play(queuePosition: Int, songPosition: Long) = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.seekTo(queuePosition, songPosition)
        controller.play()
    }

    override suspend fun pause() = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.pause()
    }

    override suspend fun seekNextSong() = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.seekToNext()
    }

    override suspend fun seekPreviousSong() = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.seekToPrevious()
    }

    override suspend fun seekForward() = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.seekForward()
    }

    override suspend fun seekBackward() = withContext(Dispatchers.Main) {
        val controller = _mediaController.value ?: return@withContext
        controller.seekBack()
    }

    override fun getController(): MediaController? {
        return _mediaController.value
    }
}

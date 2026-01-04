package com.andaagii.tacomamusicplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.ScreenData
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.PlayerDatabase
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.QueueAddType
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.service.MusicService
import com.andaagii.tacomamusicplayer.state.AlbumTabState
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil.SortingOption
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.deletePicture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * The MainViewModel of the project, will include information on current screen, logic for handling
 * permissions, and will provide the UI with media related information.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val musicRepo: MusicRepository,
    private val mediaItemUtil: MediaItemUtil
): AndroidViewModel(application) {

    private val permissionManager = AppPermissionUtil()

    /**
     * Reference to the app's mediaController.
     */
    val mediaController: LiveData<MediaController>
        get() = _mediaController
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    /**
     * List of songs to be inspected.
     */
    val currentSongGroup: LiveData<SongGroup>
        get() = _currentSongGroup
    private val _currentSongGroup: MutableLiveData<SongGroup> = MutableLiveData()

    val currentSearchList: LiveData<List<MediaItem>>
        get() = _currentSearchList
    private val _currentSearchList: MutableLiveData<List<MediaItem>> = MutableLiveData()

    /**
     * Determines if the user has granted the required Permission to play Audio, READ_MEDIA_AUDIO.
     */
    val isAudioPermissionGranted: LiveData<Boolean>
        get() = _isAudioPermissionGranted
    private val _isAudioPermissionGranted: MutableLiveData<Boolean> = MutableLiveData()

    /**
     * Determines if the user has granted the required Permission to play Audio, READ_MEDIA_AUDIO.
     */
    val isPlaylistNameDuplicate: LiveData<Boolean>
        get() = _isPlaylistNameDuplicate
    private val _isPlaylistNameDuplicate: MutableLiveData<Boolean> = MutableLiveData()

    //TODO move playlist add prompt to the overall fragment?

    /**
     * Used to observe the current screen of the app, used for navigation.
     */
    val screenState : LiveData<ScreenData>
        get() = _screenState
    private val _screenState: MutableLiveData<ScreenData> = MutableLiveData()

    val navigateToPage: LiveData<PageType>
        get() = _navigateToPage
    private val _navigateToPage: MutableLiveData<PageType> = MutableLiveData()

    private var currentPage: PageType? = null

    val currentlyPlayingSongs: LiveData<List<MediaItem>>
        get() = _currentlyPlayingSongs
    private val _currentlyPlayingSongs: MutableLiveData<List<MediaItem>> = MutableLiveData()

    val currentPlayingSongInfo: LiveData<SongData>
        get() = _currentPlayingSongInfo
    private val _currentPlayingSongInfo: MutableLiveData<SongData> = MutableLiveData()

    val isPlaying: LiveData<Boolean>
        get() = _isPlaying
    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData()

    val shuffleMode: LiveData<ShuffleType>
        get() = _shuffleMode
    private val _shuffleMode: MutableLiveData<ShuffleType> = MutableLiveData()

    val loopMode: LiveData<Int>
        get() = _loopMode
    private val _loopMode: MutableLiveData<Int> = MutableLiveData()

    val originalSongOrder: LiveData<List<MediaItem>>
        get() = _originalSongOrder
    private val _originalSongOrder: MutableLiveData<List<MediaItem>> = MutableLiveData()

    val isShowingSearchMode: LiveData<Boolean>
        get() = _isShowingSearchMode
    private val _isShowingSearchMode: MutableLiveData<Boolean> = MutableLiveData(false)

    val notifyHideKeyboard: LiveData<Int>
        get() = _notifyHideKeyboard
    private val _notifyHideKeyboard: MutableLiveData<Int> = MutableLiveData()

    val showLoadingScreen: LiveData<Boolean>
        get() = _showLoadingScreen
    private val _showLoadingScreen: MutableLiveData<Boolean> = MutableLiveData(true)

    val loadingHandler = Handler(Looper.getMainLooper())

    val clearQueue: LiveData<Boolean>
        get() = _clearQueue
    private val _clearQueue: MutableLiveData<Boolean> = MutableLiveData(false)

    val shouldShowAddPlaylistPromptOnPlaylistPage: LiveData<Boolean>
        get() = _shouldShowAddPlaylistPromptOnPlaylistPage
    private val _shouldShowAddPlaylistPromptOnPlaylistPage: MutableLiveData<Boolean> = MutableLiveData(false)

    private val playerListener = object: Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d("onMediaMetadataChanged: artist=${mediaMetadata.artist}, title=${mediaMetadata.title}, albumTitle=${mediaMetadata.albumTitle}")
            _currentPlayingSongInfo.postValue(
                SongData(
                    songUri = "UNKNOWN",
                    songTitle = mediaMetadata.title.toString(),
                    albumTitle = mediaMetadata.albumTitle.toString(),
                    artist = mediaMetadata.artist.toString(),
                    artworkUri = mediaMetadata.artworkUri.toString(),
                    duration = mediaMetadata.description.toString()
                )
            )
            super.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _isPlaying.postValue(isPlaying)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Timber.d("onRepeatModeChanged: ")
            super.onRepeatModeChanged(repeatMode)
            _loopMode.postValue(repeatMode)
        }
    }

    // Flip between search state and non search state
    fun flipSearchButtonState() {
        Timber.d("flipSearchButtonState: isSearchMode=${_isShowingSearchMode.value}")
         _isShowingSearchMode.value?.let { isSearchMode ->
             _isShowingSearchMode.postValue(!isSearchMode)
             removeVirtualKeyboard()
         }
    }

    fun handleCancelSearchButtonClick() {
        Timber.d("handleCancelSearchButtonClick: ")
        _isShowingSearchMode.postValue(false)
    }

    fun removeVirtualKeyboard() {
        Timber.d("removeVirtualKeyboard: ")
        _notifyHideKeyboard.postValue(_notifyHideKeyboard.value?.inc() ?: 0)
    }

    /**
     * Sets the currentSearchList based on user search.
     */
    fun querySearchData(search: String) {
        Timber.d("querySearchData: search=$search")
        viewModelScope.launch(Dispatchers.IO) {
            _currentSearchList.postValue(musicRepo.searchMusic(search))
        }
    }

    /**
     * Changes between songs being shuffled and songs being in original order.
     */
    fun flipShuffleState() {
        if(_shuffleMode.value == ShuffleType.SHUFFLED) {
            //Set to be original order
            _shuffleMode.value = ShuffleType.NOT_SHUFFLED
            unshuffleSongs()
            saveShufflePref(getApplication<Application>().applicationContext, ShuffleType.NOT_SHUFFLED)
            Timber.d("flipShuffleState: ${ShuffleType.NOT_SHUFFLED}")
        } else {
            //Set to be shuffled
            _shuffleMode.value = ShuffleType.SHUFFLED
            shuffleSongsInMediaController()
            saveShufflePref(getApplication<Application>().applicationContext, ShuffleType.SHUFFLED)
            Timber.d("flipShuffleState: ${ShuffleType.SHUFFLED}")
        }
    }

    fun flipLoopMode() {
        if(_loopMode.value == Player.REPEAT_MODE_OFF) {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_ONE}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_ONE
        } else if(_loopMode.value == Player.REPEAT_MODE_ONE) {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_ALL}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_ALL
        } else {
            Timber.d("flipRepeatMode: ${Player.REPEAT_MODE_OFF}")
            _mediaController.value?.repeatMode = Player.REPEAT_MODE_OFF
        }

        saveLoopingPref(getApplication<Application>().applicationContext, _mediaController.value?.repeatMode ?: Player.REPEAT_MODE_ONE)
    }

    fun flipPlayingState() {
        if(_isPlaying.value == true) {
            _mediaController.value?.pause()
            Timber.d("flipPlayingState: Pausing!")
        } else {
            _mediaController.value?.play()
            Timber.d("flipPlayingState: Playing!")
        }
    }

    private fun setMusicPlayingPrefs(context: Context) {
        Timber.d("setMusicPlayingPrefs: ")
        //determineLoopingPref(context)
        determineShufflePref(context)
    }

    private fun determineLoopingPref(context: Context) {
        Timber.d("determineLoopingPref: ")
        viewModelScope.launch {
            DataStoreUtil.getLoopingPreference(context).collect { loopingPref ->
                _mediaController.value?.repeatMode = loopingPref
            }
        }
    }

    private fun determineShufflePref(context: Context) {
        Timber.d("determineShufflePref: ")
        viewModelScope.launch {
            DataStoreUtil.getShufflePreference(context).collect { shufflePref ->
                val shuffleType = ShuffleType.determineShuffleTypeFromString(shufflePref)
                _shuffleMode.postValue(shuffleType)
            }
        }
    }

    private fun saveLoopingPref(context: Context, loopInt: Int) {
        Timber.d("saveLoopingPref: loopInt=$loopInt")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setLoopingPreference(context, loopInt)
        }
    }

    private fun saveShufflePref(context: Context, shuffleType: ShuffleType) {
        Timber.d("saveShufflePref: shuffleType=$shuffleType")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setShufflePreference(context, shuffleType)
        }
    }

    /**
     * Experimental code, which page for music chooser fragment?
     */
    fun setPage(page: PageType) {
        _navigateToPage.value = page
    }

    fun observeCurrentPage(page: PageType) {
        currentPage = page
    }

    fun getCurrentPage(): PageType? {
        return currentPage
    }

    private lateinit var mediaBrowser: MediaBrowser
    private var rootMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    init {
        Timber.d("init: ")
        checkPermissions()

        //ex. the layout of the albums / playlist fragments
        checkUserPreferences()
    }

    /**
     * Determine all saved user preferences, loopMode, shuffleMode, layout, sorting.
     */
    private fun checkUserPreferences() {
        Timber.d("checkUserPreferences: ")
        setMusicPlayingPrefs(getApplication<Application>().applicationContext)
    }

    /**
     * Creates a playlist in memory.
     * @param playlistName Name of a new playlist. TODO Don't allow two albums of the same name.
     */
    fun createNamedPlaylist(playlistName: String) {
        Timber.d("createNamedPlaylist: playlistName=$playlistName")
        viewModelScope.launch {
            musicRepo.createPlaylist(playlistName)
        }
    }

    /**
     * @param albumSongGroup A song group associated with a playlist.
     */
    fun updatePlaylistOrder(albumSongGroup: SongGroup) {
        Timber.d("updatePlaylistOrder: albumSongGroup=$albumSongGroup")
        if(albumSongGroup.type == SongGroupType.PLAYLIST) {
            viewModelScope.launch(Dispatchers.IO) {
                musicRepo.updatePlaylistSongOrder(
                    albumSongGroup.group.mediaMetadata.albumTitle.toString(),
                    albumSongGroup.songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
                )
            }
        }
    }

    /**
     * Call when playlistNameDuplicate has occurred and has been handled.
     */
    fun handledPlaylistNameDuplicate() {
        _isPlaylistNameDuplicate.postValue(false)
    }

    /**
     * Saves the current songs playing in the queue, to be loaded when the app opens next.
     */
    fun saveQueue() {
        Timber.d("saveQueue: ")
        mediaController.value?.let { controller ->
            //Save current Player state
            savePlayerState(controller)

            val songs = UtilImpl.getSongListFromMediaController(controller)

            if(songs.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    //In case queue has never been initialized
                    musicRepo.createInitialQueueIfEmpty(Const.PLAYLIST_QUEUE_TITLE)

                    musicRepo.updatePlaylistSongOrder(
                        Const.PLAYLIST_QUEUE_TITLE,
                        songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
                    )
                }
            }
        }
    }

    /**
     * When the user exits the app in shuffled mode, give the user the ability to return to ordered mode.
     */
    fun saveOriginalOrder() {
        Timber.d("saveOriginalOrder: ")
        originalSongOrder.value?.let { originalOrderSongs ->
            viewModelScope.launch(Dispatchers.IO) {
                //In case queue has never been initialized
                musicRepo.createInitialQueueIfEmpty(Const.ORIGINAL_QUEUE_ORDER)

                musicRepo.updatePlaylistSongOrder(
                    Const.ORIGINAL_QUEUE_ORDER,
                    originalOrderSongs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
                )
            }
        }
    }

    private fun savePlayerState(controller: MediaController) {
        val playbackPosition = controller.currentPosition
        val songPosition = controller.currentMediaItemIndex
        Timber.d("savePlayerState: playbackPosition=$playbackPosition, songPosition=$songPosition")

        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaybackPosition(getApplication<Application>().applicationContext, playbackPosition)
            DataStoreUtil.setSongPosition(getApplication<Application>().applicationContext, songPosition)
        }

    }

    private fun restoreQueue() {
        Timber.d("restoreQueue: ")
        viewModelScope.launch(Dispatchers.IO) {
            val playbackPosition = DataStoreUtil.getPlaybackPosition(getApplication<Application>().applicationContext).firstOrNull()
            val songPosition = DataStoreUtil.getSongPosition(getApplication<Application>().applicationContext).firstOrNull()

            val queue = musicRepo.getSongsFromPlaylist(Const.PLAYLIST_QUEUE_TITLE)

            withContext(Dispatchers.Main) {
                // Restore Playback State
                mediaController.value?.let { controller ->
                    addTracksSaveTrackOrder(
                        mediaItems = queue,
                        clearOriginalSongList = false,
                        clearCurrentSongs = true,
                        shouldAddToOriginalList = false
                    )

                    if(songPosition != null && songPosition < controller.mediaItemCount) {
                        if(playbackPosition != null) {
                            controller.seekTo(songPosition, playbackPosition)
                        } else {
                            controller.seekTo(songPosition, 0)
                        }
                    }

                    loadingHandler.postDelayed({
                        _showLoadingScreen.postValue(false)
                    }, 500)
                }
            }
        }
    }

    private fun restoreQueueOrder() {
        Timber.d("restoreQueueOrder: ")
        viewModelScope.launch(Dispatchers.IO) {
            val queueOrdered = musicRepo.getSongsFromPlaylist(Const.ORIGINAL_QUEUE_ORDER)
            _originalSongOrder.postValue(queueOrdered)
        }
    }

    /**
     * Ability to add a list of songs to a list of playlists.
     */
    fun addSongsToAPlaylist(playlistTitles: List<String>, songs: List<MediaItem>) {
        Timber.d("addSongsToAPlaylist: playlistTitles=$playlistTitles, songDescriptions=$songs")
        playlistTitles.forEach { playlist ->
            addListOfSongMediaItemsToAPlaylist(playlist, songs)
        }
    }
    
    fun updatePlaylistTitle(currentTitle: String, newTitle: String ) {
        Timber.d("updatePlaylistTitle: currentTitle=$currentTitle, newTitle=$newTitle")
        viewModelScope.launch(Dispatchers.IO) {
            musicRepo.updatePlaylistTitle(currentTitle, newTitle)
        }
    }

    /**
     * Update the playlist image.
     */
    fun updateSongGroupImage(title: String, artFileName: String) {
        Timber.d("updateSongGroupImage: title=$title, artFileName=$artFileName")
        viewModelScope.launch(Dispatchers.IO) {
            musicRepo.updateSongGroupImage(title, artFileName)
        }
    }

    /**
     * Add a list of songs to the Playlist. Even if adding only one song still use this function.
     */
    private fun addListOfSongMediaItemsToAPlaylist(playlistTitle: String, songs: List<MediaItem>) {
        Timber.d("addListOfSongMediaItemsToAPlaylist: playlistTitle=$playlistTitle, songDescriptions.size=${songs.size}")
        viewModelScope.launch(Dispatchers.IO) {
            val songDescriptions = songs.map { mediaItemUtil.getSongSearchDescriptionFromMediaItem(it) }
            musicRepo.addSongsToPlaylist(playlistTitle, songDescriptions)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared: ")

        mediaController.value?.let { controller ->
            controller.removeListener(playerListener)
        }

        if(this::mediaBrowser.isInitialized) {
            mediaBrowser.release()
        }
    }

    fun checkPermissionsIfOnPermissionDeniedScreen() {
        Timber.d("checkPermissionsIfOnPermissionDeniedScreen: ")

        screenState.value?.let { data ->
            if(data.currentScreen == ScreenType.PERMISSION_DENIED_SCREEN)
                checkPermissions()
        }
    }

    /**
     *  Clear queue and play the song group at a certain position.
     */
    fun playSongGroupAtPosition(songGroup: SongGroup, position: Int) {
        Timber.d("playSongGroupAtPosition: songGroup=$songGroup, position=$position")
        mediaController.value?.let { controller ->
            controller.pause()

            addTracksSaveTrackOrder(
                songGroup.songs,
                clearOriginalSongList = true,
                startingSongPosition = position,
                clearCurrentSongs = true,
                shouldAddToOriginalList = true
            )
            controller.play()
        }
    }

    /**
     * Clear queue and play the specified playlist.
     * @param playlistTitle The groupTitle of a playlist.
     */
    fun playPlaylist(playlistTitle: String) {
        Timber.d("playPlaylist: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlistSongs = musicRepo.getSongsFromPlaylist(playlistTitle = playlistTitle)

            withContext(Dispatchers.Main) {
                addTracksSaveTrackOrder(
                    mediaItems = playlistSongs,
                    clearOriginalSongList = true,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = true
                )

                mediaController.value?.play()
            }
        }
    }

    /**
     * Adds all playlist songs to the back of the current queue.
     * @param playlistTitle The groupTitle of a playlist.
     */
    fun addPlaylistToBackOfQueue(playlistTitle: String) {
        Timber.d("addPlaylistToBackOfQueue: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlistSongs = musicRepo.getSongsFromPlaylist(playlistTitle = playlistTitle)

            withContext(Dispatchers.Main) {
                addTracksSaveTrackOrder(
                    mediaItems = playlistSongs,
                    clearOriginalSongList = false,
                    clearCurrentSongs = false,
                    shouldAddToOriginalList = true
                )
            }
        }
    }

    /**
     * Adds multiple songs to the end of the controller in the queue
     */
    fun addSongsToEndOfQueue(songs: List<MediaItem>) {
        Timber.d("addSongsToEndOfQueue: songs=$songs")
        addTracksSaveTrackOrder(
            mediaItems = songs,
            clearOriginalSongList = false,
            clearCurrentSongs = false,
            shouldAddToOriginalList = true
        )
    }

    /**
     * Clear all songs out of Player.
     */
    fun clearQueue() {
        Timber.d("clearQueue: ")
        addTracksSaveTrackOrder(
            mediaItems = listOf(),
            clearOriginalSongList = true,
            clearCurrentSongs = true,
            shouldAddToOriginalList = false
        )
        _clearQueue.value = true
    }

    fun handledClearningQueue() {
        _clearQueue.value = false
    }

    fun showAddPlaylistPromptOnPlaylistPage(shouldShow: Boolean) {
        _shouldShowAddPlaylistPromptOnPlaylistPage.value = shouldShow
    }

    /**
     * Sets the current screen of the application.
     * @param nextScreen The next screen to be navigated to.
     */
    private fun setScreenData(nextScreen: ScreenType) {
        Timber.d("setScreenData: nextScreen=$nextScreen")
        if(screenState.value == null) {
            _screenState.value = ScreenData(nextScreen)
        } else {
            screenState.value?.let {
                if(it.currentScreen != nextScreen) _screenState.value = ScreenData(nextScreen)
            }
        }
    }

    /**
     * Starts music service and sets up the media controller and media browser.
     */
    fun initializeMusicPlaying() {
        Timber.d("initializeMusicPlaying: ")
        sessionToken = createSessionToken()
        setupMediaController(sessionToken)
        setupMediaBrowser(sessionToken)
    }

    /**
     * A session token is needed to connect to the music service. [And start the service?]
     */
    private fun createSessionToken(): SessionToken {
        Timber.d("createSessionToken: ")
        return SessionToken(getApplication<Application>().applicationContext, ComponentName(getApplication<Application>().applicationContext, MusicService::class.java))
    }

    /**
     * Returns a mediaController, used to interact with the music session.
     * @param session The session token associated with this app. [Should only be one]
     */
    private fun setupMediaController(session: SessionToken) {
        Timber.d("setupMediaController: session=$session")
        val controllerFuture = MediaController.Builder(getApplication<Application>().applicationContext, session).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()

            _mediaController.value = controller

            determineLoopingPref(getApplication<Application>().applicationContext)

            //Add old queue to the mediaController
            restoreQueue()

            //Restore the original ordering for current songs in mediaController
            restoreQueueOrder()

            _loopMode.postValue(controller.repeatMode)
            controller.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    /**
     * Sets up the MediaBrowser, which is used to browse music on the app.
     * @param session The session token associated with this app. [Should only be one]
     */
    private fun setupMediaBrowser(session: SessionToken) {
        Timber.d("DT>>> setupMediaBrowser: session=$session")
        val browserFuture = MediaBrowser.Builder(getApplication<Application>().applicationContext, sessionToken)
            .buildAsync()
        browserFuture.addListener({
            browserFuture.get().let { browser ->
                mediaBrowser = browser
                getRoot()
                Timber.d("setupMediaBrowser: sessionToken=${mediaBrowser.connectedToken}")
            }
            mediaBrowser = browserFuture.get()
            //getRoot()
        }, MoreExecutors.directExecutor())
    }

    /**
     * The root is the top most node returned from the MediaLibraryService, media is organized as
     * a tree of MediaItems.
     */
    private fun getRoot() {
        Timber.d("getRoot: ")
        mediaBrowser?.let { browser ->
            val rootFuture = browser.getLibraryRoot(null)
            rootFuture.addListener({
                rootMediaItem = rootFuture.get().value
            }, MoreExecutors.directExecutor())
        }
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on album title.
     * @param albumId The title of an album to be queried.
     */
    fun querySongsFromAlbum(album: MediaItem, queueAddType: QueueAddType = QueueAddType.QUEUE_DONT_ADD) {
        Timber.d("querySongsFromAlbum: album=$album, queueAddType=$queueAddType")

        //clear the previous album
        _currentSongGroup.value = SongGroup(
            type=SongGroupType.ALBUM,
            songs = listOf(),
            group = MediaItem.EMPTY,
        )

        val albumTitle = album.mediaMetadata.albumTitle.toString()
        viewModelScope.launch {
            val albumSongs = musicRepo.getSongsFromAlbum(albumTitle)
            val songGroup = SongGroup(
                type = SongGroupType.ALBUM,
                songs = albumSongs,
                album
            )
            _currentSongGroup.postValue(songGroup) //TODO change this to StateFlow

            if(queueAddType == QueueAddType.QUEUE_CLEAR_ADD) {
                addTracksSaveTrackOrder(
                    mediaItems = songGroup.songs,
                    clearOriginalSongList = true,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = false
                )
                mediaController.value?.let { controller ->
                    controller.play()
                }
            } else if(queueAddType == QueueAddType.QUEUE_END_ADD) {
                addTracksSaveTrackOrder(
                    mediaItems = songGroup.songs,
                    clearOriginalSongList = false,
                    startingSongPosition = null,
                    clearCurrentSongs = false,
                    shouldAddToOriginalList = true
                )
            }
        }
    }

    /**
     * Instead of adding songs directly to the mediaController instead, I can track when songs are added
     * allowing for shuffle and restore functionality. [Also track current song list here, also track current song here?, do all player manipulation here to be observed]
     */
    private fun addTracksSaveTrackOrder(
        mediaItems: List<MediaItem>,
        clearOriginalSongList: Boolean = false,
        startingSongPosition: Int? = null,
        clearCurrentSongs: Boolean = false,
        shouldAddToOriginalList: Boolean = false
    ) {
        Timber.d("addTracksSaveTrackOrder: originalSongOrder=${_originalSongOrder.value?.map { it.mediaMetadata.title }}, mediaItems=${mediaItems.map { it.mediaMetadata.title }}, " +
                "clearOriginalSongList=$clearOriginalSongList, startingSongPosition=$startingSongPosition, " +
                "clearCurrentSongs=$clearCurrentSongs, shouldAddToOriginalList=$shouldAddToOriginalList")
        if(clearCurrentSongs) {
            _mediaController.value?.clearMediaItems()
        }

        if(clearOriginalSongList) {
            _originalSongOrder.value = listOf()
        }

        //save songs to the original song order
        val songOrder = originalSongOrder.value?.toMutableList()
        songOrder?.addAll(mediaItems)

        if(shouldAddToOriginalList) {
            Timber.d("addTracksSaveTrackOrder: songOrder=${songOrder?.map { it -> it.mediaMetadata.title }}, mediaItems=${mediaItems.map { it -> it.mediaMetadata.title }}, clearOriginalSongList=$clearOriginalSongList")
            _originalSongOrder.postValue( songOrder ?: mediaItems  )
        }

        _mediaController.value?.let { controller ->
            if(_shuffleMode.value == ShuffleType.SHUFFLED) {
                val shuffledSongs = shuffleSongs(mediaItems, startingSongPosition)
                controller.addMediaItems(shuffledSongs)
                _currentlyPlayingSongs.value = shuffledSongs
            } else {
                //TODO update all places where I set / add mediaItems
                if(controller.mediaItemCount == 0) {
                    controller.setMediaItems(mediaItems)
                } else {
                    controller.addMediaItems(mediaItems)
                }

                _currentlyPlayingSongs.value = mediaItems
            }
        }



        startingSongPosition?.let { position ->
            _mediaController.value?.seekTo(position, 0L)
        }

        //Save the current state of mediaController to a live data of currently playing songs
        _mediaController.value?.let { controller ->
            _currentlyPlayingSongs.value = UtilImpl.getSongListFromMediaController(controller)
        }
    }

    /**
     * Shuffle the given songs, if startingSongPosition is given, that song will be the first in queue.
     */
    private fun shuffleSongs(mediaItems: List<MediaItem>, startingSongPosition: Int? = null): List<MediaItem> {
        Timber.d("shuffleSongs: mediaItems=$mediaItems startingSongPosition=$startingSongPosition")
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

    private fun shuffleSongsInMediaController() {
        Timber.d("shuffleSongsInMediaController: ")
        _mediaController.value?.let { controller ->
            val currentSongs = UtilImpl.getSongListFromMediaController(controller)

            val shuffledSongs = shuffleSongs(currentSongs)

            if(shuffledSongs.isNotEmpty()) {
                addTracksSaveTrackOrder(
                    mediaItems = shuffledSongs,
                    clearOriginalSongList = false,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = false
                )
            }
        }
    }

    private fun unshuffleSongs() {
        Timber.d("unshuffleSongs: ")
        _mediaController.value?.let { controller ->
            _originalSongOrder.value?.let { originalSongs ->
                Timber.d("restoreOriginalSongOrder: originalSongs.size=${originalSongs.size}")
                addTracksSaveTrackOrder(
                    mediaItems = originalSongs,
                    clearOriginalSongList = false,
                    startingSongPosition = 0,
                    clearCurrentSongs = true,
                    shouldAddToOriginalList = false
                )
            }
        }
    }

    /**
     * Clears the current queue and starts playing the chosen album.
     */
    fun playAlbum(album: MediaItem) { //TODO I don't think this is working...
        Timber.d("playAlbum: album=$album")
        querySongsFromAlbum(
            album,
            queueAddType = QueueAddType.QUEUE_CLEAR_ADD
        )
    }

    /**
     * Adds to album to the back of the current queue.
     */
    fun addAlbumToBackOfQueue(album: MediaItem) {
        Timber.d("addAlbumToBackOfQueue: album=$album")
        querySongsFromAlbum(
            album,
            queueAddType = QueueAddType.QUEUE_END_ADD
        )
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on a playlist.
     * @param albumId The title of an playlist to be queried.
     */
    fun querySongsFromPlaylist(playlist: MediaItem) {
        Timber.d("querySongsFromPlaylist: playlistId=${playlist.mediaMetadata.albumTitle}")
        viewModelScope.launch(Dispatchers.IO) {
            val playlistSongs = musicRepo.getSongsFromPlaylist(playlist.mediaMetadata.albumTitle.toString())
            val songGroupType = SongGroupType.PLAYLIST
            _currentSongGroup.postValue(
                SongGroup(
                    songGroupType,
                    playlistSongs,
                    playlist
                )
            )
        }
    }

    /**
     * Remove a list of of playlists
     * @param playlists A list of the playlist titles to be removed.
     */
    fun removePlaylists(playlists: List<String>) {
        Timber.d("removePlaylists: playlist=$playlists")
        playlists.forEach { playlistTitle ->
            removePlaylist(playlistTitle)
        }
    }

    /**
     * Removes a single playlist based on its title.
     */
    private fun removePlaylist(playlistTitle: String) {
        Timber.d("removePlaylist: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlayerDatabase.getDatabase(getApplication<Application>().applicationContext)
                .songGroupDao()
                .findSongGroupByName(playlistTitle)

            if(playlist != null) {
                PlayerDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .songGroupDao()
                    .deleteSongGroups(playlist)

                //remove associated image
                deletePicture(getApplication<Application>().applicationContext, "$playlistTitle.jpg")
            }
        }
    }

    /**
     * Check if necessary permissions are granted.
     */
    private fun checkPermissions() {
        val isAudioPermissionGranted = permissionManager.verifyReadMediaAudioPermission(getApplication<Application>().applicationContext)
        Timber.d("checkPermissions: isAudioPermissionGranted=$isAudioPermissionGranted")
        if(_isAudioPermissionGranted.value != isAudioPermissionGranted)
            _isAudioPermissionGranted.value = isAudioPermissionGranted
    }

    /**
     * Based on results from asking user for permission, determine how to proceed. The app requires
     * that read media audio permission is granted for functionality.
     * If permission is not granted, send the user to a permission denied screen.
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.d("handlePermissionResult: requestCode=$requestCode, permissions=$permissions, grantResults=$grantResults")
        if(requestCode == AppPermissionUtil.readMediaAudioRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("handlePermissionResult: read audio granted!")
                _isAudioPermissionGranted.value = true
            } else {
                Timber.d("handlePermissionResult: read audio NOT granted!")
                setScreenData(ScreenType.PERMISSION_DENIED_SCREEN)
            }
        } else if(requestCode == AppPermissionUtil.readExternalStorageCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("handlePermissionResult: read audio granted!")
                _isAudioPermissionGranted.value = true
            } else {
                Timber.d("handlePermissionResult: read audio NOT granted!")
                setScreenData(ScreenType.PERMISSION_DENIED_SCREEN)
            }
        }
    }
}
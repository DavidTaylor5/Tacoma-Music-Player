package com.andaagii.tacomamusicplayer.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.data.ScreenData
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.PlayerDatabase
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.QueueAddType
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.manager.PlaybackManager
import com.andaagii.tacomamusicplayer.manager.state.ControlState
import com.andaagii.tacomamusicplayer.manager.state.PlaybackState
import com.andaagii.tacomamusicplayer.manager.state.PositionState
import com.andaagii.tacomamusicplayer.repository.MusicProviderRepository
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.deletePicture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val musicProvider: MusicProviderRepository,
    private val mediaItemUtil: MediaItemUtil,
    private val playbackManager: PlaybackManager
): AndroidViewModel(application) {

    private val permissionManager = AppPermissionUtil()

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
     * Used to observe the current screen of the app, used for navigation.
     */
    val screenState : LiveData<ScreenData>
        get() = _screenState
    private val _screenState: MutableLiveData<ScreenData> = MutableLiveData()

    val navigateToPage: LiveData<PageType>
        get() = _navigateToPage
    private val _navigateToPage: MutableLiveData<PageType> = MutableLiveData()

    private var currentPage: PageType? = null

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

    val shouldShowAddPlaylistPromptOnPlaylistPage: LiveData<Boolean>
        get() = _shouldShowAddPlaylistPromptOnPlaylistPage
    private val _shouldShowAddPlaylistPromptOnPlaylistPage: MutableLiveData<Boolean> = MutableLiveData(false)

    val playbackStateFlow: StateFlow<PlaybackState> = playbackManager.playbackStateFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PlaybackState()
        )

    val controlStateFlow: StateFlow<ControlState> = playbackManager.controlStateFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ControlState()
        )

    val positionStateFlow: StateFlow<PositionState> = playbackManager.positionStateFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PositionState()
        )

    val availablePlaylists: StateFlow<List<MediaItem>> = musicRepo.getAllAvailablePlaylistFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf()
        )

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
    fun flipShuffleState() = viewModelScope.launch { playbackManager.flipShuffleState() }

    fun flipLoopMode() = viewModelScope.launch { playbackManager.flipLoopMode() }

    fun flipPlayingState() = viewModelScope.launch { playbackManager.flipPlayingState() }

    fun moveInQueue(from: Int, to: Int) = viewModelScope.launch { playbackManager.moveInQueue(from, to) }

    fun seekPreviousSong() = viewModelScope.launch { playbackManager.seekPreviousSong()  }

    fun seekNextSong() = viewModelScope.launch { playbackManager.seekNextSong() }

    fun seekForward() = viewModelScope.launch { playbackManager.seekForward()  }

    fun seekBackward() = viewModelScope.launch { playbackManager.seekBackward() }

    fun getController() = playbackManager.getController()

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

    init {
        Timber.d("init: ")
        checkPermissions()
        playbackManager.initialize()
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
    fun updateSongGroupImage(title: String, artFileName: String, updateSongs: Boolean = false) {
        Timber.d("updateSongGroupImage: title=$title, artFileName=$artFileName")
        viewModelScope.launch(Dispatchers.IO) {
            // Update Song Group
            musicRepo.updateSongGroupImage(title, artFileName)

            // Update an album's songs with it's new custom image
            if(updateSongs) {
                musicRepo.updateAlbumSongsWithCustomImage(title, artFileName)
            }

            //TODO hotfix, refresh the songgroup if updated songgroup == current songGroup
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
        playbackManager.release()
    }

    fun savePlaybackState() {
        playbackManager.saveState()
    }

    fun playQueueAtPosition(position: Int) {
        viewModelScope.launch { playbackManager.playQueueAtPosition(position) }
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
        viewModelScope.launch {
            playbackManager.addToQueue(
                songs = songGroup.songs,
                clear = true
            )
            playbackManager.play(
                queuePosition = position,
                songPosition = 0L
            )
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
                playbackManager.addToQueue(
                    songs = playlistSongs,
                    clear = true
                )
                playbackManager.play()
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
            playbackManager.addToQueue(
                songs = playlistSongs,
                clear = false
            )
        }
    }

    /**
     * Adds multiple songs to the end of the controller in the queue
     */
    fun addSongsToEndOfQueue(songs: List<MediaItem>) {
        Timber.d("addSongsToEndOfQueue: songs=$songs")
        viewModelScope.launch {
            playbackManager.addToQueue(
                songs = songs,
                clear = false
            )
        }
    }

    /**
     * Clear all songs out of Player.
     */
    fun clearQueue() {
        Timber.d("clearQueue: ")
        playbackManager.clearQueue()
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

            if (queueAddType == QueueAddType.QUEUE_CLEAR_ADD) {
                playbackManager.addToQueue(albumSongs, true)
                playbackManager.play()
            } else if (queueAddType == QueueAddType.QUEUE_END_ADD) {
                playbackManager.addToQueue(albumSongs, false)
            }
        }
    }

    /**
     * Given a song, query the album it's from and start playing from that position.
     * ex. If user clicks on songs from search, start playing music from that album at position of clicked song.
     */
    fun playAlbumAtSongPosition(song: MediaItem) {
        viewModelScope.launch {
            val album = musicProvider.getSongsFromAlbum(
                song.mediaMetadata.albumTitle.toString(), //TODO This title isn't coming in correct... good kid,
                useFileProviderUri = true
            )
            var position = album.indexOfFirst { it.mediaMetadata.title == song.mediaMetadata.title }
            if(position == -1) position = 0
            playbackManager.addToQueue(
                songs = album,
                clear = true
            )
            playbackManager.play(
                queuePosition = position
            )
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
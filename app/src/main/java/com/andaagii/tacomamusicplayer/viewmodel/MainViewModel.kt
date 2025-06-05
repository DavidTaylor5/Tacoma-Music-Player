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
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.data.PlaylistData
import com.andaagii.tacomamusicplayer.data.ScreenData
import com.andaagii.tacomamusicplayer.data.SearchData
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.PlaylistDatabase
import com.andaagii.tacomamusicplayer.database.SearchDatabase
import com.andaagii.tacomamusicplayer.enum.LayoutType
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.enum.QueueAddType
import com.andaagii.tacomamusicplayer.enum.ScreenType
import com.andaagii.tacomamusicplayer.enum.ShuffleType
import com.andaagii.tacomamusicplayer.enum.SongGroupType
import com.andaagii.tacomamusicplayer.service.MusicService
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.deletePicture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.Executors

/**
 * The MainViewModel of the project, will include information on current screen, logic for handling
 * permissions, and will provide the UI with media related information.
 */
class MainViewModel(application: Application): AndroidViewModel(application) {

    private val permissionManager = AppPermissionUtil()
    var availablePlaylists: LiveData<List<Playlist>>

    /**
     * Reference to the app's mediaController.
     */
    val mediaController: LiveData<MediaController>
        get() = _mediaController
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    /**
     * Livedata observable list of albums on the device.
     */
    val albumMediaItemList: LiveData<List<MediaItem>>
        get() = _albumMediaItemList
    private val _albumMediaItemList: MutableLiveData<List<MediaItem>> = MutableLiveData()

    /**
     * List of songs to be inspected.
     */
    val currentSongList: LiveData<SongGroup>
        get() = _currentSongList
    private val _currentSongList: MutableLiveData<SongGroup> = MutableLiveData()

    val currentSearchList: LiveData<List<SearchData>>
        get() = _currentSearchList
    private val _currentSearchList: MutableLiveData<List<SearchData>> = MutableLiveData()

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

    //TODO move the playlist prompt to the overall fragment?
    //TODO move playlist add prompt to the overall fragment?

    /**
     * Used to observe the current screen of the app, used for navigation.
     */
    val screenState : LiveData<ScreenData>
        get() = _screenState
    private val _screenState: MutableLiveData<ScreenData> = MutableLiveData()

    val isRootAvailable: LiveData<Boolean>
        get() = _isRootAvailable
    private val _isRootAvailable: MutableLiveData<Boolean> = MutableLiveData()

    val navigateToPage: LiveData<PageType>
        get() = _navigateToPage
    private val _navigateToPage: MutableLiveData<PageType> = MutableLiveData()

    private var currentPage: PageType? = null

    val currentPlayingSongInfo: LiveData<SongData>
        get() = _currentPlayingSongInfo
    private val _currentPlayingSongInfo: MutableLiveData<SongData> = MutableLiveData()

    val layoutForPlaylistTab: LiveData<LayoutType>
        get() = _layoutForPlaylistTab
    private val _layoutForPlaylistTab: MutableLiveData<LayoutType> = MutableLiveData()

    val sortingForPlaylistTab: LiveData<SortingUtil.SortingOption>
        get() = _sortingForPlaylistTab
    private val _sortingForPlaylistTab: MutableLiveData<SortingUtil.SortingOption> = MutableLiveData()

    val layoutForAlbumTab: LiveData<LayoutType>
        get() = _layoutForAlbumTab
    private val _layoutForAlbumTab: MutableLiveData<LayoutType> = MutableLiveData()

    val sortingForAlbumTab: LiveData<SortingUtil.SortingOption>
        get() = _sortingForAlbumTab
    private val _sortingForAlbumTab: MutableLiveData<SortingUtil.SortingOption> = MutableLiveData()

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
    private val _isShowingSearchMode: MutableLiveData<Boolean> = MutableLiveData()

    val notifyHideKeyboard: LiveData<Int>
        get() = _notifyHideKeyboard
    private val _notifyHideKeyboard: MutableLiveData<Int> = MutableLiveData()

    val showLoadingScreen: LiveData<Boolean>
        get() = _showLoadingScreen
    private val _showLoadingScreen: MutableLiveData<Boolean> = MutableLiveData(true)

    private val loadingHandler = Handler(Looper.getMainLooper())

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

    fun handleSearchButtonClick() {
        Timber.d("handleSearchButtonClick: ")
        _isShowingSearchMode.postValue(true)
    }

    fun handleCancelSearchButtonClick() {
        Timber.d("handleCancelSearchButtonClick: ")
        _isShowingSearchMode.postValue(false)
    }

    fun removeVirtualKeyboard() {
        Timber.d("removeVirtualKeyboard: ")
        _notifyHideKeyboard.postValue(_notifyHideKeyboard.value?.inc() ?: 0)
    }

    fun querySearchDatabase(search: String) {
        Timber.d("querySearchDatabase: search=$search")
        viewModelScope.launch(Dispatchers.IO) {
            val searchResults = SearchDatabase.getDatabase(getApplication<Application>().applicationContext)
                .playlistDao()
                .findDescriptionFromSearchStr(search)
            _currentSearchList.postValue(searchResults)
        }
    }

    /**
     * I need to catalog the music library so that I can add search functionality.
     */
    private fun catalogMusicLibrary() {
        Timber.d("catalogMusicLibrary: ")
        val executor = Executors.newFixedThreadPool(4)

        albumMediaItemList.value?.let { albums ->
            val updatedSearchData: MutableList<SearchData> = mutableListOf()

            for(album in albums) {
                val albumSearchData = SearchData(
                    description = "${album.mediaId} - ${album.mediaMetadata.albumArtist}",
                    albumTitle = album.mediaId,
                    artist = album.mediaMetadata.albumArtist.toString(),
                    artworkUri = album.mediaMetadata.artworkUri.toString(),
                    isAlbum = true
                )

                updatedSearchData.add(albumSearchData)

                mediaBrowser?.let { browser ->
                    val childrenFuture =
                        browser.getChildren(album.mediaId, 0, Int.MAX_VALUE, null)
                    childrenFuture.addListener({

                        val songs = childrenFuture.get().value?.toList() ?: listOf()

                        for(song in songs) {
                            updatedSearchData.add(
                                SearchData(
                                    description = "${song.mediaMetadata.title.toString()} - ${song.mediaMetadata.albumTitle}",
                                    songTitle = song.mediaMetadata.title.toString(),
                                    artworkUri = song.mediaMetadata.artworkUri.toString(),
                                    albumTitle = song.mediaMetadata.albumTitle.toString(),
                                    artist = song.mediaMetadata.artist.toString(),
                                    songUri = song.mediaId,
                                    isAlbum = false
                            ))
                        }
                    }, executor)
                }
            }

            viewModelScope.launch(Dispatchers.IO) {
                SearchDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .playlistDao()
                    .insertItems(*updatedSearchData.toTypedArray())
            }
        }

        //TODO should this code recursively run?
    }

    /**
     * Changes between songs being shuffled and songs being in original order.
     */
    fun flipShuffleState() {
        if(_shuffleMode.value == ShuffleType.SHUFFLED) {
            //Set to be original order
            _shuffleMode.postValue(ShuffleType.NOT_SHUFFLED)
            restoreOriginalSongOrder()
            saveShufflePref(getApplication<Application>().applicationContext, ShuffleType.NOT_SHUFFLED)
            Timber.d("flipShuffleState: ${ShuffleType.NOT_SHUFFLED}")
        } else {
            //Set to be shuffled
            _shuffleMode.postValue(ShuffleType.SHUFFLED)
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

    private fun setTabLayoutsFromPrefs(context: Context) {
        Timber.d("setTabLayoutsFromPrefs: ")
        determinePlaylistTabLayout(context)
        determineAlbumTabLayout(context)
    }

    private fun setTabSortingOptionFromPrefs(context: Context) {
        Timber.d("setTabSortingOptionFromPrefs: ")
        determinePlaylistTabSorting(context)
        determineAlbumTabSorting(context)
    }

    private fun setMusicPlayingPrefs(context: Context) {
        Timber.d("setMusicPlayingPrefs: ")
        //determineLoopingPref(context)
        determineShufflePref(context)
    }

    private fun determinePlaylistTabLayout(context: Context) {
        Timber.d("determinePlaylistTabLayout: ")
        viewModelScope.launch {
            DataStoreUtil.getPlaylistLayoutPreference(context).collect { savedLayoutString ->
                val layout = LayoutType.determineLayoutFromString(savedLayoutString)
                _layoutForPlaylistTab.postValue(layout)
            }
        }
    }

    private fun determineAlbumTabLayout(context: Context) {
        Timber.d("determineAlbumTabLayout: ")
        viewModelScope.launch {
            DataStoreUtil.getAlbumLayoutPreference(context).collect { savedLayoutString ->
                val layout = LayoutType.determineLayoutFromString(savedLayoutString)
                _layoutForAlbumTab.postValue(layout)
            }
        }
    }

    private fun determinePlaylistTabSorting(context: Context) {
        Timber.d("determinePlaylistTabSorting: ")
        viewModelScope.launch {
            DataStoreUtil.getPlaylistSortingPreference(context).collect { savedSortingString ->
                val sorting = SortingUtil.determineSortingOptionFromTitle(savedSortingString)
                _sortingForPlaylistTab.postValue(sorting)
            }
        }
    }

    private fun determineAlbumTabSorting(context: Context) {
        Timber.d("determineAlbumTabSorting: ")
        viewModelScope.launch {
            DataStoreUtil.getAlbumSortingPreference(context).collect { savedSortingString ->
                val sorting = SortingUtil.determineSortingOptionFromTitle(savedSortingString)
                _sortingForAlbumTab.postValue(sorting)
            }
        }
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

    fun savePlaylistLayout(context: Context, layout: LayoutType) {
        Timber.d("savePlaylistLayout: layout=$layout")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaylistLayoutPreference(context, layout)
        }
        _layoutForPlaylistTab.postValue(layout)
    }

    fun saveAlbumLayout(context: Context, layout: LayoutType) {
        Timber.d("saveAlbumLayout: layout=$layout")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setAlbumLayoutPreference(context, layout)
        }
        _layoutForAlbumTab.postValue(layout)
    }

    fun savePlaylistSorting(context: Context, sorting: SortingUtil.SortingOption) {
        Timber.d("savePlaylistSorting: sorting=$sorting")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaylistSortingPreference(context, sorting)
        }
        _sortingForPlaylistTab.postValue(sorting)
    }

    fun saveAlbumSorting(context: Context, sorting: SortingUtil.SortingOption) {
        Timber.d("saveAlbumSorting: sorting=$sorting")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setAlbumSortingPreference(context, sorting)
        }
        _sortingForAlbumTab.postValue(sorting)
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

    fun updateSortingForPage(option: SortingUtil.SortingOption) {
        if(currentPage == PageType.PLAYLIST_PAGE) {
            _sortingForPlaylistTab.postValue(option)
        } else if(currentPage == PageType.ALBUM_PAGE) {
            _sortingForAlbumTab.postValue(option)
        }
    }

    fun getCurrentPage(): PageType? {
        return currentPage
    }

    private var mediaBrowser: MediaBrowser? = null
    private var rootMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    private val mediaItemUtil: MediaItemUtil = MediaItemUtil()

    init {
        Timber.d("init: ")
        checkPermissions()

        //ex. the layout of the albums / playlist fragments
        checkUserPreferences()

        //TODO this should be moved into dependency injection...
        availablePlaylists = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().getAllPlaylists()
    }

    /**
     * Determine all saved user preferences, loopMode, shuffleMode, layout, sorting.
     */
    private fun checkUserPreferences() {
        Timber.d("checkUserPreferences: ")
        setTabLayoutsFromPrefs(getApplication<Application>().applicationContext)
        setTabSortingOptionFromPrefs(getApplication<Application>().applicationContext)
        setMusicPlayingPrefs(getApplication<Application>().applicationContext)
    }

    /**
     * Creates a playlist in memory.
     * @param playlistName Name of a new playlist. TODO Don't allow two albums of the same name.
     */
    fun createNamedPlaylist(playlistName: String) {
        Timber.d("createNamedPlaylist: playlistName=$playlistName")

        val playlist = Playlist(
            title = playlistName,
            artFile = "",
            songs = PlaylistData(listOf()),
            creationTimestamp = LocalDateTime.now().toString(),
            lastModificationTimestamp = LocalDateTime.now().toString()
        )

        viewModelScope.launch(Dispatchers.IO) {
            PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().insertPlaylists(
                playlist
            )
        }
    }

    fun removeSongsFromPlaylist(playlistTitle: String, songs: List<MediaItem>) {
        Timber.d("removeSongsFromPlaylist: playlistTitle=$playlistTitle, songs=$songs")
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val currentPlaylist =
                    PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                        .playlistDao()
                        .findPlaylistByName(playlistTitle)

                val removeSongTitles = MediaItemUtil().createSongDataFromListOfMediaItem(songs).map { removeSong ->
                    removeSong.songTitle
                }

                val modSongList = currentPlaylist.songs.songs.toMutableList()
                modSongList.removeAll { song ->
                        removeSongTitles.contains(song.songTitle)
                    }

                val updatePlaylist = Playlist(
                    id = currentPlaylist.id,
                    title = currentPlaylist.title,
                    artFile = currentPlaylist.artFile,
                    songs = PlaylistData(modSongList),
                    creationTimestamp = currentPlaylist.creationTimestamp,
                    lastModificationTimestamp = LocalDateTime.now().toString()
                )

                PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .playlistDao()
                    .updatePlaylists(
                        updatePlaylist
                    )
            } catch (e: Exception) {
                Timber.d("removeSongsFromPlaylist: Error removing song from playlist, e=$e")
            }
        }
    }

    /**
     * @param albumSongGroup A song group associated with a playlist.
     */
    fun updatePlaylistOrder(albumSongGroup: SongGroup) {
        Timber.d("updatePlaylistOrder: albumSongGroup=$albumSongGroup")
        if(albumSongGroup.type == SongGroupType.PLAYLIST) {

            viewModelScope.launch(Dispatchers.IO) {
                val currentPlaylist = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .playlistDao()
                    .findPlaylistByName(albumSongGroup.title)

                //Turn the media items into a list of SongData
                val modifySongData = MediaItemUtil().createSongDataFromListOfMediaItem(albumSongGroup.songs)

                //Modify the original playlist
                val modifyPlaylist = Playlist(
                    id = currentPlaylist.id,
                    title = currentPlaylist.title,
                    artFile = currentPlaylist.artFile,
                    songs = PlaylistData(modifySongData),
                    creationTimestamp = currentPlaylist.creationTimestamp,
                    lastModificationTimestamp = LocalDateTime.now().toString()
                )

                //Update the database with the updated playlist
                PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .playlistDao()
                    .updatePlaylists(
                        modifyPlaylist
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
     * TODO I also want to save the position of the last song I was in.
     */
    fun saveQueue() {
        Timber.d("saveQueue: ")
        mediaController.value?.let { controller ->

            //Save current Player state
            savePlayerState(controller)

            val songs = UtilImpl.getSongListFromMediaController(controller)

            if(!songs.isNullOrEmpty()) {

                val playlistData = PlaylistData(
                    mediaItemUtil.createSongDataFromListOfMediaItem(songs)
                )

                viewModelScope.launch(Dispatchers.IO) {
                    val savedQueue = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                        .playlistDao()
                        .findPlaylistByName(Const.PLAYLIST_QUEUE_TITLE)

                    //Make sure to save the queue with the same id, so there isn't duplicates for queue in datastore
                    val updateStoredQueue = if(savedQueue != null) {
                        Playlist(
                            id = savedQueue.id,
                            title = savedQueue.title,
                            artFile = savedQueue.artFile,
                            songs = playlistData,
                            creationTimestamp = savedQueue.creationTimestamp,
                            lastModificationTimestamp = LocalDateTime.now().toString()
                        )
                    } else {
                        Playlist(
                            title = Const.PLAYLIST_QUEUE_TITLE,
                            artFile = "",
                            songs = playlistData,
                            creationTimestamp = LocalDateTime.now().toString(),
                            lastModificationTimestamp = LocalDateTime.now().toString()
                        )
                    }

                    PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                        .playlistDao()
                        .insertPlaylists(updateStoredQueue)
                }
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
            val oldQueue = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                .playlistDao()
                .findPlaylistByName(Const.PLAYLIST_QUEUE_TITLE)

            val playbackPosition = DataStoreUtil.getPlaybackPosition(getApplication<Application>().applicationContext).firstOrNull()
            val songPosition = DataStoreUtil.getSongPosition(getApplication<Application>().applicationContext).firstOrNull()

            //oldQueue can be null if this is a fresh install or if there is no previous queue
            if(oldQueue == null || oldQueue.songs.songs.isEmpty()) {
                Timber.d("restoreQueue: No queue to restore!")

                //Remove Loading Screen [100ms added to cover image switch]
                loadingHandler.postDelayed({
                    _showLoadingScreen.postValue(false)
                }, 100)
                return@launch
            }

            withContext(Dispatchers.Main) {
                mediaController.value?.let { controller ->
                    controller.setMediaItems(
                        mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(
                            oldQueue.songs.songs
                        )
                    )

                    // Restore Playback State
                    if(songPosition != null && songPosition < controller.mediaItemCount) {
                        if(playbackPosition != null) {
                            controller.seekTo(songPosition, playbackPosition)
                        } else {
                            controller.seekTo(songPosition, 0)
                        }
                    }

                    //Remove Loading Screen [100ms added to cover image switch]
                    loadingHandler.postDelayed({
                        _showLoadingScreen.postValue(false)
                    }, 100)
                }
            }
        }
    }

    /**
     * Ability to add a list of songs to a list of playlists.
     */
    fun addSongsToAPlaylist(playlistTitles: List<String>, songs: List<MediaItem>) {
        Timber.d("addSongsToAPlaylist: playlistTitles=$playlistTitles, songs=$songs")
        playlistTitles.forEach { playlist ->
            addListOfSongMediaItemsToAPlaylist(playlist, songs)
        }
    }
    
    fun updatePlaylistTitle(currentTitle: String, newTitle: String ) {
        Timber.d("updatePlaylistTitle: currentTitle=$currentTitle, newTitle=$newTitle")
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(currentTitle)

            //If playlist is null I should create one?
            if(playlist == null) {
                Timber.d("addListOfSongMediaItemsToAPlaylist: No playlist found for playlistTitle=$currentTitle")
                return@launch
            }

            val updatedPlaylist = Playlist(
                id = playlist.id,
                title = newTitle,
                artFile = "$newTitle.jpg",
                songs = playlist.songs,
                creationTimestamp = playlist.creationTimestamp,
                lastModificationTimestamp = LocalDateTime.now().toString()
            )

            //The playlistImage is saved using playlistTitle, update playlist image file name
           UtilImpl.renamePlaylistImageFile(getApplication<Application>().applicationContext, currentTitle, newTitle)

            PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().updatePlaylists(updatedPlaylist)
        }
    }

    /**
     * Update the playlist image.
     */
    fun updatePlaylistImage(playlistTitle: String, artFileName: String) {
        Timber.d("updatePlaylistImage: playlistTitle=$playlistTitle, artFileName=$artFileName")
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistTitle)

            //If playlist is null I should create one?
            if(playlist == null) {
                Timber.d("addListOfSongMediaItemsToAPlaylist: No playlist found for playlistTitle=$playlistTitle")
                return@launch
            }

            val updatedPlaylist = Playlist(
                id = playlist.id,
                title = playlist.title,
                artFile = artFileName,
                songs = playlist.songs,
                creationTimestamp = playlist.creationTimestamp,
                lastModificationTimestamp = LocalDateTime.now().toString()
            )

            PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().updatePlaylists(updatedPlaylist)
        }
    }

    /**
     * Add a list of songs to the Playlist. Even if adding only one song still use this function.
     */
    private fun addListOfSongMediaItemsToAPlaylist(playlistTitle: String, songs: List<MediaItem>) {
        Timber.d("addListOfSongMediaItemsToAPlaylist: playlistTitle=$playlistTitle, songs.size=${songs.size}")
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistTitle)

            //If playlist is null I should create one?
            if(playlist == null) {
                Timber.d("addListOfSongMediaItemsToAPlaylist: No playlist found for playlistTitle=$playlistTitle")
                return@launch
            }
            
            val storableSongs = MediaItemUtil().createSongDataFromListOfMediaItem(songs)
            
            val modifiedSongList = playlist.songs.songs.toMutableList()
            modifiedSongList.addAll(storableSongs)

            playlist.songs = PlaylistData(modifiedSongList)
            playlist.lastModificationTimestamp = LocalDateTime.now().toString()
            PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().updatePlaylists(playlist)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared: ")

        mediaController.value?.let { controller ->
            controller.removeListener(playerListener)
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
            controller.clearMediaItems()
            controller.pause()

            addTracksSaveTrackOrder(songGroup.songs, clearOriginalSongList = true)

            controller.seekTo(position, 0L)
            controller.play()
        }
    }

    /**
     * Clear queue and play the specified playlist.
     */
    fun playPlaylist(playlistTitle: String) {
        Timber.d("playPlaylist: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            //Grab the media items based on the playlistTitle
            val playlist =  PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistTitle)
            val songs = playlist.songs.songs
            val playlistMediaItems = mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(songs)

            withContext(Dispatchers.Main) {
                //Remove current songs in the queue
                mediaController.value?.clearMediaItems()

                addTracksSaveTrackOrder(playlistMediaItems, clearOriginalSongList = true)

                mediaController.value?.play()
            }
        }
    }

    fun addPlaylistToBackOfQueue(playlistTitle: String) {
        Timber.d("addPlaylistToBackOfQueue: playlistTitle=$playlistTitle")
        viewModelScope.launch(Dispatchers.IO) {
            //Grab the media items based on the playlistTitle
            val playlist =  PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistTitle)
            val songs = playlist.songs.songs
            val playlistMediaItems = mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(songs)

            withContext(Dispatchers.Main) {
                addTracksSaveTrackOrder(playlistMediaItems)
            }
        }
    }

    /**
     * Adds multiple songs to the end of the controller in the queue
     */
    fun addSongsToEndOfQueue(songs: List<MediaItem>) {
        Timber.d("addSongsToEndOfQueue: songs=$songs")
        addTracksSaveTrackOrder(songs)
    }

    /**
     * Clear all songs out of Player.
     */
    fun clearQueue() {
        Timber.d("clearQueue: ")
        mediaController.value?.clearMediaItems()
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

            _loopMode.postValue(controller.repeatMode)
            controller.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    /**
     * Sets up the MediaBrowser, which is used to browse music on the app.
     * @param session The session token associated with this app. [Should only be one]
     */
    private fun setupMediaBrowser(session: SessionToken) {
        Timber.d("setupMediaBrowser: session=$session")

        val browserFuture = MediaBrowser.Builder(getApplication<Application>().applicationContext, sessionToken).buildAsync()
        browserFuture.addListener({
            mediaBrowser = browserFuture.get()
            getRoot()
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
                _isRootAvailable.value = true
            }, MoreExecutors.directExecutor())
        }
    }

    /**
     * Will return a list of MediaItems associated with albums on device storage.
     */
    fun queryAvailableAlbums() {
        Timber.d("queryAvailableAlbums: ")
        if(mediaBrowser != null) {

            mediaBrowser?.let { browser ->
                rootMediaItem?.let { rootItem ->
                    val childrenFuture =
                        browser.getChildren(rootItem.mediaId, 0, Int.MAX_VALUE, null)
                    childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                        _albumMediaItemList.value =  childrenFuture.get().value?.toList() ?: listOf()

                        //Start Cataloging the Songs found in the music library
                        catalogMusicLibrary()
                    }, MoreExecutors.directExecutor())
                }
            }
        } else {
            Timber.d("queryAvailableAlbums: mediaBrowser isn't ready...")
        }
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on album title.
     * @param albumId The title of an album to be queried.
     */
    fun querySongsFromAlbum(albumId: String, queueAddType: QueueAddType = QueueAddType.QUEUE_DONT_ADD) {
        Timber.d("querySongsFromAlbum: albumId=$albumId, queueAddType=$queueAddType")
        if(mediaBrowser != null) {
            mediaBrowser?.let { browser ->
                val childrenFuture =
                    browser.getChildren(albumId, 0, Int.MAX_VALUE, null)
                childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                    val songs = childrenFuture.get().value?.toList() ?: listOf()
                    val title = albumId
                    val songGroupType = SongGroupType.ALBUM
                    _currentSongList.value = SongGroup(songGroupType, songs, title)

                    //When I query the album, determine if/how album should be played
                    if(queueAddType == QueueAddType.QUEUE_END_ADD) {
                        addTracksSaveTrackOrder(songs, clearOriginalSongList = true)
                    } else if(queueAddType == QueueAddType.QUEUE_CLEAR_ADD) {
                        _mediaController.value?.clearMediaItems()
                        addTracksSaveTrackOrder(songs, clearOriginalSongList = true)
                        _mediaController.value?.play()
                    }

                }, MoreExecutors.directExecutor())
            }
        } else {
            Timber.d("querySongsFromAlbum: mediaBrowser isn't ready...")
        }
    }

    /**
     * Instead of adding songs directly to the mediaController instead, I can track when songs are added
     * allowing for shuffle and restore functionality.
     */
    private fun addTracksSaveTrackOrder(mediaItems: List<MediaItem>, clearOriginalSongList: Boolean = false) {
        if(clearOriginalSongList) {
            _originalSongOrder.value = listOf()
        }

        //save songs to the original song order
        val songOrder = originalSongOrder.value?.toMutableList()
        songOrder?.addAll(mediaItems)

        Timber.d("addTracksSaveTrackOrder: songOrder=${songOrder?.map { it -> it.mediaMetadata.title }}, mediaItems=${mediaItems.map { it -> it.mediaMetadata.title }}, clearOriginalSongList=$clearOriginalSongList")
        _originalSongOrder.postValue( songOrder ?: mediaItems  )

        if(_shuffleMode.value == ShuffleType.SHUFFLED) {
            val shuffledSongs = shuffleSongs(mediaItems)
            _mediaController.value?.addMediaItems(shuffledSongs)
        } else {
            _mediaController.value?.addMediaItems(mediaItems)
        }
    }

    private fun shuffleSongs(mediaItems: List<MediaItem>): List<MediaItem> {
        Timber.d("shuffleSongs: mediaItems=$mediaItems")
        return mediaItems.shuffled()
    }

    private fun shuffleSongsInMediaController() {
        Timber.d("shuffleSongsInMediaController: ")
        _mediaController.value?.let { controller ->
            val currentSongs = UtilImpl.getSongListFromMediaController(controller)

            val shuffledSongs = shuffleSongs(currentSongs)

            controller.clearMediaItems()

            _mediaController.value?.addMediaItems(shuffledSongs)
        }
    }

    private fun restoreOriginalSongOrder() {
        Timber.d("restoreOriginalSongOrder: ")
        _mediaController.value?.let { controller ->
            _originalSongOrder.value?.let { originalSongs ->
                controller.clearMediaItems()

                controller.addMediaItems(originalSongs)
            }
        }
    }

    /**
     * Clears the current queue and starts playing the chosen album.
     */
    fun playAlbum(albumTitle: String) {
        Timber.d("playAlbum: albumTitle=$albumTitle")
        querySongsFromAlbum(
            albumTitle,
            queueAddType = QueueAddType.QUEUE_CLEAR_ADD
        )
    }

    /**
     * Adds to album to the back of the current queue.
     */
    fun addAlbumToBackOfQueue(albumTitle: String) {
        Timber.d("addAlbumToBackOfQueue: albumTitle=$albumTitle")
        querySongsFromAlbum(
            albumTitle,
            queueAddType = QueueAddType.QUEUE_END_ADD
        )
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on a playlist.
     * @param albumId The title of an playlist to be queried.
     */
    fun querySongsFromPlaylist(playlistId: String) {
        Timber.d("querySongsFromPlaylist: playlistId=$playlistId")
        viewModelScope.launch(Dispatchers.IO) {
            val playlist =  PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistId)
            val songs = playlist.songs.songs
            val mediaItems = mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(songs)

            val songGroupType = SongGroupType.PLAYLIST
            val title = playlistId

            _currentSongList.postValue(SongGroup(songGroupType, mediaItems, title))
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
            val playlist = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                .playlistDao()
                .findPlaylistByName(playlistTitle)

            if(playlist != null) {
                PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .playlistDao()
                    .deletePlaylists(playlist)

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
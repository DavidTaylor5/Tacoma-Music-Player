package com.andaagii.tacomamusicplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
import com.andaagii.tacomamusicplayer.activity.dataStore
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.data.PlaylistData
import com.andaagii.tacomamusicplayer.database.PlaylistDatabase
import com.andaagii.tacomamusicplayer.data.ScreenData
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.enum.LayoutType
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.enum.ScreenType
import com.andaagii.tacomamusicplayer.enum.SongGroupType
import com.andaagii.tacomamusicplayer.service.MusicService
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.andaagii.tacomamusicplayer.util.MediaItemUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.Util
import timber.log.Timber

//TODO make a util class called PlaylistUtils, with specific playlist functionality
//TODO make a util class called AlbumUtils, with specific album functionality
//This should be more organized, there are way to many functions in this one class.

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

    val currentPage: LiveData<PageType>
        get() = _currentPage
    private val _currentPage: MutableLiveData<PageType> = MutableLiveData()

    val currentPlayingSongInfo: LiveData<SongData>
        get() = _currentPlayingSongInfo
    private val _currentPlayingSongInfo: MutableLiveData<SongData> = MutableLiveData()

    val layoutForPlaylistTab: LiveData<LayoutType>
        get() = _layoutForPlaylistTab
    private val _layoutForPlaylistTab: MutableLiveData<LayoutType> = MutableLiveData(LayoutType.LINEAR_LAYOUT)

    val layoutForAlbumTab: LiveData<LayoutType>
        get() = _layoutForAlbumTab
    private val _layoutForAlbumTab: MutableLiveData<LayoutType> = MutableLiveData(LayoutType.LINEAR_LAYOUT)

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
    }

    fun setTabLayoutsFromUserSettings(context: Context) {
        determinePlaylistTabLayout(context)
        determineAlbumTabLayout(context)
    }

    private fun determinePlaylistTabLayout(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val layoutString = DataStoreUtil.getPlaylistLayoutPreference(context).single()
            val layout = LayoutType.determineLayoutFromString(layoutString)
            _layoutForPlaylistTab.postValue(layout)
        }
    }

    private fun determineAlbumTabLayout(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val layoutString = DataStoreUtil.getAlbumLayoutPreference(context).single()
            val layout = LayoutType.determineLayoutFromString(layoutString)
            _layoutForAlbumTab.postValue(layout)
        }
    }

    /**
     * Experimental code, which page for music chooser fragment?
     */
    fun setPage(page: PageType) {
        _currentPage.value = page
    }

    private var mediaBrowser: MediaBrowser? = null
    private var rootMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    private val mediaItemUtil: MediaItemUtil = MediaItemUtil()

    init {
        Timber.d("init: ")
        checkPermissions()

        //TODO this should be moved into dependency injection...
        availablePlaylists = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().getAllPlaylists()
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
            songs = PlaylistData(listOf())
        )

        viewModelScope.launch(Dispatchers.IO) {
            PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().insertPlaylists(
                playlist
            )
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

        //TODO function to save position of current song

        mediaController.value?.let { controller ->
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
                            savedQueue.id,
                            savedQueue.title,
                            savedQueue.artFile,
                            playlistData
                        )
                    } else {
                        Playlist(
                            title = Const.PLAYLIST_QUEUE_TITLE,
                            artFile = "",
                            songs = playlistData
                        )
                    }

                    PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                        .playlistDao()
                        .insertPlaylists(updateStoredQueue)
                }
            }
        }
    }

    //TODO the empty playingmusicfragment shows briefly, I might have to remove the dog image.
    //Or add it later...
    //TODO make the queue playlist not show up
    //OR have a constant for the playlist ID that no one will think to use as a title....
    private fun restoreQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            val oldQueue = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                .playlistDao()
                .findPlaylistByName(Const.PLAYLIST_QUEUE_TITLE)

            //TODO I need a function to turn a Playlist into a list<mediaItems>

            //oldQueue can be null if this is a fresh install or if there is no previous queue
            if(oldQueue == null || oldQueue.songs.songs.isEmpty()) {
                Timber.d("restoreQueue: No queue to restore!")
                return@launch
            }

            //TODO I can't call mediaController from inside this thread?
            withContext(Dispatchers.Main) {
                mediaController.value?.let { controller ->
                    controller.setMediaItems(
                        mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(
                            oldQueue.songs.songs
                        )
                    )
                }
            }
        }
    }

//    private fun doesPlaylistExist(playlistName: String): Boolean {
//        PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
//            .playlistDao()
//            .findPlaylistByName(playlistName)
//    }

    //TODO I should move all of the database function to a new util class.

    /**
     * Ability to add a list of songs to a list of playlists.
     */
    fun addSongsToAPlaylist(playlistTitles: List<String>, songs: List<MediaItem>) {
        playlistTitles.forEach { playlist ->
            addListOfSongMediaItemsToAPlaylist(playlist, songs)
        }
    }

  //I need to add back that playlist id...
    fun updatePlaylistTitle(currentTitle: String, newTitle: String ) {
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
                songs = playlist.songs
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
                songs = playlist.songs
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

//TODO crash
            val modifiedSongList = playlist.songs.songs.toMutableList()
            modifiedSongList.addAll(storableSongs)

            playlist.songs = PlaylistData(modifiedSongList)
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

    /**
     * Returns a list of playlists saved on the device.
     */
    private fun getCurrentPlaylists(): List<Playlist> {
        return availablePlaylists.value ?: listOf()
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

        mediaController.value?.let { controller ->
            controller.clearMediaItems()
            controller.pause()

            controller.addMediaItems(songGroup.songs)

            controller.seekTo(position, 0L)
            controller.play()
        }
    }

    //TODO add a play button the playlists and the albums, so that the user can quickly play just those albums or playlists

    /**
     * Clear queue and play the specified playlist.
     */
    fun playPlaylist(playlistTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            //Grab the media items based on the playlistTitle
            val playlist =  PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistTitle)
            val songs = playlist.songs.songs
            val playlistMediaItems = mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(songs)

            withContext(Dispatchers.Main) {
                //Remove current songs in the queue
                mediaController.value?.clearMediaItems()

                mediaController.value?.addMediaItems(playlistMediaItems)

                mediaController.value?.play()
            }
        }
    }

    fun addPlaylistToBackOfQueue(playlistTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            //Grab the media items based on the playlistTitle
            val playlist =  PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext).playlistDao().findPlaylistByName(playlistTitle)
            val songs = playlist.songs.songs
            val playlistMediaItems = mediaItemUtil.convertListOfSongDataIntoListOfMediaItem(songs)

            withContext(Dispatchers.Main) {
                mediaController.value?.addMediaItems(playlistMediaItems)
            }
        }
    }

    /**
     * Adds multiple songs to the end of the controller in the queue
     */
    fun addSongsToEndOfQueue(songs: List<MediaItem>) {
        mediaController.value?.addMediaItems(songs)
    }

    /**
     * Clear all songs out of Player.
     */
    fun clearQueue() {
        mediaController.value?.clearMediaItems()
    }

    /**
     * Sets the current screen of the application.
     * @param nextScreen The next screen to be navigated to.
     */
    private fun setScreenData(nextScreen: ScreenType) {
        Timber.d("setScreenData: ")

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
        sessionToken = createSessionToken()
        setupMediaController(sessionToken)
        setupMediaBrowser(sessionToken)
        setScreenData(ScreenType.MUSIC_PLAYING_SCREEN)
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

            //Add old queue to the mediaController
            restoreQueue()

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
    fun querySongsFromAlbum(albumId: String, shouldPlayAlbum: Boolean = false) {
        Timber.d("querySongsFromAlbum: ")
        if(mediaBrowser != null) {

            mediaBrowser?.let { browser ->
                val childrenFuture =
                    browser.getChildren(albumId, 0, Int.MAX_VALUE, null)
                childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                    val songs = childrenFuture.get().value?.toList() ?: listOf()
                    val title = albumId
                    val songGroupType = SongGroupType.ALBUM
                    _currentSongList.value = SongGroup(songGroupType, songs, title)

                    if(shouldPlayAlbum) {
                        _mediaController.value?.clearMediaItems()
                        _mediaController.value?.addMediaItems(songs)
                        _mediaController.value?.play()
                    }
                }, MoreExecutors.directExecutor())
            }
        } else {
            Timber.d("querySongsFromAlbum: mediaBrowser isn't ready...")
        }
    }

    fun playAlbum(albumTitle: String) {
        Timber.d("playAlbum: ")
        querySongsFromAlbum(
            albumTitle,
            shouldPlayAlbum = true
        )
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on a playlist.
     * @param albumId The title of an playlist to be queried.
     */
    fun querySongsFromPlaylist(playlistId: String) {
        Timber.d("querySongsFromPlaylist: ")
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

        playlists.forEach { playlistTitle ->
            removePlaylist(playlistTitle)
        }
    }

    /**
     * Removes a single playlist based on its title.
     */
    private fun removePlaylist(playlistTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                .playlistDao()
                .findPlaylistByName(playlistTitle)

            if(playlist != null) {
                PlaylistDatabase.getDatabase(getApplication<Application>().applicationContext)
                    .playlistDao()
                    .deletePlaylists(playlist)
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
        Timber.d("handlePermissionResult: ")
        if(requestCode == AppPermissionUtil.readMediaAudioRequestCode) {
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
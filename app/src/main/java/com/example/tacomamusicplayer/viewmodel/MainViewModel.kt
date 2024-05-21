package com.example.tacomamusicplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.tacomamusicplayer.data.ScreenData
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.service.MusicService
import com.example.tacomamusicplayer.util.AppPermissionUtil
import com.google.common.util.concurrent.MoreExecutors
import timber.log.Timber

/**
 * The MainViewModel of the project, will include information on current screen, logic for handling
 * permissions, and will provide the UI with media related information.
 */
class MainViewModel(application: Application): AndroidViewModel(application) {

    private val permissionManager = AppPermissionUtil()

    val mediaController: LiveData<MediaController>
        get() = _mediaController
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    val albumMediaItemList: LiveData<List<MediaItem>>
        get() = _albumMediaItemList
    private val _albumMediaItemList: MutableLiveData<List<MediaItem>> = MutableLiveData()

    val songQueue: LiveData<List<MediaItem>>
        get() = _songQueue
    private val _songQueue: MutableLiveData<List<MediaItem>> = MutableLiveData(listOf())

    //List of songs to be inspected... albums or playlists
    val currentSongList: LiveData<List<MediaItem>>
        get() = _currentSongList
    private val _currentSongList: MutableLiveData<List<MediaItem>> = MutableLiveData(listOf())

    val arePermissionsGranted: LiveData<Boolean>
        get() = _arePermissionsGranted
    private val _arePermissionsGranted: MutableLiveData<Boolean> = MutableLiveData()

    val isAudioPermissionGranted: LiveData<Boolean>
        get() = _isAudioPermissionGranted
    private val _isAudioPermissionGranted: MutableLiveData<Boolean> = MutableLiveData()


    val screenState : LiveData<ScreenData>
        get() = _screenState
    private val _screenState: MutableLiveData<ScreenData> = MutableLiveData()

    val isRootAvailable: LiveData<Boolean>
        get() = _isRootAvailable
    private val _isRootAvailable: MutableLiveData<Boolean> = MutableLiveData()

    private var mediaBrowser: MediaBrowser? = null
    private var rootMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    init {
        Timber.d("init: ")
        checkPermissions()
    }

    fun checkPermissionsIfOnPermissionDeniedScreen() {
        Timber.d("checkPermissionsIfOnPermissionDeniedScreen: ")

        screenState.value?.let { data ->
            if(data.currentScreen == ScreenType.PERMISSION_DENIED_SCREEN)
                checkPermissions()
        }
    }

    //TODO I should be able to move all of this queue logic to a seperate class?
    /**
     * Add a single song to the end of the queue.
     */
    fun addSongToQueue(song: MediaItem) {
        val songList = _songQueue.value ?: listOf()
        val changeSongList = songList.toMutableList()
        changeSongList.add(song)

        _songQueue.value = changeSongList
    }

    /**
     * Add a list of songs to the end of the queue
     */
    private fun addSongListToQueue(songs: List<MediaItem>) {
        val songList = _songQueue.value ?: listOf()
        val changeSongList = songList.toMutableList()
        changeSongList.addAll(songs)

        _songQueue.value = changeSongList
    }

    /**
     * Remove the Queue and replace with a new song list.
     */
    private fun replaceAllSongsInQueueWithSongList(songs: List<MediaItem>) {
        _songQueue.value = songs
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
            _mediaController.value = controllerFuture.get()
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

    //TODO I'll just keep playlists as database in room, I'll remove library node, I won't need it...

    /**
     * Will return a list of MediaItems associated with albums on device storage.
     */
    fun queryAvailableAlbums() { //this will actually return playlist and library item.... [do I really need playlist...]
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
            //TOAST MESSAGE THAT mediaBrowser isn't ready...
        }
    }

    /**
     * High level function that will attempt to set a list of songs (MediaItems) based on album title.
     * @param albumId The title of an album to be queried.
     */
    fun querySongsFromAlbum(albumId: String) {
        Timber.d("querySongsFromAlbum: ")
        if(mediaBrowser != null) {

            mediaBrowser?.let { browser ->
                val childrenFuture =
                    browser.getChildren(albumId, 0, Int.MAX_VALUE, null)
                childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                    _currentSongList.value = childrenFuture.get().value?.toList() ?: listOf()
                }, MoreExecutors.directExecutor())
            }
        } else {
            //TOAST MESSAGE THAT mediaBrowser isn't ready...
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
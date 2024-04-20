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
import com.example.tacomamusicplayer.data.PermissionData
import com.example.tacomamusicplayer.service.MusicService
import com.example.tacomamusicplayer.util.AppPermissionUtil
import com.google.common.util.concurrent.MoreExecutors
import timber.log.Timber

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val permissionManager = AppPermissionUtil()

    val mediaController: LiveData<MediaController>
        get() = _mediaController
    private val _mediaController: MutableLiveData<MediaController> = MutableLiveData()

    val albumMediaItemList: LiveData<List<MediaItem>>
        get() = _albumMediaItemList
    private val _albumMediaItemList: MutableLiveData<List<MediaItem>> = MutableLiveData()

    val currentAlbumsSongMediaItemList: LiveData<List<MediaItem>>
        get() = _currentAlbumsSongMediaItemList
    private val _currentAlbumsSongMediaItemList: MutableLiveData<List<MediaItem>> = MutableLiveData()

    val arePermissionsGranted: LiveData<Boolean>
        get() = _arePermissionsGranted
    private val _arePermissionsGranted: MutableLiveData<Boolean> = MutableLiveData()

    val requestReadMediaAudioPermission: LiveData<Boolean>
        get() = _requestReadMediaAudioPermission
    private val _requestReadMediaAudioPermission: MutableLiveData<Boolean> = MutableLiveData()


    private var permissionData: PermissionData = PermissionData()


    private var mediaBrowser: MediaBrowser? = null
    private var rootMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    init {
        Timber.d("init: ")
//        initalizeMusicPlaying()
        checkPermissions()
    }

    private fun initalizeMusicPlaying() {
        sessionToken = createSessionToken()
        setupMediaController(sessionToken)
        setupMediaBrowser(sessionToken)
    }

    private fun createSessionToken(): SessionToken {
        Timber.d("createSessionToken: ")
        //SessionToken must start the Service!
        return SessionToken(getApplication<Application>().applicationContext, ComponentName(getApplication<Application>().applicationContext, MusicService::class.java))
    }

    private fun setupMediaController(session: SessionToken) {
        Timber.d("setupMediaController: session=$session")
        val controllerFuture = MediaController.Builder(getApplication<Application>().applicationContext, session).buildAsync()
        controllerFuture.addListener({
            _mediaController.value = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    //TODO move all of this update logic into the viewmodel...
    private fun setupMediaBrowser(session: SessionToken) {
        Timber.d("setupMediaBrowser: session=$session")

        val browserFuture = MediaBrowser.Builder(getApplication<Application>().applicationContext, sessionToken).buildAsync()
        browserFuture.addListener({
            mediaBrowser = browserFuture.get()
            getRoot()
        }, MoreExecutors.directExecutor())
    }

    fun handledRequestForReadMediaAudioPermission() {
        _requestReadMediaAudioPermission.value = false
    }

    private fun getRoot() {
        Timber.d("getRoot: ")
        mediaBrowser?.let { browser ->
            val rootFuture = browser.getLibraryRoot(null)
            rootFuture.addListener({
                //root node MediaItem is available here with rootFuture.get().value
                rootMediaItem = rootFuture.get().value
            }, MoreExecutors.directExecutor())
            //notify ui that root is ready
        }
    }

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
            //TOAST MESSAGE THAT mediaBrowser isn't ready...
        }
    }

    fun querySongsFromAlbum(albumId: String) {
        Timber.d("querySongsFromAlbum: ")
        if(mediaBrowser != null) {

            mediaBrowser?.let { browser ->
                val childrenFuture =
                    browser.getChildren(albumId, 0, Int.MAX_VALUE, null)
                childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                    _currentAlbumsSongMediaItemList.value = childrenFuture.get().value?.toList() ?: listOf()
                }, MoreExecutors.directExecutor())
            }
        } else {
            //TOAST MESSAGE THAT mediaBrowser isn't ready...
        }
    }

    private fun checkPermissions() {
        val isAudioPermissionGranted = permissionManager.verifyReadMediaAudioPermission(getApplication<Application>().applicationContext)
        Timber.d("checkPermissions: isAudioPermissionGranted=$isAudioPermissionGranted")
        _requestReadMediaAudioPermission.value = isAudioPermissionGranted
    }

    /**
     * I'm not sure that I need the notification permission.
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
                permissionData = PermissionData(allowedToReadAudio = true, permissionData.allowedToShowNotification)
                checkPermissions()
            } else {
                Timber.d("handlePermissionResult: read audio NOT granted!")
                //TODO UI to show permission error
            }
        }
    }
}
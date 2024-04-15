package com.example.tacomamusicplayer.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.tacomamusicplayer.databinding.ActivityMainBinding
import com.example.tacomamusicplayer.service.MusicService
import com.example.tacomamusicplayer.util.AppPermissionUtil
import com.example.tacomamusicplayer.util.UtilImpl
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.google.common.util.concurrent.MoreExecutors
import androidx.activity.viewModels

//TODO I NEED TO FIGURE OUT THE MEDIACONTROLLER, MEDIASESSION, AND UI of my app...
//TODO Figure out the session and controller first, then work on UI
//TODO work on making the UI pleasant
//TODO work on the statistics aspect
//TODO work on editing songs maybe? //This is probably not going to work...

//TODO I can just have a button for refreshing the data / music...

//TODO I need to make smart goals...

//TODO I should use a ViewPager for determining what music to play / creating a playlist...

//TODO when does service know when it can query MediaStore?

//TODO learn data store and implement in the project
//TODO figure out a system for querying songs [should this be it's own utility...]


class MainActivity : AppCompatActivity() {

    val TAG: String = MainActivity::class.java.simpleName
    private val permissionManager = AppPermissionUtil()

    private var mediaController: MediaController? = null
    private var mediaBrowser: MediaBrowser? = null
    private var rootMediaItem: MediaItem? = null
    private var albumMediaItemList: List<MediaItem>? = null
    private var currentAlbumsSongMediaItemList: List<MediaItem>? = null
    //TODOwhen these values are updated I can update the main ui...

    var psychoMediaItem: MediaItem? = null
    private lateinit var sessionToken: SessionToken

    val viewModel: MainViewModel by viewModels()


    private lateinit var binding: ActivityMainBinding

    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")

        // Setup view binding in the project
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.doSomething()

        //determine if I have the permission to read media
        if(!permissionManager.verifyReadMediaAudioPermission(this))
            //request permission
            permissionManager.requestReadMediaAudioPermission(this)
        else {
            //else I already have the permission, I can query audio from storage
        }


        //check permission for notification [notification is needed for foreground service]
        if(!permissionManager.verifyNotificationPermission(this))
            permissionManager.requestNotificationPermission(this)
        else {

            createSessionToken()

            //TODO REMOVE ALL OF THIS BELOW CODE, it will be handled by viewmodel livedata observers.

            //TODO why is this code in the else clause? this should be moved out?
            val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
            controllerFuture.addListener({
                //MediaController is available here with ControllerFuture.get()
                mediaController = controllerFuture.get()
                binding.playerView.player = mediaController
                binding.playerView.showController() //why is this an unstable api!!!

                mediaController

            }, MoreExecutors.directExecutor())

            //This browserFuture logic needs to be simplified and moved to a different function....

            val browserFuture = MediaBrowser.Builder(this, sessionToken).buildAsync()
            browserFuture.addListener({
                mediaBrowser = browserFuture.get()
                val rootFuture = mediaBrowser!!.getLibraryRoot(null)
                rootFuture.addListener({
                    //root node MediaItem is available here with rootFuture.get().value
                    val rootNode = rootFuture.get().value
                    val b = "what what"

                    val childrenFuture =
                        mediaBrowser!!.getChildren(rootNode!!.mediaId, 0, Int.MAX_VALUE, null)
                    //I guess I can do this when I want the children? Don't allow browsing files If I don't have permission...

                    //I should actually have room database here //if I have queried before and have permission, I can just start the music...

                    childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                        val children = childrenFuture.get().value
                        val c = "wu huh?"
                    }, MoreExecutors.directExecutor())
                }, MoreExecutors.directExecutor())
            }, MoreExecutors.directExecutor())
        }

            //Ideas on setting new media on the service... How to choose music from UI...
        psychoMediaItem = MediaItem.fromUri("/storage/emulated/0/Music/YEAT/2093 (P3) Digital Download/14 Keep Pushin.mp3") //This actually works huh!?
        val yeatAlbum = MediaItem.fromUri("/storage/emulated/0/Music/YEAT/2093 (P3) Digital Download") //This actually works huh!?

        val meta = MediaMetadataRetriever()
        meta.setDataSource("/storage/emulated/0/Music/YEAT/2093 (P3) Digital Download/14 Keep Pushin.mp3")
        val duration = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) //good for manually getting information from uri...

        ///storage/emulated/0/Music/01 Psycho CEO.mp3
        binding.psychoButton.setOnClickListener {
            mediaController?.setMediaItem(psychoMediaItem!!)
        }

        binding.navigateChooseMusic.setOnClickListener {
            val intent = Intent(this, ChooseMusicActivity::class.java)
            startActivity(intent)
        }

    }

    private fun createSessionToken() {
        //SessionToken must start the Service!
        sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
    }

    @OptIn(UnstableApi::class) fun setupMediaController(session: SessionToken) {
        //TODO why is this code in the else clause? this should be moved out?
        val controllerFuture = MediaController.Builder(this, session).buildAsync()
        controllerFuture.addListener({
            //MediaController is available here with ControllerFuture.get()
            mediaController = controllerFuture.get()
            binding.playerView.player = mediaController
            binding.playerView.showController() //why is this an unstable api!!!
            //mediaController is available!
        }, MoreExecutors.directExecutor())
    }

    //TODO move all of this update logic into the viewmodel...
    fun setupMediaBrowser(session: SessionToken) {
        val browserFuture = MediaBrowser.Builder(this, sessionToken).buildAsync()
        browserFuture.addListener({
            mediaBrowser = browserFuture.get()
            //mediaBrowser is available...
        }, MoreExecutors.directExecutor())

    }

    private fun getRoot() {
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

        if(mediaBrowser != null) {

            mediaBrowser?.let { browser ->
                rootMediaItem?.let { rootItem ->
                    val childrenFuture =
                        browser.getChildren(rootItem.mediaId, 0, Int.MAX_VALUE, null)
                    childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                        albumMediaItemList =  childrenFuture.get().value?.toList() ?: listOf()
                    }, MoreExecutors.directExecutor())
                }
            }
        } else {
            //TOAST MESSAGE THAT mediaBrowser isn't ready...
        }
    }

    fun querySongsFromAlbum(albumId: String) {

        if(mediaBrowser != null) {

            mediaBrowser?.let { browser ->
                val childrenFuture =
                    browser.getChildren(albumId, 0, Int.MAX_VALUE, null)
                childrenFuture.addListener({ //OKAY THIS MAKE MORE SENSE AND THIS IS COMING TOGETHER!
                    currentAlbumsSongMediaItemList = childrenFuture.get().value?.toList() ?: listOf()
                }, MoreExecutors.directExecutor())
            }
        } else {
            //TOAST MESSAGE THAT mediaBrowser isn't ready...
        }
    }

    override fun onResume() {
        super.onResume()
        UtilImpl.hideNavigationUI(window)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == AppPermissionUtil.notificationRequestCode) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, MusicService::class.java)
                this.startForegroundService(intent)
            }
        }

        else if(requestCode == AppPermissionUtil.readMediaAudioRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //readAudioFromStorage()
            } else {
                //TODO show permission error UI!
            }
        }
    }
}
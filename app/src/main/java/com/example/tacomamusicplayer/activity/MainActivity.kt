package com.example.tacomamusicplayer.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.example.tacomamusicplayer.databinding.ActivityMainBinding
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.fragment.CurrentQueueFragment
import com.example.tacomamusicplayer.fragment.MusicChooserFragment
import com.example.tacomamusicplayer.fragment.MusicPlayingFragment
import com.example.tacomamusicplayer.fragment.PermissionDeniedFragment
import com.example.tacomamusicplayer.util.AppPermissionUtil
import com.example.tacomamusicplayer.util.UtilImpl
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

//TODO I need to make smart goals...

//TODO I should be able to add a list of songs, rather than one song with the live data

//TODO HIGHEST PRIORITY
//TODO FIX FOR ADDING MUTIPLE SONGS into the current queue, this is breaking by adding previous selected songs again...
//TODO FIX for Playlists / Creation, should already be mostly setup...
//TODO Add feature to start playing an entire album from specific point in an album
//TODO Add feature to mix songs in a playlist without repeating old songs before queue is up. [shuffle songs literally?]

//TODO HIGH PRIORITY TASKS
// Click to Play from Song List : I want the song to start playing off the rip if item clicked
// Playlist Capability : I need to use data store to implment the playlist functionality [Can I store mediaItems in database?]
// Current Queue Fragment : I want another fragment to swipe up from the bottom which will have the current queue [whats on deck...]

//TODO MEDIUM PRIORITY TASKS

//TODO LOW PRIORITY TASKS
// UI CHANGES ...
// Small Current Player Floating : When I start playing, I want a small screen on the bottom of the screen to show currently playing music [this might be difficult]
// Orientation Change: Stay on Music Chooser Fragment, currently I'm being sent back to must player fragment

//TODO I want to animate sliding up and down from library to player, I also want to implement gesture detector for this...


class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val permissionManager = AppPermissionUtil()
    private lateinit var navController: NavController

    val TAG = MainActivity::class.java.simpleName

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("handleOnBackPressed: BACK PRESSED!")

            if(!navController.popBackStack()) {
                finish()
            }
        }
    }

    lateinit var gesture: GestureDetector

    val detector = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d(TAG, "onDoubleTap: ")
            return super.onDoubleTap(e)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            Log.d(TAG,"onFling: ")

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //Log.d(TAG, "onTouchEvent: ")
//        event?.let {
//            gesture.onTouchEvent(event)
//        }
        return super.onTouchEvent(event)
    }



    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate: ")

        gesture = GestureDetector(this, detector)

        // Setup view binding in the project
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.isAudioPermissionGranted.observe(this) { isGranted ->
            if(!isGranted) {
                Timber.d("onCreate: isGranted=$isGranted")
                permissionManager.requestReadMediaAudioPermission(this)
            } else {
                viewModel.initializeMusicPlaying()
            }
        }

        viewModel.availablePlaylists.observe(this) { playlists ->
            Timber.d("AllPlaylistLiveData: playlists has updated size=${playlists.size}  ")
            if(playlists.isNotEmpty()) {
                for(playlist in playlists) {
                    Timber.d("AllPlaylistLiveData: playlist.id=${playlist.uid} playlist.title=${playlist.title}, songs=${playlist.songs}")
                }
            }
        }

        viewModel.isRootAvailable.observe(this) {isAvailable ->
            //The root is available, I can now check albums and stuff
            if(isAvailable) {
                //TODO what should I do here?
                //query available albums
                viewModel.queryAvailableAlbums()
                //TODO query available playlists...
            }
        }

        viewModel.screenState.observe(this) {data ->
            Timber.d("onCreate: observe screenState data.route=${data.currentScreen.route()}")

            navController.navigate(data.currentScreen.route())
        }

        setupNavigation()

        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun setupNavigation() {
        //Retrieve the NavController [will instantiate the fragment before grabbing it's navController]
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController

        // Add navigation graph to the NavController
        navController.graph = navController.createGraph(
            startDestination = ScreenType.MUSIC_PLAYING_SCREEN.route()
        ) {
            //associate each destination with one of the route constants.
            fragment<MusicPlayingFragment>(ScreenType.MUSIC_PLAYING_SCREEN.route()) {
                label = "Player"
            }
            fragment<MusicChooserFragment>(ScreenType.MUSIC_CHOOSER_SCREEN.route()) {
                label = "Choose Music!"
            }
            fragment<PermissionDeniedFragment>(ScreenType.PERMISSION_DENIED_SCREEN.route()) {
                label = "Permission Denied"
            }
            fragment<CurrentQueueFragment>(ScreenType.MUSIC_QUEUE_SCREEN.route()) {
                label = "Music Queue"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume: ")

        UtilImpl.hideNavigationUI(window)

        viewModel.checkPermissionsIfOnPermissionDeniedScreen()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.d("onRequestPermissionsResult: requestCode=$requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.handlePermissionResult(requestCode, permissions, grantResults)
    }
}
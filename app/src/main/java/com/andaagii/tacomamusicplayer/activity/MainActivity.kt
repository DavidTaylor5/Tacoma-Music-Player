package com.andaagii.tacomamusicplayer.activity

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.andaagii.tacomamusicplayer.databinding.ActivityMainBinding
import com.andaagii.tacomamusicplayer.enum.ScreenType
import com.andaagii.tacomamusicplayer.fragment.CurrentQueueFragment
import com.andaagii.tacomamusicplayer.fragment.MusicChooserFragment
import com.andaagii.tacomamusicplayer.fragment.MusicPlayingFragment
import com.andaagii.tacomamusicplayer.fragment.PermissionDeniedFragment
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

//Preferences DataStore, for storing settings in my app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val permissionManager = AppPermissionUtil()
    private lateinit var navController: NavController

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("handleOnBackPressed: BACK PRESSED!")
            if(!navController.popBackStack()) {
                finish()
            }
        }
    }

    private lateinit var gesture: GestureDetector

    private val detector = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Timber.d("onDoubleTap: ")
            return super.onDoubleTap(e)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            Timber.d("onFling: ")
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate: ")

        gesture = GestureDetector(this, detector)

        // Setup view binding in the project
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.notifyHideKeyboard.observe(this) { _ ->
            removeVirtualKeyboard()
        }

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
                    Timber.d("AllPlaylistLiveData: playlist.title=${playlist.title}, songs=${playlist.songs}")
                }
            }
        }

        viewModel.isRootAvailable.observe(this) { isAvailable ->
            //The root is available, I can now check albums and stuff
            if(isAvailable) {
                //query available albums
                viewModel.queryAvailableAlbums()
            }
        }

        viewModel.showLoadingScreen.observe(this) { showLoadingScreen ->
            if(showLoadingScreen) {
                binding.loadingScreen.visibility = View.VISIBLE
            } else {
                binding.loadingScreen.visibility = View.INVISIBLE
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

    override fun onPause() {
        super.onPause()
        viewModel.saveQueue()
    }

    private fun removeVirtualKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.rootView.windowToken, 0)
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
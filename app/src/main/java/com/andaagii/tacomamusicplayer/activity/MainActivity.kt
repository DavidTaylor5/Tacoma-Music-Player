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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.andaagii.tacomamusicplayer.databinding.ActivityMainBinding
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.fragment.PermissionDeniedFragment
import com.andaagii.tacomamusicplayer.fragment.PlayerDisplayFragment
import com.andaagii.tacomamusicplayer.observer.MusicContentObserver
import com.andaagii.tacomamusicplayer.util.AppPermissionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.worker.CatalogMusicWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

//Preferences DataStore, for storing settings in my app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val permissionManager = AppPermissionUtil()
    private lateinit var navController: NavController
    private lateinit var workManager: WorkManager
    private lateinit var currentWorkerId: UUID
    private var musicObserver: MusicContentObserver? = null

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

                // Music Access is allowed, start looking through user library
                queryMusic()

                //TODO add back code for music content observer?
//                val handler = Handler(Looper.getMainLooper())
//                musicObserver = MusicContentObserver(
//                    handler = handler,
//                    context = this,
//                    onContentChange = viewModel::queryAvailableAlbums
//                )
//                contentResolver.registerContentObserver(
//                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                    true,
//                    musicObserver!!
//                )
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availablePlaylists.collect { playlists ->
                    Timber.d("availablePlaylists: playlists has updated size=${playlists.size}  ")
                    if(playlists.isNotEmpty()) {
                        for(playlist in playlists) {
                            Timber.d("availablePlaylists: playlist.title=${playlist.mediaMetadata.albumTitle}")
                        }
                    }
                }
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
            startDestination = ScreenType.MUSIC_CHOOSER_SCREEN.route()
        ) {
            //associate each destination with one of the route constants.
//            fragment<MusicPlayingFragment>(ScreenType.MUSIC_PLAYING_SCREEN.route()) {
//                label = "Player"
//            }
            fragment<PlayerDisplayFragment>(ScreenType.MUSIC_CHOOSER_SCREEN.route()) {
                label = "Choose Music!"
            }
            fragment<PermissionDeniedFragment>(ScreenType.PERMISSION_DENIED_SCREEN.route()) {
                label = "Permission Denied"
            }
//            fragment<CurrentQueueFragment>(ScreenType.MUSIC_QUEUE_SCREEN.route()) {
//                label = "Music Queue"
//            }
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
        //Saves the current song list in queue
        viewModel.saveQueue()
        //Saves the original song list order [in case the user has shuffled]
        viewModel.saveOriginalOrder()

        musicObserver?.let {
            contentResolver.unregisterContentObserver(it)
            Timber.d("onDestroy: unregistered, musicobserver")
        }
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

    /**
     * Start cataloging user's music library using a worker. If a previous worker is running, stop it
     * and start again. I don't want multiple workers running in parallel.
     */
    fun queryMusic() {
        //Catalog all of the music on the user's device to a database in the background
        //TODO I also need to run a workrequest everytime I observe a change in the MUSIC folder
        val catalogWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<CatalogMusicWorker>()
            .build()

        workManager = WorkManager.getInstance(this)
        currentWorkerId = catalogWorkRequest.id

        //TODO there is still another parallel execution that is happening, two workers are finishing at the same
        //time and I need to figure that out...

        //Cancel previous work
        workManager.cancelAllWork()

        //TODO BRING THIS BACK LATER...
        //workManager.enqueue(catalogWorkRequest)
    }
}
package com.example.tacomamusicplayer.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.example.tacomamusicplayer.databinding.ActivityMainBinding
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.fragment.MusicChooserFragment
import com.example.tacomamusicplayer.fragment.MusicPlayingFragment
import com.example.tacomamusicplayer.fragment.PermissionDeniedFragment
import com.example.tacomamusicplayer.util.AppPermissionUtil
import com.example.tacomamusicplayer.util.UtilImpl
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

//TODO I need to make smart goals...

//TODO when I click an album I want the songListFragment to populate
//TODO When I click a songlist, I want the song to start playing...
//TODO When I start playing, I want a small screen on the bottom of the screen to show currently playing music
//TODO I need to use data store to implment the playlist functionality [Can I store mediaItems in database?]
//TODO I need to redo the main music playing screen, I also want to be able to scroll through the music playing screen

//TODO the app crashes when I rotate it [viewmodel recreation issues...]
/*
* TODO It appears I need to refactor my fragment logic based on this,
*  Every fragment must have an empty constructor, so it can be instantiated when restoring its activity's
* state. It is strongly recommended that subclasses do not have other constructors with paramters, since
* these constructors will not be called when the fragment is re-instantiated; instead, arguments can
* be supplied by the caller with setArguments(Bundle) and later retrieved by the Fragment with
* getArguments().
*
* Applications should generally not implement a constructor. Prefer onAttach(android.content.Context)
* instead. It is the first place application code can run where the fragment is ready to be used -
* the point where the fragment is actually associated with its context. Some applications may also
* want to implement onInflate(Activity, AttributeSet, Bundle) to retrieve attributes from a layout
* resource, although note this happens when the fragment is attached.
*
* */

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

    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate: ")

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
            //TODO add all other fragments
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
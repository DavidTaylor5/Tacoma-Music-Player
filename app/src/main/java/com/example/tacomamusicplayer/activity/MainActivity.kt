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

//TODO I need to solve permissions structure in new app
//TODO I need start music service after permissions are implemented
//TODO make the main activity just hold a framecontainer and swap around fragments...
//TODO Create music playing screen as a fragment
//TODO hook up UI to controller
//TODO hook up UI to browser
//TODO setup music service so that I can browse albums
//TODO get music data from a specific album
//TODO figure out how to send information when I'm navigating between fragments!

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
                viewModel.initalizeMusicPlaying()
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
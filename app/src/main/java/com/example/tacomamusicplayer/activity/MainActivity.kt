package com.example.tacomamusicplayer.activity

import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.ActivityMainBinding
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

        viewModel.requestReadMediaAudioPermission.observe(this) {requestPermission ->
            if(requestPermission) {
                permissionManager.requestReadMediaAudioPermission(this)
                viewModel.handledRequestForReadMediaAudioPermission()
            }
        }

        //Retreive the NavController [will instantiate the fragment before grabbing it's navController]
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController

        // Add navigation graph to the NavController
        navController.graph = navController.createGraph(
            startDestination = "player"
        ) {
            //associate each destination with one of the route constants.
            fragment<MusicPlayingFragment>("player") {
                label = "Player"
            }
            fragment<MusicChooserFragment>("chooser") {
                label = "Choose Music!"
            }
            fragment<PermissionDeniedFragment>("permission") {
                label = "Permission Denied"
            }
            //TODO add all other fragments
        }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)
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
        Timber.d("onRequestPermissionsResult: requestCode=$requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.handlePermissionResult(requestCode, permissions, grantResults)
    }
}
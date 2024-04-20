package com.example.tacomamusicplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.example.tacomamusicplayer.databinding.ActivityMainBinding
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

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val permissionManager = AppPermissionUtil()

    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate: ")

        // Setup view binding in the project
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.mediaController.observe(this) { controller ->
            binding.playerView.player = controller
            binding.playerView.showController()
        }

        viewModel.requestReadMediaAudioPermission.observe(this) {requestPermission ->
            if(requestPermission) {
                permissionManager.requestReadMediaAudioPermission(this)
                viewModel.handledRequestForReadMediaAudioPermission()
            }
        }

        //Main activity will tell viewmodel when it is time to initalize music player
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
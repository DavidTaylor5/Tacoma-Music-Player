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
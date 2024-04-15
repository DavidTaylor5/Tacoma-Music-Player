package com.example.tacomamusicplayer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController

class MainViewModel: ViewModel() {

    private var mediaController: MediaController? = null
    private var mediaBrowser: MediaBrowser? = null
    private var rootMediaItem: MediaItem? = null
    private var albumMediaItemList: List<MediaItem>? = null
    private var currentAlbumsSongMediaItemList: List<MediaItem>? = null

/*    TODO I HAVE TO MOVE ALL OF THE BROWSING AND CONTROLLING TO THE VIEWMODEL */

    val TAG = MainViewModel::class.java.simpleName

    init {
        Log.d(TAG, "init: ")
    }

    fun doSomething() {
        Log.d(TAG, "doSomething: ")
    }



}
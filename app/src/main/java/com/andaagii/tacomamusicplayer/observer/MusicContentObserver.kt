package com.andaagii.tacomamusicplayer.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import timber.log.Timber

class MusicContentObserver(
    handler: Handler,
    val context: Context,
    val onContentChange: () -> Unit
    ): ContentObserver(handler) {

    private val delayCheckHandler = Handler(Looper.getMainLooper())
    private var currentlyLoadingSongs: Boolean = false

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Timber.d("onChange: selfChange=$selfChange, uri=$uri")

        if(!currentlyLoadingSongs) {
            currentlyLoadingSongs = true
            Toast.makeText(context, "Updating Album List...", Toast.LENGTH_SHORT).show()

            delayCheckHandler.postDelayed({
                //TODO only update album list if new album list != old album list
                //query new albums
                onContentChange()

                currentlyLoadingSongs = false
            }, 30_000)
        }
    }
}
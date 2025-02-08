package com.example.tacomamusicplayer.util

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import timber.log.Timber
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class UtilImpl {

    companion object {

        /**
         * Translates a given millisecond value into minutes and seconds.
         * @param msDuration Length in milliseconds.
         */
        fun calculateHumanReadableTimeFromMilliseconds(msDuration: Long): String {
            //duration object from given milliseconds
            val duration = msDuration.toDuration(DurationUnit.MILLISECONDS)
            //calculate in whole minutes
            val minutes = duration.inWholeMinutes
            //subtract whole minutes from original milliseconds to get remaining whole seconds.
            val seconds = duration.minus(minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds
            //Return formatted string

            return if(seconds < 10) "$minutes:0$seconds" else "$minutes:$seconds"
        }

        /**
         * Call this function onResume(). Removes the navigation bar from the bottom of the screen.
         */
        fun hideNavigationUI(window: Window) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.navigationBars())
            } else {
                val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

                window.decorView.systemUiVisibility = flags
            }
        }

        /**
         * Call this function to draw a Uri onto an ImageView, return true if drawn without exception.
         */
        fun drawUriOntoImageView(view: ImageView, uri: Uri, size: Size): Boolean {
            Timber.d("drawUriOntoImageView: view=$view, uri=$uri, size=$size")
            val resolver = view.context.contentResolver
            try {
                //Album art as a bitmap, I need to work on what to do when this is blank / null?
                val albumArt = resolver.loadThumbnail(uri, size, null)
                val albumDrawable = BitmapDrawable(view.context.resources, albumArt)

                view.setImageDrawable(albumDrawable)

                Timber.d("drawUriOntoImageView: SUCCESSFUL! Uri is placed on View!")
                return true
            } catch (e: Exception) {
                Timber.d("drawUriOntoImageView: ERROR ON adding URI to VIEW e=$e")
                return false
            }
        }

        /**
         *  A function to grab the current songs in a media controller.
         */
        fun getSongListFromMediaController(controller: MediaController): MutableList<MediaItem> {
            val controllerSongLength = controller.mediaItemCount
            val songList = mutableListOf<MediaItem>()
            for(i in 0..<controllerSongLength) {
                songList.add(controller.getMediaItemAt(i))
            }
            return songList
        }
    }
}
package com.example.tacomamusicplayer.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
         *  Function to return all current mediaItems inside of the MediaController.
         *  @return List of songs currently in the MediaController.
         */
        fun getSongListFromMediaController(controller: MediaController): MutableList<MediaItem> {
            val controllerSongLength = controller.mediaItemCount
            val songList = mutableListOf<MediaItem>()
            for(i in 0..<controllerSongLength) {
                songList.add(controller.getMediaItemAt(i))
            }
            return songList
        }

        /**
         * Save Image to File in app specific storage, taken from Phind.
         */
        fun saveImageToFile(context: Context, sourceUri: Uri) {
            try {
                // Get app-specific directory
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

                appDir?.let { directory ->
                    val fileName = "${System.currentTimeMillis()}.jpg"
                    val destination = File(directory, fileName)

                    // Copy file
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        FileOutputStream(destination).use { outputStream ->
                            inputStream.copyTo(outputStream, bufferSize = 8192)
                        }
                    }
                }
            } catch (e: IOException) {
                Timber.d("saveImageToFile: Error copying file")
            }
        }
    }
}
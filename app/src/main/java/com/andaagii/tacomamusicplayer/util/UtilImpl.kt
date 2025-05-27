package com.andaagii.tacomamusicplayer.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import coil.load
import com.andaagii.tacomamusicplayer.R
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

        fun drawImageAssociatedWithAlbum(view: ImageView, uri: Uri, imageSize: Size, customImageName: String = "", ) {
            Timber.d("drawImageAssociatedWithAlbum: view=$view, uri=$uri, customImageName=$customImageName")
            view.setImageURI(null)

            //Determine if there is a custom Album image, associated with albums and its songs
            val possibleImageSuffix = listOf(".jpg", ".png") //TODO add other options (?) gifs at some point?
            val appDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            var usingCustomImage = false
            for(suffix in possibleImageSuffix) {
                val customAlbumImage = File(appDir, "${customImageName}$suffix")
                if(customAlbumImage.exists()) {
                    Timber.d("drawImageAssociatedWithAlbum: customAlbumImage=$customAlbumImage exists, setting image...")
                    try {
                        val artUri = Uri.fromFile(customAlbumImage)
                        view.load(artUri) {
                            crossfade(true)
                            size(imageSize.width, imageSize.height)
                            error(R.drawable.white_note)
                            fallback(R.drawable.white_note)
                        }
                        usingCustomImage = true
                    } catch(e: Exception) {
                        Timber.d("onBindViewHolder: exception when setting playlist art customAlbumImage=$customAlbumImage e=$e")
                    }
                    break
                }
            }

            if(!usingCustomImage) { //No custom image found, use metadata album art uri
                drawUriOntoImageViewCoil(view, uri, imageSize)
            }
        }

        /**
         * TODO why does this function only work with the album uris?
         */
        fun drawUriOntoImageViewCoil(view: ImageView, uri: Uri, imageSize: Size): Boolean {
            Timber.d("drawUriOntoImageViewCoil: view=$view, uri=$uri, size=$imageSize")
            val resolver = view.context.contentResolver
            try {
                //Album art as a bitmap, I need to work on what to do when this is blank / null?

                view.load(uri) {
                    crossfade(true)
                    size(imageSize.width, imageSize.height)
                    error(R.drawable.white_note)
                    fallback(R.drawable.white_note)
                }

                Timber.d("drawUriOntoImageView: SUCCESSFUL! Uri is placed on View!")
                return true
            } catch (e: Exception) {
                Timber.d("drawUriOntoImageView: ERROR ON adding URI to VIEW e=$e")
                return false
            }
        }

        /**
         * Call this function to draw a Uri onto an ImageView, return true if drawn without exception.
         */
        fun drawUriOntoImageView(view: ImageView, uri: Uri, imageSize: Size): Boolean {
            Timber.d("drawUriOntoImageView: view=$view, uri=$uri, size=$imageSize")
            val resolver = view.context.contentResolver
            try {
                //Album art as a bitmap, I need to work on what to do when this is blank / null?
                val albumArt = resolver.loadThumbnail(uri, imageSize, null)
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

        fun deletePicture(context: Context, fileName: String): Boolean {
            return try {
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File(appDir, fileName)
                if(file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch(e: Exception) {
                Timber.d("deleteFile: e=$e")
                return false
            }
        }

        /**
         * Save Image to File in app specific storage, taken from Phind.
         */
        fun saveImageToFile(context: Context, sourceUri: Uri, fileName: String) {
            try {
                // Get app-specific directory
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

                appDir?.let { directory ->
                    val fileName = "$fileName.jpg"
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


        /**
         * I store images in the app-specific storaage, this function will take a f
         * @return Will return true if there was a playlist image found else false.
         */
        fun setPlaylistImageFromAppStorage(view: ImageView, playlistTitle: String): Boolean {
            Timber.d("setPlaylistImageFromAppStorage: playlistTitle=$playlistTitle")
            val playlistFile = "$playlistTitle.jpg"

            if(playlistFile.isNotEmpty()) {
                val appDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val imageFile = File(appDir, playlistFile)
                if(imageFile.exists()) {
                    try {
                        val artUri = Uri.fromFile(imageFile)
                        view.setImageURI(artUri)
                        return true
                    } catch(e: Exception) {
                        Timber.d("onBindViewHolder: exception when setting playlist art e=$e")
                    }
                }
            }

            return false
        }

        /**
         * Renames the playlistImage associated with the playlist.
         * @param oldPlaylistName The oldPlaylistName associated with playlist image
         * @param newPlaylistName The new name for the playlist image
         */
        fun renamePlaylistImageFile(context: Context, oldPlaylistName: String, newPlaylistName: String) {
            Timber.d("renamePlaylistImageFile: ")
            val playlistFileName = "$oldPlaylistName.jpg"
            val newPlaylistFileName = "$newPlaylistName.jpg"

            if(playlistFileName.isNotEmpty()) {
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

                val currentImageFile = File(appDir, playlistFileName)
                val updatedNameFile = File(appDir, newPlaylistFileName)

                if(currentImageFile.exists()) {
                    currentImageFile.renameTo(updatedNameFile)
                }
            }
        }
    }
}
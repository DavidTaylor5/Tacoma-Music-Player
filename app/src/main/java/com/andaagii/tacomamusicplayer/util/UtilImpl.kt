package com.andaagii.tacomamusicplayer.util

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.widget.ImageView
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import coil.load
import com.andaagii.tacomamusicplayer.R
import com.mpatric.mp3agic.Mp3File
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.floor
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import androidx.core.graphics.drawable.toDrawable
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.ArtInfo
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

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

        private fun drawMp3agicBitmap(view: ImageView, uri: Uri, imageSize: Size): Boolean {
            Timber.d("drawMp3agicBitmap: uri=$uri")
            val fixUrl = Uri.fromFile(File("/storage/emulated/0/Music/Clipse/let-god-sort-em-out/11-so-far-ahead-(pharrell-williams).mp3"))
            val file = UtilImpl.uriToFile(view.context, fixUrl)
            val mp3File = Mp3File(file)

            if(mp3File.hasId3v2Tag()) {
                val tag = mp3File.id3v2Tag
                val imageData = tag.albumImage

                val albumArt = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                view.load(albumArt) {
                    crossfade(true)
                    size(imageSize.width, imageSize.height)
                    error(R.drawable.white_note)
                    fallback(R.drawable.white_note)
                }


                //view.setImageBitmap(albumArt)
                return true
            }

            return false
        }

        /**
         * TODO why does this function only work with the album uris?
         */
        private fun drawUriOntoImageViewCoil(view: ImageView, uri: Uri, imageSize: Size): Boolean {
            Timber.d("drawUriOntoImageViewCoil: view=$view, uri=$uri, size=$imageSize")
            try {
                //Album art as a bitmap, I need to work on what to do when this is blank / null?

                view.load(uri) {
                    crossfade(true)
                    size(imageSize.width, imageSize.height)
                    error(R.drawable.white_note)
                    fallback(R.drawable.white_note)
//                    listener( //TODO I want to save the bitmaps based on ID tags to custom storage to reference instead?...
//                        onError = { request, throwable ->
//                            drawMp3agicBitmap(view, uri, imageSize)
//                        }
//                    )
                }

                Timber.d("drawUriOntoImageView: SUCCESSFUL! Uri is placed on View!")
                return true
            } catch (e: Exception) {
                Timber.d("drawUriOntoImageView: ERROR ON adding URI to VIEW e=$e")
                return false
            }
        }

        fun drawMediaItemArt(view: ImageView, uri: Uri, imageSize: Size, customAlbumImageName: String, synchronous: Boolean = false) {
            Timber.d("drawSongArt: uri=$uri, imageSize=$imageSize, customAlbumImageName=$customAlbumImageName")
            view.setImageURI(null)

            // Try to draw custom image
            val usingCustomImage = loadCustomImage(view, uri, imageSize, customAlbumImageName, synchronous)
            if(usingCustomImage) {
                return
            }

            // Try to draw based on URI
            val drewURI = drawUriOntoImageViewCoil(view, uri, imageSize)
            if(drewURI) {
                return
            }

            // Try to draw based on ID3v2 tag
            val drewMp3agic = drawMp3agicBitmap(view, uri, imageSize)
            if(drewMp3agic) {
                return
            }

            // Draw default
            drawDefault(view)
        }

        fun getArtInfoFromSongGroupEntity(songGroupEntity: SongGroupEntity): ArtInfo {
            return ArtInfo(
                artFileOriginal = songGroupEntity.artFileOriginal,
                artFileCustom = songGroupEntity.artFileCustom,
                useCustomArt = songGroupEntity.useCustomArt
            )
        }

//        fun catalogAlbumArtFromMediaStoreUri(
//            context: Context,
//            uri: Uri,
//            fileName: String,
//            fileFolder: String
//        ): Uri {
//            val imageSaved = saveImageFromMediaStoreUri(context, uri, fileName)
//            if(imageSaved) {
//                Timber.d("catalogAlbumArtFromMediaStoreUri: fileName=$fileName is successfully saved!")
//            } else {
//                Timber.d("catalogAlbumArtFromMediaStoreUri: Unable to save fileName=$fileName")
//            }
//            return getFileProviderUri(context, fileName, fileFolder)
//        }

        fun getFileProviderUri(
            context: Context,
            fileName: String,
        ): Uri {
            var contentUri = Uri.EMPTY
            val imageFile = File(fileName)
            try {
                contentUri = getUriForFile(
                    context,
                    "com.andaagii.tacomamusicplayer",
                    imageFile
                )

                //Then grant permission so that Android Auto can read the URI
                context.grantUriPermission(
                    "com.google.android.projection.gearhead", // phone Auto app
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                context.grantUriPermission(
                    "com.google.android.gms", // car side services
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

            } catch (e: Exception) {
                Timber.d("catalogAlbums: Error generating content URI e=$e")
            }

            return contentUri
        }

        //TODO another file to add the file provider content uri
        /**
         * When I query the songs from mediaStore, I save the album art as external app storage files.
         * This allows me to create secure URIs using FileProvider which I can use to send images to
         * Android Auto.
         * @return The true if file is stored in app external storage, false if unable to save file.
         */
        fun saveImageFromMediaStoreUri(
            context: Context,
            uri: Uri,
            fileName: String
        ): String {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val bytes = retriever.embeddedPicture
            if(bytes != null) {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val dir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
                    val destFile = File(dir, "$fileName.jpg")
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(destFile))

                    return destFile.path
                } catch(e: Exception) {
                    Timber.e("saveImageFromMediaStoreUri: Error saving image e=$e")
                }
            } else {
                try {
                    val fixUrl = Uri.fromFile(File(uri.path.toString()))
                    val file = uriToFile(context, fixUrl)
                    val mp3File = Mp3File(file)

                    if(mp3File.hasId3v2Tag()) {
                        val tag = mp3File.id3v2Tag
                        val imageData = tag.albumImage

                        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                        val dir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
                        val destFile = File(dir, "$fileName.jpg")
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(destFile))

                        return destFile.path
                    }
                } catch (e: Exception) {
                    Timber.d("saveImageFromMediaStoreUri: failure on mp3agic file check on path=${uri.path.toString()}, e=$e")
                }
            }
            return ""
        }

        private fun loadCustomImage(view: ImageView, uri: Uri, imageSize: Size, customAlbumImageName: String, synchronous: Boolean = false): Boolean {
            val possibleImageSuffix = listOf(".jpg", ".png") //TODO add other options (?) gifs at some point?
            val appDir = view.context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
            for(suffix in possibleImageSuffix) {
                val customAlbumImage = File(appDir, "${customAlbumImageName}$suffix")
                if(customAlbumImage.exists()) {
                    Timber.d("drawImageAssociatedWithAlbum: customAlbumImage=$customAlbumImage exists, setting image...")
                    try {
                        val artUri = Uri.fromFile(customAlbumImage)

                        if(synchronous) {
                            view.setImageURI(artUri)
                            return true
                        } else {
                            view.load(artUri) {
                                crossfade(true)
                                size(imageSize.width, imageSize.height)
                                error(R.drawable.white_note)
                                fallback(R.drawable.white_note)
                            }
                            return true
                        }
                    } catch(e: Exception) {
                        Timber.d("onBindViewHolder: exception when setting playlist art customAlbumImage=$customAlbumImage e=$e")
                        return false
                    }
                }
            }

            return false
        }

        private fun drawDefault(view: ImageView) {
            val defaultArt = ResourcesCompat.getDrawable(view.resources, R.drawable.white_note, null)
            view.load(defaultArt)
        }

        /**
         * Call this function to draw a Uri onto an ImageView, return true if drawn without exception.
         */
        private fun drawUriOntoImageView(view: ImageView, uri: Uri, imageSize: Size, synchronous: Boolean = false): Boolean {
            Timber.d("drawUriOntoImageView: view=$view, uri=$uri, size=$imageSize")
            val resolver = view.context.contentResolver
            try {
                //Album art as a bitmap, I need to work on what to do when this is blank / null?
                val albumArt = resolver.loadThumbnail(uri, imageSize, null)
                val albumDrawable = albumArt.toDrawable(view.context.resources)

                if(synchronous) {
                    view.setImageDrawable(albumDrawable)
                } else {
                    view.load(albumDrawable) {
                        crossfade(true)
                        size(imageSize.width, imageSize.height)
                        error(R.drawable.white_note)
                        fallback(R.drawable.white_note)
                    }
                }
                Timber.d("drawUriOntoImageView: SUCCESSFUL! Uri is placed on View!")
                return true
            } catch (e: Exception) {
                Timber.d("drawUriOntoImageView: ERROR ON adding URI to VIEW [setting default] e=$e")
                return false
            }
        }

        private fun uriToFile(context: Context, uri: Uri): File? {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("temp_audio", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            return tempFile
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

        fun determineGridSize(): Int {
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val dp = displayMetrics.density
            val widthDp = screenWidth / dp

            //The album/playlist card is a set 150dp width and I factor in some padding
            val widthInGrids = widthDp / 170
            val maxGridSize = floor(widthInGrids).toInt()

            Timber.d("updateAlbumLayout: screenWidth=$screenWidth, screenHeight=${displayMetrics.heightPixels}, dp=${dp}, widthDp=$widthDp, widthInGrids=$widthInGrids, maxGridSize=$maxGridSize")
            return maxGridSize
        }

        fun deletePicture(context: Context, fileName: String): Boolean {
            return try {
                val appDir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
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
         * Get's the image name of album art which is stored in external app storage
         */
        fun getImageBaseNameFromExternalStorage(groupTitle: String, artist: String, songGroupType: SongGroupType): String {
            return when(songGroupType) {
                SongGroupType.ALBUM -> {
                    sanitizeFileName(if(artist.isEmpty()) "album_$groupTitle" else "album_${groupTitle}_$artist")
                }
                SongGroupType.PLAYLIST -> {
                    sanitizeFileName("playlist_$groupTitle")
                }
                else -> "UNKNOWN FILE NAME"
            }
        }

        /**
         * Because I'm going to store an album art's file on external app storage, I need to make
         * sure that either album or artist doesn't contain an illegal character.
         */
        private fun sanitizeFileName(fileName: String): String {
            return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        }

        /**
         * Grab file from BaseName.
         * @param dir The directory to be checked.
         * @param baseName The base name (no extension) for an image.
         * @return The file which has a baseName in the search directory.
         */
        fun findImageByBaseName(dir: File, baseName: String): File? {
            if (!dir.exists() || !dir.isDirectory) return null

            val supportedExtensions = listOf("png", "jpg", "jpeg", "webp")

            return dir.listFiles()?.firstOrNull { file ->
                val name = file.nameWithoutExtension
                val ext = file.extension.lowercase()

                name == baseName && ext in supportedExtensions
            }
        }

        /**
         * Save Image to File in app specific storage, taken from Phind.
         */
        fun saveImageToFile(context: Context, sourceUri: Uri, fileName: String, isCustom: Boolean): String {
            try {
                // Get app-specific directory
                val appDir = context.getExternalFilesDir(
                    if(isCustom) Const.ALBUM_ART_CUSTOM_FOLDER else Const.ALBUM_ART_FOLDER
                )

                appDir?.let { directory ->

                    if(!directory.exists()) {
                        directory.mkdirs()
                    }

                    val fileName = "${fileName}.jpg"
                    val destination = File(directory, fileName)

                    // Copy file
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        var bitmap = BitmapFactory.decodeStream(inputStream)
                        if(bitmap.width >= 700 || bitmap.height >= 700) {
                            bitmap = cropCenter(bitmap)
                        }
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(destination))
                    }

                    return destination.toString()
                }
            } catch (e: IOException) {
                Timber.d("saveImageToFile: Error copying file e=$e")
            } catch (e: Exception) {
                Timber.d("saveImageToFile: e=$e") //TODO Error on adding some images as playlist covers...
            }

            return "UNKNOWN FILE"
        }

        fun cropCenter(bitmap: Bitmap, cropSize: Int = 700): Bitmap {
            require(bitmap.width >= cropSize && bitmap.height >= cropSize) {
                "Bitmap must be at least $cropSize x $cropSize"
            }

            val left = (bitmap.width - cropSize) / 2
            val top = (bitmap.height - cropSize) / 2

            return Bitmap.createBitmap(
                bitmap,
                left,
                top,
                cropSize,
                cropSize
            )
        }


        /**
         * I store images in the app-specific storaage, this function will take a f
         * @return Will return true if there was a playlist image found else false.
         */
        fun setPlaylistImageFromAppStorage(view: ImageView, playlistTitle: String): Boolean {
            Timber.d("setPlaylistImageFromAppStorage: playlistTitle=$playlistTitle")
            val playlistFile = "$playlistTitle.jpg"

            if(playlistFile.isNotEmpty()) {
                val appDir = view.context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
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
                val appDir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)

                val currentImageFile = File(appDir, playlistFileName)
                val updatedNameFile = File(appDir, newPlaylistFileName)

                if(currentImageFile.exists()) {
                    currentImageFile.renameTo(updatedNameFile)
                }
            }
        }
    }
}
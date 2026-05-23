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
import androidx.activity.result.ActivityResultLauncher
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
import androidx.core.net.toUri
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.ArtInfo
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.yalantis.ucrop.UCrop

/**
 * Miscellaneous static utilities in a `companion object`.
 *
 * Covers:
 * - Duration formatting ([calculateHumanReadableTimeFromMilliseconds])
 * - System UI helpers ([hideNavigationUI])
 * - Artwork loading with a waterfall fallback strategy ([drawMediaItemArt])
 * - FileProvider URI generation for Android Auto ([getFileProviderUri])
 * - MediaStore image extraction and caching ([saveImageFromMediaStoreUri])
 * - App external-storage helpers ([loadCustomImage], [findImageByBaseName], [getSaveFileUri])
 * - Bitmap utilities ([saveImageToFile], [cropCenter])
 * - MediaController queue helpers ([getSongListFromMediaController])
 * - Grid column calculation ([determineGridSize])
 */
class UtilImpl {

    companion object {

        /**
         * Converts a millisecond duration into a human-readable `"m:ss"` string.
         *
         * Whole minutes are extracted first, then the remaining seconds are computed by
         * subtracting the full-minute duration from the original. Seconds below 10 are
         * zero-padded so the output is always `"m:0s"` rather than `"m:s"` (e.g., `"3:07"`
         * instead of `"3:7"`).
         *
         * @param msDuration The track length in milliseconds.
         * @return A formatted string such as `"3:45"` or `"1:07"`.
         */
        fun calculateHumanReadableTimeFromMilliseconds(msDuration: Long): String {
            val duration = msDuration.toDuration(DurationUnit.MILLISECONDS)
            val minutes = duration.inWholeMinutes
            // Subtract whole minutes to isolate the remaining seconds portion.
            val seconds = duration.minus(minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds
            return if (seconds < 10) "$minutes:0$seconds" else "$minutes:$seconds"
        }

        /**
         * Hides the system navigation bar from the given [window].
         *
         * On API ≥ 30, uses [android.view.WindowInsetsController] (the current API).
         * On older devices, falls back to the deprecated `systemUiVisibility` flags with
         * `IMMERSIVE_STICKY` so the bar auto-hides after user swipes rather than locking
         * the UI. Called from `onResume` to re-apply immersive mode after the bar reappears.
         *
         * @param window The activity window whose navigation bar should be hidden.
         */
        fun hideNavigationUI(window: Window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
         * Investigative function that attempts to load album art from an ID3v2 tag via mp3agic.
         *
         * **Note:** The URI is hardcoded to a specific debug file and this function is not
         * called in the normal art-loading path. It is retained as a reference implementation
         * for the mp3agic ID3v2 art extraction technique used by [saveImageFromMediaStoreUri].
         *
         * @param view The [ImageView] to load the bitmap onto.
         * @param uri Unused — the function always reads from the hardcoded debug file path.
         * @param imageSize The target width/height in pixels passed to Coil.
         * @return `true` if an ID3v2 image was found and loaded; `false` otherwise.
         */
        fun drawMp3agicBitmap(view: ImageView, uri: Uri, imageSize: Size): Boolean {
            Timber.d("drawMp3agicBitmap: uri=$uri")
            val fixUrl = Uri.fromFile(File("/storage/emulated/0/Music/Clipse/let-god-sort-em-out/11-so-far-ahead-(pharrell-williams).mp3"))
            val file = UtilImpl.uriToFile(view.context, fixUrl)
            val mp3File = Mp3File(file)

            if (mp3File.hasId3v2Tag()) {
                val tag = mp3File.id3v2Tag
                val imageData = tag.albumImage

                val albumArt = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                view.load(albumArt) {
                    crossfade(true)
                    size(imageSize.width, imageSize.height)
                    error(R.drawable.white_note)
                    fallback(R.drawable.white_note)
                }

                return true
            }

            return false
        }

        /**
         * Loads artwork from a `content://` URI onto [view] using Coil.
         *
         * Only reliably works with album-art `content://` URIs produced by MediaStore (e.g.,
         * `content://media/external/audio/albumart/<id>`). Plain `file://` URIs are not
         * supported here — use [loadCustomImage] for file-based art or [drawUriOntoImageView]
         * for MediaStore thumbnail URIs.
         *
         * @param view The [ImageView] target.
         * @param uri The `content://` URI of the album artwork.
         * @param imageSize The target dimensions in pixels passed to Coil.
         * @return `true` if the load was initiated without an exception; `false` on error.
         */
        private fun drawUriOntoImageViewCoil(view: ImageView, uri: Uri, imageSize: Size): Boolean {
            Timber.d("drawUriOntoImageViewCoil: view=$view, uri=$uri, size=$imageSize")
            try {
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
         * Attempts to draw artwork onto [view] using a four-step waterfall fallback strategy:
         *
         * 1. **Custom image** — looks for a user-chosen `.jpg` or `.png` in app external storage
         *    under the name [customAlbumImageName] via [loadCustomImage].
         * 2. **URI via Coil** — attempts to load from [uri] using Coil (works for MediaStore
         *    `content://` album-art URIs).
         * 3. **ID3v2 tag via mp3agic** — attempts to read the embedded art from the audio file's
         *    ID3v2 tag (fallback for files where the retriever returns `null`).
         * 4. **Default placeholder** — sets the white-note drawable via [drawDefault].
         *
         * Each step returns early if it succeeds, so the chain stops at the first success.
         *
         * @param view The [ImageView] to draw the artwork onto.
         * @param uri The MediaStore or file URI used in steps 2 and 3.
         * @param imageSize The target dimensions in pixels passed to Coil.
         * @param customAlbumImageName The base file name (without extension) used in step 1.
         * @param synchronous When `true`, uses [ImageView.setImageURI] instead of Coil's
         *   async load. Required for callers that need the image set before the next draw pass
         *   (e.g., the mini-player in [PlayerDisplayFragment]).
         */
        fun drawMediaItemArt(view: ImageView, uri: Uri, imageSize: Size, customAlbumImageName: String, synchronous: Boolean = false) {
            Timber.d("drawSongArt: uri=$uri, imageSize=$imageSize, customAlbumImageName=$customAlbumImageName")
            view.setImageURI(null)

            // Step 1 — custom image from app external storage.
            val usingCustomImage = loadCustomImage(view, uri, imageSize, customAlbumImageName, synchronous)
            if (usingCustomImage) return

            // Step 2 — Coil from URI (works for MediaStore content:// album-art URIs).
            val drewURI = drawUriOntoImageViewCoil(view, uri, imageSize)
            if (drewURI) return

            // Step 3 — ID3v2 tag via mp3agic (fallback for files the retriever can't decode).
            val drewMp3agic = drawMp3agicBitmap(view, uri, imageSize)
            if (drewMp3agic) return

            // Step 4 — white-note placeholder.
            drawDefault(view)
        }

        /**
         * Extracts the three artwork fields from [songGroupEntity] into an [ArtInfo] value object.
         *
         * Consumers of [ArtInfo] (e.g., adapters) use it to decide which file path to load
         * and whether to prefer the custom or original art. See [ArtInfo] for field semantics.
         *
         * @param songGroupEntity The entity whose artwork state should be read.
         * @return An [ArtInfo] snapshot of the entity's artwork fields.
         */
        fun getArtInfoFromSongGroupEntity(songGroupEntity: SongGroupEntity): ArtInfo {
            return ArtInfo(
                artFileOriginal = songGroupEntity.artFileOriginal,
                artFileCustom = songGroupEntity.artFileCustom,
                useCustomArt = songGroupEntity.useCustomArt
            )
        }

        /**
         * Converts a file path to a `content://` FileProvider URI and grants read access to
         * both Android Auto packages.
         *
         * Android Auto runs in a separate process that cannot read `file://` URIs directly.
         * Two permission grants are issued — one for the phone-side Auto app
         * (`com.google.android.projection.gearhead`) and one for the car-side services
         * (`com.google.android.gms`) — so artwork is visible on both surfaces.
         *
         * @param context The application context used for the FileProvider lookup and URI permission grants.
         * @param fileName The absolute file path of the artwork file to share.
         * @return A `content://` FileProvider URI with read permission granted, or [Uri.EMPTY]
         *   if the file could not be resolved.
         */
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

                // Grant read permission to both Android Auto packages so artwork displays
                // on the phone Auto app and on the car's head unit simultaneously.
                context.grantUriPermission(
                    "com.google.android.projection.gearhead", // phone-side Auto app
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                context.grantUriPermission(
                    "com.google.android.gms", // car-side services
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

            } catch (e: Exception) {
                Timber.d("catalogAlbums: Error generating content URI e=$e")
            }

            return contentUri
        }

        /**
         * Extracts the embedded album artwork from the audio file at [uri] and saves it as a
         * JPEG to app external storage, returning the saved file's absolute path.
         *
         * Two-path extraction:
         * 1. **Primary** — [MediaMetadataRetriever.embeddedPicture] reads the embedded image
         *    byte array directly; this works for most well-tagged files.
         * 2. **Fallback** — if the retriever returns `null`, the file is copied to a temp
         *    location and parsed by mp3agic to read the ID3v2 tag's album image. This covers
         *    edge cases where the system retriever cannot decode the tag format.
         *
         * The saved JPEG is compressed at quality 90 and placed under [Const.ALBUM_ART_FOLDER]
         * in app external storage with the name `"<fileName>.jpg"`.
         *
         * @param context The application context used for file-system access.
         * @param uri The `content://` or `file://` URI of the audio track.
         * @param fileName The base name (without extension) for the saved JPEG.
         * @return The absolute path of the saved JPEG, or an empty string if extraction failed.
         */
        fun saveImageFromMediaStoreUri(
            context: Context,
            uri: Uri,
            fileName: String
        ): String {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val bytes = retriever.embeddedPicture
            if (bytes != null) {
                // Primary path — retriever decoded the embedded picture successfully.
                try {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val dir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
                    val destFile = File(dir, "$fileName.jpg")
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(destFile))

                    return destFile.path
                } catch (e: Exception) {
                    Timber.e("saveImageFromMediaStoreUri: Error saving image e=$e")
                }
            } else {
                // Fallback path — retriever returned null; try mp3agic's ID3v2 parser instead.
                // This handles files where the tag format is non-standard or the retriever
                // skips certain codec variations.
                try {
                    val fixUrl = Uri.fromFile(File(uri.path.toString()))
                    val file = uriToFile(context, fixUrl)
                    val mp3File = Mp3File(file)

                    if (mp3File.hasId3v2Tag()) {
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

        /**
         * Scans app external storage for a custom image file matching [customAlbumImageName]
         * and loads it onto [view] if found.
         *
         * Checks for `.jpg` then `.png` variants of the file name. Uses [ImageView.setImageURI]
         * when [synchronous] is `true` (for callers that need the image set before the next
         * draw pass); otherwise uses Coil's async [ImageView.load].
         *
         * @param view The [ImageView] target.
         * @param uri Unused — kept as a parameter for consistency with the [drawMediaItemArt]
         *   waterfall signature.
         * @param imageSize The target dimensions passed to Coil (async path only).
         * @param customAlbumImageName The base file name (without extension) to search for.
         * @param synchronous When `true`, uses synchronous [ImageView.setImageURI].
         * @return `true` if a matching file was found and loaded; `false` otherwise.
         */
        private fun loadCustomImage(view: ImageView, uri: Uri, imageSize: Size, customAlbumImageName: String, synchronous: Boolean = false): Boolean {
            val possibleImageSuffix = listOf(".jpg", ".png")
            val appDir = view.context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
            for (suffix in possibleImageSuffix) {
                val customAlbumImage = File(appDir, "${customAlbumImageName}$suffix")
                if (customAlbumImage.exists()) {
                    Timber.d("drawImageAssociatedWithAlbum: customAlbumImage=$customAlbumImage exists, setting image...")
                    try {
                        val artUri = Uri.fromFile(customAlbumImage)

                        if (synchronous) {
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
                    } catch (e: Exception) {
                        Timber.d("onBindViewHolder: exception when setting playlist art customAlbumImage=$customAlbumImage e=$e")
                        return false
                    }
                }
            }

            return false
        }

        /**
         * Sets the white-note placeholder drawable on [view].
         *
         * Used as the final fallback in the [drawMediaItemArt] waterfall when no artwork
         * source succeeds.
         */
        private fun drawDefault(view: ImageView) {
            val defaultArt = ResourcesCompat.getDrawable(view.resources, R.drawable.white_note, null)
            view.load(defaultArt)
        }

        /**
         * Loads a MediaStore `content://` thumbnail URI onto [view] using
         * [android.content.ContentResolver.loadThumbnail].
         *
         * `loadThumbnail` is more reliable than Coil for MediaStore `content://` URIs because
         * the resolver handles the thumbnail scaling internally. Does not work for plain
         * `file://` URIs — use [loadCustomImage] for those.
         *
         * @param view The [ImageView] target.
         * @param uri The MediaStore `content://` URI of the media item.
         * @param imageSize The requested thumbnail dimensions.
         * @param synchronous When `true`, sets the drawable directly via
         *   [ImageView.setImageDrawable]; otherwise uses Coil's async [ImageView.load].
         * @return `true` if the thumbnail was loaded without an exception; `false` on error.
         */
        private fun drawUriOntoImageView(view: ImageView, uri: Uri, imageSize: Size, synchronous: Boolean = false): Boolean {
            Timber.d("drawUriOntoImageView: view=$view, uri=$uri, size=$imageSize")
            val resolver = view.context.contentResolver
            try {
                val albumArt = resolver.loadThumbnail(uri, imageSize, null)
                val albumDrawable = albumArt.toDrawable(view.context.resources)

                if (synchronous) {
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

        /**
         * Copies the content at [uri] to a temporary `.mp3` file in the cache directory.
         *
         * mp3agic requires a [File] object rather than a stream or URI. This helper bridges
         * the gap by copying the URI's content to a uniquely named temp file that mp3agic can
         * open directly. The temp file persists in the cache until the system clears it.
         *
         * @param context The application context used to open the URI via [android.content.ContentResolver].
         * @param uri The `file://` or `content://` URI to copy.
         * @return The temp [File], or `null` if the URI could not be opened.
         */
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
         * Returns all [MediaItem] objects currently in [controller]'s queue as a mutable list.
         *
         * [MediaController] does not implement [Iterable], so the queue is walked by index from
         * `0` until [MediaController.mediaItemCount]. The resulting list order matches the
         * playback queue order.
         *
         * @param controller The [MediaController] whose queue should be read.
         * @return A new [MutableList] containing all queued [MediaItem] objects in order.
         */
        fun getSongListFromMediaController(controller: MediaController): MutableList<MediaItem> {
            val controllerSongLength = controller.mediaItemCount
            val songList = mutableListOf<MediaItem>()
            for (i in 0..<controllerSongLength) {
                songList.add(controller.getMediaItemAt(i))
            }
            return songList
        }

        /**
         * Calculates the maximum number of grid columns that fit on the current screen.
         *
         * Divides the screen width in dp by 170 dp (150 dp card width + 20 dp padding) and
         * floors the result to a whole number. This ensures each card has at least its
         * minimum padding regardless of screen size.
         *
         * @return The maximum integer grid column count for the current display.
         */
        fun determineGridSize(): Int {
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val dp = displayMetrics.density
            val widthDp = screenWidth / dp

            // Divide screen width by the total slot width (card + padding) and floor to an int.
            val widthInGrids = widthDp / 170
            val maxGridSize = floor(widthInGrids).toInt()

            Timber.d("updateAlbumLayout: screenWidth=$screenWidth, screenHeight=${displayMetrics.heightPixels}, dp=${dp}, widthDp=$widthDp, widthInGrids=$widthInGrids, maxGridSize=$maxGridSize")
            return maxGridSize
        }

        /**
         * Deletes the artwork file named [fileName] from app external storage.
         *
         * @param context The application context used to resolve the external files directory.
         * @param fileName The file name (including extension) within [Const.ALBUM_ART_FOLDER].
         * @return `true` if the file was deleted; `false` if the file does not exist or if
         *   an exception occurs.
         */
        fun deletePicture(context: Context, fileName: String): Boolean {
            return try {
                val appDir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
                val file = File(appDir, fileName)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                Timber.d("deleteFile: e=$e")
                return false
            }
        }

        /**
         * Returns the base file name (without extension) used to store artwork in app external
         * storage for a given song group.
         *
         * The naming scheme encodes the group type to avoid collisions between an album and a
         * playlist with the same title:
         * - **ALBUM**: `"album_<title>"` or `"album_<title>_<artist>"` (artist included when
         *   non-empty).
         * - **PLAYLIST**: `"playlist_<title>"`.
         *
         * All names are passed through [sanitizeFileName] to replace illegal file-system
         * characters before use.
         *
         * @param groupTitle The title of the album or playlist.
         * @param artist The artist name (used for ALBUM; ignored for PLAYLIST).
         * @param songGroupType The type of the group; only [SongGroupType.ALBUM] and
         *   [SongGroupType.PLAYLIST] produce valid names.
         * @return The sanitised base file name, or `"UNKNOWN FILE NAME"` for unsupported types.
         */
        fun getImageBaseNameFromExternalStorage(groupTitle: String, artist: String, songGroupType: SongGroupType): String {
            return when (songGroupType) {
                SongGroupType.ALBUM -> {
                    sanitizeFileName(if (artist.isEmpty()) "album_$groupTitle" else "album_${groupTitle}_$artist")
                }
                SongGroupType.PLAYLIST -> {
                    sanitizeFileName("playlist_$groupTitle")
                }
                else -> "UNKNOWN FILE NAME"
            }
        }

        /**
         * Replaces characters that are illegal in file names on most file systems.
         *
         * The characters `\ / : * ? " < > |` are replaced with `_`. This is required before
         * saving artwork files because album titles and artist names can legally contain these
         * characters in metadata but not in file names.
         *
         * @param fileName The raw string to sanitise.
         * @return The sanitised string with illegal characters replaced by `_`.
         */
        private fun sanitizeFileName(fileName: String): String {
            return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        }

        /**
         * Searches [dir] for an image file whose base name (without extension) exactly matches
         * [baseName].
         *
         * Checks `.png`, `.jpg`, `.jpeg`, and `.webp` extensions. Returns the first match
         * found; order depends on [File.listFiles] which is not guaranteed to be alphabetical.
         *
         * @param dir The directory to search. Returns `null` immediately if it does not exist
         *   or is not a directory.
         * @param baseName The exact base name to match (case-sensitive, extension excluded).
         * @return The matching [File], or `null` if no match is found.
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
         * Builds the destination [Uri] for a uCrop crop operation.
         *
         * The URI points to a `.jpg` file in either [Const.ALBUM_ART_CUSTOM_FOLDER] (for
         * user-chosen custom art) or [Const.ALBUM_ART_FOLDER] (for original/scanned art),
         * depending on [isCustom]. This URI is passed directly to [UCrop.of] as the output
         * destination.
         *
         * @param context The application context used to resolve the external files directory.
         * @param fileName The base name (without extension) for the destination file.
         * @param isCustom When `true`, the file is placed in the custom art folder; when
         *   `false`, in the original art folder.
         * @return A `file://` [Uri] pointing to the target `.jpg` file.
         */
        fun getSaveFileUri(
            context: Context,
            fileName: String,
            isCustom: Boolean,
        ): Uri {
            val saveFile = File(
                context.getExternalFilesDir(
                    if (isCustom) Const.ALBUM_ART_CUSTOM_FOLDER else Const.ALBUM_ART_FOLDER
                ),
                "${fileName}.jpg"
            )

            return saveFile.toUri()
        }

        /**
         * Saves the image at [sourceUri] as a JPEG to app external storage and returns the
         * saved file's absolute path.
         *
         * If the decoded bitmap is 700 px or larger in either dimension, [cropCenter] is
         * applied first to ensure a square crop at exactly 700 × 700 px before compression.
         * This guards against very large artwork files that would waste storage and slow down
         * subsequent loads.
         *
         * @param context The application context used to open [sourceUri] and resolve the
         *   external files directory.
         * @param sourceUri The URI of the image to save (typically the uCrop output URI).
         * @param fileName The base name (without extension) for the saved file.
         * @param isCustom When `true`, the file is saved to [Const.ALBUM_ART_CUSTOM_FOLDER];
         *   when `false`, to [Const.ALBUM_ART_FOLDER].
         * @return The absolute path of the saved JPEG, or `"UNKNOWN FILE"` on failure.
         */
        fun saveImageToFile(context: Context, sourceUri: Uri, fileName: String, isCustom: Boolean): String {
            try {
                val appDir = context.getExternalFilesDir(
                    if (isCustom) Const.ALBUM_ART_CUSTOM_FOLDER else Const.ALBUM_ART_FOLDER
                )

                appDir?.let { directory ->

                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val destFileName = "${fileName}.jpg"
                    val destination = File(directory, destFileName)

                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        var bitmap = BitmapFactory.decodeStream(inputStream)
                        // Centre-crop images ≥ 700 px to keep file sizes and load times reasonable.
                        if (bitmap.width >= 700 || bitmap.height >= 700) {
                            bitmap = cropCenter(bitmap)
                        }
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(destination))
                    }

                    return destination.toString()
                }
            } catch (e: IOException) {
                Timber.d("saveImageToFile: Error copying file e=$e")
            } catch (e: Exception) {
                Timber.d("saveImageToFile: e=$e")
            }

            return "UNKNOWN FILE"
        }

        /**
         * Centre-crops [bitmap] to a square of [cropSize] × [cropSize] pixels.
         *
         * The horizontal and vertical offsets are computed as half the excess width/height,
         * ensuring the crop window is centred on the original image. Requires that both
         * dimensions of [bitmap] are at least [cropSize]; throws [IllegalArgumentException]
         * if the precondition is not met.
         *
         * @param bitmap The source bitmap to crop.
         * @param cropSize The side length in pixels of the resulting square. Defaults to `700`.
         * @return A new square [Bitmap] of size [cropSize] × [cropSize].
         * @throws IllegalArgumentException if [bitmap] is smaller than [cropSize] in either dimension.
         */
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
         * Loads a `.jpg` artwork file for [playlistTitle] from the original art folder and
         * sets it on [view].
         *
         * Looks up `<playlistTitle>.jpg` in [Const.ALBUM_ART_FOLDER] within app external
         * storage. Uses [ImageView.setImageURI] for synchronous display. Returns `false`
         * silently if the file is not found, so callers can fall back to a placeholder.
         *
         * @param view The [ImageView] to load the artwork onto.
         * @param playlistTitle The playlist title used as the file's base name.
         * @return `true` if the file exists and was set on [view]; `false` otherwise.
         */
        fun setPlaylistImageFromAppStorage(
            view: ImageView,
            playlistTitle: String
        ): Boolean {
            Timber.d("setPlaylistImageFromAppStorage: playlistTitle=$playlistTitle")
            val playlistFile = "$playlistTitle.jpg"

            if (playlistFile.isNotEmpty()) {
                val appDir = view.context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)
                val imageFile = File(appDir, playlistFile)
                if (imageFile.exists()) {
                    try {
                        val artUri = Uri.fromFile(imageFile)
                        view.setImageURI(artUri)
                        return true
                    } catch (e: Exception) {
                        Timber.d("onBindViewHolder: exception when setting playlist art e=$e")
                    }
                }
            }

            return false
        }

        /**
         * Renames the `.jpg` artwork file associated with a playlist from [oldPlaylistName]
         * to [newPlaylistName] in app external storage.
         *
         * Only the `.jpg` variant is renamed. Silently does nothing if the source file does
         * not exist, so this is safe to call even for playlists that have no artwork.
         *
         * @param context The application context used to resolve the external files directory.
         * @param oldPlaylistName The current base name (without extension) of the artwork file.
         * @param newPlaylistName The new base name (without extension) to rename the file to.
         */
        fun renamePlaylistImageFile(context: Context, oldPlaylistName: String, newPlaylistName: String) {
            Timber.d("renamePlaylistImageFile: ")
            val playlistFileName = "$oldPlaylistName.jpg"
            val newPlaylistFileName = "$newPlaylistName.jpg"

            if (playlistFileName.isNotEmpty()) {
                val appDir = context.getExternalFilesDir(Const.ALBUM_ART_FOLDER)

                val currentImageFile = File(appDir, playlistFileName)
                val updatedNameFile = File(appDir, newPlaylistFileName)

                if (currentImageFile.exists()) {
                    currentImageFile.renameTo(updatedNameFile)
                }
            }
        }
    }
}

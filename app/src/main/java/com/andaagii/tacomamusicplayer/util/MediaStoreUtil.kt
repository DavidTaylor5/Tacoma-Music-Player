package com.andaagii.tacomamusicplayer.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.data.SongData
import timber.log.Timber

/**
 * This class handles logic related to the android class MediaStore. MediaStore is an abstraction of
 * the on device files in android. Newer versions of the android sdk has an emphasis on security, and
 * having applications able to directly access on board storage could be dangerous. By using MediaStore
 * I can request safe permissions from the user and query audio to use in the mp3 app.
 */
class MediaStoreUtil {

    private val mediaItemUtil: MediaItemUtil = MediaItemUtil()

    /**
     * Query all songs from associated album on device storage.
     * @param context Context associated with the application. Context needs permission READ_MEDIA_AUDIO.
     * @param album The title of an album.
     * @return A list of MediaItems songs associated with the given album title.
     */
    fun querySongsFromAlbum(context: Context, album: String): List<MediaItem> {
        Timber.d("querySongsFromAlbum: ")

        val albumSongs = mutableListOf<MediaItem>()

        val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val uriAlbum = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        var albumIdCheck = 0L

        val proj2: Array<String?> = arrayOf(
            MediaStore.Audio.Albums.ALBUM_ID,
        )

        context.contentResolver.query(
            uriAlbum,
            proj2,
            "${MediaStore.Audio.Albums.ALBUM} = ?",
            arrayOf(album),
            null
        )?.use {cursor ->
            while(cursor.moveToNext()) {
                val albumId = cursor.getLong(0)
                albumIdCheck = albumId
            }

        }

        val b = "what"

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA,     //0 -> url
            MediaStore.Audio.AudioColumns.TITLE,    //1 -> song title
            MediaStore.Audio.AudioColumns.ALBUM,    //2 -> album title
            MediaStore.Audio.ArtistColumns.ARTIST,  //3 -> artist
            MediaStore.Audio.AudioColumns.DURATION, //4 -> duration in  milliseconds
            MediaStore.Audio.AudioColumns.TRACK,    //5 -> track # in album
            MediaStore.Audio.AudioColumns._ID,      //6 id
            MediaStore.Audio.AudioColumns.ALBUM_ID,
        )

        context.contentResolver.query(
            uriExternal,
            projection,
            "${MediaStore.Audio.AudioColumns.ALBUM} = ?",
            arrayOf(album),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                Timber.d(
                    "querySongsFromAlbum: ${cursor.getString(0)}, ${cursor.getString(1)}, ${
                        cursor.getString(
                            2
                        )
                    }, ${cursor.getString(3)}, ${cursor.getString(4)}, ${cursor.getString(5)}, ${
                        cursor.getString(
                            6
                        )
                    }"
                )

                val url = cursor.getString(0)
                val title = cursor.getString(1)
                val album = cursor.getString(2)
                val artist = cursor.getString(3)
                val duration = cursor.getString(4)
                val track = cursor.getInt(5)
                val songId = cursor.getLong(6)
                val albumId = cursor.getLong(7)

                val albumUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                val artworkUri = ContentUris.withAppendedId(uriExternal, songId)

                val a= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()
                val b = Build.VERSION_CODES.Q
                val testUri = ContentUris.withAppendedId(uriExternal, albumId)

                val songMediaItem = mediaItemUtil.createMediaItemFromSongData(
                    SongData(
                        songUri = url,
                        songTitle = title,
                        albumTitle = album,
                        artist = artist,
                        artworkUri = artworkUri.toString(),
                        duration = duration
                    )
                )
                albumSongs.add(songMediaItem)
            }
        }
        Timber.d("querySongsFromAlbum: DONE SEARCHING!")

        return albumSongs
    }

    /**
     * Query all available albums on device storage.
     * @param context Context associated with the application. Context needs permission READ_MEDIA_AUDIO.
     * @return A list of mediaItems associated with albums on device.
     */
    fun queryAvailableAlbums(context: Context): MutableList<MediaItem> { //TODO should probably be AlbumModel
        Timber.d("queryAvailableAlbums: ")

        val albumList: MutableList<MediaItem> = mutableListOf()

        val uriExternal: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR
        )

        context.contentResolver.query(
            uriExternal,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                Timber.d(
                    "queryAvailableAlbums: ${cursor.getString(0)}, ${cursor.getString(1)}, ${
                        cursor.getString(
                            2
                        )
                    }"
                )

                var albumId = 0L
                var albumTitle = ""
                var artist = ""
                var releaseYear = 0

                try {
                    albumId = cursor.getLong(0)
                } catch (e: Exception) {
                    Timber.d("queryAvailableAlbums: No album ID specified.")
                }

                try {
                    albumTitle = cursor.getString(1)
                } catch (e: Exception) {
                    Timber.d("queryAvailableAlbums: No album title specified.")
                }

                try {
                    artist = cursor.getString(2)
                } catch (e: Exception) {
                    Timber.d("queryAvailableAlbums: No artist specified.")
                }

                try {
                    val releaseYearString = cursor.getString(3)
                    releaseYearString.toIntOrNull()?.let { releaseYear = it }
                } catch (e: Exception) {
                    Timber.d("queryAvailableAlbums: No release year specified.")
                }

                val artworkUri = ContentUris.withAppendedId(uriExternal, albumId)

                //Create Media Item from information
                val albumMediaItem =
                    mediaItemUtil.createAlbumMediaItem(albumTitle, artist, artworkUri, releaseYear)

                albumList.add(albumMediaItem)
            }
        }
        Timber.d("queryAvailableAlbums: DONE SEARCHING!")

        return albumList
    }
}

//Can I use MediaMetadataRetriever with music stored in Music folder on android device?
//PHIND-70B MODEL
//
//Yes, you can use MediaMetadataRetriever with music stored in the Music folder on an Android device. Here's how to implement it:
//Basic Implementation
//
//class MusicMetadataLoader(private val context: Context) {
//    fun getAlbumArtFromMusicFolder(): List<MusicFile> {
//        val musicFiles = mutableListOf<MusicFile>()
//        val projection = arrayOf(
//            MediaStore.Audio.Media.DATA,  // File path
//            MediaStore.Audio.Media.TITLE, // Song title
//            MediaStore.Audio.Media.ARTIST // Artist name
//        )
//
//        context.contentResolver.query(
//            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//            projection,
//            "${MediaStore.Audio.Media.DATA} LIKE ?",
//            arrayOf("%/Music/%"),
//            null
//        )?.use { cursor ->
//            while (cursor.moveToNext()) {
//                val filePath = cursor.getString(
//                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
//                )
//                val title = cursor.getString(
//                    cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
//                )
//                val artist = cursor.getString(
//                    cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
//                )
//
//                musicFiles.add(MusicFile(filePath, title, artist))
//            }
//        }
//
//        return musicFiles
//    }
//
//    fun getAlbumArt(filePath: String): Bitmap? {
//        val retriever = MediaMetadataRetriever()
//        return try {
//            retriever.setDataSource(filePath)
//            val artwork = retriever.embeddedPicture
//
//            artwork?.let { bytes ->
//                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//            }
//        } catch (e: Exception) {
//            Log.e("MusicMetadata", "Error getting album art: ${e.message}")
//            null
//        } finally {
//            retriever.release()
//        }
//    }
//}
//
//data class MusicFile(
//    val filePath: String,
//    val title: String,
//    val artist: String
//)
//
//Usage Example
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var musicMetadataLoader: MusicMetadataLoader
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        musicMetadataLoader = MusicMetadataLoader(this)
//
//        // Get all music files
//        val musicFiles = musicMetadataLoader.getAlbumArtFromMusicFolder()
//
//        // Process each file
//        musicFiles.forEach { musicFile ->
//            val albumArt = musicMetadataLoader.getAlbumArt(musicFile.filePath)
//            if (albumArt != null) {
//                // Use the album art
//                imageView.setImageBitmap(albumArt)
//            }
//        }
//    }
//}
//
//Important Considerations
//
//Required Permissions
//
//<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
//
//Error Handling
//Always handle IOException and IllegalArgumentException
//Release MediaMetadataRetriever in finally block
//Provide fallback artwork for missing images
//Performance Tips
//Process files in batches for large collections
//Consider caching successful loads
//Use appropriate image sizes to manage memory
//Memory Management
//Always release resources
//Handle bitmap recycling
//Consider using image loading libraries for complex scenarios
//
//This implementation provides a robust way to work with music files in the Music folder while properly handling metadata retrieval and error cases.







/*
I have added some examples of selection and selection args that can be used in media store for
reference.

This code works as a selector -> will return 3 gza songs that are greater than 5 minutes long
"${MediaStore.Audio.AudioColumns.DURATION} >= ?",
        arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()),*/

/*        This code works as a selector -> will return all songs with associated title Liquid Swords [Explicit]
        "${MediaStore.Audio.AudioColumns.ALBUM} = ?",
        arrayOf("Liquid Swords [Explicit]"),*/
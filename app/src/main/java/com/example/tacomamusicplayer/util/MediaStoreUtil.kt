package com.example.tacomamusicplayer.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.tacomamusicplayer.data.SongModel
import timber.log.Timber
import java.lang.Exception

/**
 * This class handles logic related to the android class MediaStore. MediaStore is an abstraction of
 * the on device files in android. Newer versions of the android sdk has an emphasis on security, and
 * having applications able to directly access on board storage could be dangerous. By using MediaStore
 * I can request safe permissions from the user and query audio to use in the mp3 app.
 */
class MediaStoreUtil {

    /**
     * Query all songs from associated album on device storage.
     * @param context Context associated with the application. Context needs permission READ_MEDIA_AUDIO.
     * @param album The title of an album.
     * @return A list of MediaItems [songs] associated with the given album title.
     */
    fun querySongsFromAlbum(context: Context, album: String): List<SongModel> {
        Timber.d("querySongsFromAlbum: ")

        val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA, // 0 -> url
            MediaStore.Audio.AudioColumns.TITLE, //1 -> song title
            MediaStore.Audio.AudioColumns.ALBUM, //2 -> album title
            MediaStore.Audio.ArtistColumns.ARTIST, //3 -> artist
            MediaStore.Audio.AudioColumns.DURATION, //4 -> duration in  milliseconds
            MediaStore.Audio.AudioColumns.TRACK, //5 -> track # in album
            MediaStore.Audio.AudioColumns._ID, //6 id
        )

        context.contentResolver.query(
            uriExternal,
            projection,
            "${MediaStore.Audio.AudioColumns.ALBUM} = ?",
            arrayOf(album),
            null
        )?.use { cursor ->
            while(cursor.moveToNext()) {
                Timber.d("querySongsFromAlbum: ${cursor.getString(0)}, ${cursor.getString(1)}, ${cursor.getString(2)}, ${cursor.getString(3)}, ${cursor.getString(4)}, ${cursor.getString(5)}, ${cursor.getString(6)}")
            }
        }
        Timber.d("querySongsFromAlbum: DONE SEARCHING!")

        return emptyList() //TODO this should probabaly return some information...
    }

    /**
     * Query all available albums on device storage.
     * @param context Context associated with the application. Context needs permission READ_MEDIA_AUDIO.
     * @return A list of mediaItems associated with albums on device.
     */
    fun queryAvailableAlbums(context: Context): List<SongModel> { //TODO should probably be AlbumModel

        //WOW THIS IS ACTUALLY  WORKING!

        Timber.d("queryAvailableAlbums: ")

        val tempAudioList: MutableList<SongModel> = ArrayList()

        //val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val uriExternal: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.Media.ALBUM_ID, //1 -> what the hell is this?
            MediaStore.Audio.Albums.ALBUM, //2 -> album name again
            MediaStore.Audio.Albums.ARTIST, //3 -> artist again...
        )

        context.contentResolver.query(
            uriExternal,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while(cursor.moveToNext()) {
                Timber.d("queryAvailableAlbums: ${cursor.getString(0)}, ${cursor.getString(1)}, ${cursor.getString(2)}")
            }
        }
        Timber.d("queryAvailableAlbums: DONE SEARCHING!")

        return listOf<SongModel>() //TODO I'll have to implement this, I should probably return some information here...
    }

    /**
     * Query all possible Audio items on device storage. Should get all songs available.
     * I don't think this is too efficient, this might be phased out.
     * @param context Context associated with the application. Context needs permission READ_MEDIA_AUDIO.
     * @return A list of all audio items in device storage.
     */
    fun queryAllMediaItems(context: Context):  HashMap<String, MutableList<MediaItem>> {
        Timber.d("queryAllMediaItems: ")

        val map: HashMap<String, MutableList<MediaItem>> = hashMapOf()

        val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA, // 0 -> url
            MediaStore.Audio.AudioColumns.TITLE, //1 -> song title
            MediaStore.Audio.AudioColumns.ALBUM, //2 -> album title
            MediaStore.Audio.ArtistColumns.ARTIST, //3 -> artist
            MediaStore.Audio.AudioColumns.DURATION, //4 -> duration in  milliseconds
            MediaStore.Audio.AudioColumns.TRACK, //5 -> track # in album
            MediaStore.Audio.Media.ALBUM_ID, //6 -> what the hell is this?
            MediaStore.Audio.Albums.ALBUM, //7 -> album name again
            MediaStore.Audio.Albums.ARTIST, //8 -> artist again...
            MediaStore.Audio.AudioColumns._ID, //9 id
        )

        val resolver = context.contentResolver

        context.contentResolver.query(
            uriExternal,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while(cursor.moveToNext()) {
                Timber.d("queryAllMediaItems: ${cursor.getString(0)}, ${cursor.getString(1)}, ${cursor.getString(2)}, ${cursor.getString(3)}, ${cursor.getString(4)}, ${cursor.getString(5)}, ${cursor.getString(6)}, ${cursor.getString(7)}, ${cursor.getString(8)}, ${cursor.getString(9)}") //setMedia items here?

                val songUrl = cursor.getString(0)
                val album = cursor.getString(2)
                val artist = cursor.getString(3)
                val songTitle = cursor.getString(1)
                val songId = cursor.getLong(9)  //used when I'm getting the album art...

                try {

                    //Create URI from MediaStore location and returned ID [Keeps actual file location protected?]
                    val imageUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

                    Timber.d("queryAllMediaItems: Getting album art from URI=${imageUri.toString()}")

                    //Album art as a bitmap, I need to work on what to do when this is blank / null?
                    val albumArt = resolver.loadThumbnail(imageUri, Size(100, 100), null)

                    Timber.d("queryAllMediaItems: SUCCESSFUL! ALBUM ART FOUND!")

                } catch (e: Exception) {
                    Timber.d("queryAllMediaItems: ERROR ON LOADING ALBUM ART e=$e")
                }

                val songMediaItem = MediaItem.fromUri(songUrl)
                val updatedSongMediaItem = songMediaItem.buildUpon().setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setTitle(songTitle)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .build()
                ).setMediaId(songTitle)
                    .build()


                if(!map.containsKey(album)) {
                    map[album] = mutableListOf(updatedSongMediaItem)
                } else {
                    map[album]?.add(updatedSongMediaItem)
                }
            }
        }
        Timber.d("queryAllMediaItems: DONE SEARCHING!")

        return map
    }

    /**
     * Create a media item from a song.
     * @param songTitle
     * @param albumTitle
     * @param artist
     * @param songDuration
     * @param trackNumber
     * @return A MediaItem with the associated data.
     */
    private fun createSongMediaItem(
        songTitle: String = "UNKONWN SONG TITLE",
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        songDuration: Long,
        trackNumber: Int
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("libraryItem")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle("musicapprootwhichisnotvisibletocontrollers")
                    .setAlbumTitle("ALBUM TITLE")
                    .setArtist("ARTIST")
                    .setDescription("Description I'll just pass song length here... TODO calculate song minutes and seconds")
                    .setTrackNumber(0)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    /**
     * Creates a media item that represents an album.
     * @param albumTitle
     * @param artist
     */
    private fun createAlbumMediaItem(
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("libraryItem")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setAlbumArtist("ARTIST")
                    .setAlbumTitle("ALBUM TITLE")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .build()
            )
            .build()
    }
}


/*
I have added some examples of selection and selection args that can be used in media store for
reference.

This code works as a selector -> will return 3 gza songs that are greater than 5 minutes long
"${MediaStore.Audio.AudioColumns.DURATION} >= ?",
        arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()),*/

/*        This code works as a selector -> will return all songs with associated title Liquid Swords [Explicit]
        "${MediaStore.Audio.AudioColumns.ALBUM} = ?",
        arrayOf("Liquid Swords [Explicit]"),*/
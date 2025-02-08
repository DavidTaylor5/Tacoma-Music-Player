package com.example.tacomamusicplayer.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import com.example.tacomamusicplayer.data.SongData
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

        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA,     //0 -> url
            MediaStore.Audio.AudioColumns.TITLE,    //1 -> song title
            MediaStore.Audio.AudioColumns.ALBUM,    //2 -> album title
            MediaStore.Audio.ArtistColumns.ARTIST,  //3 -> artist
            MediaStore.Audio.AudioColumns.DURATION, //4 -> duration in  milliseconds
            MediaStore.Audio.AudioColumns.TRACK,    //5 -> track # in album
            MediaStore.Audio.AudioColumns._ID,      //6 id
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
                val artworkUri = ContentUris.withAppendedId(uriExternal, songId)

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

/*
I have added some examples of selection and selection args that can be used in media store for
reference.

This code works as a selector -> will return 3 gza songs that are greater than 5 minutes long
"${MediaStore.Audio.AudioColumns.DURATION} >= ?",
        arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()),*/

/*        This code works as a selector -> will return all songs with associated title Liquid Swords [Explicit]
        "${MediaStore.Audio.AudioColumns.ALBUM} = ?",
        arrayOf("Liquid Swords [Explicit]"),*/
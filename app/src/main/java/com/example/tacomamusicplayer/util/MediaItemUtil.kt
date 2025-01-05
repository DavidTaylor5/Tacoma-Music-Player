package com.example.tacomamusicplayer.util

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.tacomamusicplayer.data.SongData

class MediaItemUtil {
    /**
     * Create a media item from a song.
     * @param songTitle
     * @param albumTitle
     * @param artist
     * @param songDuration
     * @param trackNumber
     * @return A MediaItem with the associated data.
     */
    fun createMediaItemFromSongData(
        songUri: String = "UNKNOWN URI", //I'm adding the Uri to be mediaid however this is going to be a security breach...
        songTitle: String = "UNKONWN SONG TITLE",
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        artworkUri: Uri = Uri.EMPTY,
        trackNumber: Int
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(songUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle(songTitle)
                    .setAlbumTitle(albumTitle)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .setDescription("Description I'll just pass song length here... TODO calculate song minutes and seconds")
                    .setTrackNumber(trackNumber)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    /**
     * Convert a list of songdata into mediaItems.
     */
    fun convertListOfSongDataIntoListOfMediaItem(
        songs: List<SongData>
    ): List<MediaItem> {
         return songs.map {data ->
             createMediaItemFromSongData(data)
         }
    }


    /**
     * SongData is used in the database, so I use this function to convert songData into MediaItems
     * that I can play.
     */
    private fun createMediaItemFromSongData(
        song: SongData
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.songUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle(song.songTitle)
                    .setAlbumTitle(song.albumTitle)
                    .setArtist(song.artist)
                    .setArtworkUri(Uri.parse(song.artworkUri))
                    .setDescription("Description I'll just pass song length here... TODO calculate song minutes and seconds")
//                    .setTrackNumber(5) //TODO should this be changed or removed?
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
                )
            .build()
    }

    /**
     * Used when I have a media item and I want to store it into a playlist!
     * @param songMediaItem associated with a song.
     */
    fun createSongDataFromMediaItem(
        songMediaItem: MediaItem
    ): SongData {
        return SongData(
            songUri = songMediaItem.mediaId,
            songTitle = songMediaItem.mediaMetadata.title.toString(),
            albumTitle = songMediaItem.mediaMetadata.albumTitle.toString(),
            artist = songMediaItem.mediaMetadata.artist.toString(),
            artworkUri = songMediaItem.mediaMetadata.artworkUri.toString()
        )
    }

    /**
     * Creates a media item that represents an album.
     * @param albumTitle
     * @param artist
     */
    fun createAlbumMediaItem(
        albumTitle: String = "UNKNOWN ALBUM",
        artist: String = "UNKNOWN ARTIST",
        artworkUri: Uri = Uri.EMPTY
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(albumTitle)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setAlbumArtist(artist)
                    .setAlbumTitle(albumTitle)
                    .setArtworkUri(artworkUri)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .build()
            )
            .build()
    }
}
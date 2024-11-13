package com.example.tacomamusicplayer.util

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

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
    fun createSongMediaItem(
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
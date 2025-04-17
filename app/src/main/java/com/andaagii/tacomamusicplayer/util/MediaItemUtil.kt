package com.andaagii.tacomamusicplayer.util

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.andaagii.tacomamusicplayer.data.SearchData
import com.andaagii.tacomamusicplayer.data.SongData

class MediaItemUtil {

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
    fun createMediaItemFromSongData(
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
                    .setDescription(song.duration)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
                )
            .build()
    }

    fun convertListOfSearchDataIntoListOfMediaItem(
        searchItems: List<SearchData>
    ): List<MediaItem> {
        return searchItems.map { searchItem ->
            createMediaItemFromSearchData(searchItem)
        }
    }

    fun createMediaItemFromSearchData(
        searchItem: SearchData
    ): MediaItem {
        if(searchItem.isAlbum) {
            return MediaItem.Builder()
                .setMediaId(searchItem.albumTitle)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("")
                        .setAlbumTitle(searchItem.albumTitle)
                        .setArtist(searchItem.artist)
                        .setArtworkUri(Uri.parse(searchItem.artworkUri))
                        .setDescription(searchItem.description)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                        .build()
                )
                .build()
        } else {
            return MediaItem.Builder()
                .setMediaId(searchItem.songUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setTitle(searchItem.songTitle)
                        .setAlbumTitle(searchItem.albumTitle)
                        .setArtist(searchItem.artist)
                        .setArtworkUri(Uri.parse(searchItem.artworkUri))
                        .setDescription(searchItem.description)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()
        }
    }

    /**
     * Creates a list of SongData from a List of MediaItems that represent songs.
     */
    fun createSongDataFromListOfMediaItem(
        mediaItems: List<MediaItem>
    ): List<SongData> {
        return mediaItems.map { songMediaItem ->
            createSongDataFromMediaItem(songMediaItem)
        }
    }

    /**
     * Used when I have a media item and I want to store it into a playlist!
     * @param songMediaItem associated with a song.
     */
    private fun createSongDataFromMediaItem(
        songMediaItem: MediaItem
    ): SongData {
        return SongData(
            songUri = songMediaItem.mediaId,
            songTitle = songMediaItem.mediaMetadata.title.toString(),
            albumTitle = songMediaItem.mediaMetadata.albumTitle.toString(),
            artist = songMediaItem.mediaMetadata.artist.toString(),
            artworkUri = songMediaItem.mediaMetadata.artworkUri.toString(),
            duration = songMediaItem.mediaMetadata.description.toString()
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
        artworkUri: Uri = Uri.EMPTY,
        releaseYear: Int = 0
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
                    .setReleaseYear(releaseYear)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .build()
            ).build()
    }
}
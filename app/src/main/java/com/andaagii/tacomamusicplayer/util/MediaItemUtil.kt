package com.andaagii.tacomamusicplayer.util

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.andaagii.tacomamusicplayer.data.SearchData
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import javax.inject.Inject

class MediaItemUtil @Inject constructor() {

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
    fun getSongSearchDescriptionFromMediaItem(song: MediaItem): String {
        val songInfo = song.mediaMetadata
        val songDescription = "${songInfo.title}_${songInfo.albumTitle}_${songInfo.artist}"
        return songDescription
    }

    fun createMediaItemFromArtist(artist: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId("artist:$artist")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .build()
            )
            .build()
    }

    fun createAlbumMediaItemFromSongGroupEntity(album: SongGroupEntity): MediaItem {
        return MediaItem.Builder()
            .setMediaId("album:${album.groupTitle}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle(album.groupTitle)
                    .setAlbumArtist(album.groupArtist)
                    .setArtworkUri(album.artUri?.toUri())
                    .setReleaseYear(album.releaseYear.toIntOrNull())
                    .setDescription(album.groupDuration)
                    .setIsBrowsable(true)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    fun createPlaylistMediaItemFromSongGroupEntity(playlist: SongGroupEntity): MediaItem {
        return MediaItem.Builder()
            .setMediaId("playlist:${playlist.groupTitle}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle(playlist.groupTitle)
                    .setAlbumArtist(playlist.groupArtist)
                    .setArtworkUri(playlist.artUri?.toUri())
                    .setDescription("${playlist.creationTimestamp}:${playlist.lastModificationTimestamp}")
                    .setIsBrowsable(true)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    fun createMediaItemFromSongEntity(
        song: SongEntity
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle(song.name)
                    .setAlbumTitle(song.albumTitle)
                    .setArtist(song.artist)
                    .setArtworkUri(song.artworkUri.toUri())
                    .setDescription(song.songDuration)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    fun removeMediaItemPrefix(
        mediaItemId: String
    ): String {
        val prefixEnd = mediaItemId.indexOfFirst { char ->
            char == ':'
        }

        return if(prefixEnd != -1) mediaItemId.substring(prefixEnd + 1) else mediaItemId
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
                    .setArtworkUri(song.artworkUri.toUri())
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
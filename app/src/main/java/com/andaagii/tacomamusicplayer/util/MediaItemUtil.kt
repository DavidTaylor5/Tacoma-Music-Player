package com.andaagii.tacomamusicplayer.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.andaagii.tacomamusicplayer.data.AndroidAutoPlayData
import com.andaagii.tacomamusicplayer.data.ArtInfo
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType.Companion.determineSongGroupTypeFromString
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.getFileProviderUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaItemUtil @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

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
                    .setIsPlayable(false)
                    .setTitle(artist)
                    .setSubtitle(artist)
                    .build()
            )
            .build()
    }

    /**
     * Determine two things, if I'm using a custom or original art, and
     * if I'm using android auto I need to use a file provider for secure sharing
     * of files.
     */
    fun determineArtUri(
        songGroup: SongGroupEntity,
        useFileProviderUri: Boolean = false
    ): Uri {
        return if(useFileProviderUri) {
            if(songGroup.useCustomArt) {
                getFileProviderUri(appContext, songGroup.artFileCustom)
            } else {
                getFileProviderUri(appContext, songGroup.artFileOriginal)
            }
        } else {
            if(songGroup.useCustomArt) {
                songGroup.artFileCustom.toUri()
            } else {
                songGroup.artFileOriginal.toUri()
            }
        }
    }

    fun createAlbumMediaItemFromSongGroupEntity(
        album: SongGroupEntity,
        artUri: Uri? = null
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("album:${album.groupTitle}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle(album.groupTitle)
                    .setAlbumArtist(album.groupArtist)
                    .setArtworkUri(
                        artUri ?:
                        if(album.useCustomArt) album.artFileCustom.toUri()
                        else album.artFileOriginal.toUri()
                    )
                    .setReleaseYear(album.releaseYear.toIntOrNull())
                    .setDescription(album.groupDuration)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(album.groupTitle)
                    .setSubtitle(album.searchDescription)
                    .build()
            )
            .build()
    }

    fun createPlaylistMediaItemFromSongGroupEntity(
        playlist: SongGroupEntity,
        artUri: Uri? = null
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("playlist:${playlist.groupTitle}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle(playlist.groupTitle)
                    .setAlbumArtist(playlist.groupArtist)
                    .setArtworkUri(artUri ?: playlist.artFileCustom.toUri())
                    .setDescription("${playlist.creationTimestamp}:${playlist.lastModificationTimestamp}")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(playlist.groupTitle)
                    .setSubtitle(playlist.searchDescription)
                    .build()
            )
            .build()
    }

    /**
     * Creates a song entity from a mediaItem, don't use this in the worker as I'm adding in album uri there.
     * TODO I might remove this... Unless I find a use for it.
     */
    fun createSongEntityFromMediaItem(mediaItem: MediaItem): SongEntity {
        val songInfo = mediaItem.mediaMetadata
        val songDescription = getSongSearchDescriptionFromMediaItem(mediaItem)

        return SongEntity(
            albumTitle = songInfo.albumTitle.toString(),
            artist = songInfo.artist.toString(),
            searchDescription = songDescription,
            name = songInfo.title.toString(),
            uri = mediaItem.mediaId,
            songDuration = songInfo.description.toString(),
            artworkUri = songInfo.artworkUri.toString()
        )
    }

    /**
     * Because android auto is going to delete all information except the mediaId, I need to have a descriptive
     * mediaId when I query albums and playlists from the media library  ex. android auto
     */
    fun createMediaItemFromSongEntity(
        song: SongEntity,
        position: Int? = null,
        songGroupType: SongGroupType? = null,
        playlistTitle: String? = null,
        useFileProviderUri: Boolean = false
    ): MediaItem {
        val mediaId = if(position != null && songGroupType != null) {
            "songGroupType=${songGroupType.name}, groupTitle=${ if(playlistTitle != null) playlistTitle else song.albumTitle}, position=$position, songTitle=${song.name}"
        } else {
            song.name
        }

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle(song.name)
                    .setAlbumTitle(song.albumTitle)
                    .setArtist(song.artist)
                    .setArtworkUri(if(useFileProviderUri)
                        getFileProviderUri(appContext, song.artworkUri)
                    else song.artworkUri.toUri()
                    )
                    .setDescription(song.songDuration)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setSubtitle(song.searchDescription)
                    .build()
            )
            .build()
    }

    /**
     * Android Auto needs to know the position of the song, to add song group to the queue and update player position.
     * mediaId -> position=SONG_POSITION:SONG_NAME ex. position=3:DNA
     * Return -1 if position isn't found. Return int if found.
     */
    fun checkPositionFromMediaItem(mediaItem: MediaItem): Int {

        //TODO update this to determine  "${songGroupType.name}=${song.albumTitle}, position=$position, song=${song.name}"
        //I need to return SongGroupType -> which function to call, group title -> query argument, position -> move player position to correct spot.


        val positionKey = "position="
        val positionKeyIndex = mediaItem.mediaId.indexOf(positionKey)
        val dividerIndex = mediaItem.mediaId.indexOf(":")
        if(positionKeyIndex > -1 && dividerIndex > -1) {
            val positionStart = positionKeyIndex + positionKey.length
            val songPosition = mediaItem.mediaId.substring(positionStart, dividerIndex).toIntOrNull()

            songPosition?.let { position ->
                return position
            }
        }

        return -1
    }

    fun getAndroidAutoPlayDataFromMediaItem(mediaItem: MediaItem): AndroidAutoPlayData {
        val songGroupType = determineSongGroupTypeFromString(determineFieldFromMediaId(mediaId = mediaItem.mediaId, field = "songGroupType="))
        val groupTitle = determineFieldFromMediaId(mediaId = mediaItem.mediaId, field = "groupTitle=")
        val position = determineFieldFromMediaId(mediaId = mediaItem.mediaId, field = "position=").toIntOrNull() ?: 0
        val songTitle = determineFieldFromMediaId(mediaId = mediaItem.mediaId, field = "songTitle=")

        return AndroidAutoPlayData(
            songGroupType = songGroupType,
            groupTitle = groupTitle,
            position = position,
            songTitle = songTitle
        )
    }

    /**
     * Given a mediaId in the form of "songgrouptype=${songGroupType.name}, groupTitle=${song.albumTitle}, position=$position, song=${song.name}"
     * Determine a certain field.
     */
    private fun determineFieldFromMediaId(mediaId: String, field: String): String {
        val positionField = mediaId.indexOf(field)
        if(positionField >= 0) {
            var value = ""
            val startPosition = positionField + field.length
            for(i in startPosition until mediaId.length) {
                if(mediaId[i] == ',') break
                else value += mediaId[i]
            }
            return value
        } else {
            return ""
        }
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
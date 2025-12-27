package com.andaagii.tacomamusicplayer.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.andaagii.tacomamusicplayer.data.AndroidAutoPlayData
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.database.entity.SongEntity
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType.Companion.determineSongGroupTypeFromString
import com.andaagii.tacomamusicplayer.util.UtilImpl.Companion.getFileProviderUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class MediaItemUtil @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
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
                    //.setSubtitle(artist) //TODO amount of albums?
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
        useFileProviderUri: Boolean = false
    ): MediaItem {
        val albumArtUri = if(useFileProviderUri) {
            if(album.useCustomArt) {
                getFileProviderUri(appContext, album.artFileCustom)
            } else {
                getFileProviderUri(appContext, album.artFileOriginal)
            }
        } else {
            if(album.useCustomArt && !album.artFileCustom.isEmpty()) {
                album.artFileCustom.toUri()
            } else {
                album.artFileOriginal.toUri()
            }
        }

        return MediaItem.Builder()
            .setMediaId("album:${album.groupTitle}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle(album.groupTitle)
                    .setAlbumArtist(album.groupArtist)
                    .setArtworkUri(albumArtUri)
                    .setReleaseYear(album.releaseYear.toIntOrNull())
                    .setDescription("${System.currentTimeMillis()}") //TODO set last modification time... //TODO add diff util
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(album.groupTitle)
                    .setSubtitle(album.groupArtist)
                    .build()
            )
            .build()
    }

    fun createPlaylistMediaItemFromSongGroupEntity(
        playlist: SongGroupEntity,
        useFileProviderUri: Boolean = false
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("playlist:${playlist.groupTitle}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle(playlist.groupTitle)
                    .setAlbumArtist(playlist.groupArtist)
                    .setArtworkUri(if(useFileProviderUri)
                        getFileProviderUri(appContext, playlist.artFileCustom)
                    else playlist.artFileCustom.toUri()
                    )
                    .setDescription("${playlist.creationTimestamp}:${playlist.lastModificationTimestamp}")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(playlist.groupTitle)
                    //.setSubtitle(playlist.searchDescription) //TODO song length?
                    .build()
            )
            .build()
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
        Timber.d("createMediaItemFromSongEntity: song=$song, position=$position, playlistTitle=$playlistTitle")
        val mediaId = if(position != null && songGroupType != null) {
            "songGroupType=${songGroupType.name}|||groupTitle=${ if(playlistTitle != null) playlistTitle else song.albumTitle}|||position=$position|||songTitle=${song.name}"
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
     * Given a mediaId in the form of "songGroupType=${songGroupType.name}||| groupTitle=${ if(playlistTitle != null) playlistTitle else song.albumTitle}||| position=$position, songTitle=${song.name}"
     * Determine a certain field.
     */
    private fun determineFieldFromMediaId(mediaId: String, field: String): String {
        val fields = mediaId.split("|||")
        val fieldIndex = fields.indexOfFirst { it.contains(field) }

        if(fieldIndex > -1) {
            val checkField = fields[fieldIndex]

            return checkField.removePrefix(field)
        }

        return ""
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
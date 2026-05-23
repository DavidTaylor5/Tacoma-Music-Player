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

/**
 * Hilt-injectable converter between Room entities / legacy data classes and Media3
 * [MediaItem] objects.
 *
 * Also encodes and decodes the structured media IDs used by `MusicService` for Android Auto
 * browsing. When a song is prepared for Android Auto playback, its media ID is a
 * `|||`-delimited string:
 * ```
 * songGroupType=<type>|||groupTitle=<title>|||position=<index>|||songTitle=<name>
 * ```
 * [getAndroidAutoPlayDataFromMediaItem] and [determineFieldFromMediaId] handle the reverse
 * parse, reconstructing an [AndroidAutoPlayData] from that ID.
 *
 * @param appContext Application context used by [determineArtUri] and [getFileProviderUri]
 *   when building `content://` URIs for Android Auto.
 */
class MediaItemUtil @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    /**
     * Reconstructs the composite search key `"title_albumTitle_artist"` from a [MediaItem]'s
     * metadata fields.
     *
     * This key matches the `searchDescription` primary key format in [SongEntity] and is used
     * to find the Room entity that corresponds to a given [MediaItem].
     *
     * @param song The [MediaItem] whose search description should be derived.
     * @return A `"title_albumTitle_artist"` string suitable for a database lookup.
     */
    fun getSongSearchDescriptionFromMediaItem(song: MediaItem): String {
        val songInfo = song.mediaMetadata
        return "${songInfo.title}_${songInfo.albumTitle}_${songInfo.artist}"
    }

    /**
     * Creates a browsable, non-playable [MediaItem] representing an artist node in the
     * Android Auto content hierarchy.
     *
     * The media ID is prefixed with `"artist:"` so the service can distinguish artist nodes
     * from album and playlist nodes when handling `onGetChildren` callbacks.
     *
     * @param artist The display name of the artist.
     * @return A browsable [MediaItem] with media ID `"artist:<artist>"`.
     */
    fun createMediaItemFromArtist(artist: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId("artist:$artist")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(artist)
                    .build()
            )
            .build()
    }

    /**
     * Resolves the artwork [Uri] for [songGroup], choosing between the custom and original art
     * files and optionally wrapping the result in a [android.content.ContentProvider] `content://` URI.
     *
     * The FileProvider URI is required for Android Auto because the car-side process cannot
     * read `file://` URIs directly — it needs a `content://` URI with an explicit permission
     * grant. See [getFileProviderUri] for the grant details.
     *
     * @param songGroup The entity whose artwork fields are used for resolution.
     * @param useFileProviderUri When `true`, the returned URI is a `content://` FileProvider
     *   URI suitable for Android Auto. When `false` (default), a plain `file://` URI is returned.
     * @return The resolved artwork [Uri].
     */
    fun determineArtUri(
        songGroup: SongGroupEntity,
        useFileProviderUri: Boolean = false
    ): Uri {
        return if (useFileProviderUri) {
            if (songGroup.useCustomArt) {
                getFileProviderUri(appContext, songGroup.artFileCustom)
            } else {
                getFileProviderUri(appContext, songGroup.artFileOriginal)
            }
        } else {
            if (songGroup.useCustomArt) {
                songGroup.artFileCustom.toUri()
            } else {
                songGroup.artFileOriginal.toUri()
            }
        }
    }

    /**
     * Converts a [SongGroupEntity] of type ALBUM into a browsable [MediaItem].
     *
     * The media ID is prefixed with `"album:"` so downstream consumers (e.g., `MusicService`
     * and [removeMediaItemPrefix]) can identify album items among mixed lists.
     *
     * The `description` field is set to the current epoch millisecond timestamp. This acts as
     * a change token: DiffUtil detects a new description value as a payload update, triggering
     * artwork refreshes in the RecyclerView adapter without a full rebind.
     *
     * @param album The [SongGroupEntity] to convert.
     * @param useFileProviderUri When `true`, the artwork URI is wrapped in a FileProvider
     *   `content://` URI for Android Auto compatibility. Defaults to `false`.
     * @return A browsable, non-playable [MediaItem] with media ID `"album:<title>"`.
     */
    fun createAlbumMediaItemFromSongGroupEntity(
        album: SongGroupEntity,
        useFileProviderUri: Boolean = false
    ): MediaItem {
        val albumArtUri = if (useFileProviderUri) {
            if (album.useCustomArt) {
                getFileProviderUri(appContext, album.artFileCustom)
            } else {
                getFileProviderUri(appContext, album.artFileOriginal)
            }
        } else {
            if (album.useCustomArt && !album.artFileCustom.isEmpty()) {
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
                    // currentTimeMillis acts as a change token — a new value on each emission
                    // signals DiffUtil that the item has been updated, triggering artwork refreshes.
                    .setDescription("${System.currentTimeMillis()}")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(album.groupTitle)
                    .setSubtitle(album.groupArtist)
                    .build()
            )
            .build()
    }

    /**
     * Converts a [SongGroupEntity] of type PLAYLIST into a browsable [MediaItem].
     *
     * The `description` field encodes both timestamps as `"<creation>:<modification>"` so
     * that [SortingUtil.sortPlaylists] can sort by creation or last-modification date without
     * a separate database query. [SortingUtil] reads index `[0]` for creation and index `[1]`
     * for modification after splitting on `":"`.
     *
     * @param playlist The [SongGroupEntity] to convert.
     * @param useFileProviderUri When `true`, the artwork URI is wrapped in a FileProvider
     *   `content://` URI for Android Auto compatibility. Defaults to `false`.
     * @return A browsable, non-playable [MediaItem] with media ID `"playlist:<title>"`.
     */
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
                    .setArtworkUri(
                        if (useFileProviderUri)
                            getFileProviderUri(appContext, playlist.artFileCustom)
                        else playlist.artFileCustom.toUri()
                    )
                    // Encodes both timestamps so SortingUtil can sort by creation or modification
                    // date without an additional database query.
                    .setDescription("${playlist.creationTimestamp}:${playlist.lastModificationTimestamp}")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(playlist.groupTitle)
                    .build()
            )
            .build()
    }

    /**
     * Converts a [SongEntity] into a playable [MediaItem], with two possible media ID formats:
     *
     * - **Simple name** (UI use, default): the media ID is just [SongEntity.name]. Used when
     *   building the local RecyclerView queue.
     * - **Structured Android Auto ID**: when [useFileProviderUri] is `true` and both [position]
     *   and [songGroupType] are provided, the media ID is a `|||`-delimited string:
     *   `songGroupType=<type>|||groupTitle=<title>|||position=<index>|||songTitle=<name>`.
     *   This allows `MusicService.onPlayFromMediaId` to reconstruct playback context after
     *   Android Auto issues a play command with only the media ID.
     *
     * @param song The [SongEntity] to convert.
     * @param position The zero-based position within the parent group, required for the
     *   structured Android Auto ID. Pass `null` for a simple-name ID.
     * @param songGroupType The type of the parent group (ALBUM or PLAYLIST), required for the
     *   structured Android Auto ID. Pass `null` for a simple-name ID.
     * @param playlistTitle The playlist title to embed in the structured ID; if `null`, the
     *   song's album title is used. Only relevant when [useFileProviderUri] is `true`.
     * @param useFileProviderUri When `true`, generates the structured Android Auto media ID
     *   and wraps artwork in a FileProvider `content://` URI. Defaults to `false`.
     * @return A playable, non-browsable [MediaItem].
     */
    fun createMediaItemFromSongEntity(
        song: SongEntity,
        position: Int? = null,
        songGroupType: SongGroupType? = null,
        playlistTitle: String? = null,
        useFileProviderUri: Boolean = false
    ): MediaItem {
        Timber.d("createMediaItemFromSongEntity: song=$song, position=$position, playlistTitle=$playlistTitle")

        val mediaId = if (useFileProviderUri && position != null && songGroupType != null) {
            // Structured ID for Android Auto — encodes enough context to restore playback
            // without an additional database query in the service's onPlayFromMediaId callback.
            "songGroupType=${songGroupType.name}|||groupTitle=${if (playlistTitle != null) playlistTitle else song.albumTitle}|||position=$position|||songTitle=${song.name}"
        } else {
            song.name
        }

        val artFile = if (song.useCustomArt) song.artFileCustom else song.artFileOriginal

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
                    .setArtworkUri(
                        if (useFileProviderUri)
                            getFileProviderUri(appContext, artFile)
                        else artFile.toUri()
                    )
                    .setDescription(song.songDuration)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setSubtitle(song.searchDescription)
                    .build()
            )
            .build()
    }

    /**
     * Parses a structured Android Auto media ID back into an [AndroidAutoPlayData] object.
     *
     * Delegates field extraction to [determineFieldFromMediaId] for each of the four fields
     * encoded in the `|||`-delimited ID. An unrecognised or missing `position` field defaults
     * to `0` via [String.toIntOrNull].
     *
     * @param mediaItem The [MediaItem] whose media ID should be parsed.
     * @return An [AndroidAutoPlayData] containing the decoded playback context; fields are
     *   empty strings or `0` if the ID was not in the structured Android Auto format.
     */
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
     * Extracts a single named field from a `|||`-delimited structured media ID.
     *
     * The expected format is:
     * ```
     * songGroupType=<type>|||groupTitle=<title>|||position=<index>|||songTitle=<name>
     * ```
     * The ID is split on `"|||"`. The segment whose text contains [field] is identified with
     * `indexOfFirst`, then [field] is stripped as a prefix via [String.removePrefix] to yield
     * the bare value.
     *
     * @param mediaId The full structured media ID string.
     * @param field The key prefix to search for, including its trailing `"="` (e.g.,
     *   `"groupTitle="`).
     * @return The extracted field value, or an empty string if the field is not present.
     */
    private fun determineFieldFromMediaId(mediaId: String, field: String): String {
        val fields = mediaId.split("|||")
        val fieldIndex = fields.indexOfFirst { it.contains(field) }

        if (fieldIndex > -1) {
            val checkField = fields[fieldIndex]
            return checkField.removePrefix(field)
        }

        return ""
    }

    /**
     * Strips the type prefix before the first `":"` from a media item ID.
     *
     * For example: `"album:Dark Side of the Moon"` → `"Dark Side of the Moon"`,
     * `"playlist:My Mix"` → `"My Mix"`. If no `":"` is found the original ID is returned
     * unchanged.
     *
     * @param mediaItemId The full prefixed media item ID string.
     * @return The portion of the ID after the first `":"`, or [mediaItemId] if no `":"` exists.
     */
    fun removeMediaItemPrefix(
        mediaItemId: String
    ): String {
        val prefixEnd = mediaItemId.indexOfFirst { char ->
            char == ':'
        }

        return if (prefixEnd != -1) mediaItemId.substring(prefixEnd + 1) else mediaItemId
    }

    /**
     * Converts a legacy [SongData] snapshot (persisted via Moshi to Room) into a playable
     * [MediaItem].
     *
     * The media ID is set to [SongData.songUri] (the string form of the audio file URI) so
     * that a restored queue item can be matched back to its source file without an additional
     * database lookup. This differs from [createMediaItemFromSongEntity], which uses
     * [SongEntity.name] as the media ID.
     *
     * @param song The [SongData] snapshot to convert.
     * @return A playable, non-browsable [MediaItem] whose media ID is the audio file URI string.
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
     * Creates a browsable [MediaItem] representing an album from primitive fields.
     *
     * Used by [MediaStoreUtil.queryAvailableAlbums] to build album items directly from
     * MediaStore cursor data without a Room entity. The media ID is set to [albumTitle]
     * (no prefix), so callers that need prefix-stripping should use
     * [createAlbumMediaItemFromSongGroupEntity] instead.
     *
     * @param albumTitle The album title. Defaults to `"UNKNOWN ALBUM"`.
     * @param artist The album artist. Defaults to `"UNKNOWN ARTIST"`.
     * @param artworkUri The MediaStore `content://` artwork URI for this album.
     *   Defaults to [Uri.EMPTY].
     * @param releaseYear The four-digit release year, or `0` if not available. Defaults to `0`.
     * @return A browsable, non-playable [MediaItem] of type [MediaMetadata.MEDIA_TYPE_ALBUM].
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

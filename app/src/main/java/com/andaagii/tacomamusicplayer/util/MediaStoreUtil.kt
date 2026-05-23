package com.andaagii.tacomamusicplayer.util

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.data.SongData
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Queries [android.provider.MediaStore] for audio content on the device.
 *
 * MediaStore is the Android-recommended abstraction layer for on-device media files. Using
 * it instead of direct file-system access lets the app request only the scoped permissions
 * required (`READ_MEDIA_AUDIO` on API ≥ 33, `READ_EXTERNAL_STORAGE` on older devices) while
 * remaining compliant with Android's storage security model.
 *
 * Two public query methods are exposed:
 * - [querySongsFromAlbum] — fetches the track list for a specific album title.
 * - [queryAvailableAlbums] — fetches every distinct album available on the device.
 *
 * **Permission prerequisite:** the calling context must already hold the appropriate read
 * permission before calling either method; no permission checks are performed internally.
 */
class MediaStoreUtil @Inject constructor(
    private val mediaItemUtil: MediaItemUtil
) {

    /**
     * Returns all tracks belonging to [album] as playable [MediaItem] objects.
     *
     * The query is a two-phase operation:
     * 1. The `Albums` table is queried first to resolve the numeric `ALBUM_ID` for [album].
     *    MediaStore's `Audio.Media` table stores artwork under a per-album `content://` URI
     *    constructed from this ID, so the album-level query is needed to obtain it.
     * 2. The `Audio.Media` table is queried with `ALBUM = ?` to retrieve the actual tracks.
     *
     * Song URIs are normalised via `Uri.fromFile(File(url))` rather than using the raw path
     * string. Paths containing `#` or `!` characters cause ExoPlayer to misparse the URI,
     * treating them as fragment or authority delimiters; `fromFile` percent-encodes these
     * characters, producing a safe `file://` URI.
     *
     * @param context Application context; must hold `READ_MEDIA_AUDIO` or
     *   `READ_EXTERNAL_STORAGE` permission.
     * @param album The exact album title as stored in MediaStore.
     * @return A list of playable [MediaItem] objects for each track in the album.
     */
    fun querySongsFromAlbum(context: Context, album: String): List<MediaItem> {
        Timber.d("querySongsFromAlbum: ")

        val albumSongs = mutableListOf<MediaItem>()

        val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val uriAlbum = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        var albumIdCheck = 0L

        // Phase 1 — resolve the album's numeric ID from the Albums table.
        // This ID is used later to build the per-album artwork content URI.
        val albumIdProjection: Array<String?> = arrayOf(
            MediaStore.Audio.Albums.ALBUM_ID,
        )

        context.contentResolver.query(
            uriAlbum,
            albumIdProjection,
            "${MediaStore.Audio.Albums.ALBUM} = ?",
            arrayOf(album),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                albumIdCheck = cursor.getLong(0)
            }
        }

        // Phase 2 — fetch all tracks whose ALBUM column matches the requested album title.
        val projection: Array<String?> = arrayOf(
            MediaStore.Audio.AudioColumns.DATA,     // 0 → file path (used to build safe URI)
            MediaStore.Audio.AudioColumns.TITLE,    // 1 → song title
            MediaStore.Audio.AudioColumns.ALBUM,    // 2 → album title
            MediaStore.Audio.ArtistColumns.ARTIST,  // 3 → artist
            MediaStore.Audio.AudioColumns.DURATION, // 4 → duration in milliseconds
            MediaStore.Audio.AudioColumns.TRACK,    // 5 → track number within the album
            MediaStore.Audio.AudioColumns._ID,      // 6 → song row ID
            MediaStore.Audio.AudioColumns.ALBUM_ID, // 7 → album row ID
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
                    "querySongsFromAlbum: ${cursor.getString(0)}, ${cursor.getString(1)}, " +
                    "${cursor.getString(2)}, ${cursor.getString(3)}, ${cursor.getString(4)}, " +
                    "${cursor.getString(5)}, ${cursor.getString(6)}"
                )

                val url = cursor.getString(0)

                // Normalise the path to a safe file:// URI — raw paths with '#' or '!'
                // cause ExoPlayer to misparse the URI, skipping or corrupting the audio source.
                val fixUrl = Uri.fromFile(File(url))

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, fixUrl)

                val title = cursor.getString(1)
                val albumTitle = cursor.getString(2)
                val artist = cursor.getString(3)
                val duration = cursor.getString(4)
                val songId = cursor.getLong(6)
                val albumId = cursor.getLong(7)

                val artworkUri = ContentUris.withAppendedId(uriExternal, songId)

                val songMediaItem = mediaItemUtil.createMediaItemFromSongData(
                    SongData(
                        songUri = fixUrl.toString(),
                        songTitle = title,
                        albumTitle = albumTitle,
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
     * Returns every distinct album on the device as browsable [MediaItem] objects.
     *
     * Each field is extracted inside its own `try/catch` block because MediaStore rows can
     * return `null` for any column (e.g., a ripped file may have no artist metadata). Swallowing
     * per-field exceptions ensures one corrupt row does not abort the entire scan.
     *
     * After all rows are processed, duplicate album titles are filtered: MediaStore can return
     * multiple rows for the same album title (e.g., different disc numbers or variant releases).
     * Only the first occurrence is added so each album appears exactly once in the UI.
     *
     * @param context Application context; must hold `READ_MEDIA_AUDIO` or
     *   `READ_EXTERNAL_STORAGE` permission.
     * @return A mutable list of browsable [MediaItem] objects, one per distinct album title.
     */
    fun queryAvailableAlbums(context: Context): MutableList<MediaItem> {
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
                    "queryAvailableAlbums: ${cursor.getString(0)}, ${cursor.getString(1)}, " +
                    "${cursor.getString(2)}"
                )

                var albumId = 0L
                var albumTitle = ""
                var artist = ""
                var releaseYear = 0

                // Each field is wrapped individually — a null or malformed value in one column
                // must not prevent the remaining fields from being extracted for this row.
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

                Timber.d("ALBUMART>>> artworkUri=$artworkUri")

                val albumMediaItem =
                    mediaItemUtil.createAlbumMediaItem(albumTitle, artist, artworkUri, releaseYear)

                // Skip duplicate album titles — MediaStore can list the same album multiple
                // times (e.g., different disc numbers). Only the first occurrence is kept so
                // each album appears exactly once in the UI.
                if (albumList.map { it.mediaMetadata.albumTitle }.contains(albumTitle)) {
                    Timber.d("queryAvailableAlbums: albumTitle found in albumList, skipping duplicate.")
                } else {
                    Timber.d("queryAvailableAlbums: Adding albumTitle=$albumTitle to albumList")
                    albumList.add(albumMediaItem)
                }
            }
        }
        Timber.d("queryAvailableAlbums: DONE SEARCHING!")

        return albumList
    }
}

/*
Reference: MediaStore selection examples

This selector returns songs by duration (e.g., tracks ≥ 5 minutes):
    "${MediaStore.Audio.AudioColumns.DURATION} >= ?",
    arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString())

This selector returns all songs on a specific album:
    "${MediaStore.Audio.AudioColumns.ALBUM} = ?",
    arrayOf("Liquid Swords [Explicit]")
*/

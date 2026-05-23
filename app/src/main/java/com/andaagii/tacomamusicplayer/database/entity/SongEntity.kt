package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity representing a single playable audio track in `song_table`.
 *
 * The primary key [searchDescription] is a composite string in the format
 * `"songName_albumTitle_artist"`, constructed during library cataloging by
 * `CatalogMusicWorker`. Using a composite string key allows exact lookups without a
 * separate numeric surrogate key and doubles as the search target for full-text queries.
 *
 * Indices on [artist], [albumTitle], and [name] keep the most common browsing and
 * filtering queries efficient as the library grows.
 *
 * Artwork fields mirror the structure of `ArtInfo` and are resolved to a displayable URI
 * by `MediaItemUtil.determineArtUri` rather than being read directly by consumers.
 *
 * @param albumTitle Display title of the album this track belongs to.
 * @param artist Track artist name.
 * @param searchDescription Composite primary key in the format `"songName_albumTitle_artist"`.
 *   Used by the repository to locate a specific track and as the target for search queries.
 * @param name Track display title.
 * @param uri MediaStore content URI string identifying the audio file on device storage
 *   (e.g., `"content://media/external/audio/media/42"`).
 * @param songDuration Track duration as a formatted display string (e.g., `"3:45"`).
 * @param artFileOriginal Absolute file-system path to the cover art extracted from MediaStore
 *   during cataloging. Empty string if no embedded art was found.
 * @param artFileCustom Absolute file-system path to a user-uploaded image cropped via uCrop.
 *   Empty string when no custom art has been set for this track's album.
 * @param useCustomArt `true` to display [artFileCustom] instead of [artFileOriginal].
 */
@Entity(
    tableName = "song_table",
    indices = [
        Index(value = ["song_artist"]),
        Index(value = ["album_title"]),
        Index(value = ["song_name"])
    ]
)
data class SongEntity(
    @ColumnInfo(name = "album_title") val albumTitle: String,
    @ColumnInfo(name = "song_artist") val artist: String,
    @PrimaryKey
    @ColumnInfo(name = "search_description") val searchDescription: String,
    @ColumnInfo(name = "song_name") val name: String,
    @ColumnInfo(name = "song_uri") val uri: String,
    @ColumnInfo(name = "song_duration") val songDuration: String,
    @ColumnInfo(name = "art_file_original") val artFileOriginal: String = "",
    @ColumnInfo(name = "art_file_custom") val artFileCustom: String = "",
    @ColumnInfo(name = "use_custom_art") val useCustomArt: Boolean = false,
)

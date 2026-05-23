package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

/**
 * Room entity representing an album or playlist header in `song_group_table`.
 *
 * Acts as the parent in a one-to-many relationship with [SongEntity] rows, linked through
 * [SongGroupCrossRefEntity]. The [songGroupType] field distinguishes albums (discovered from
 * MediaStore by `CatalogMusicWorker`) from user-created playlists.
 *
 * [searchDescription] is a composite string used as the target for full-text search queries.
 * It follows the pattern `"groupTitle_groupArtist"` and is matched case-insensitively by
 * `SongGroupDao.findDescriptionFromSearchStr`.
 *
 * Timestamp fields ([creationTimestamp], [lastModificationTimestamp]) are ISO-formatted
 * `LocalDateTime` strings updated by `MusicRepositoryImpl` whenever a playlist is modified.
 *
 * Indices on [groupTitle] and [groupArtist] keep browsing and artist-filter queries fast.
 *
 * @param groupId Auto-generated integer primary key assigned by Room on first insert.
 * @param songGroupType Whether this group is a [SongGroupType.ALBUM] or
 *   [SongGroupType.PLAYLIST], controlling display behaviour and available menu options.
 * @param artFileOriginal Absolute path to cover art extracted from MediaStore. Empty when absent.
 * @param artFileCustom Absolute path to user-uploaded cover art cropped via uCrop.
 *   Empty when no custom art has been set.
 * @param useCustomArt `true` to display [artFileCustom] instead of [artFileOriginal].
 * @param groupTitle Display title of the album or playlist.
 * @param groupArtist Primary artist for the group. `null` for playlists, which may span
 *   multiple artists and have no single attributed artist.
 * @param searchDescription Composite search key, typically `"groupTitle_groupArtist"`.
 * @param groupDuration Total playback duration of all tracks in the group as a display string.
 *   `null` when not yet computed.
 * @param releaseYear Album release year as a string. Empty for playlists and for albums
 *   where MediaStore provides no year metadata.
 * @param creationTimestamp Set once when the group is first inserted. Used to sort playlists
 *   by creation date.
 * @param lastModificationTimestamp Updated whenever tracks are added, removed, or reordered
 *   within a playlist.
 */
@Entity(
    tableName = "song_group_table",
    indices = [
        Index(value = ["group_artist"]),
        Index(value = ["group_title"]),
    ]
)
data class SongGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val groupId: Int = 0,
    @ColumnInfo(name = "song_group_type") val songGroupType: SongGroupType,
    @ColumnInfo(name = "art_file_original") val artFileOriginal: String = "",
    @ColumnInfo(name = "art_file_custom") val artFileCustom: String = "",
    @ColumnInfo(name = "use_custom_art") val useCustomArt: Boolean = false,
    @ColumnInfo(name = "group_title") val groupTitle: String,
    @ColumnInfo(name = "group_artist") val groupArtist: String?,
    @ColumnInfo(name = "search_description") val searchDescription: String,
    @ColumnInfo(name = "group_duration") val groupDuration: String?,

    @ColumnInfo(name = "release_year") val releaseYear: String = "",

    @ColumnInfo(name = "creation_timestamp") var creationTimestamp: String = "",
    @ColumnInfo(name = "last_modification_timestamp") var lastModificationTimestamp: String = "",
)

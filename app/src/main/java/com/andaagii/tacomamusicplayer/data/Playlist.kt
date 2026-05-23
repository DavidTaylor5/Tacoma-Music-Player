package com.andaagii.tacomamusicplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Legacy Room entity representing a playlist stored as a serialised JSON blob.
 *
 * **Superseded.** This entity is replaced by `SongGroupEntity` + `SongGroupCrossRefEntity`,
 * which normalise the relationship between playlists and songs into proper relational tables.
 * This class is kept in the codebase for reference only and is no longer written to
 * or read from at runtime.
 *
 * Timestamp fields ([creationTimestamp], [lastModificationTimestamp]) are string
 * representations of `LocalDateTime` objects.
 */
@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "playlist_title") val title: String,
    @ColumnInfo(name = "playlist_art_file") val artFile: String?,
    @ColumnInfo(name = "playlist_songs") var songs: PlaylistData,
    @ColumnInfo(name = "creation_timestamp") var creationTimestamp: String,
    @ColumnInfo(name = "last_modification_timestamp") var lastModificationTimestamp: String,
)

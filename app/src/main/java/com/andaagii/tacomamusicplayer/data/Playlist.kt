package com.andaagii.tacomamusicplayer.data
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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

//TimeStamps will be string representations of LocalDateTime data objects.
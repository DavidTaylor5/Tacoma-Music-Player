package com.example.tacomamusicplayer.data
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "playlist_title") val title: String?,
    @ColumnInfo(name = "playlist_songs") val songs: PlaylistData
)
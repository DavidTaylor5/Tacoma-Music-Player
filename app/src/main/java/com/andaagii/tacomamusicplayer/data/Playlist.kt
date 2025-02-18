package com.andaagii.tacomamusicplayer.data
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "playlist_title") val title: String,
    @ColumnInfo(name = "playlist_art_file") val artFile: String?,
    @ColumnInfo(name = "playlist_songs") var songs: PlaylistData
)
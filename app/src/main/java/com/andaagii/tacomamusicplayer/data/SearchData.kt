package com.andaagii.tacomamusicplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SearchData(
    @PrimaryKey
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "songTitle") val songTitle: String = "",
    @ColumnInfo(name = "albumTitle") val albumTitle: String = "",
    @ColumnInfo(name = "artist") val artist: String = "",
    @ColumnInfo(name = "is_album") val isAlbum: Boolean = false,
    @ColumnInfo(name = "song_uri") val songUri: String = "",
    @ColumnInfo(name = "artworkUri") val artworkUri: String = ""
)

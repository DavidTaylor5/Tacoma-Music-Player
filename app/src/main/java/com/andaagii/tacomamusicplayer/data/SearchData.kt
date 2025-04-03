package com.andaagii.tacomamusicplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SearchData(
    @PrimaryKey
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "is_album") val isAlbum: Boolean = false,
    @ColumnInfo(name = "art_uri") val artUri: String = ""
)

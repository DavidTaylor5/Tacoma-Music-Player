package com.andaagii.tacomamusicplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//TODO add an optional bitmap to the database?
// Save the bitmap to app storage // save a reference to it..
//TODO move the saving songs to the background
//TODO Use workmanager to check albums, update what songs are available, save the references to images.
// TODO move both DAOs to the same database

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

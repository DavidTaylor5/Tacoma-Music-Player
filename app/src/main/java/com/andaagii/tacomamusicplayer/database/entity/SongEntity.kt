package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_table")
data class SongEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 0,
    @ColumnInfo(name = "album_title") val albumTitle: String,
    @ColumnInfo(name = "song_artist") val artist: String,
    @PrimaryKey
    @ColumnInfo(name = "search_description") val searchDescription: String,
    @ColumnInfo(name = "song_name") val name: String,
    @ColumnInfo(name = "song_uri") val uri: String,
    @ColumnInfo(name = "song_duration") val songDuration: String,
)
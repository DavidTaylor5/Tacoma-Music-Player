package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

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
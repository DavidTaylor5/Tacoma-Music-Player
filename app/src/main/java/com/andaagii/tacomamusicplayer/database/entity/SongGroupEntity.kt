package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

@Entity(tableName = "song_group_table")
data class SongGroupEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 0,
    @ColumnInfo(name = "song_group_type") val songGroupType: SongGroupType,
    @ColumnInfo(name = "art_file") val artFile: String?,
    @PrimaryKey
    @ColumnInfo(name = "group_title") val groupTitle: String,
    @ColumnInfo(name = "group_artist") val groupArtist: String?,
    @ColumnInfo(name = "search_description") val searchDescription: String,
    @ColumnInfo(name = "group_duration") val groupDuration: String?,

    @ColumnInfo(name = "release_year") val releaseYear: String = "",

    @ColumnInfo(name = "creation_timestamp") var creationTimestamp: String = "",
    @ColumnInfo(name = "last_modification_timestamp") var lastModificationTimestamp: String = "",
)
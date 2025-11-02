package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_ref_table",
    foreignKeys = [
        ForeignKey(
            entity = SongGroupEntity::class,
            parentColumns = ["group_title"],
            childColumns = ["groupTitle"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["search_description"],
            childColumns = ["searchDescription"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupTitle"]),
        Index(value = ["searchDescription"])
    ]
)
data class SongGroupCrossRefEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupTitle: String,
    val searchDescription: String,
    val position: Int
)

//Position 100 Increments, swapping places will be between two songs 100 200 -> 150
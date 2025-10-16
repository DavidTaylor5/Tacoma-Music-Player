package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "song_ref_table",
    primaryKeys = ["groupTitle", "searchDescription"],
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
    val groupTitle: Long,
    val searchDescription: Long
)
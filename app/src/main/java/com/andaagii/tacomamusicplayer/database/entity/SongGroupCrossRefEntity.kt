package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "song_ref_table",
    primaryKeys = ["songGroupId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = SongGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["songGroupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongGroupCrossRefEntity (
    val songGroupId: Long,
    val songId: Long
)
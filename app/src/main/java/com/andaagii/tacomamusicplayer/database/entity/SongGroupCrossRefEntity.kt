package com.andaagii.tacomamusicplayer.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Junction table entity linking a [SongGroupEntity] to its member [SongEntity] rows
 * with a stable playback position ordering (`song_ref_table`).
 *
 * Both foreign keys use `CASCADE` delete: removing a [SongGroupEntity] or a [SongEntity]
 * automatically removes all cross-reference rows for that parent, preventing orphaned records
 * without requiring explicit cleanup in the repository layer.
 *
 * **Position scheme:** positions are assigned in increments of 100 (100, 200, 300, …)
 * rather than consecutive integers. When a track is moved between two adjacent tracks, its
 * new position is set to the midpoint of the surrounding positions (e.g., inserting between
 * positions 100 and 200 yields 150). This avoids renumbering the entire list on every drag
 * reorder. All queries that respect playback order must sort by [position] `ASC`.
 *
 * @param id Auto-generated integer primary key.
 * @param groupId Foreign key referencing [SongGroupEntity.groupId]. Cascade-deleted when
 *   the parent group is removed.
 * @param searchDescription Foreign key referencing [SongEntity.searchDescription].
 *   Cascade-deleted when the referenced song is removed from the library.
 * @param position Ordering value for the track within its group. Assigned in increments
 *   of 100; midpoint values are used for reorders to avoid full-list renumbering.
 */
@Entity(
    tableName = "song_ref_table",
    foreignKeys = [
        ForeignKey(
            entity = SongGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
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
        Index(value = ["groupId"]),
        Index(value = ["searchDescription"])
    ]
)
data class SongGroupCrossRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val searchDescription: String,
    val position: Int
)

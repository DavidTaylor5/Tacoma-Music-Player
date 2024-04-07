package com.example.tacomamusicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val album: String,
    val duration: Int
)

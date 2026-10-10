package com.andaagii.tacomamusicplayer.manager.state

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.data.SongData

data class PositionState(
    val queue: List<MediaItem> = listOf(),
    val unShuffledQueue: List<MediaItem> = listOf(),
    val songPosition: Int = 0,
    val currentlyPlayingSong: SongData? = null
)

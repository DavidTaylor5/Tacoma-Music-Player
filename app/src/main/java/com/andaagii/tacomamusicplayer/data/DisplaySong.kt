package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem

data class DisplaySong(
    val mediaItem: MediaItem,
    var showPlayIndicator: Boolean,
)

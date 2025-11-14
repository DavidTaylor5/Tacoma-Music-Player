package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem

data class SearchData(
    val songs: List<MediaItem>,
    val albums: List<MediaItem>,
    val playlists: List<MediaItem>
)

package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enum.SongGroupType

/**
 * Song group representation, ex Playlist or Album.
 */
data class SongGroup(
    val type: SongGroupType = SongGroupType.PLAYLIST,
    var songs: List<MediaItem>,
    val title: String,
)

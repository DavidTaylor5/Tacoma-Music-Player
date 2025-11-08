package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

/**
 * Song group representation, ex Playlist Album, Search list.
 */
data class SongGroup(
    val type: SongGroupType = SongGroupType.PLAYLIST,
    var songs: List<MediaItem>,
    val group: MediaItem,
)

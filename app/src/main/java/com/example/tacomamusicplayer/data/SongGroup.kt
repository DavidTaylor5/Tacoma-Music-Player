package com.example.tacomamusicplayer.data

import androidx.media3.common.MediaItem
import com.example.tacomamusicplayer.enum.SongGroupType

/**
 * Song group representation, ex Playlist or Album.
 */
data class SongGroup(
    val type: SongGroupType = SongGroupType.PLAYLIST,
    val songs: List<MediaItem>,
    val title: String,
)

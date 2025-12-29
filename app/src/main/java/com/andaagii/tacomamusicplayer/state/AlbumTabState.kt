package com.andaagii.tacomamusicplayer.state

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.util.SortingUtil

data class AlbumTabState(
    val albums: List<MediaItem>,
    val sorting: SortingUtil.SortingOption,
    val layout: LayoutType
)
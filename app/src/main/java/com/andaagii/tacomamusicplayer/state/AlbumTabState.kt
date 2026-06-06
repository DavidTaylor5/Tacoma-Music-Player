package com.andaagii.tacomamusicplayer.state

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.util.SortingUtil

/**
 * Single [StateFlow] payload for the albums browsing page.
 *
 * Bundling [albums], [sorting], and [layout] into one object ensures that
 * [com.andaagii.tacomamusicplayer.composables.AlbumListScreen] reacts to all three
 * pieces of state atomically — a sort change and a layout change emitted together produce
 * exactly one UI update rather than two.
 *
 * @param albums The current sorted list of album [MediaItem]s to display.
 * @param sorting The active sort order applied to [albums].
 * @param layout Whether to render [albums] in a linear list or a two-column grid.
 */
data class AlbumTabState(
    val albums: List<MediaItem>,
    val sorting: SortingUtil.SortingOption,
    val layout: LayoutType
)
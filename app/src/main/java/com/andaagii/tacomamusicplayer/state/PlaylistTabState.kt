package com.andaagii.tacomamusicplayer.state

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.util.SortingUtil

/**
 * Single [StateFlow] payload for the playlists browsing page.
 *
 * Mirrors [com.andaagii.tacomamusicplayer.state.AlbumTabState] for playlists: bundling the list,
 * sort order, and layout together means [com.andaagii.tacomamusicplayer.composables.PlaylistScreen]
 * always receives a consistent snapshot and redraws exactly once per state change.
 *
 * @param playlists The current sorted list of playlist [MediaItem]s to display.
 * @param sorting The active sort order applied to [playlists].
 * @param layout Whether to render [playlists] in a linear list or a two-column grid.
 */
data class PlaylistTabState(
    val playlists: List<MediaItem>,
    val sorting: SortingUtil.SortingOption,
    val layout: LayoutType
)

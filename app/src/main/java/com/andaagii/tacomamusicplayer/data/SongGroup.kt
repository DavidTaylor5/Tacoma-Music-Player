package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

/**
 * Self-contained browsable unit that pairs an album or playlist header with its track list.
 *
 * `MainViewModel` constructs a [SongGroup] whenever the user taps an album or playlist,
 * bundling the header metadata and ordered tracks into a single object. This drives both
 * the song-list display (via `SongListFragment`) and queue construction
 * (via `MainViewModel.playSongGroupAtPosition`), avoiding separate lookups at each call site.
 *
 * @param type The category of this group — [SongGroupType.ALBUM], [SongGroupType.PLAYLIST],
 *   [SongGroupType.SEARCH_LIST], etc. — used to determine which menu options and display
 *   behaviours apply.
 * @param songs Ordered list of track [MediaItem]s belonging to this group. Declared `var`
 *   so the queue can be shuffled in place without allocating a new [SongGroup].
 * @param group [MediaItem] representing the group header. Its [MediaMetadata] carries the
 *   album or playlist title, artist, and artwork URI used by header views.
 */
data class SongGroup(
    val type: SongGroupType = SongGroupType.PLAYLIST,
    var songs: List<MediaItem>,
    val group: MediaItem,
)

package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem

/**
 * Display-layer wrapper around a [MediaItem] for a single row in the current-queue list.
 *
 * The [showPlayIndicator] flag is kept separate from the [MediaItem] so that
 * `QueueListAdapter` can toggle the animated playing indicator on the active track
 * without mutating the underlying media object or triggering unnecessary list diffs.
 *
 * @param mediaItem The Media3 item representing the track, including all metadata and the
 *   content URI required for playback.
 * @param showPlayIndicator `true` when this row is the currently playing track and the
 *   animated playing indicator should be visible.
 */
data class DisplaySong(
    val mediaItem: MediaItem,
    var showPlayIndicator: Boolean,
)

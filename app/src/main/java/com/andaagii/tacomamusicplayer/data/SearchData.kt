package com.andaagii.tacomamusicplayer.data

import androidx.media3.common.MediaItem

/**
 * Aggregated result buckets returned by `MusicRepository.searchMusic`.
 *
 * Results are pre-partitioned by category so the search UI can render each section
 * independently without an additional grouping pass on the collected list. Within each
 * bucket, items are ordered by match position — titles that begin with the query string
 * appear before titles that contain it further along.
 *
 * Each list is capped at 25 entries as enforced by the repository query.
 *
 * @param songs [MediaItem]s whose track title matches the search query.
 * @param albums [MediaItem]s whose album title matches the search query.
 * @param playlists [MediaItem]s whose playlist title matches the search query.
 */
data class SearchData(
    val songs: List<MediaItem>,
    val albums: List<MediaItem>,
    val playlists: List<MediaItem>
)

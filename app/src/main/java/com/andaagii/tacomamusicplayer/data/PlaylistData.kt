package com.andaagii.tacomamusicplayer.data

import com.squareup.moshi.JsonClass

/**
 * Legacy Moshi-serialisable wrapper used by the [Playlist] entity to persist a song
 * list as a JSON column in the Room database.
 *
 * **Superseded.** No longer written to or read from at runtime. Kept alongside [Playlist]
 * for reference only.
 *
 * @param songs Ordered list of tracks belonging to the playlist at the time it was saved.
 */
@JsonClass(generateAdapter = true)
data class PlaylistData(
    val songs: List<SongData> = listOf()
)

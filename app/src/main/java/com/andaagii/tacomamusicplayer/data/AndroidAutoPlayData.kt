package com.andaagii.tacomamusicplayer.data

import com.andaagii.tacomamusicplayer.enumtype.SongGroupType

/**
 * Parsed result of an Android Auto playback request, decoded from a structured media item ID.
 *
 * Android Auto communicates playback intent by embedding a structured ID in the [MediaItem]
 * it sends to `MusicService.onAddMediaItems`. This class captures the decoded fields so the
 * service can locate the correct group in the library and seek to the right track.
 *
 * A `null` [songGroupType] indicates the ID could not be matched to a known group type.
 *
 * @param songGroupType The category of the requested group ([SongGroupType.ALBUM],
 *   [SongGroupType.PLAYLIST], etc.), or `null` if the media item ID format was unrecognised.
 * @param groupTitle Display title of the album, artist, or playlist to load.
 * @param position Zero-based index of the track to begin playback from within the group.
 * @param songTitle Display title of the individual track requested by Auto, used as a
 *   fallback when [position] alone is ambiguous.
 */
data class AndroidAutoPlayData(
    val songGroupType: SongGroupType? = null,
    val groupTitle: String = "",
    val position: Int = 0,
    val songTitle: String = ""
)

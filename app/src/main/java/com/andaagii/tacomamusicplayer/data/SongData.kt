package com.andaagii.tacomamusicplayer.data

import com.squareup.moshi.JsonClass

/**
 * Moshi-serialisable snapshot of a single track used for queue persistence across restarts.
 *
 * The current playback queue is serialised into a [PlaylistData] JSON blob and stored in
 * the Room database under the special playlist title defined by `Const.PLAYLIST_QUEUE_TITLE`.
 * On the next app launch, `MainViewModel` reads this blob and reconstructs the queue,
 * allowing playback to resume from where the user left off.
 *
 * All fields are plain `String` values rather than typed equivalents (e.g., `Uri`, `Long`)
 * because Moshi cannot serialise Android's `Uri` type natively, and keeping a consistent
 * string-only contract simplifies the Moshi adapter generation.
 *
 * @param songUri MediaStore content URI string identifying the audio file
 *   (e.g., `"content://media/external/audio/media/42"`).
 * @param songTitle Display title of the track.
 * @param albumTitle Display title of the album this track belongs to.
 * @param artist Artist name for the track.
 * @param artworkUri File URI string pointing to the artwork image on device storage.
 * @param duration Track duration as a formatted display string (e.g., `"3:45"`).
 */
@JsonClass(generateAdapter = true)
data class SongData(
    val songUri: String,
    val songTitle: String,
    val albumTitle: String,
    val artist: String,
    val artworkUri: String,
    val duration: String,
) {
    companion object {

        /**
         * Returns `true` when [song] represents a missing or placeholder track.
         *
         * MediaStore can produce rows with incomplete metadata for audio files that lack
         * embedded tags. In those cases the title or album may be absent (empty string),
         * the literal string `"null"`, or the sentinel value `"UNKNOWN"`. Any of these
         * combinations signals that the track should be treated as invalid.
         *
         * @param song The [SongData] to evaluate, or `null` itself.
         * @return `true` if [song] is `null`, or if both its [SongData.songTitle] and
         *   [SongData.albumTitle] are empty, `"null"`, or `"UNKNOWN"`.
         */
        fun isNullSong(song: SongData?): Boolean {
            if (song == null) {
                return true
            }

            return (song.songTitle == "null" || song.songTitle.isEmpty() || song.songTitle == "UNKNOWN") &&
                (song.albumTitle == "null" || song.albumTitle.isEmpty() || song.albumTitle == "UNKNOWN")
        }
    }
}

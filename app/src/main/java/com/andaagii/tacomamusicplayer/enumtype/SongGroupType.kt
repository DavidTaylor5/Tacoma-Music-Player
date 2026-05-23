package com.andaagii.tacomamusicplayer.enumtype

/**
 * Categorises a collection of songs for display, queue construction, and database persistence.
 *
 * Used throughout the app to determine which UI behaviour, menu options, and repository
 * operations apply to a given group of tracks. Stored as its [toString] name in the
 * `SongGroupEntity.songGroupType` column and round-tripped via [determineSongGroupTypeFromString].
 */
enum class SongGroupType {

    /** A user-created playlist persisted as a `SongGroupEntity` with cross-reference rows. */
    PLAYLIST,

    /** An album discovered from MediaStore during library cataloging. */
    ALBUM,

    /**
     * A transient list of search results assembled at query time.
     * Never written to the database.
     */
    SEARCH_LIST,

    /**
     * The system-managed playback queue, stored as a special playlist under
     * `Const.PLAYLIST_QUEUE_TITLE`. Treated separately from user playlists in the UI.
     */
    QUEUE,

    /**
     * Sentinel value returned by [determineSongGroupTypeFromString] when the stored string
     * does not match any known type. Indicates corrupt or unrecognised persisted data.
     */
    UNKNOWN;

    companion object {

        /**
         * Deserialises a persisted string back to a [SongGroupType].
         *
         * Matches against each entry's [toString] name (e.g., `"PLAYLIST"`, `"ALBUM"`).
         * Returns [UNKNOWN] rather than throwing when no match is found, so callers can
         * handle unexpected database values without crashing.
         *
         * @param type The string previously stored via `SongGroupType.toString()`.
         * @return The matching [SongGroupType], or [UNKNOWN] if unrecognised.
         */
        fun determineSongGroupTypeFromString(type: String): SongGroupType {
            return when (type) {
                PLAYLIST.toString() -> PLAYLIST
                ALBUM.toString() -> ALBUM
                SEARCH_LIST.toString() -> SEARCH_LIST
                QUEUE.toString() -> QUEUE
                else -> UNKNOWN
            }
        }
    }
}

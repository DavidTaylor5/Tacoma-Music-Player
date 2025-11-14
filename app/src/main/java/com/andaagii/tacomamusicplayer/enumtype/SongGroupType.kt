package com.andaagii.tacomamusicplayer.enumtype

/**
 * Groups of songs can be categorized as either a playlist or an album.
 */
enum class SongGroupType {
    PLAYLIST,
    ALBUM,
    SEARCH_LIST,
    QUEUE,
    UNKNOWN;

    companion object {
        fun determineSongGroupTypeFromString(type: String): SongGroupType {
            return when(type) {
                PLAYLIST.toString() -> PLAYLIST
                ALBUM.toString() -> ALBUM
                SEARCH_LIST.toString() -> SEARCH_LIST
                QUEUE.toString() -> QUEUE
                else -> UNKNOWN
            }
        }
    }
}
package com.andaagii.tacomamusicplayer.enum

/**
 * Pages associated to choosing a song.
 */
enum class PageType {
    PLAYLIST_PAGE {
        override fun type(): Int {
            return 0
        }
    },
    ALBUM_PAGE {
        override fun type(): Int {
            return 1
        }
    },
    SONG_PAGE {
        override fun type(): Int {
            return 2
        }
    };

    abstract fun type(): Int

    companion object {
        fun determinePageFromPosition(position: Int): PageType {
            return when(position) {
                0 -> PLAYLIST_PAGE
                1 -> ALBUM_PAGE
                2 -> SONG_PAGE
                else -> PLAYLIST_PAGE
            }
        }
    }
}
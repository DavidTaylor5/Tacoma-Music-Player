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
}
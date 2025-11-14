package com.andaagii.tacomamusicplayer.enumtype

/**
 * Pages associated to choosing a song.
 */
enum class PageType {

    QUEUE_PAGE {
        override fun type(): Int {
            return 0
        }
    },

    PLAYER_PAGE {
        override fun type(): Int {
            return 1
        }
    },

    PLAYLIST_PAGE {
        override fun type(): Int {
            return 2
        }
    },
    ALBUM_PAGE {
        override fun type(): Int {
            return 3
        }
    },
    SONG_PAGE {
        override fun type(): Int {
            return 4
        }
    };

    abstract fun type(): Int

    companion object {
        fun determinePageFromPosition(position: Int): PageType {
            return when(position) {
                0 -> QUEUE_PAGE
                1 -> PLAYER_PAGE
                2 -> PLAYLIST_PAGE
                3 -> ALBUM_PAGE
                4 -> SONG_PAGE
                else -> QUEUE_PAGE
            }
        }
    }
}
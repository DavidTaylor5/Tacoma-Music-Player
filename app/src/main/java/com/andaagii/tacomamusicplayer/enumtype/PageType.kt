package com.andaagii.tacomamusicplayer.enumtype

/**
 * ViewPager2 page index mapping for the `PlayerDisplayFragment` swipe layout.
 *
 * Each entry corresponds to one page in the pager and the `Fragment` that occupies it.
 * The integer returned by [type] is the zero-based position used by `ViewPager2` and
 * `ScreenSlidePagerAdapter`. Use [determinePageFromPosition] to convert a raw scroll
 * position back to a typed value.
 *
 * Page order (left → right): Queue → Player → Playlist → Album → Song.
 */
enum class PageType {

    /** Index 0 — `CurrentQueueFragment`. Displays the active playback queue. */
    QUEUE_PAGE {
        override fun type(): Int = 0
    },

    /** Index 1 — `MusicPlayingFragment`. The mini/full player with controls and album art. */
    PLAYER_PAGE {
        override fun type(): Int = 1
    },

    /** Index 2 — `PlaylistFragment`. Browses user-created playlists. */
    PLAYLIST_PAGE {
        override fun type(): Int = 2
    },

    /** Index 3 — `AlbumListFragment`. Browses albums discovered from MediaStore. */
    ALBUM_PAGE {
        override fun type(): Int = 3
    },

    /** Index 4 — `SongListFragment`. Displays all tracks in the library. */
    SONG_PAGE {
        override fun type(): Int = 4
    };

    /** Returns the zero-based ViewPager2 position index for this page. */
    abstract fun type(): Int

    companion object {

        /**
         * Maps a ViewPager2 scroll position to the corresponding [PageType].
         *
         * Falls back to [QUEUE_PAGE] for any position outside the range 0–4, so callers
         * always receive a valid page rather than a null or exception.
         *
         * @param position The zero-based page index reported by the `ViewPager2`.
         * @return The matching [PageType], or [QUEUE_PAGE] if the position is out of range.
         */
        fun determinePageFromPosition(position: Int): PageType {
            return when (position) {
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

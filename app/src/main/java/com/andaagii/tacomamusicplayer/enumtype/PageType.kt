package com.andaagii.tacomamusicplayer.enumtype

/**
 * Page index mapping for the [com.andaagii.tacomamusicplayer.screen.MusicChooserScreen]
 * [HorizontalPager].
 *
 * Each entry corresponds to one page in the pager. The integer returned by [type] is the
 * zero-based position used by [HorizontalPager]. Use [determinePageFromPosition] to convert
 * a raw scroll position back to a typed value.
 *
 * Page order (left → right): Queue → Player → Playlist → Album → Song.
 */
enum class PageType {

    /** Index 0 — [com.andaagii.tacomamusicplayer.screen.CurrentQueueScreen]. Displays the active playback queue. */
    QUEUE_PAGE {
        override fun type(): Int = 0
    },

    /** Index 1 — [com.andaagii.tacomamusicplayer.screen.MusicPlayingScreen]. The full player with controls and album art. */
    PLAYER_PAGE {
        override fun type(): Int = 1
    },

    /** Index 2 — [com.andaagii.tacomamusicplayer.screen.PlaylistScreen]. Browses user-created playlists. */
    PLAYLIST_PAGE {
        override fun type(): Int = 2
    },

    /** Index 3 — [com.andaagii.tacomamusicplayer.screen.AlbumListScreen]. Browses albums discovered from MediaStore. */
    ALBUM_PAGE {
        override fun type(): Int = 3
    },

    /** Index 4 — [com.andaagii.tacomamusicplayer.screen.SongListScreen]. Displays all tracks in the library. */
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

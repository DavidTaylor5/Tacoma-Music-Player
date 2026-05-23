package com.andaagii.tacomamusicplayer.util

import com.andaagii.tacomamusicplayer.constants.Const
import timber.log.Timber

/**
 * Container for the [MenuOption] enum and its string-to-enum factory function.
 *
 * XML popup menus surface menu item titles as raw strings; [determineMenuOptionFromTitle]
 * converts those strings into typed [MenuOption] values so the rest of the app never branches
 * on raw title strings directly.
 */
class MenuOptionUtil {

    /**
     * Type-safe representation of every popup menu action available in the app.
     *
     * Each entry's [type] function returns the [Const] string that appears as the menu item
     * title in XML menu resource files. [determineMenuOptionFromTitle] maps those title strings
     * back to these entries.
     */
    enum class MenuOption {

        // --- Song group actions ---

        /** Plays the entire song group (album or playlist) from the beginning. */
        PLAY_SONG_GROUP {
            override fun type(): String = Const.PLAY_SONG_GROUP
        },

        // --- Playlist actions ---

        /** Adds the selected song(s) to a playlist chosen by the user. */
        ADD_TO_PLAYLIST {
            override fun type(): String = Const.ADD_TO_PLAYLIST
        },

        /** Removes the selected song(s) from the current playlist. */
        REMOVE_FROM_PLAYLIST {
            override fun type(): String = Const.REMOVE_FROM_PLAYLIST
        },

        /** Renames an existing playlist via the text-input prompt overlay. */
        RENAME_PLAYLIST {
            override fun type(): String = Const.RENAME_PLAYLIST
        },

        /** Replaces a playlist's cover artwork using the system image picker and uCrop. */
        ADD_PLAYLIST_IMAGE {
            override fun type(): String = Const.ADD_PLAYLIST_IMAGE
        },

        /** Permanently deletes a playlist and its cross-reference entries from the database. */
        REMOVE_PLAYLIST {
            override fun type(): String = Const.REMOVE_PLAYLIST
        },

        /** Plays the playlist in isolation without merging it with the current queue. */
        PLAY_PLAYLIST_ONLY {
            override fun type(): String = Const.PLAY_PLAYLIST_ONLY
        },

        // --- Queue actions ---

        /** Removes a single track from the current playback queue. */
        REMOVE_FROM_QUEUE {
            override fun type(): String = Const.REMOVE_FROM_QUEUE
        },

        /** Clears all tracks from the current playback queue. */
        CLEAR_QUEUE {
            override fun type(): String = Const.CLEAR_QUEUE
        },

        /** Appends the selected song(s) or group to the end of the current queue. */
        ADD_TO_QUEUE {
            override fun type(): String = Const.ADD_TO_QUEUE
        },

        // --- Album actions ---

        /** Plays the selected album from the beginning. */
        PLAY_ALBUM {
            override fun type(): String = Const.PLAY_ALBUM
        },

        /** Replaces an album's cover artwork using the system image picker and uCrop. */
        ADD_ALBUM_IMAGE {
            override fun type(): String = Const.ADD_ALBUM_IMAGE
        },

        // --- Miscellaneous ---

        /** Opens a stats view for the selected item. Not yet implemented. */
        CHECK_STATS {
            override fun type(): String = Const.CHECK_STATS
        },

        /** Sentinel value returned when no menu title matches a known [Const] string. */
        UNKNOWN {
            override fun type(): String = Const.UNKNOWN
        };

        /** Returns the [Const] string that identifies this option in XML menu resources. */
        abstract fun type(): String
    }

    companion object {
        /**
         * Maps an XML popup menu item title string to the corresponding [MenuOption].
         *
         * Menu item titles come from the user-visible `android:title` attribute in XML menu
         * resource files, which mirrors the [Const] strings returned by [MenuOption.type].
         * Returns [MenuOption.UNKNOWN] for any title that does not match a known constant.
         *
         * @param title The raw menu item title string (e.g., `"Play Album"`).
         * @return The matching [MenuOption], or [MenuOption.UNKNOWN] if unrecognised.
         */
        fun determineMenuOptionFromTitle(title: String): MenuOption {
            Timber.d("determineMenuOptionFromTitle: title=$title")
            return when (title) {
                Const.PLAY_SONG_GROUP -> MenuOption.PLAY_SONG_GROUP

                Const.PLAY_PLAYLIST_ONLY -> MenuOption.PLAY_PLAYLIST_ONLY
                Const.ADD_TO_PLAYLIST -> MenuOption.ADD_TO_PLAYLIST
                Const.REMOVE_FROM_PLAYLIST -> MenuOption.REMOVE_FROM_PLAYLIST
                Const.RENAME_PLAYLIST -> MenuOption.RENAME_PLAYLIST
                Const.ADD_PLAYLIST_IMAGE -> MenuOption.ADD_PLAYLIST_IMAGE
                Const.REMOVE_PLAYLIST -> MenuOption.REMOVE_PLAYLIST

                Const.PLAY_ALBUM -> MenuOption.PLAY_ALBUM
                Const.ADD_ALBUM_IMAGE -> MenuOption.ADD_ALBUM_IMAGE

                Const.CLEAR_QUEUE -> MenuOption.CLEAR_QUEUE
                Const.ADD_TO_QUEUE -> MenuOption.ADD_TO_QUEUE
                Const.REMOVE_FROM_QUEUE -> MenuOption.REMOVE_FROM_QUEUE

                Const.CHECK_STATS -> MenuOption.CHECK_STATS
                else -> MenuOption.UNKNOWN
            }
        }
    }
}

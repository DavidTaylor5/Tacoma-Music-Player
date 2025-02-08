package com.example.tacomamusicplayer.util

class MenuOptionUtil {

    /**
     * MenuOptions that can be handled in the app. Usually interactions with the media player.
     */
    enum class MenuOption {
        ADD_TO_PLAYLIST,
        CLEAR_QUEUE,
        ADD_TO_QUEUE,
        CHECK_STATS,
        REMOVE_FROM_QUEUE,
        REMOVE_FROM_PLAYLIST,
        UNKNOWN
    }

    companion object {

        /**
         * UI Menu options return a string, which I use to determine specific actions
         * within the app. I use MenuOption enum for simplicity.
         */
        fun determineMenuOptionFromTitle(title: String): MenuOption {
            return when(title) {
                "Add to Playlist" ->  MenuOption.ADD_TO_PLAYLIST
                "Clear Queue" -> MenuOption.CLEAR_QUEUE
                "Add to Queue" -> MenuOption.ADD_TO_QUEUE
                "Check Stats" -> MenuOption.CHECK_STATS
                "Remove from Queue" -> MenuOption.REMOVE_FROM_QUEUE
                "Remove from Playlist" -> MenuOption.REMOVE_FROM_PLAYLIST

                else -> MenuOption.UNKNOWN
            }
        }
    }
}
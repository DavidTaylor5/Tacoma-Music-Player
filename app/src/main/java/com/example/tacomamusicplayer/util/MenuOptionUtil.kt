package com.example.tacomamusicplayer.util

class MenuOptionUtil {

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
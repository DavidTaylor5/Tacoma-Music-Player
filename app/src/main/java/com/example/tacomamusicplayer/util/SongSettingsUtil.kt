package com.example.tacomamusicplayer.util

class SongSettingsUtil {

    enum class Setting {
        ADD_TO_PLAYLIST,
        ADD_TO_QUEUE,
        CHECK_STATS,
        REMOVE_FROM_QUEUE,
        REMOVE_FROM_PLAYLIST,
        UNKNOWN
    }

    companion object {
        fun determineSettingFromTitle(title: String): Setting {
            return when(title) {
                "Add to Playlist" ->  Setting.ADD_TO_PLAYLIST
                "Add to Queue" -> Setting.ADD_TO_QUEUE
                "Check Stats" -> Setting.CHECK_STATS
                "Remove from Queue" -> Setting.REMOVE_FROM_QUEUE
                "Remove from Playlist" -> Setting.REMOVE_FROM_PLAYLIST
                else -> Setting.UNKNOWN
            }
        }
    }
}
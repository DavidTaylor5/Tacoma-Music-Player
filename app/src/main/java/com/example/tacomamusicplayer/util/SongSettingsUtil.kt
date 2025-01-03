package com.example.tacomamusicplayer.util

class SongSettingsUtil {

    enum class Setting {
        ADD_TO_PLAYLIST,
        ADD_TO_QUEUE,
        CHECK_STATS,
        UNKNOWN
    }

    companion object {
        fun determineSettingFromTitle(title: String): Setting {
            return when(title) {
                "Add to Playlist" ->  Setting.ADD_TO_PLAYLIST
                "Add to Queue" -> Setting.ADD_TO_QUEUE
                "Check Stats" -> Setting.CHECK_STATS
                else -> Setting.UNKNOWN
            }
        }
    }
}
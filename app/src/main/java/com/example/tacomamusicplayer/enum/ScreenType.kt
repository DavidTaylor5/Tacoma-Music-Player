package com.example.tacomamusicplayer.enum

enum class ScreenType {

    MUSIC_PLAYING_SCREEN {
        override fun route(): String = "MUSIC_PLAYING_SCREEN"
    },

    MUSIC_CHOOSER_SCREEN {
        override fun route(): String = "MUSIC_CHOOSER_SCREEN"
    },

    PERMISSION_DENIED_SCREEN {
        override fun route(): String = "PERMISSION_DENIED_SCREEN"
    },

    MUSIC_QUEUE_SCREEN {
        override fun route(): String = "MUSIC_QUEUE_SCREEN"
    };

    abstract fun route(): String
}
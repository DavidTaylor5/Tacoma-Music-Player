package com.andaagii.tacomamusicplayer.enumtype

/**
 * Top-level navigation destinations registered in `MainActivity`'s programmatic
 * `NavController` graph.
 *
 * Each entry provides a unique [route] string used as the destination ID when calling
 * `NavController.navigate` from [com.andaagii.tacomamusicplayer.composables.TacomaMusicPlayerApp].
 * Navigation events are delivered via [com.andaagii.tacomamusicplayer.viewmodel.MainViewModel.screenState].
 *
 * [MUSIC_CHOOSER_SCREEN] is the start destination of the nav graph.
 */
enum class ScreenType {

    /** Full-screen player UI showing album art, track metadata, and playback controls. */
    MUSIC_PLAYING_SCREEN {
        override fun route(): String = "MUSIC_PLAYING_SCREEN"
    },

    /**
     * Start destination — [com.andaagii.tacomamusicplayer.screen.MusicChooserScreen], which
     * hosts the [HorizontalPager] with the queue, player, playlist, album, and song pages.
     */
    MUSIC_CHOOSER_SCREEN {
        override fun route(): String = "MUSIC_CHOOSER_SCREEN"
    },

    /** Shown when the `READ_MEDIA_AUDIO` runtime permission has been denied by the user. */
    PERMISSION_DENIED_SCREEN {
        override fun route(): String = "PERMISSION_DENIED_SCREEN"
    },

    /** Dedicated view for the current playback queue. */
    MUSIC_QUEUE_SCREEN {
        override fun route(): String = "MUSIC_QUEUE_SCREEN"
    };

    /** Returns the unique route string used to identify this destination in the nav graph. */
    abstract fun route(): String
}

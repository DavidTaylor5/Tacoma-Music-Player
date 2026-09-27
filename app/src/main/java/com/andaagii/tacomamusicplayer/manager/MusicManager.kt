package com.andaagii.tacomamusicplayer.manager

/**
 * Manage all of the Music State, and Music Controls.
 * Internally handles the mediaController and mediaBrowser.
 */
interface MusicManager {
    fun initialize()
    fun release()
}
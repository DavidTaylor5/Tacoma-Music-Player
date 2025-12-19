package com.andaagii.tacomamusicplayer.data

/**
 * SongGroups can either be the original art, or a user set custom image.
 */
data class ArtInfo(
    val artFileOriginal: String,
    val artFileCustom: String,
    val useCustomArt: Boolean
)

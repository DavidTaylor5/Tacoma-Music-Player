package com.andaagii.tacomamusicplayer.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SongData(
    val songUri: String,
    val songTitle: String,
    val albumTitle: String,
    val artist: String,
    val artworkUri: String,
    val duration: String,
)
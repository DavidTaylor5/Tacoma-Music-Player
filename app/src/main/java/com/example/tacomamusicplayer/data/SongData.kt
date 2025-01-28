package com.example.tacomamusicplayer.data

import com.squareup.moshi.JsonClass

//TODO why can I not add duration to the SongData!!!
@JsonClass(generateAdapter = true)
data class SongData(
    val songUri: String,
    val songTitle: String,
    val albumTitle: String,
    val artist: String,
    val artworkUri: String,
    val duration: String,
)
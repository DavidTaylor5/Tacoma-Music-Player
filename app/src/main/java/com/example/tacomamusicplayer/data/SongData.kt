package com.example.tacomamusicplayer.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SongData(
    val songUri: String,
    val songTitle: String,
    val albumTitle: String,
    val artist: String,
    val artworkUri: Long,
)

//songUri = url, songTitle = title, albumTitle = album, artist = artist, artworkUri = artworkUri,
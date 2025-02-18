package com.andaagii.tacomamusicplayer.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaylistData (
    val songs: List<SongData> = listOf()
)
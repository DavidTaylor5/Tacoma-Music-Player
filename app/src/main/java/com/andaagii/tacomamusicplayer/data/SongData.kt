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
) {
    companion object {
        fun isNullSong(song: SongData?): Boolean {
            if(song == null) {
                return true
            }

            return (song.songTitle == "null" || song.songTitle.isEmpty() || song.songTitle == "UNKNOWN") &&
            (song.albumTitle == "null" || song.albumTitle.isEmpty() || song.albumTitle == "UNKNOWN")
        }
    }
}
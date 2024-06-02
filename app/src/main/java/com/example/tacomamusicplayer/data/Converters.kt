package com.example.tacomamusicplayer.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi

class Converters {

    val moshi = Moshi.Builder().build()
    val playlistAdapter = moshi.adapter(PlaylistData::class.java)

    @TypeConverter
    fun SongDataFromString(data: String): PlaylistData {
        return playlistAdapter.fromJson(data) ?: PlaylistData()
    }

    @TypeConverter
    fun stringFromSongData(playlist: PlaylistData): String {
        return playlistAdapter.toJson(playlist)
    }



}
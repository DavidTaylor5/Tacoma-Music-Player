package com.andaagii.tacomamusicplayer.database

import androidx.room.TypeConverter
import com.andaagii.tacomamusicplayer.data.PlaylistData
import com.squareup.moshi.Moshi

class Converters {

    private val moshi = Moshi.Builder().build()
    private val playlistAdapter = moshi.adapter(PlaylistData::class.java)

    @TypeConverter
    fun songDataFromString(data: String): PlaylistData {
        return playlistAdapter.fromJson(data) ?: PlaylistData()
    }

    @TypeConverter
    fun stringFromSongData(playlist: PlaylistData): String {
        return playlistAdapter.toJson(playlist)
    }



}
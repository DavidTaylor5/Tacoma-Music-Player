package com.andaagii.tacomamusicplayer.database

import androidx.room.TypeConverter
import com.andaagii.tacomamusicplayer.data.PlaylistData
import com.squareup.moshi.Moshi

/**
 * Room [TypeConverter]s for the legacy [com.andaagii.tacomamusicplayer.data.Playlist] entity's
 * JSON song-list column.
 *
 * **Not currently active.** The [com.andaagii.tacomamusicplayer.data.Playlist] entity is no
 * longer listed in the [PlayerDatabase] `@Database` declaration, so these converters are not
 * registered and have no effect at runtime. Kept alongside the legacy data classes for reference.
 *
 * Moshi adapters are created once at construction time and reused across all conversion calls
 * to avoid repeated reflection overhead.
 */
class Converters {

    private val moshi = Moshi.Builder().build()
    private val playlistAdapter = moshi.adapter(PlaylistData::class.java)

    /**
     * Deserialises a JSON string from the database back into a [PlaylistData] object.
     *
     * Falls back to an empty [PlaylistData] if [data] is malformed or Moshi cannot parse it,
     * preventing a crash on corrupt or partially migrated rows.
     *
     * @param data JSON string previously produced by [stringFromSongData].
     * @return The deserialised [PlaylistData], or an empty instance if parsing fails.
     */
    @TypeConverter
    fun songDataFromString(data: String): PlaylistData {
        return playlistAdapter.fromJson(data) ?: PlaylistData()
    }

    /**
     * Serialises [playlist] to a JSON string for storage in the database.
     *
     * @param playlist The [PlaylistData] to serialise.
     * @return A JSON string representation of [playlist].
     */
    @TypeConverter
    fun stringFromSongData(playlist: PlaylistData): String {
        return playlistAdapter.toJson(playlist)
    }
}

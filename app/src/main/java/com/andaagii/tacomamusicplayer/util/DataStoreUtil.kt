package com.andaagii.tacomamusicplayer.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.activity.dataStore
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.enum.LayoutType
import com.andaagii.tacomamusicplayer.enum.ShuffleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * DataStoreUtil requires context from MainActivity, which has access to DataStore "Settings".
 */
class DataStoreUtil {

    companion object {

        private val SETTING_PLAYLIST_LAYOUT = stringPreferencesKey(Const.SETTING_PLAYLIST_LAYOUT)
        private val SETTING_ALBUM_LAYOUT = stringPreferencesKey(Const.SETTING_ALBUM_LAYOUT)

        private val SETTING_PLAYLIST_SORTING = stringPreferencesKey(Const.SETTING_PLAYLIST_SORTING)
        private val SETTING_ALBUM_SORTING = stringPreferencesKey(Const.SETTING_ALBUM_SORTING)

        private val SETTING_LOOPING = intPreferencesKey(Const.SETTING_LOOPING)
        private val SETTING_SHUFFLE = stringPreferencesKey(Const.SETTING_SHUFFLE)

        private val PLAYBACK_POSITION = longPreferencesKey(Const.PLAYBACK_POSITION)
        private val SONG_POSITION = intPreferencesKey(Const.SONG_POSITION)

        suspend fun setPlaybackPosition(context: Context, position: Long) {
            context.dataStore.edit { preferences ->
                preferences[PLAYBACK_POSITION] = position
            }
        }

        fun getPlaybackPosition(context: Context): Flow<Long> {
            val latestPlaybackPosition = context.dataStore.data.map { preferences ->
                preferences[PLAYBACK_POSITION] ?: 0
            }
            return latestPlaybackPosition
        }

        suspend fun setSongPosition(context: Context, position: Int) {
            context.dataStore.edit { preferences ->
                preferences[SONG_POSITION] = position
            }
        }

        fun getSongPosition(context: Context): Flow<Int> {
            val latestSongPosition = context.dataStore.data.map { preferences ->
               preferences[SONG_POSITION] ?: 0
            }
            return latestSongPosition
        }

        suspend fun setLoopingPreference(context: Context, loopInt: Int) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_LOOPING] = loopInt
            }
        }

        fun getLoopingPreference(context: Context): Flow<Int> {
            val latestLoopingPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_LOOPING] ?: Player.REPEAT_MODE_OFF
            }
            return latestLoopingPref
        }

        suspend fun setShufflePreference(context: Context, shuffleType: ShuffleType) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_SHUFFLE] = shuffleType.type()
            }
        }

        fun getShufflePreference(context: Context): Flow<String> {
            val latestShufflePref = context.dataStore.data.map { preferences ->
                preferences[SETTING_SHUFFLE] ?: ShuffleType.NOT_SHUFFLED.type()
            }
            return latestShufflePref
        }

        suspend fun setPlaylistLayoutPreference(context: Context, layout: LayoutType) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_PLAYLIST_LAYOUT] = layout.type()
            }
        }

        fun getPlaylistLayoutPreference(context: Context): Flow<String> {
            val latestPlaylistPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_PLAYLIST_LAYOUT] ?: LayoutType.LINEAR_LAYOUT.type()
            }
            return latestPlaylistPref
        }

        suspend fun setAlbumLayoutPreference(context: Context, layout: LayoutType) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_ALBUM_LAYOUT] = layout.type()
            }
        }

        fun getAlbumLayoutPreference(context: Context): Flow<String> {
            val latestPlaylistPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_ALBUM_LAYOUT] ?: LayoutType.LINEAR_LAYOUT.type()
            }
            return latestPlaylistPref
        }

        suspend fun setPlaylistSortingPreference(context: Context, sorting: SortingUtil.SortingOption) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_PLAYLIST_SORTING] = sorting.type()
            }
        }

        fun getPlaylistSortingPreference(context: Context): Flow<String> {
            val latestSortingPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_PLAYLIST_SORTING] ?: "default"
            }
            return latestSortingPref
        }

        suspend fun setAlbumSortingPreference(context: Context, sorting: SortingUtil.SortingOption) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_ALBUM_SORTING] = sorting.type()
            }
        }

        fun getAlbumSortingPreference(context: Context): Flow<String> {
            val latestSortingPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_ALBUM_SORTING] ?: "default"
            }
            return latestSortingPref
        }
    }
}
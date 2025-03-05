package com.andaagii.tacomamusicplayer.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andaagii.tacomamusicplayer.activity.dataStore
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.enum.LayoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStoreUtil requires context from MainActivity, which has access to DataStore "Settings".
 */
class DataStoreUtil {

    companion object {

        val SETTING_PLAYLIST_LAYOUT = stringPreferencesKey(Const.SETTING_PLAYLIST_LAYOUT)
        val SETTING_ALBUM_LAYOUT = stringPreferencesKey(Const.SETTING_ALBUM_LAYOUT)

        suspend fun setPlaylistLayoutPreference(context: Context, layout: LayoutType) {
            context.dataStore.edit { settings ->
                settings[SETTING_PLAYLIST_LAYOUT] = layout.type()
            }
        }

        fun getPlaylistLayoutPreference(context: Context): Flow<String> {
            val latestPlaylistPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_PLAYLIST_LAYOUT] ?: LayoutType.LINEAR_LAYOUT.type()
            }
            return latestPlaylistPref
        }

        suspend fun setAlbumLayoutPreference(context: Context, layout: LayoutType) {
            context.dataStore.edit { settings ->
                settings[SETTING_ALBUM_LAYOUT] = layout.type()
            }
        }

        fun getAlbumLayoutPreference(context: Context): Flow<String> {
            val latestPlaylistPref = context.dataStore.data.map { preferences ->
                preferences[SETTING_ALBUM_LAYOUT] ?: LayoutType.LINEAR_LAYOUT.type()
            }
            return latestPlaylistPref
        }
    }
}
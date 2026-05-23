package com.andaagii.tacomamusicplayer.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.activity.dataStore
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Centralises all DataStore Preferences reads and writes for the app.
 *
 * All preference keys are private to this class — callers interact only through the typed
 * getter/setter pairs exposed in the companion object. The DataStore instance lives on
 * `MainActivity` (accessed via the `Context.dataStore` extension property), so a `Context`
 * that references `MainActivity` must be supplied to every function.
 *
 * Keys fall into two categories:
 * - **UI preferences** — layout type (linear/grid) and sort order for the playlist and album
 *   tabs; shuffle mode; loop/repeat mode.
 * - **Playback state** — position within the current track (ms) and the active queue index,
 *   used to restore exactly where the user left off on next launch.
 */
class DataStoreUtil {

    companion object {

        // --- UI preference keys ---
        // Persist the user's chosen list layout (linear vs grid) and sort order for the
        // playlist and album tabs, as well as playback mode toggles (shuffle and loop).
        private val SETTING_PLAYLIST_LAYOUT = stringPreferencesKey(Const.SETTING_PLAYLIST_LAYOUT)
        private val SETTING_ALBUM_LAYOUT = stringPreferencesKey(Const.SETTING_ALBUM_LAYOUT)

        private val SETTING_PLAYLIST_SORTING = stringPreferencesKey(Const.SETTING_PLAYLIST_SORTING)
        private val SETTING_ALBUM_SORTING = stringPreferencesKey(Const.SETTING_ALBUM_SORTING)

        private val SETTING_LOOPING = intPreferencesKey(Const.SETTING_LOOPING)
        private val SETTING_SHUFFLE = stringPreferencesKey(Const.SETTING_SHUFFLE)

        // --- Playback state keys ---
        // Persisted on pause/stop so the app resumes mid-track at the correct queue position.
        private val PLAYBACK_POSITION = longPreferencesKey(Const.PLAYBACK_POSITION)
        private val SONG_POSITION = intPreferencesKey(Const.SONG_POSITION)

        /**
         * Saves the current playback position in milliseconds to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param position The playback position in milliseconds to persist.
         */
        suspend fun setPlaybackPosition(context: Context, position: Long) {
            context.dataStore.edit { preferences ->
                preferences[PLAYBACK_POSITION] = position
            }
        }

        /**
         * Reads the saved playback position as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the saved position in milliseconds; defaults to `0` if
         *   the key has not been written yet.
         */
        fun getPlaybackPosition(context: Context): Flow<Long> {
            return context.dataStore.data.map { preferences ->
                preferences[PLAYBACK_POSITION] ?: 0
            }
        }

        /**
         * Saves the current queue index (which song is playing) to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param position The zero-based queue index to persist.
         */
        suspend fun setSongPosition(context: Context, position: Int) {
            context.dataStore.edit { preferences ->
                preferences[SONG_POSITION] = position
            }
        }

        /**
         * Reads the saved queue index as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the saved queue index; defaults to `0` if the key has
         *   not been written yet.
         */
        fun getSongPosition(context: Context): Flow<Int> {
            return context.dataStore.data.map { preferences ->
                preferences[SONG_POSITION] ?: 0
            }
        }

        /**
         * Saves the current loop/repeat mode integer to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param loopInt A Media3 [Player] repeat-mode constant (e.g.,
         *   [Player.REPEAT_MODE_OFF], [Player.REPEAT_MODE_ONE], [Player.REPEAT_MODE_ALL]).
         */
        suspend fun setLoopingPreference(context: Context, loopInt: Int) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_LOOPING] = loopInt
            }
        }

        /**
         * Reads the saved loop/repeat mode as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the saved repeat-mode integer; defaults to
         *   [Player.REPEAT_MODE_OFF] if the key has not been written yet.
         */
        fun getLoopingPreference(context: Context): Flow<Int> {
            return context.dataStore.data.map { preferences ->
                preferences[SETTING_LOOPING] ?: Player.REPEAT_MODE_OFF
            }
        }

        /**
         * Saves the current shuffle mode to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param shuffleType The [ShuffleType] to persist; serialised via [ShuffleType.type].
         */
        suspend fun setShufflePreference(context: Context, shuffleType: ShuffleType) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_SHUFFLE] = shuffleType.type()
            }
        }

        /**
         * Reads the saved shuffle mode as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the [ShuffleType.type] string; defaults to
         *   [ShuffleType.NOT_SHUFFLED] if the key has not been written yet.
         */
        fun getShufflePreference(context: Context): Flow<String> {
            return context.dataStore.data.map { preferences ->
                preferences[SETTING_SHUFFLE] ?: ShuffleType.NOT_SHUFFLED.type()
            }
        }

        /**
         * Saves the user's preferred playlist list layout to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param layout The [LayoutType] to persist; serialised via [LayoutType.type].
         */
        suspend fun setPlaylistLayoutPreference(context: Context, layout: LayoutType) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_PLAYLIST_LAYOUT] = layout.type()
            }
        }

        /**
         * Reads the saved playlist list layout as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the [LayoutType.type] string; defaults to
         *   [LayoutType.LINEAR_LAYOUT] if the key has not been written yet.
         */
        fun getPlaylistLayoutPreference(context: Context): Flow<String> {
            return context.dataStore.data.map { preferences ->
                preferences[SETTING_PLAYLIST_LAYOUT] ?: LayoutType.LINEAR_LAYOUT.type()
            }
        }

        /**
         * Saves the user's preferred album list layout to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param layout The [LayoutType] to persist; serialised via [LayoutType.type].
         */
        suspend fun setAlbumLayoutPreference(context: Context, layout: LayoutType) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_ALBUM_LAYOUT] = layout.type()
            }
        }

        /**
         * Reads the saved album list layout as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the [LayoutType.type] string; defaults to
         *   [LayoutType.LINEAR_LAYOUT] if the key has not been written yet.
         */
        fun getAlbumLayoutPreference(context: Context): Flow<String> {
            return context.dataStore.data.map { preferences ->
                preferences[SETTING_ALBUM_LAYOUT] ?: LayoutType.LINEAR_LAYOUT.type()
            }
        }

        /**
         * Saves the user's preferred playlist sort order to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param sorting The [SortingUtil.SortingOption] to persist; serialised via
         *   [SortingUtil.SortingOption.type].
         */
        suspend fun setPlaylistSortingPreference(context: Context, sorting: SortingUtil.SortingOption) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_PLAYLIST_SORTING] = sorting.type()
            }
        }

        /**
         * Reads the saved playlist sort order as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the [SortingUtil.SortingOption.type] string; defaults to
         *   `"default"` if the key has not been written yet. [SortingUtil.determineSortingOptionFromTitle]
         *   maps `"default"` to [SortingUtil.SortingOption.SORTING_NEWEST_RELEASE].
         */
        fun getPlaylistSortingPreference(context: Context): Flow<String> {
            return context.dataStore.data.map { preferences ->
                preferences[SETTING_PLAYLIST_SORTING] ?: "default"
            }
        }

        /**
         * Saves the user's preferred album sort order to DataStore.
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @param sorting The [SortingUtil.SortingOption] to persist; serialised via
         *   [SortingUtil.SortingOption.type].
         */
        suspend fun setAlbumSortingPreference(context: Context, sorting: SortingUtil.SortingOption) {
            context.dataStore.edit { preferences ->
                preferences[SETTING_ALBUM_SORTING] = sorting.type()
            }
        }

        /**
         * Reads the saved album sort order as a [Flow].
         *
         * @param context A context that references the `MainActivity` DataStore instance.
         * @return A [Flow] emitting the [SortingUtil.SortingOption.type] string; defaults to
         *   `"default"` if the key has not been written yet. [SortingUtil.determineSortingOptionFromTitle]
         *   maps `"default"` to [SortingUtil.SortingOption.SORTING_NEWEST_RELEASE].
         */
        fun getAlbumSortingPreference(context: Context): Flow<String> {
            return context.dataStore.data.map { preferences ->
                preferences[SETTING_ALBUM_SORTING] ?: "default"
            }
        }
    }
}

package com.andaagii.tacomamusicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * ViewModel that manages multi-select state for song list screens.
 *
 * Tracks which songs the user has long-pressed to select and whether the multi-select
 * action bar is visible. Playlist checkbox state is managed internally by the
 * [com.andaagii.tacomamusicplayer.composables.PlaylistPrompt] composable.
 *
 * Note: Hilt injection is currently disabled (`@HiltViewModel` is commented out); this ViewModel
 * must be instantiated manually at the call site.
 *
 * Exposed state:
 * - [isShowingPlaylistPrompt] — whether the add-to-playlist sheet is visible.
 * - [isShowingMultiSelectPrompt] — whether the multi-select action bar is visible.
 * - [currentlySelectedSongs] — songs the user has selected via long-press.
 */
//@HiltViewModel
class SongListViewModel: ViewModel() {

    /** `true` while the add-to-playlist bottom sheet is displayed. */
    val isShowingPlaylistPrompt: StateFlow<Boolean>
        get() = _isShowingPlaylistPrompt
    private val _isShowingPlaylistPrompt = MutableStateFlow(false)

    /**
     * `true` while at least one song is selected and the multi-select action bar should be shown.
     * Set to `false` automatically when all songs are deselected via [unselectSong] or
     * [clearMultiSelectSongs].
     */
    val isShowingMultiSelectPrompt: StateFlow<Boolean>
        get() = _isShowingMultiSelectPrompt
    private val _isShowingMultiSelectPrompt = MutableStateFlow(false)

    /**
     * Accumulates [MediaItem] objects as the user long-presses songs.
     * Cleared by [clearMultiSelectSongs].
     */
    val currentlySelectedSongs: StateFlow<List<MediaItem>>
        get() = _currentlySelectedSongs
    private val _currentlySelectedSongs = MutableStateFlow<List<MediaItem>>(emptyList())

    /**
     * Adds [songs] to [currentlySelectedSongs] and optionally shows the multi-select action bar.
     *
     * @param songs The songs to add to the current selection.
     * @param showPrompt If `true` and the resulting selection is non-empty, shows the multi-select
     *   action bar via [isShowingMultiSelectPrompt].
     */
    fun selectSongs(songs: List<MediaItem>, showPrompt: Boolean) {
        val currentSongs = _currentlySelectedSongs.value.toMutableList()
        Timber.d("selectSongs: songs=${songs.map { it.mediaMetadata.title }}, _currentlySelectedSongs=${_currentlySelectedSongs.value.map { it.mediaMetadata.title }}")

        currentSongs.addAll(songs)
        if(currentSongs.isNotEmpty() && showPrompt) {
            _isShowingMultiSelectPrompt.value = true
        }
        _currentlySelectedSongs.value = currentSongs
    }

    /**
     * Removes [song] from [currentlySelectedSongs] by media ID.
     *
     * Hides the multi-select action bar via [isShowingMultiSelectPrompt] if the selection
     * becomes empty after removal.
     *
     * @param song The song to deselect.
     */
    fun unselectSong(song: MediaItem) {
        Timber.d("unselectSong: song=${ song.mediaMetadata.title }")

        val currentSongs = _currentlySelectedSongs.value.toMutableList()
        currentSongs.removeAll {
            it.mediaId == song.mediaId
        }
        if(currentSongs.isEmpty()) {
            _isShowingMultiSelectPrompt.value = false
        }
        _currentlySelectedSongs.value = currentSongs
    }

    /**
     * Clears [currentlySelectedSongs] and hides the multi-select action bar.
     *
     * Call when the user dismisses the multi-select mode or after songs have been acted on.
     */
    fun clearMultiSelectSongs() {
        Timber.d("clearMultiSelectSongs: ")

        _currentlySelectedSongs.value = emptyList()
        _isShowingMultiSelectPrompt.value = false
    }
}

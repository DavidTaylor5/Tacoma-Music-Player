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
 * Tracks which songs the user has long-pressed to select, which playlists they have ticked in
 * the add-to-playlist prompt, and whether the prompt dialogs are visible.
 *
 * Note: Hilt injection is currently disabled (`@HiltViewModel` is commented out); this ViewModel
 * must be instantiated manually at the call site.
 *
 * Exposed state:
 * - [isShowingPlaylistPrompt] — whether the add-to-playlist sheet is visible.
 * - [isShowingMultiSelectPrompt] — whether the multi-select action bar is visible.
 * - [checkedPlaylists] — playlist titles the user has ticked in the prompt.
 * - [currentlySelectedSongs] — songs the user has selected via long-press.
 * - [isPlaylistPromptAddClickable] — whether the "Add" button in the prompt is enabled.
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
     * Titles of playlists ticked by the user in the add-to-playlist prompt.
     * Reset to an empty list by [prepareSongsForPlaylists] at the start of each add flow.
     */
    val checkedPlaylists: StateFlow<List<String>>
        get() = _checkedPlaylists
    private val _checkedPlaylists = MutableStateFlow<List<String>>(emptyList())

    /**
     * Accumulates [MediaItem] objects as the user long-presses songs.
     * Cleared by [clearMultiSelectSongs].
     */
    val currentlySelectedSongs: StateFlow<List<MediaItem>>
        get() = _currentlySelectedSongs
    private val _currentlySelectedSongs = MutableStateFlow<List<MediaItem>>(emptyList())

    /**
     * `true` when at least one playlist is checked in the prompt; controls the enabled state of
     * the "Add" button. Updated by [updatePlaylistPromptAddClickability].
     */
    val isPlaylistPromptAddClickable: StateFlow<Boolean>
        get() = _isPlaylistPromptAddClickable
    private val _isPlaylistPromptAddClickable = MutableStateFlow(false)

    /**
     * Resets [checkedPlaylists] to empty and disables the Add button in preparation for a fresh
     * add-to-playlist flow.
     *
     * Call this before showing the add-to-playlist prompt so that any previously ticked playlists
     * do not carry over to the new selection.
     */
    fun prepareSongsForPlaylists() {
        Timber.d("prepareSongsForPlaylists: ")

        val resetCheckedPlaylists = emptyList<String>()

        // No playlist selected yet at the start of a new add flow
        _checkedPlaylists.value = resetCheckedPlaylists

        updatePlaylistPromptAddClickability(resetCheckedPlaylists)
    }

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

    /**
     * Adds or removes [playlistTitle] from [checkedPlaylists] based on [isChecked], then updates
     * [isPlaylistPromptAddClickable] as a side effect.
     *
     * @param playlistTitle The title of the playlist the user tapped in the prompt.
     * @param isChecked `true` if the user checked the playlist; `false` if they unchecked it.
     */
    fun updateCheckedPlaylists(playlistTitle: String, isChecked: Boolean ) {
        Timber.d("updateCheckedPlaylists: ")

        // Maintain a local copy to pass to clickability update after the StateFlow update
        var updatedPlaylistsWithCheckmarks = mutableListOf<String>()

        if(isChecked) {
            // Add the playlist if it isn't already in the checked list
            val checkedPlaylists = _checkedPlaylists.value
            if(!checkedPlaylists.contains(playlistTitle)) {
                updatedPlaylistsWithCheckmarks = checkedPlaylists.toMutableList()
                updatedPlaylistsWithCheckmarks.add(playlistTitle)
                _checkedPlaylists.value = updatedPlaylistsWithCheckmarks
            }

        } else {
            // Remove the playlist if it is currently checked
            val checkedPlaylists = _checkedPlaylists.value
            if(checkedPlaylists.contains(playlistTitle)) {
                updatedPlaylistsWithCheckmarks = checkedPlaylists.toMutableList()
                updatedPlaylistsWithCheckmarks.removeAll {
                    it == playlistTitle
                }
                _checkedPlaylists.value = updatedPlaylistsWithCheckmarks
            }
        }

        updatePlaylistPromptAddClickability(updatedPlaylistsWithCheckmarks)
    }

    /**
     * Enables the "Add" button in the add-to-playlist prompt when at least one playlist is
     * checked; disables it when the list is empty.
     *
     * @param checkedPlaylists The current list of ticked playlist titles.
     */
    private fun updatePlaylistPromptAddClickability(checkedPlaylists: List<String>) {
        _isPlaylistPromptAddClickable.value = checkedPlaylists.isNotEmpty()
    }
}

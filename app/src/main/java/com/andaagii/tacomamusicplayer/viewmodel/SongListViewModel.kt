package com.andaagii.tacomamusicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isShowingPlaylistPrompt: LiveData<Boolean>
        get() = _isShowingPlaylistPrompt
    private val _isShowingPlaylistPrompt: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * `true` while at least one song is selected and the multi-select action bar should be shown.
     * Set to `false` automatically when all songs are deselected via [unselectSong] or
     * [clearMultiSelectSongs].
     */
    val isShowingMultiSelectPrompt: LiveData<Boolean>
        get() = _isShowingMultiSelectPrompt
    private val _isShowingMultiSelectPrompt: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * Titles of playlists ticked by the user in the add-to-playlist prompt.
     * Reset to an empty list by [prepareSongsForPlaylists] at the start of each add flow.
     */
    val checkedPlaylists: LiveData<List<String>>
        get() = _checkedPlaylists
    private val _checkedPlaylists: MutableLiveData<List<String>> = MutableLiveData(listOf())

    /**
     * Accumulates [MediaItem] objects as the user long-presses songs.
     * Cleared by [clearMultiSelectSongs].
     */
    val currentlySelectedSongs: LiveData<List<MediaItem>>
        get() = _currentlySelectedSongs
    private val _currentlySelectedSongs: MutableLiveData<List<MediaItem>> = MutableLiveData(listOf())

    /**
     * `true` when at least one playlist is checked in the prompt; controls the enabled state of
     * the "Add" button. Updated by [updatePlaylistPromptAddClickability].
     */
    val isPlaylistPromptAddClickable: LiveData<Boolean>
        get() = _isPlaylistPromptAddClickable
    private val _isPlaylistPromptAddClickable: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * Resets [checkedPlaylists] to empty and disables the Add button in preparation for a fresh
     * add-to-playlist flow.
     *
     * Call this before showing the add-to-playlist prompt so that any previously ticked playlists
     * do not carry over to the new selection.
     */
    fun prepareSongsForPlaylists() {
        Timber.d("prepareSongsForPlaylists: ")

        val resetCheckedPlaylists = mutableListOf<String>()

        // No playlist selected yet at the start of a new add flow
        _checkedPlaylists.postValue(resetCheckedPlaylists)

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
        val currentSongs = _currentlySelectedSongs.value?.toMutableList() ?: mutableListOf()
        Timber.d("selectSongs: songs=${songs.map { it.mediaMetadata.title }}, _currentlySelectedSongs=${_currentlySelectedSongs.value?.map { it.mediaMetadata.title }}")

        currentSongs.addAll(songs)
        if(currentSongs.isNotEmpty() && showPrompt) {
            _isShowingMultiSelectPrompt.postValue(true)
        }
        _currentlySelectedSongs.postValue(currentSongs)
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

        val currentSongs = _currentlySelectedSongs.value?.toMutableList() ?: mutableListOf()
        currentSongs.removeAll {
            it.mediaId == song.mediaId
        }
        if(currentSongs.isEmpty()) {
            _isShowingMultiSelectPrompt.postValue(false)
        }
        _currentlySelectedSongs.postValue(currentSongs)
    }

    /**
     * Clears [currentlySelectedSongs] and hides the multi-select action bar.
     *
     * Call when the user dismisses the multi-select mode or after songs have been acted on.
     */
    fun clearMultiSelectSongs() {
        Timber.d("clearMultiSelectSongs: ")

        _currentlySelectedSongs.postValue(listOf())
        _isShowingMultiSelectPrompt.postValue(false)
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

        // Maintain a local copy to pass to clickability update after the LiveData post
        var updatedPlaylistsWithCheckmarks = mutableListOf<String>()

        if(isChecked) {
            // Add the playlist if it isn't already in the checked list
            _checkedPlaylists.value?.let { checkedPlaylists ->
                if(!checkedPlaylists.contains(playlistTitle)) {
                    updatedPlaylistsWithCheckmarks = checkedPlaylists.toMutableList()
                    updatedPlaylistsWithCheckmarks.add(playlistTitle)
                    _checkedPlaylists.postValue(updatedPlaylistsWithCheckmarks)
                }
            }

        } else {
            // Remove the playlist if it is currently checked
            _checkedPlaylists.value?.let { checkedPlaylists ->
                if(checkedPlaylists.contains(playlistTitle)) {
                    updatedPlaylistsWithCheckmarks = checkedPlaylists.toMutableList()
                    updatedPlaylistsWithCheckmarks.removeAll {
                        it == playlistTitle
                    }
                    _checkedPlaylists.postValue(updatedPlaylistsWithCheckmarks)
                }
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
    private fun updatePlaylistPromptAddClickability(checkedPlaylists: MutableList<String>) {
        if(checkedPlaylists.isEmpty()) {
            _isPlaylistPromptAddClickable.postValue(false)
        } else {
            _isPlaylistPromptAddClickable.postValue(true)
        }
    }
}
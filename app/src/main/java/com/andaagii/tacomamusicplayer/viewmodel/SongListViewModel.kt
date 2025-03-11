package com.andaagii.tacomamusicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem

class SongListViewModel: ViewModel() {

    val isShowingPlaylistPrompt: LiveData<Boolean>
        get() = _isShowingPlaylistPrompt
    private val _isShowingPlaylistPrompt: MutableLiveData<Boolean> = MutableLiveData(false)

    val isShowingMultiSelectPrompt: LiveData<Boolean>
        get() = _isShowingMultiSelectPrompt
    private val _isShowingMultiSelectPrompt: MutableLiveData<Boolean> = MutableLiveData(false)

    val checkedPlaylists: LiveData<List<String>>
        get() = _checkedPlaylists
    private val _checkedPlaylists: MutableLiveData<List<String>> = MutableLiveData(listOf())

    val currentlySelectedSongs: LiveData<List<MediaItem>>
        get() = _currentlySelectedSongs
    private val _currentlySelectedSongs: MutableLiveData<List<MediaItem>> = MutableLiveData(listOf())

    val isPlaylistPromptAddClickable: LiveData<Boolean>
        get() = _isPlaylistPromptAddClickable
    private val _isPlaylistPromptAddClickable: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * @param newSongs List of songs that can be potentially added to a playlist.
     */
    fun prepareSongsForPlaylists() {
        val resetCheckedPlaylists = mutableListOf<String>()

        //As I prepare a song for playlists, I don't yet know which playlist I'm going to add it to
        _checkedPlaylists.postValue(resetCheckedPlaylists)

        updatePlaylistPromptAddClickability(resetCheckedPlaylists)
    }

    fun selectSongs(songs: List<MediaItem>, showPrompt: Boolean) {
        val currentSongs = _currentlySelectedSongs.value?.toMutableList() ?: mutableListOf()
        currentSongs.addAll(songs)
        if(currentSongs.isNotEmpty() && showPrompt) {
            _isShowingMultiSelectPrompt.postValue(true)
        }
        _currentlySelectedSongs.postValue(currentSongs)
    }

    fun unselectSong(song: MediaItem) {
        val currentSongs = _currentlySelectedSongs.value?.toMutableList() ?: mutableListOf()
        currentSongs.removeAll {
            it.mediaId == song.mediaId
        }
        if(currentSongs.isEmpty()) {
            _isShowingMultiSelectPrompt.postValue(false)
        }
        _currentlySelectedSongs.postValue(currentSongs)
    }

    fun clearPreparedSongsForPlaylists() {
        _currentlySelectedSongs.postValue(listOf())
        _isShowingMultiSelectPrompt.postValue(false)
    }

    /**
     * Determines whether a given playlist is checked, aka part of the selected playlists for
     * adding songs.
     * @param playlistTitle The title of a playlist.
     * @param isChecked Boolean value, true if user selected the playlist.
     */
    fun updateCheckedPlaylists(playlistTitle: String, isChecked: Boolean ) {

        //I keep a copy of updated playlists, to determine if 'Add' button is enabled.
        var updatedPlaylistsWithCheckmarks = mutableListOf<String>()

        if(isChecked) {
            _checkedPlaylists.value?.let { checkedPlaylists ->
                if(!checkedPlaylists.contains(playlistTitle)) {
                    updatedPlaylistsWithCheckmarks = checkedPlaylists.toMutableList()
                    updatedPlaylistsWithCheckmarks.add(playlistTitle)
                    _checkedPlaylists.postValue(updatedPlaylistsWithCheckmarks)
                }
            }

        } else {
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
     * If no playlists are selected, don't let the user press the "add" button.
     */
    private fun updatePlaylistPromptAddClickability(checkedPlaylists: MutableList<String>) {
        if(checkedPlaylists.isEmpty()) {
            _isPlaylistPromptAddClickable.postValue(false)
        } else {
            _isPlaylistPromptAddClickable.postValue(true)
        }
    }
}
package com.example.tacomamusicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem

class SongListViewModel: ViewModel() {

    val isShowingPlaylistPrompt: LiveData<Boolean>
        get() = _isShowingPlaylistPrompt
    private val _isShowingPlaylistPrompt: MutableLiveData<Boolean> = MutableLiveData(false)

    val checkedPlaylists: LiveData<List<String>>
        get() = _checkedPlaylists
    private val _checkedPlaylists: MutableLiveData<List<String>> = MutableLiveData(listOf())

    val playlistAddSongs: LiveData<List<MediaItem>>
        get() = _playlistAddSongs
    private val _playlistAddSongs: MutableLiveData<List<MediaItem>> = MutableLiveData(listOf())

    val isPlaylistPromptAddClickable: LiveData<Boolean>
        get() = _isPlaylistPromptAddClickable
    private val _isPlaylistPromptAddClickable: MutableLiveData<Boolean> = MutableLiveData(false)

    fun showPlaylistPrompt() {
        _isShowingPlaylistPrompt.postValue(true)
    }

    fun removePlaylistPrompt() {
        _isShowingPlaylistPrompt.postValue(false)
    }

    //Clicking on the song settings for a song should add it as a potential add for playlists.
    fun prepareSongForPlaylists(song: MediaItem) {
        _playlistAddSongs.value?.let { songs ->
            val modList = songs.toMutableList()
            modList.add(song)
            _playlistAddSongs.postValue(modList)
        }
    }

    fun clearPreparedSongsForPlaylists() {
        _playlistAddSongs.postValue(listOf())
    }

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
     * If no playlists are available I should not let the add button be clicked...
     */
    private fun updatePlaylistPromptAddClickability(checkedPlaylists: MutableList<String>) {
        if(checkedPlaylists.isEmpty()) {
            _isPlaylistPromptAddClickable.postValue(false)
        } else {
            _isPlaylistPromptAddClickable.postValue(true)
        }
    }

}
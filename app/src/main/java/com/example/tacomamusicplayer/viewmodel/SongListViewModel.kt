package com.example.tacomamusicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tacomamusicplayer.enum.PageType

class SongListViewModel: ViewModel() {

    val isShowingPlaylistPrompt: LiveData<Boolean>
        get() = _isShowingPlaylistPrompt
    private val _isShowingPlaylistPrompt: MutableLiveData<Boolean> = MutableLiveData(false)

    public fun showPlaylistPrompt() {
        _isShowingPlaylistPrompt.postValue(true)
    }

    public fun removePlaylistPrompt() {
        _isShowingPlaylistPrompt.postValue(false)
    }
}
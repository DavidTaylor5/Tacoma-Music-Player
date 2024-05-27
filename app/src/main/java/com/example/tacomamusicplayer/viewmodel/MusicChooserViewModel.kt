package com.example.tacomamusicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.enum.PageType

class MusicChooserViewModel: ViewModel() {

    val currentpage: LiveData<PageType>
        get() = _currentpage
    private val _currentpage: MutableLiveData<PageType> = MutableLiveData()

    fun setPage(page: PageType) {
        _currentpage.value = page
    }
}
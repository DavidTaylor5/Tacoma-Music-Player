package com.andaagii.tacomamusicplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.repository.MusicRepository
import com.andaagii.tacomamusicplayer.state.AlbumTabState
import com.andaagii.tacomamusicplayer.state.PlaylistTabState
import com.andaagii.tacomamusicplayer.util.DataStoreUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil.SortingOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistTabViewModel @Inject constructor(
    application: Application,
    private val musicRepo: MusicRepository,
): AndroidViewModel(application) {
    private val _sortingFlow = DataStoreUtil.getPlaylistSortingPreference(application.applicationContext)
        .map { layoutStr -> SortingUtil.determineSortingOptionFromTitle(layoutStr) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SortingOption.SORTING_TITLE_ALPHABETICAL
        )

    private val _layoutFlow = DataStoreUtil.getPlaylistLayoutPreference(application.applicationContext)
        .map { layoutStr ->  LayoutType.determineLayoutFromString(layoutStr) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LayoutType.LINEAR_LAYOUT
        )

    private val _playlists: StateFlow<List<MediaItem>> = musicRepo.getAllAvailablePlaylistFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf()
        )

    val playlistTabState: StateFlow<PlaylistTabState> = combine(
        _sortingFlow,
        _layoutFlow,
        _playlists
    ) { sorting, layout, playlists ->
        PlaylistTabState(
            playlists = playlists,
            sorting = sorting,
            layout = layout
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlaylistTabState(
            playlists = listOf(),
            sorting = SortingOption.SORTING_TITLE_ALPHABETICAL,
            layout = LayoutType.LINEAR_LAYOUT
        )
    )

    fun savePlaylistLayout(context: Context, layout: LayoutType) {
        Timber.d("savePlaylistLayout: layout=$layout")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaylistLayoutPreference(context, layout)
        }
    }

    fun savePlaylistSorting(context: Context, sorting: SortingUtil.SortingOption) {
        Timber.d("savePlaylistSorting: sorting=$sorting")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaylistSortingPreference(context, sorting)
        }
    }
}
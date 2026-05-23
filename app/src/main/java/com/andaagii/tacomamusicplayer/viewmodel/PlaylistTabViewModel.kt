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

/**
 * ViewModel for the Playlists tab.
 *
 * Combines the full playlist list, the user's chosen sort order, and the user's chosen layout
 * type into a single [playlistTabState] for the UI to collect. Layout and sort preferences are
 * persisted to DataStore so they survive process death.
 *
 * Exposes [playlistTabState] as a [StateFlow] of [PlaylistTabState].
 *
 * All DataStore writes run on [Dispatchers.IO] via [viewModelScope].
 *
 * @param musicRepo Source of truth for the playlist library.
 */
@HiltViewModel
class PlaylistTabViewModel @Inject constructor(
    application: Application,
    private val musicRepo: MusicRepository,
): AndroidViewModel(application) {

    /**
     * Reads the saved playlist sort preference from DataStore and maps the raw string to a
     * [SortingOption]. Defaults to [SortingOption.SORTING_TITLE_ALPHABETICAL].
     */
    private val _sortingFlow = DataStoreUtil.getPlaylistSortingPreference(application.applicationContext)
        .map { layoutStr -> SortingUtil.determineSortingOptionFromTitle(layoutStr) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SortingOption.SORTING_TITLE_ALPHABETICAL
        )

    /**
     * Reads the saved playlist layout preference from DataStore and maps the raw string to a
     * [LayoutType]. Defaults to [LayoutType.LINEAR_LAYOUT].
     */
    private val _layoutFlow = DataStoreUtil.getPlaylistLayoutPreference(application.applicationContext)
        .map { layoutStr ->  LayoutType.determineLayoutFromString(layoutStr) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LayoutType.LINEAR_LAYOUT
        )

    /**
     * Live stream of all playlist [MediaItem] objects from the Room database via [MusicRepository].
     * Emits a new list whenever the underlying database changes.
     */
    private val _playlists: StateFlow<List<MediaItem>> = musicRepo.getAllAvailablePlaylistFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf()
        )

    /**
     * Combines [_sortingFlow], [_layoutFlow], and [_playlists] into a single [PlaylistTabState]
     * snapshot. This is the single source of truth the UI observes for the Playlists tab.
     */
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

    /**
     * Persists [layout] to DataStore on [Dispatchers.IO].
     *
     * [_layoutFlow] updates automatically via the DataStore → Flow pipeline.
     *
     * @param context Application context used to access DataStore.
     * @param layout The [LayoutType] chosen by the user.
     */
    fun savePlaylistLayout(context: Context, layout: LayoutType) {
        Timber.d("savePlaylistLayout: layout=$layout")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaylistLayoutPreference(context, layout)
        }
    }

    /**
     * Persists [sorting] to DataStore on [Dispatchers.IO].
     *
     * [_sortingFlow] updates automatically via the DataStore → Flow pipeline.
     *
     * @param context Application context used to access DataStore.
     * @param sorting The [SortingOption] chosen by the user.
     */
    fun savePlaylistSorting(context: Context, sorting: SortingUtil.SortingOption) {
        Timber.d("savePlaylistSorting: sorting=$sorting")
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.setPlaylistSortingPreference(context, sorting)
        }
    }
}
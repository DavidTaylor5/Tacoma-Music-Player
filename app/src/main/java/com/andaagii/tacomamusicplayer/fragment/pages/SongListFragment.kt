package com.andaagii.tacomamusicplayer.fragment.pages

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.composables.SongListScreen
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.SongGroup
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.viewmodel.SongListViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * Thin shell fragment that hosts the [SongListScreen] composable.
 *
 * All state is collected from [MainViewModel] (activity-scoped) and [SongListViewModel]
 * (fragment-scoped) inside the `setContent` block. Playback, search, playlist, and
 * multi-select operations are delegated back to the appropriate ViewModel methods via
 * the composable's callback parameters.
 */
@AndroidEntryPoint
class SongListFragment : Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: SongListViewModel by viewModels()

    override fun onPause() {
        super.onPause()
        // Clear multi-select on pause to avoid stale highlights when the user returns.
        viewModel.clearMultiSelectSongs()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val songGroup      by parentViewModel.currentSongGroup.collectAsStateWithLifecycle()
                val searchResults  by parentViewModel.currentSearchList.collectAsStateWithLifecycle()
                val isSearchMode   by parentViewModel.isShowingSearchMode.collectAsStateWithLifecycle()
                val selectedSongs  by viewModel.currentlySelectedSongs.collectAsStateWithLifecycle()
                val allPlaylists   by parentViewModel.availablePlaylists.collectAsStateWithLifecycle()

                val playlists = allPlaylists.filter {
                    it.mediaMetadata.albumTitle != Const.PLAYLIST_QUEUE_TITLE &&
                        it.mediaMetadata.albumTitle != Const.ORIGINAL_QUEUE_ORDER
                }

                val displaySongs: List<MediaItem> =
                    if (isSearchMode) searchResults.orEmpty() else songGroup?.songs.orEmpty()
                val displayType: SongGroupType =
                    if (isSearchMode) SongGroupType.SEARCH_LIST else (songGroup?.type ?: SongGroupType.ALBUM)
                val showHeader: Boolean =
                    !isSearchMode && songGroup != null && displaySongs.isNotEmpty()

                val artFile = songGroup?.group?.mediaMetadata?.artworkUri?.toString()?.let { File(it) }
                val artUri: Uri? =
                    if (artFile?.exists() == true) songGroup?.group?.mediaMetadata?.artworkUri else null
                val groupTitle: String =
                    songGroup?.group?.mediaMetadata?.albumTitle?.toString().orEmpty()

                SongListScreen(
                    songs                  = displaySongs,
                    songGroupType          = displayType,
                    showHeader             = showHeader,
                    songGroupArtUri        = artUri,
                    songGroupTitle         = groupTitle,
                    isShowingSearchMode    = isSearchMode,
                    selectedSongs          = selectedSongs,
                    availablePlaylists     = playlists,
                    onSongClick            = { pos ->
                        songGroup?.let { parentViewModel.playSongGroupAtPosition(it, pos) }
                        parentViewModel.removeVirtualKeyboard()
                    },
                    onArtworkClick         = { song, isNowSelected ->
                        if (isNowSelected) viewModel.selectSongs(listOf(song), showPrompt = true)
                        else viewModel.unselectSong(song)
                    },
                    onAddToQueue           = { songs -> parentViewModel.addSongsToEndOfQueue(songs) },
                    onRemoveFromPlaylist   = { songs ->
                        parentViewModel.removeSongsFromCurrentPlaylist(songs)
                        viewModel.clearMultiSelectSongs()
                    },
                    onHeaderPlayClick      = {
                        songGroup?.let { parentViewModel.playSongGroupAtPosition(it, 0) }
                    },
                    onSearchQueryChanged   = { query -> parentViewModel.querySearchData(query) },
                    onToggleSearch         = { parentViewModel.flipSearchButtonState() },
                    onCancelSearch         = {
                        parentViewModel.handleCancelSearchButtonClick()
                        parentViewModel.removeVirtualKeyboard()
                    },
                    onMovePlaylistItem     = { from, to ->
                        songGroup?.let { sg ->
                            val newSongs = sg.songs.toMutableList().apply { add(to, removeAt(from)) }
                            sg.songs = newSongs
                            parentViewModel.updatePlaylistOrder(sg)
                        }
                    },
                    onDismissMultiSelect   = { viewModel.clearMultiSelectSongs() },
                    onConfirmAddToPlaylists = { names, songs ->
                        parentViewModel.addSongsToAPlaylist(names, songs)
                        viewModel.clearMultiSelectSongs()
                    },
                    onCreatePlaylist       = { name -> parentViewModel.createNamedPlaylist(name) },
                    onSearchAlbumClick     = { album ->
                        parentViewModel.querySongsFromAlbum(album)
                        parentViewModel.removeVirtualKeyboard()
                        parentViewModel.handleCancelSearchButtonClick()
                    },
                    onSearchPlaylistClick  = { playlist ->
                        parentViewModel.querySongsFromPlaylist(playlist)
                        parentViewModel.removeVirtualKeyboard()
                        parentViewModel.handleCancelSearchButtonClick()
                    },
                    onSearchSongClick      = { song ->
                        parentViewModel.playAlbumAtSongPosition(song)
                        parentViewModel.removeVirtualKeyboard()
                    },
                )
            }
        }
    }
}

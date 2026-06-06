package com.andaagii.tacomamusicplayer.fragment.pages

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.andaagii.tacomamusicplayer.composables.PlaylistScreen
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.viewmodel.PlaylistTabViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Playlists browsing page hosted at index 2 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Thin ComposeView shell — all display logic, sorting, layout switching, and the create/rename
 * prompt overlays live in [PlaylistScreen]. This fragment retains only the [ActivityResultContracts]
 * launchers for the image-picker → uCrop workflow, which cannot be moved to Compose because they
 * require a Fragment lifecycle owner.
 *
 * Custom playlist art uses the same two-step async flow as albums:
 * 1. [getPicture] opens the system image picker.
 * 2. [getCroppedPicture] receives the uCrop result and forwards it to [MainViewModel].
 */
@AndroidEntryPoint
class PlaylistFragment : Fragment() {
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: PlaylistTabViewModel by activityViewModels()

    /**
     * The title of the playlist whose artwork is being replaced. Stored as a field because
     * [getCroppedPicture] fires asynchronously after [addPlaylistImage] returns.
     */
    private var playlistThatNeedsNewImage = "empty"

    /**
     * Receives the cropped image result from uCrop.
     *
     * On success, extracts the output [Uri] and calls [MainViewModel.updateSongGroupImage]
     * to persist the new artwork for the selected playlist.
     */
    private val getCroppedPicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Timber.d("getCroppedPicture: RESULT_OK")
            result.data?.let { cropData ->
                val croppedUri = UCrop.getOutput(cropData)
                croppedUri?.let { uri ->
                    parentViewModel.updateSongGroupImage(
                        title = playlistThatNeedsNewImage,
                        artFileName = uri.path.toString()
                    )
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data
            Timber.d("getCroppedPicture: RESULT_ERROR cropError=${error?.let { e -> UCrop.getError(e) }}")
        }
    }

    /**
     * Opens the system image picker for custom playlist art selection.
     *
     * On a successful pick, sends the chosen [Uri] to uCrop with a 1:1 aspect ratio and
     * a maximum resolution of 700×700 px, then hands off to [getCroppedPicture].
     */
    private val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            Timber.d("getPicture: The picture is null!")
            return@registerForActivityResult
        }

        val saveFileUri = UtilImpl.getSaveFileUri(
            context = requireContext(),
            fileName = playlistThatNeedsNewImage,
            isCustom = true
        )
        UCrop.of(uri, saveFileUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(700, 700)
            .start(requireActivity(), getCroppedPicture)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val context = LocalContext.current
                val state by viewModel.playlistTabState.collectAsStateWithLifecycle()
                PlaylistScreen(
                    playlists = state.playlists,
                    layoutType = state.layout,
                    sorting = state.sorting,
                    onPlaylistClick = ::onPlaylistClick,
                    onPlayClick = parentViewModel::playPlaylist,
                    onAddToQueue = parentViewModel::addPlaylistToBackOfQueue,
                    onAddPlaylistImage = ::addPlaylistImage,
                    onDeletePlaylist = { title -> parentViewModel.removePlaylists(listOf(title)) },
                    onCreatePlaylist = parentViewModel::createNamedPlaylist,
                    onRenamePlaylist = { old, new -> parentViewModel.updatePlaylistTitle(old, new) },
                    onLayoutToggle = {
                        val next = if (state.layout == LayoutType.LINEAR_LAYOUT)
                            LayoutType.TWO_GRID_LAYOUT
                        else
                            LayoutType.LINEAR_LAYOUT
                        viewModel.savePlaylistLayout(context, next)
                    },
                    onSortingSelected = { option ->
                        viewModel.savePlaylistSorting(context, option)
                    }
                )
            }
        }
    }

    /**
     * Stores the target playlist title and launches the image picker.
     *
     * [playlistThatNeedsNewImage] is set before launching because [getPicture] and
     * [getCroppedPicture] fire asynchronously after this function returns.
     *
     * @param playlistTitle The title of the playlist whose artwork should be replaced.
     */
    private fun addPlaylistImage(playlistTitle: String) {
        playlistThatNeedsNewImage = playlistTitle
        getPicture.launch("image/*")
    }

    /**
     * Queries the songs for [playlist] and navigates to [PageType.SONG_PAGE] to display them.
     *
     * @param playlist The playlist whose track list should be shown.
     */
    private fun onPlaylistClick(playlist: MediaItem) {
        parentViewModel.querySongsFromPlaylist(playlist)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }
}

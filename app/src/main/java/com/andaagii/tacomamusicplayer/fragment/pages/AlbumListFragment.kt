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
import com.andaagii.tacomamusicplayer.composables.AlbumListScreen
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.AlbumTabViewModel
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Albums browsing page hosted at index 3 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Thin ComposeView shell — all display logic lives in [AlbumListScreen]. This fragment retains
 * only the [ActivityResultContracts] launchers for the image-picker → uCrop workflow, which
 * cannot be moved to Compose because they require a Fragment lifecycle owner.
 *
 * Custom album art is selected via a two-step async flow:
 * 1. [getPicture] opens the system image picker.
 * 2. The chosen image is passed to uCrop for cropping via [getCroppedPicture].
 * 3. The cropped result URI is forwarded to [MainViewModel.updateSongGroupImage].
 */
@AndroidEntryPoint
class AlbumListFragment : Fragment() {
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: AlbumTabViewModel by activityViewModels()

    /**
     * The display name used as the uCrop output file name. Stored as a field because the
     * [getCroppedPicture] result callback fires asynchronously after [addCustomAlbumImage]
     * returns, so state must survive across the two calls.
     */
    private var albumCustomImageName = "empty"

    /**
     * The album title that should receive the new custom image. Stored as a field for the
     * same async-callback reason as [albumCustomImageName].
     */
    private var selectedAlbumName = "unknown"

    /**
     * Receives the cropped image result from uCrop.
     *
     * On success, extracts the output [Uri] and calls [MainViewModel.updateSongGroupImage]
     * to persist the new artwork for the selected album and all its tracks.
     */
    private val getCroppedPicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Timber.d("getCroppedPicture: RESULT_OK")
            result.data?.let { cropData ->
                val croppedUri = UCrop.getOutput(cropData)
                croppedUri?.let { uri ->
                    parentViewModel.updateSongGroupImage(
                        title = selectedAlbumName,
                        artFileName = uri.path.toString(),
                        updateSongs = true
                    )
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data
            Timber.d("getCroppedPicture: RESULT_ERROR cropError=${error?.let { e -> UCrop.getError(e) }}")
        }
    }

    /**
     * Opens the system image picker for custom album art selection.
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
            fileName = selectedAlbumName,
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
                val state by viewModel.albumTabState.collectAsStateWithLifecycle()
                AlbumListScreen(
                    albums = state.albums,
                    layoutType = state.layout,
                    sorting = state.sorting,
                    onAlbumClick = ::onAlbumClick,
                    onPlayClick = parentViewModel::playAlbum,
                    onMenuOption = ::handleAlbumSetting,
                    onLayoutToggle = {
                        val next = if (state.layout == LayoutType.LINEAR_LAYOUT)
                            LayoutType.TWO_GRID_LAYOUT
                        else
                            LayoutType.LINEAR_LAYOUT
                        viewModel.saveAlbumLayout(context, next)
                    },
                    onSortingSelected = { option ->
                        viewModel.saveAlbumSorting(context, option)
                    }
                )
            }
        }
    }

    /**
     * Stores the target album's context fields and launches the image picker.
     *
     * [albumCustomImageName] and [selectedAlbumName] must be set before launching because
     * [getPicture] and [getCroppedPicture] fire asynchronously after this function returns.
     *
     * @param album The album whose artwork should be replaced.
     * @param customAlbumImageName The file name to use when saving the cropped image.
     */
    private fun addCustomAlbumImage(album: MediaItem, customAlbumImageName: String) {
        albumCustomImageName = customAlbumImageName
        selectedAlbumName = album.mediaMetadata.albumTitle.toString()
        getPicture.launch("image/*")
    }

    /**
     * Routes a popup menu selection to the appropriate action for [album].
     *
     * @param option The menu action chosen by the user.
     * @param album The [MediaItem] the action applies to.
     * @param customAlbumImageName The image file name passed through to [addCustomAlbumImage]
     *   when the user chooses to set a custom image.
     */
    private fun handleAlbumSetting(option: MenuOptionUtil.MenuOption, album: MediaItem, customAlbumImageName: String? = null) {
        when (option) {
            MenuOptionUtil.MenuOption.PLAY_ALBUM -> parentViewModel.playAlbum(album)
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> parentViewModel.addAlbumToBackOfQueue(album)
            MenuOptionUtil.MenuOption.ADD_ALBUM_IMAGE -> {
                customAlbumImageName?.let {
                    addCustomAlbumImage(album, customAlbumImageName)
                }
            }
            else -> Timber.d("handleAlbumSetting: unhandled album menu option")
        }
    }

    /**
     * Queries the songs for [album] and navigates to [PageType.SONG_PAGE] to display them.
     *
     * @param album The album whose track list should be shown.
     */
    private fun onAlbumClick(album: MediaItem) {
        parentViewModel.querySongsFromAlbum(album)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }
}

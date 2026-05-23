package com.andaagii.tacomamusicplayer.fragment.pages

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.AlbumGridAdapter
import com.andaagii.tacomamusicplayer.adapter.AlbumListAdapter
import com.andaagii.tacomamusicplayer.databinding.FragmentAlbumlistBinding
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.AlbumTabViewModel
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Albums browsing page hosted at index 3 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Collects [AlbumTabViewModel.albumTabState] to react to sort-order and layout-preference
 * changes, swapping between [AlbumListAdapter] (linear) and [AlbumGridAdapter] (grid) as
 * needed. Routes album tap, play, and add-to-queue actions to [MainViewModel].
 *
 * Custom album art is selected via a two-step async flow:
 * 1. [getPicture] opens the system image picker.
 * 2. The chosen image is passed to uCrop for cropping via [getCroppedPicture].
 * 3. The cropped result URI is forwarded to [MainViewModel.updateSongGroupImage].
 */
@AndroidEntryPoint
class AlbumListFragment : Fragment() {
    private lateinit var binding: FragmentAlbumlistBinding
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

    /** Tracks the current layout so the toggle button icon can be updated reactively. */
    private var currLayout: LayoutType = LayoutType.LINEAR_LAYOUT

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
        binding = FragmentAlbumlistBinding.inflate(inflater)

        // Collect with STARTED lifecycle so the flow pauses while the fragment is off-screen
        // in the ViewPager2, matching when the RecyclerView is actually visible.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.albumTabState.collect { state ->

                    val sortedAlbums = SortingUtil.sortAlbums(state.albums, state.sorting)

                    // Keep the layout-toggle icon in sync with the current preference.
                    currLayout = state.layout
                    if (currLayout == LayoutType.LINEAR_LAYOUT) {
                        binding.layoutOption.setBackgroundResource(R.drawable.baseline_table_rows_24)
                    } else {
                        binding.layoutOption.setBackgroundResource(R.drawable.baseline_grid_view_24)
                    }

                    if (binding.displayRecyclerview.adapter != null) {
                        // Adapter already exists — check whether the layout type changed.
                        // If the type mismatches, reinitialise the whole adapter to swap
                        // LayoutManager and adapter class. Otherwise just submit the new list.
                        if (binding.displayRecyclerview.adapter is AlbumListAdapter
                            && state.layout != LayoutType.LINEAR_LAYOUT) {
                            initializeGridLayout(albums = sortedAlbums)
                        } else if (binding.displayRecyclerview.adapter is AlbumGridAdapter
                            && state.layout != LayoutType.TWO_GRID_LAYOUT) {
                            initializeLinearLayout(albums = sortedAlbums)
                        } else {
                            val adapter = binding.displayRecyclerview.adapter
                            when (adapter) {
                                is AlbumListAdapter -> adapter.submitList(sortedAlbums)
                                is AlbumGridAdapter -> adapter.submitList(sortedAlbums)
                                else -> Timber.e("onCreateView: Error Unable to submit list of unknown adapter type.")
                            }
                        }
                    } else {
                        // First emission — create the adapter for the user's saved layout preference.
                        when (state.layout) {
                            LayoutType.LINEAR_LAYOUT -> initializeLinearLayout(sortedAlbums)
                            LayoutType.TWO_GRID_LAYOUT -> initializeGridLayout(sortedAlbums)
                        }
                    }
                }
            }
        }

        binding.layoutOption.setOnClickListener {
            if (currLayout == LayoutType.LINEAR_LAYOUT) {
                viewModel.saveAlbumLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                viewModel.saveAlbumLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
            }
        }

        binding.settingsOption.setOnClickListener {
            val menu = PopupMenu(
                this.context,
                binding.settingsOption,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )
            menu.menuInflater.inflate(R.menu.sorting_options_album, menu.menu)

            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                val chosenSortingOption = SortingUtil.determineSortingOptionFromTitle(it.title.toString())
                viewModel.saveAlbumSorting(requireContext(), chosenSortingOption)
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        return binding.root
    }

    /** Sets up a vertical [LinearLayoutManager] and attaches an [AlbumListAdapter] with [albums]. */
    private fun initializeLinearLayout(albums: List<MediaItem>) {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.displayRecyclerview.adapter = AlbumListAdapter(
            this@AlbumListFragment::onAlbumClick,
            parentViewModel::playAlbum,
            this@AlbumListFragment::handleAlbumSetting
        )
        (binding.displayRecyclerview.adapter as AlbumListAdapter).submitList(albums)
    }

    /** Sets up a [GridLayoutManager] and attaches an [AlbumGridAdapter] with [albums]. */
    private fun initializeGridLayout(albums: List<MediaItem>) {
        binding.displayRecyclerview.layoutManager = GridLayoutManager(context, UtilImpl.determineGridSize())
        binding.displayRecyclerview.adapter = AlbumGridAdapter(
            this@AlbumListFragment::onAlbumClick,
            parentViewModel::playAlbum,
            this@AlbumListFragment::handleAlbumSetting
        )
        (binding.displayRecyclerview.adapter as AlbumGridAdapter).submitList(albums)
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

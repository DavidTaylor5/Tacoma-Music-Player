package com.andaagii.tacomamusicplayer.fragment.pages

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.andaagii.tacomamusicplayer.adapter.PlaylistAdapter
import com.andaagii.tacomamusicplayer.adapter.PlaylistGridAdapter
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.databinding.FragmentPlaylistBinding
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import com.andaagii.tacomamusicplayer.viewmodel.PlaylistTabViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import android.widget.PopupMenu
import android.widget.Toast

/**
 * Playlists browsing page hosted at index 2 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Mirrors [AlbumListFragment] for playlists: collects [PlaylistTabViewModel.playlistTabState]
 * to react to layout and sort changes, and swaps between [PlaylistAdapter] (linear) and
 * [PlaylistGridAdapter] (grid). Additionally hosts the `CustomInputTextPrompt` overlay for
 * creating and renaming playlists.
 *
 * Custom playlist art uses the same two-step async flow as albums:
 * 1. [getPicture] opens the system image picker.
 * 2. [getCroppedPicture] receives the uCrop result and forwards it to [MainViewModel].
 */
@AndroidEntryPoint
class PlaylistFragment : Fragment() {

    private lateinit var binding: FragmentPlaylistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: PlaylistTabViewModel by activityViewModels()

    /**
     * The title of the playlist whose artwork is being replaced. Stored as a field because
     * [getCroppedPicture] fires asynchronously after [addPlaylistImage] returns.
     */
    private var playlistThatNeedsNewImage = "empty"

    /** Tracks the current layout so the toggle button icon can be updated reactively. */
    private var currLayout: LayoutType = LayoutType.LINEAR_LAYOUT

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
        binding = FragmentPlaylistBinding.inflate(inflater)

        // Collect with STARTED lifecycle so the flow pauses while the fragment is off-screen
        // in the ViewPager2, matching when the RecyclerView is actually visible.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlistTabState.collect { state ->
                    val sortedPlaylists = SortingUtil.sortPlaylists(state.playlists, state.sorting)

                    // Keep the layout-toggle icon in sync with the current preference.
                    currLayout = state.layout
                    if (currLayout == LayoutType.LINEAR_LAYOUT) {
                        binding.layoutOption.setBackgroundResource(R.drawable.baseline_table_rows_24)
                    } else {
                        binding.layoutOption.setBackgroundResource(R.drawable.baseline_grid_view_24)
                    }

                    if (binding.displayRecyclerview.adapter != null) {
                        // Adapter already exists — swap it out if the layout type changed,
                        // otherwise just push the updated list to the existing adapter.
                        if (binding.displayRecyclerview.adapter is PlaylistAdapter
                            && state.layout != LayoutType.LINEAR_LAYOUT) {
                            initializeGridLayout(playlists = sortedPlaylists)
                        } else if (binding.displayRecyclerview.adapter is PlaylistGridAdapter
                            && state.layout != LayoutType.TWO_GRID_LAYOUT) {
                            initializeLinearLayout(playlists = sortedPlaylists)
                        } else {
                            val adapter = binding.displayRecyclerview.adapter
                            when (adapter) {
                                is PlaylistAdapter -> adapter.submitList(sortedPlaylists)
                                is PlaylistGridAdapter -> adapter.submitList(sortedPlaylists)
                                else -> Timber.e("onCreateView: Error Unable to submit list of unknown adapter type.")
                            }
                        }
                    } else {
                        // First emission — create the adapter for the user's saved layout preference.
                        when (state.layout) {
                            LayoutType.LINEAR_LAYOUT -> initializeLinearLayout(sortedPlaylists)
                            LayoutType.TWO_GRID_LAYOUT -> initializeGridLayout(sortedPlaylists)
                        }
                    }
                }
            }
        }

        // Driven by MainViewModel so that SongListFragment can trigger the create-playlist
        // prompt on this page after the user selects "Create playlist" from the song list.
        parentViewModel.shouldShowAddPlaylistPromptOnPlaylistPage.observe(viewLifecycleOwner) { showPrompt ->
            if (showPrompt) {
                binding.playlistPrompt.resetUserInput()
                binding.playlistPrompt.visibility = View.VISIBLE
                parentViewModel.showAddPlaylistPromptOnPlaylistPage(false)
            }
        }

        binding.layoutOption.setOnClickListener {
            if (currLayout == LayoutType.LINEAR_LAYOUT) {
                viewModel.savePlaylistLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                viewModel.savePlaylistLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
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
            menu.menuInflater.inflate(R.menu.sorting_options_playlist, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                val chosenSortingOption = SortingUtil.determineSortingOptionFromTitle(it.title.toString())
                viewModel.savePlaylistSorting(requireContext(), chosenSortingOption)
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        binding.addPlaylistBtn.setOnClickListener {
            parentViewModel.showAddPlaylistPromptOnPlaylistPage(true)
        }

        setupCreatePlaylistPrompt()
        return binding.root
    }

    /** Sets up a vertical [LinearLayoutManager] and attaches a [PlaylistAdapter] with [playlists]. */
    private fun initializeLinearLayout(playlists: List<MediaItem>) {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.displayRecyclerview.adapter = PlaylistAdapter(
            this::onPlaylistClick,
            parentViewModel::playPlaylist,
            this::handlePlaylistSetting
        )
        (binding.displayRecyclerview.adapter as PlaylistAdapter).submitList(playlists)
    }

    /** Sets up a [GridLayoutManager] and attaches a [PlaylistGridAdapter] with [playlists]. */
    private fun initializeGridLayout(playlists: List<MediaItem>) {
        binding.displayRecyclerview.layoutManager = GridLayoutManager(context, UtilImpl.determineGridSize())
        binding.displayRecyclerview.adapter = PlaylistGridAdapter(
            this::onPlaylistClick,
            parentViewModel::playPlaylist,
            this::handlePlaylistSetting
        )
        (binding.displayRecyclerview.adapter as PlaylistGridAdapter).submitList(playlists)
    }

    /**
     * Routes a popup menu selection to the appropriate action.
     *
     * [playlists] must be non-empty for any action to proceed; an empty list is logged and
     * returned early.
     *
     * @param option The menu action chosen by the user.
     * @param playlists The list of playlist titles the action should be applied to.
     */
    private fun handlePlaylistSetting(option: MenuOptionUtil.MenuOption, playlists: List<String>) {
        Timber.d("handlePlaylistSetting: option=$option, playlists=$playlists")
        if (playlists.isEmpty()) {
            Timber.d("handlePlaylistSetting: Playlists are empty, cannot handle setting.")
            return
        }
        when (option) {
            MenuOptionUtil.MenuOption.PLAY_PLAYLIST_ONLY -> playPlaylistOnly(playlists)
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> addPlaylistToQueue(listOf(playlists[0]))
            MenuOptionUtil.MenuOption.RENAME_PLAYLIST -> renamePlaylist(playlists[0])
            MenuOptionUtil.MenuOption.ADD_PLAYLIST_IMAGE -> addPlaylistImage(playlists[0])
            MenuOptionUtil.MenuOption.REMOVE_PLAYLIST -> removePlaylists(playlists)
            else -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    /**
     * Plays the first (and expected only) playlist in [playlists].
     *
     * Only the first element is used; passing more than one playlist title has no effect
     * beyond the first entry.
     *
     * @param playlists A list containing the title of the playlist to play. Should have exactly
     *   one element.
     */
    private fun playPlaylistOnly(playlists: List<String>) {
        Timber.d("playPlaylistOnly: ")
        if (playlists.isNotEmpty()) {
            parentViewModel.playPlaylist(playlists[0])
        }
    }

    /**
     * Appends the first playlist in [playlists] to the end of the current queue.
     *
     * @param playlists A list containing the title of the playlist to enqueue.
     */
    private fun addPlaylistToQueue(playlists: List<String>) {
        Timber.d("addPlaylistToQueue: ")
        if (playlists.isNotEmpty()) {
            parentViewModel.addPlaylistToBackOfQueue(playlists[0])
        }
    }

    /**
     * Reconfigures the text input prompt for renaming and makes it visible.
     *
     * After the rename completes (option 2), the prompt is reset to the create-playlist
     * configuration by calling [setupCreatePlaylistPrompt].
     *
     * @param playlistTitle The current title of the playlist being renamed.
     */
    private fun renamePlaylist(playlistTitle: String) {
        setupRenamePlaylistPrompt(playlistTitle)
        binding.playlistPrompt.resetUserInput()
        binding.playlistPrompt.visibility = View.VISIBLE
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
     * Deletes each playlist in [playlists] via [MainViewModel].
     *
     * @param playlists The titles of the playlists to remove.
     */
    private fun removePlaylists(playlists: List<String>) {
        Timber.d("removePlaylists: playlists=$playlists")
        parentViewModel.removePlaylists(playlists)
    }

    /**
     * Wires the text prompt for the rename-playlist flow.
     *
     * Option 1 (Cancel) dismisses the prompt without saving.
     * Option 2 (Update) calls [MainViewModel.updatePlaylistTitle] with the user-entered name,
     * then resets the prompt to the create-playlist configuration via [setupCreatePlaylistPrompt].
     *
     * @param playlistTitle The current title passed to [MainViewModel.updatePlaylistTitle] as
     *   the "old" name to be replaced.
     */
    private fun setupRenamePlaylistPrompt(playlistTitle: String) {
        binding.playlistPrompt.setTextInputHint(Const.RENAME_PLAYLIST_HINT)

        binding.playlistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.playlistPrompt.setOption1ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
        }

        binding.playlistPrompt.setOption2ButtonText(Const.UPDATE)
        binding.playlistPrompt.setOption2ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
            parentViewModel.updatePlaylistTitle(playlistTitle, binding.playlistPrompt.getUserInputtedText())
            // Reset so the next time the prompt opens it is in "create" mode, not "rename" mode.
            setupCreatePlaylistPrompt()
        }
    }

    /**
     * Wires the text prompt for the create-playlist flow.
     *
     * Option 1 (Cancel) dismisses the prompt. Option 2 (Add) calls
     * [MainViewModel.createNamedPlaylist] with the user-entered name and dismisses the prompt.
     */
    private fun setupCreatePlaylistPrompt() {
        binding.playlistPrompt.setTextInputHint(Const.NEW_PLAYLIST_HINT)

        binding.playlistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.playlistPrompt.setOption1ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
        }

        binding.playlistPrompt.setOption2ButtonText(Const.ADD)
        binding.playlistPrompt.setOption2ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
            parentViewModel.createNamedPlaylist(binding.playlistPrompt.getUserInputtedText())
        }
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

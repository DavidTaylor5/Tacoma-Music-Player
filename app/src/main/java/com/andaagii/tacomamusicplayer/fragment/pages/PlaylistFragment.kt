package com.andaagii.tacomamusicplayer.fragment.pages

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
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
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PlaylistFragment: Fragment() {

    private lateinit var binding: FragmentPlaylistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    //The name of the most recent playlist that I want to update the image for
    private var playlistThatNeedsNewImage = "empty"

    private var currentLayout = LayoutType.LINEAR_LAYOUT
    private var currentPlaylists: List<MediaItem> = listOf()


    private val getCroppedPicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK) {
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
        } else if(result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data
            Timber.d("getCroppedPicture: RESULT_ERROR cropError=${error?.let { e ->  UCrop.getError(e)} }")
        }
    }

    //Callback for when user chooses a playlist Image
    private val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the returned Uri
        val pictureUri = uri

        if(pictureUri == null) {
            Timber.d("getPicture: The picture is null!")
        }

        pictureUri?.let { uri ->
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
    }

    private fun updatePlaylistLayout(layout: LayoutType) {
        Timber.d("updatePlaylistLayout: layout=$layout")
        currentLayout = layout
        if(layout == LayoutType.LINEAR_LAYOUT) {
            binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.displayRecyclerview.adapter = PlaylistAdapter(
                currentPlaylists,
                this::onPlaylistClick,
                parentViewModel::playPlaylist,
                this::handlePlaylistSetting
            )
        } else if(layout == LayoutType.TWO_GRID_LAYOUT) {
            binding.displayRecyclerview.layoutManager = GridLayoutManager(context, UtilImpl.determineGridSize())
            binding.displayRecyclerview.adapter = PlaylistGridAdapter(
                currentPlaylists,
                this::onPlaylistClick,
                parentViewModel::playPlaylist,
                this::handlePlaylistSetting
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(inflater)


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.availablePlaylists.collect { playlists ->
                    Timber.d("onCreateView: availablePlaylists updated! playlists.size=${playlists.size}")
                                //Queue, QUEUE_ORDERED is saved as a playlist in database, user doesn't need to access it.
                    val playlistsWithoutQueue = playlists.filter { playlist ->
                        playlist.mediaMetadata.albumTitle != Const.PLAYLIST_QUEUE_TITLE &&
                                playlist.mediaMetadata.albumTitle != Const.ORIGINAL_QUEUE_ORDER
                    }

                    currentPlaylists = SortingUtil.sortPlaylists(
                        playlistsWithoutQueue,
                        parentViewModel.sortingForPlaylistTab.value
                            ?: SortingUtil.SortingOption.SORTING_BY_MODIFICATION_DATE
                    )
                    if(parentViewModel.layoutForPlaylistTab.value == LayoutType.TWO_GRID_LAYOUT) {
                        binding.displayRecyclerview.adapter = PlaylistGridAdapter(
                            currentPlaylists,
                            this@PlaylistFragment::onPlaylistClick,
                            parentViewModel::playPlaylist,
                            this@PlaylistFragment::handlePlaylistSetting
                        )
                    } else {
                        binding.displayRecyclerview.adapter = PlaylistAdapter(
                            currentPlaylists,
                            this@PlaylistFragment::onPlaylistClick,
                            parentViewModel::playPlaylist,
                            this@PlaylistFragment::handlePlaylistSetting
                        )
                    }
                }
            }
        }


        parentViewModel.layoutForPlaylistTab.observe(viewLifecycleOwner) { layout ->
            updatePlaylistLayout(layout)
        }
        parentViewModel.sortingForPlaylistTab.observe(viewLifecycleOwner) { sorting ->
            updatePlaylistSorting(sorting)
        }

        parentViewModel.shouldShowAddPlaylistPromptOnPlaylistPage.observe(viewLifecycleOwner) { showPrompt ->
            if(showPrompt) {
                binding.playlistPrompt.resetUserInput()
                binding.playlistPrompt.visibility = View.VISIBLE
                parentViewModel.showAddPlaylistPromptOnPlaylistPage(false)
            }
        }
        setupCreatePlaylistPrompt()
        setupPage()
        return binding.root
    }

    private fun updatePlaylistSorting(sorting: SortingUtil.SortingOption) {
        Timber.d("updatePlaylistSorting: sorting=$sorting")
        currentPlaylists = SortingUtil.sortPlaylists(currentPlaylists, sorting)

        //Set the current album list to be shown
        binding.displayRecyclerview.adapter.let { adapter ->
            when(adapter) {
                is PlaylistAdapter -> {
                    adapter.updateData(currentPlaylists)
                }
                is PlaylistGridAdapter -> {
                    adapter.updateData(currentPlaylists)
                }
            }
        }
    }

    private fun handlePlaylistSetting(option: MenuOptionUtil.MenuOption, playlists: List<String>) {
        Timber.d("handlePlaylistSetting: option=$option, playlists=$playlists")
        if(playlists.isEmpty()) {
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
     * @param playlists Should be a list with only 1 playlist.
     */
    private fun playPlaylistOnly(playlists: List<String>) {
        Timber.d("playPlaylistOnly: ")
        if(playlists.isNotEmpty()) {
            parentViewModel.playPlaylist(playlists[0])
        }
    }

    private fun addPlaylistToQueue(playlists: List<String>) {
        Timber.d("addPlaylistToQueue: ")
        if(playlists.isNotEmpty()) {
            parentViewModel.addPlaylistToBackOfQueue(playlists[0])
        }
    }

    private fun renamePlaylist(playlistTitle: String) {
        setupRenamePlaylistPrompt(playlistTitle)

        binding.playlistPrompt.resetUserInput()
        binding.playlistPrompt.visibility = View.VISIBLE
    }

    private fun addPlaylistImage(playlistTitle: String) {
        //Playlist that I should update the image for
        playlistThatNeedsNewImage = playlistTitle

        // ActivityResultLauncher is able to launch the activity to kick off the request for a result.
        getPicture.launch("image/*")
    }

    private fun removePlaylists(playlists: List<String>) {
        Timber.d("removePlaylists: playlists=${playlists}")
        parentViewModel.removePlaylists(playlists)
    }

    private fun setupRenamePlaylistPrompt(playlistTitle: String) {
        //set playlist prompt hint
        binding.playlistPrompt.setTextInputHint(Const.RENAME_PLAYLIST_HINT)

        // Option 1 will be to cancel
        binding.playlistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.playlistPrompt.setOption1ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
        }

        // Option 2 will be to create a new playlist with given name
        binding.playlistPrompt.setOption2ButtonText(Const.UPDATE)
        binding.playlistPrompt.setOption2ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
            parentViewModel.updatePlaylistTitle(playlistTitle, binding.playlistPrompt.getUserInputtedText())

            //set it back to the create playlist prompt
            setupCreatePlaylistPrompt()
        }
    }

    private fun setupCreatePlaylistPrompt() {
        //set playlist prompt hint
        binding.playlistPrompt.setTextInputHint(Const.NEW_PLAYLIST_HINT)

        // Option 1 will be to cancel
        binding.playlistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.playlistPrompt.setOption1ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
        }

        // Option 2 will be to create a new playlist with given name
        binding.playlistPrompt.setOption2ButtonText(Const.ADD)
        binding.playlistPrompt.setOption2ButtonOnClick {
            parentViewModel.removeVirtualKeyboard()
            binding.playlistPrompt.visibility = View.GONE
            parentViewModel.createNamedPlaylist(binding.playlistPrompt.getUserInputtedText())
        }
    }

    private fun onPlaylistClick(playlist: MediaItem) {
        parentViewModel.querySongsFromPlaylist(playlist)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }

    private fun setupPage() {
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
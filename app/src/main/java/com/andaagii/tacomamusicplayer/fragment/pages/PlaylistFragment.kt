package com.andaagii.tacomamusicplayer.fragment.pages

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.adapter.PlaylistAdapter
import com.andaagii.tacomamusicplayer.adapter.PlaylistGridAdapter
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.Playlist
import com.andaagii.tacomamusicplayer.databinding.FragmentPlaylistBinding
import com.andaagii.tacomamusicplayer.enum.LayoutType
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class PlaylistFragment: Fragment() {

    private lateinit var binding: FragmentPlaylistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    //The name of the most recent playlist that I want to update the image for
    private var playlistThatNeedsNewImage = "empty"

    private var currentLayout = LayoutType.LINEAR_LAYOUT
    private var currentPlaylists: List<Playlist> = listOf()

    //Callback for when user chooses a playlist Image
    private val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the returned Uri
        val pictureUri = uri

        if(pictureUri == null) {
            Timber.d("getPicture: The picture is null!")
        }

        pictureUri?.let { uri ->
            this.context?.let { fragmentContext ->
                //Save picture to local data
                UtilImpl.saveImageToFile(fragmentContext, uri, playlistThatNeedsNewImage)

                parentViewModel.updatePlaylistImage(playlistThatNeedsNewImage, "$playlistThatNeedsNewImage.jpg")
            }
        }
    }

    private fun updatePlaylistLayout(layout: LayoutType) {
        Timber.d("updatePlaylistLayout: layout=$layout")
        currentLayout = layout

        //TODO update the currentPlaylists to be ordered by SortingOption

        //TODO Dangerous, what if I only update one adapter... this is not efficient?
        if(layout == LayoutType.LINEAR_LAYOUT) {
            binding.layoutButton.text = LayoutType.LINEAR_LAYOUT.type()
            binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.displayRecyclerview.adapter = PlaylistAdapter(
                currentPlaylists,
                this::onPlaylistClick,
                parentViewModel::playPlaylist,
                this::handlePlaylistSetting
            )
        } else if(layout == LayoutType.TWO_GRID_LAYOUT) {
            binding.layoutButton.text = LayoutType.TWO_GRID_LAYOUT.type()
            binding.displayRecyclerview.layoutManager = GridLayoutManager(context, 2)
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

        parentViewModel.availablePlaylists.observe(viewLifecycleOwner) { playlists ->

            //Queue is saved as a playlist in database, user doesn't need to access it.
            val playlistsWithoutQueue = playlists.filter { playlist ->
                playlist.title != Const.PLAYLIST_QUEUE_TITLE
            }

            currentPlaylists = SortingUtil.sortPlaylists(
                playlistsWithoutQueue,
                parentViewModel.sortingForPlaylistTab.value
                    ?: SortingUtil.SortingOption.SORTING_BY_MODIFICATION_DATE
            )

            if(parentViewModel.layoutForPlaylistTab.value == LayoutType.TWO_GRID_LAYOUT) {
                binding.displayRecyclerview.adapter = PlaylistGridAdapter(
                    currentPlaylists,
                    this::onPlaylistClick,
                    parentViewModel::playPlaylist,
                    this::handlePlaylistSetting
                )
            } else {
                binding.displayRecyclerview.adapter = PlaylistAdapter(
                    currentPlaylists,
                    this::onPlaylistClick,
                    parentViewModel::playPlaylist,
                    this::handlePlaylistSetting
                )
            }
        }

        parentViewModel.layoutForPlaylistTab.observe(viewLifecycleOwner) { layout ->
            updatePlaylistLayout(layout)
        }

        parentViewModel.sortingForPlaylistTab.observe(viewLifecycleOwner) { sorting ->
            updatePlaylistSorting(sorting)
        }

        binding.createPlaylistButton.setOnClickListener{
            deactivatePlaylistButton()
            binding.playlistPrompt.resetUserInput()
            binding.playlistPrompt.visibility = View.VISIBLE
        }

        //TODO replace with logic for setting new layout
        binding.layoutButton.setOnClickListener {
            //update the current layout...
            //If I'm on gridlayout, switch to linear layout and vice versa.
            if(currentLayout == LayoutType.LINEAR_LAYOUT) {
                //Update Layout State / Save to datastore
                parentViewModel.savePlaylistLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                //Update Layout State / Save to datastore
                parentViewModel.savePlaylistLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
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

    private fun activatePlaylistButton() {
        binding.createPlaylistButton.isClickable = true
        binding.createPlaylistButton.setBackgroundColor(Color.parseColor("#4CAF50"))
    }

    private fun deactivatePlaylistButton() {
        binding.createPlaylistButton.isClickable = false
        binding.createPlaylistButton.setBackgroundColor(Color.parseColor("#000000"))
    }

    private fun handlePlaylistSetting(option: MenuOptionUtil.MenuOption, playlists: List<String>) {
        when (option) {
            MenuOptionUtil.MenuOption.PLAY_PLAYLIST_ONLY -> playPlaylistOnly(playlists)
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> {
                if(playlists.isNotEmpty()) {
                    addPlaylistToQueue(
                        listOf(playlists[0])
                    )
                } else {
                    Timber.d("handlePlaylistSetting: Tried setting playlist image, but given playlists is empty!")
                }
            }
            MenuOptionUtil.MenuOption.RENAME_PLAYLIST -> {
                if(playlists.isNotEmpty()) {
                    renamePlaylist(playlists[0])
                } else {
                    Timber.d("handlePlaylistSetting: Tried renaming playlist, but given playlists is empty!")
                }
            }
            MenuOptionUtil.MenuOption.ADD_PLAYLIST_IMAGE -> {
                if(playlists.isNotEmpty()) {
                    addPlaylistImage(playlists[0])
                } else {
                    Timber.d("handlePlaylistSetting: Tried setting playlist image, but given playlists is empty!")
                }
            }
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
            activatePlaylistButton()
            binding.playlistPrompt.visibility = View.GONE
        }

        // Option 2 will be to create a new playlist with given name
        binding.playlistPrompt.setOption2ButtonText(Const.UPDATE)
        binding.playlistPrompt.setOption2ButtonOnClick {
            activatePlaylistButton()
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
            activatePlaylistButton()
            binding.playlistPrompt.visibility = View.GONE
        }

        // Option 2 will be to create a new playlist with given name
        binding.playlistPrompt.setOption2ButtonText(Const.ADD)
        binding.playlistPrompt.setOption2ButtonOnClick {
            activatePlaylistButton()
            binding.playlistPrompt.visibility = View.GONE
            parentViewModel.createNamedPlaylist(binding.playlistPrompt.getUserInputtedText())
        }
    }

    private fun onPlaylistClick(playlistTitle: String) {
        parentViewModel.querySongsFromPlaylist(playlistTitle)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PLAYLISTS"
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
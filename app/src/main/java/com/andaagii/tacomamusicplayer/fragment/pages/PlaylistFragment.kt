package com.andaagii.tacomamusicplayer.fragment.pages

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.andaagii.tacomamusicplayer.adapter.PlaylistAdapter
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.databinding.FragmentPlaylistBinding
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class PlaylistFragment(

): Fragment() {
    private lateinit var binding: FragmentPlaylistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    //The name of the most recent playlist that I want to update the image for
    private var playlistThatNeedsNewImage = "empty"


    //TODO Give the user the ability to set an image for a playlist
    //TODO I probably also want to save a copy of the image, to app data and reference it later.
    // Sets up the callback
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPlaylistBinding.inflate(inflater)

        parentViewModel.availablePlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.displayRecyclerview.adapter = PlaylistAdapter(
                playlists,
                this::onPlaylistClick,
                this::handlePlaylistSetting
            )
        }

        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE
            binding.createPlaylistPrompt.resetUserInput()
            binding.createPlaylistPrompt.visibility = View.VISIBLE
        }

        setupRenamePlaylistPrompt()
        setupCreatePlaylistPrompt()
        setupPage()

        return binding.root
    }

    private fun handlePlaylistSetting(option: MenuOptionUtil.MenuOption, playlists: List<String>) {
        when (option) {
            MenuOptionUtil.MenuOption.PLAY_PLAYLIST_ONLY -> playPlaylistOnly(playlists)
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> addPlaylistToQueue()
            MenuOptionUtil.MenuOption.RENAME_PLAYLIST -> renamePlaylist()
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

    private fun addPlaylistToQueue() {
        //TODO Finish this...
    }

    private fun renamePlaylist() {
        //TODO Finish this...
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

    private fun setupRenamePlaylistPrompt() {
        //set playlist prompt hint
        binding.renamePlaylistPrompt.setTextInputHint(Const.RENAME_PLAYLIST_HINT)

        // Option 1 will be to cancel
        binding.renamePlaylistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.renamePlaylistPrompt.setOption1ButtonOnClick {
            binding.fab.visibility = View.VISIBLE
            binding.renamePlaylistPrompt.visibility = View.GONE
        }

        // Option 2 will be to create a new playlist with given name
        binding.renamePlaylistPrompt.setOption2ButtonText(Const.UPDATE)
        binding.renamePlaylistPrompt.setOption2ButtonOnClick {
            binding.fab.visibility = View.VISIBLE
            binding.renamePlaylistPrompt.visibility = View.GONE
            //TODO UPDATE -> parentViewModel.createNamedPlaylist(binding.renamePlaylistPrompt.getUserInputtedText())
        }
    }

    private fun setupCreatePlaylistPrompt() {
        //set playlist prompt hint
        binding.createPlaylistPrompt.setTextInputHint(Const.NEW_PLAYLIST_HINT)

        // Option 1 will be to cancel
        binding.createPlaylistPrompt.setOption1ButtonText(Const.CANCEL)
        binding.createPlaylistPrompt.setOption1ButtonOnClick {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistPrompt.visibility = View.GONE
        }

        // Option 2 will be to create a new playlist with given name
        binding.createPlaylistPrompt.setOption2ButtonText(Const.ADD)
        binding.createPlaylistPrompt.setOption2ButtonOnClick {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistPrompt.visibility = View.GONE
            parentViewModel.createNamedPlaylist(binding.createPlaylistPrompt.getUserInputtedText())
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
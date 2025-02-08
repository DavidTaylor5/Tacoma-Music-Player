package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.adapter.SongListAdapter
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.databinding.FragmentSonglistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.enum.SongGroupType
import com.example.tacomamusicplayer.util.MenuOptionUtil
import com.example.tacomamusicplayer.util.MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST
import com.example.tacomamusicplayer.util.MenuOptionUtil.MenuOption.ADD_TO_QUEUE
import com.example.tacomamusicplayer.util.MenuOptionUtil.MenuOption.CHECK_STATS
import com.example.tacomamusicplayer.util.UtilImpl
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.example.tacomamusicplayer.viewmodel.SongListViewModel
import timber.log.Timber

class SongListFragment(

): Fragment() {
    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: SongListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSonglistBinding.inflate(inflater)

        parentViewModel.currentSongList.observe(viewLifecycleOwner) { songGroup ->
            Timber.d("onCreateView: title=${songGroup.title}, songs.size=${songGroup.songs.size}")
            binding.displayRecyclerview.adapter = SongListAdapter(
                songGroup.songs,
                this::handleSongSetting,
                songGroup.type,
                {  } //TODO update playlist order if this is a playlist...
            )
            determineIfShowingInformationScreen(songGroup.songs, songGroup.type)

            binding.songGroupInfo.setSongGroupTitleText(songGroup.title)

            // Determine what icon to display for song group
            if(songGroup.type == SongGroupType.ALBUM && songGroup.songs.isNotEmpty()) {
                songGroup.songs[0].mediaMetadata.artworkUri?.let { songArt ->
                    UtilImpl.drawUriOntoImageView(
                        binding.songGroupInfo.getSongGroupImage(),
                        songArt,
                        Size(200, 200)
                    )
                }

            } else { // Playlist icon

            }
        }

        parentViewModel.availablePlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.playlistPrompt.setPlaylistData(playlists)
        }

        viewModel.isShowingPlaylistPrompt.observe(viewLifecycleOwner) { isShowing ->
            if(isShowing) {
                binding.playlistPrompt.visibility = View.VISIBLE
            } else {
                binding.playlistPrompt.visibility = View.GONE
            }
        }

        //TODO Give the user the ability to set an image for a playlist
        //TODO I probably also want to save a copy of the image, to app data and reference it later.
        // Sets up the callback
//        val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//            // Handle the returned Uri
//            val what = uri
//            val huh = "huh"
//        }

        // ActivityResultLauncher is able to launch the activity to kick off the request for a result.
        //getPicture.launch("image/*")

        //I want an extra option on menu that will differentiate adding all to the end of the queue
        //adding all to the empty queue...

        binding.songGroupInfo.setOnMenuIconPressed {
            val menu = PopupMenu(binding.root.context, binding.songGroupInfo.getMenuIconView())

            menu.menuInflater.inflate(R.menu.songlist_song_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(binding.root.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleSongSetting(
                    MenuOptionUtil.determineMenuOptionFromTitle(it.toString()),
                    parentViewModel.currentSongList.value?.songs ?: listOf()
                )
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        binding.songGroupInfo.setOnClickListener {
            //CLEAR THE QUEUE
            //START PLAYING THE ALBUM FROM THE START
        }

        setupCreatePlaylistPrompt()
        setupPlaylistPrompt()

        setupPage()

        return binding.root
    }

    private fun setupCreatePlaylistPrompt() {
        binding.createPlaylistPrompt.setAddButtonFunctionality {
            parentViewModel.createNamedPlaylist(binding.createPlaylistPrompt.getCurrentPlaylistTitle())
        }
        binding.createPlaylistPrompt.setCancelButtonFunctionality {
            binding.createPlaylistPrompt.closePrompt()
            viewModel.clearPreparedSongsForPlaylists()
        }
    }

    /**
     * Sets up the adding a song to a Playlist Prompt functionality.
     */
    private fun setupPlaylistPrompt() {
        //When add button is clicked, I should add songs into playlists
        binding.playlistPrompt.onAddButtonClick {
            val checkedPlaylists: List<String> = viewModel.checkedPlaylists.value ?: listOf()
            val playlistAddSongs: List<MediaItem> = viewModel.playlistAddSongs.value ?: listOf()

            parentViewModel.addSongsToAPlaylist(
                checkedPlaylists,
                playlistAddSongs
            )

            viewModel.clearPreparedSongsForPlaylists()
            binding.playlistPrompt.closePrompt()
        }

        binding.playlistPrompt.onCreateNewPlaylistClicked {
            binding.createPlaylistPrompt.showPrompt()
        }
        binding.playlistPrompt.onCloseButtonClicked {
            binding.createPlaylistPrompt.closePrompt()
            binding.playlistPrompt.closePrompt()
        }

        binding.playlistPrompt.setPlaylistCheckedHandler { playlistTitle, isChecked ->
            viewModel.updateCheckedPlaylists(playlistTitle, isChecked)
        }

        viewModel.isPlaylistPromptAddClickable.observe(viewLifecycleOwner) { isClickable ->
            binding.playlistPrompt.updateAddButtonClickability(isClickable)
        }
    }

    //TODO move this logic ?
    private fun handleSongSetting(menuOption: MenuOptionUtil.MenuOption, mediaItem: List<MediaItem>) {
        viewModel.prepareSongForPlaylists(mediaItem)

        when (menuOption) {
            ADD_TO_PLAYLIST -> handleAddToPlaylist()
            ADD_TO_QUEUE -> handleAddToQueue(mediaItem)
            CHECK_STATS -> handleCheckStats()
            else -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
        }
    }

    private fun handleAddToPlaylist() {
        binding.playlistPrompt.showPrompt()
    }

    private fun handleAddToQueue(songs: List<MediaItem>?) {
        songs?.let {
            parentViewModel.addSongsToEndOfQueue(it)
        }
    }

    private fun handleCheckStats() {
        //TODO...
    }

    /**
     * Shows a prompt for the user to choose a playlist or album.
     * Should show when there is no songs in the current song list, not an empty playlist.
     */
    private fun determineIfShowingInformationScreen(songs: List<MediaItem>, songGroupType: SongGroupType) {
        //Only show user information screen on app startup [?]
        if( songGroupType != SongGroupType.PLAYLIST && songs.isEmpty()) {
            binding.songListInformationScreen.visibility = View.VISIBLE
        } else {
            binding.songListInformationScreen.visibility = View.GONE
        }
    }

    private fun setupPage() {
        binding.songGroupInfo.setSongGroupTitleText("PARTICULAR ALBUM - ARTIST")

        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        //First Icon will be the playlists
        binding.songListInformationScreen.setFirstInfo("Choose a playlist to View")
        binding.songListInformationScreen.setFirstIcon(resources.getDrawable(R.drawable.playlist_icon)) //TODO add theme here?
        binding.songListInformationScreen.setFirstIconCallback { parentViewModel.setPage(PageType.PLAYLIST_PAGE) }

        //Second Icon will be the Albums
        binding.songListInformationScreen.setSecondInfo("Choose an album to View")
        binding.songListInformationScreen.setSecondIcon(resources.getDrawable(R.drawable.browse_album_icon)) //TODO add theme here?
        binding.songListInformationScreen.setSecondIconCallback { parentViewModel.setPage(PageType.ALBUM_PAGE) }
    }
}
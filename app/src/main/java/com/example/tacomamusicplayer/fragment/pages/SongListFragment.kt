package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.tacomamusicplayer.util.SongSettingsUtil
import com.example.tacomamusicplayer.util.SongSettingsUtil.Setting.*
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.example.tacomamusicplayer.viewmodel.SongListViewModel
import timber.log.Timber

class SongListFragment(

): Fragment() {

    //TODO what to do if the current song list is empty?

    //TODO Give the ability for the user to load an entire album or playlist into a new queue...

    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: SongListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSonglistBinding.inflate(inflater)

        //TODO I'll instead query the current mediaItem list -> this can be a playlist or an album of songs

        parentViewModel.currentSongList.observe(viewLifecycleOwner) {songs ->
            Timber.d("onCreateView: songs.size=${songs.size}")
            binding.displayRecyclerview.adapter = SongListAdapter(
                songs,
                this::handleSongSetting
            )
            determineIfShowingInformationScreen(songs)
        }

        parentViewModel.songListTitle.observe(viewLifecycleOwner) { title ->
            binding.sectionTitle.text = title
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

        binding.overallAddIcon.setOnClickListener {
            parentViewModel.currentSongList.value?.let { currentSongs ->
                parentViewModel.addSongsToEndOfQueueViaController(currentSongs)

                //TODO for overall playlist or album, I probably want to clear the previous playlist first...
            }
        }

        binding.overallMenuIcon.setOnClickListener {
            //TODO open the menu prompt
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

            parentViewModel.addSongToPlaylists(
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

    private fun handleSongSetting(setting: SongSettingsUtil.Setting, mediaItem: MediaItem) {
        viewModel.prepareSongForPlaylists(mediaItem)

        when (setting) {
            ADD_TO_PLAYLIST -> handleAddToPlaylist()
            ADD_TO_QUEUE -> handleAddToQueue(mediaItem)
            CHECK_STATS -> handleCheckStats()
            UNKNOWN -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
        }
    }

    private fun handleAddToPlaylist() {
        binding.playlistPrompt.showPrompt()
    }

    private fun handleAddToQueue(mediaItem: MediaItem?) {
        mediaItem?.let {
            parentViewModel.addSongToEndOfQueueViaController(it)
        }
    }

    private fun handleCheckStats() {
        //TODO...
    }

    /**
     * Shows a prompt for the user to choose a playlist or album.
     * Should show when there is no songs in the current song list, not an empty playlist.
     */
    private fun determineIfShowingInformationScreen(songs: List<MediaItem>) {
        if(songs.isEmpty()) {
            binding.songListInformationScreen.visibility = View.VISIBLE
        } else {
            binding.songListInformationScreen.visibility = View.GONE
        }
    }

    //TODO create a new adapter for the new prompt??
    private fun determineIfShowingEmptyPlaylistScreen() {
        //TODO show Empty Playlist screen...
    }

    //Should display "Add to Playlist Prompt" Based on available Playlists.
    private fun displayAddToPlaylistPrompt(playlists: List<Playlist>) { //TODO should be given a list of playlists from the viewmodel
        //Make view visible
        //Make the view have a new recyclerview based on playlists from viewmodel...
    }

    /**
     * Clicking the "Add to Playlist" option should open a prompt where the user can add song/s to playlist/s.
     */
    private fun onPlaylistSettingClicked() {
        val playlistOptions = parentViewModel.getCurrentPlaylists()
        displayAddToPlaylistPrompt(playlistOptions)
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

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
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
import com.example.tacomamusicplayer.databinding.FragmentSonglistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.example.tacomamusicplayer.viewmodel.MusicChooserViewModel
import timber.log.Timber

class SongListFragment(

): Fragment() {

    //TODO what to do if the current song list is empty?

    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

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
                parentViewModel::addSongToEndOfQueueViaController, //TODO This is way better, I need to comment out the old logic...
                { /*TODO what to do on menu icon click [hint show the menu icon stuff]*/ }
            )
            determineIfShowingInformationScreen(songs)
        }

        parentViewModel.songListTitle.observe(viewLifecycleOwner) { title ->
            binding.sectionTitle.text = title
        }

        setupPage()

        return binding.root
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

    private fun determineIfShowingEmptyPlaylistScreen() {
        //TODO show Empty Playlist screen...
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
package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.PlaylistAdapter
import com.example.tacomamusicplayer.constants.Const
import com.example.tacomamusicplayer.databinding.FragmentPlaylistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel

class PlaylistFragment(

): Fragment() {
    private lateinit var binding: FragmentPlaylistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPlaylistBinding.inflate(inflater)

        parentViewModel.availablePlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.displayRecyclerview.adapter = PlaylistAdapter(playlists, this::onPlaylistClick)
        }

        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE
            binding.createPlaylistPrompt.resetUserInput()
            binding.createPlaylistPrompt.visibility = View.VISIBLE
        }

        setupCreatePlaylistPrompt()
        setupPage()

        return binding.root
    }

    private fun setupCreatePlaylistPrompt() {
        //set playlist prompt hint
        binding.createPlaylistPrompt.setTextInputHint(Const.PLAYLIST_HINT)

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
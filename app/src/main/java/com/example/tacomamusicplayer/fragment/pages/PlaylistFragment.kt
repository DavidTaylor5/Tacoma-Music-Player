package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.PlaylistAdapter
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
            binding.createPlaylistPrompt.resetCurrentPlaylistTitle()
            binding.createPlaylistPrompt.visibility = View.VISIBLE
        }

        binding.createPlaylistPrompt.setAddButtonFunctionality {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistPrompt.visibility = View.GONE
            parentViewModel.createNamedPlaylist(binding.createPlaylistPrompt.getCurrentPlaylistTitle())
        }

        binding.createPlaylistPrompt.setCancelButtonFunctionality {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistPrompt.visibility = View.GONE
        }

        setupPage()

        return binding.root
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
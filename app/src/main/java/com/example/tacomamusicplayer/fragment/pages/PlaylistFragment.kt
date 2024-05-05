package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.PlaylistAdapter
import com.example.tacomamusicplayer.databinding.FragmentPlaylistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel

class PlaylistFragment(
    val navigationCallback: (PageType) -> Unit
): Fragment() {

    private lateinit var binding: FragmentPlaylistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPlaylistBinding.inflate(inflater)


        //TODO I'll want to query data store for playlists

        setupPage()

        return binding.root
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PLAYLISTS"

        binding.displayRecyclerview.adapter = PlaylistAdapter(listOf(MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
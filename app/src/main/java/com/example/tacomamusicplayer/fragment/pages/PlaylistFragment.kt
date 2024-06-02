package com.example.tacomamusicplayer.fragment.pages

import android.opengl.Visibility
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.PlaylistAdapter
import com.example.tacomamusicplayer.databinding.FragmentPlaylistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.example.tacomamusicplayer.viewmodel.MusicChooserViewModel

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


        //TODO I'll want to query data store for playlists
        //TODO Add actual functionality of adding a playlist here...

        parentViewModel.getAllPlaylistLiveData().observe(viewLifecycleOwner) { playlists ->
            //set the rv adapter here... or modify the rv here?...
            binding.displayRecyclerview.adapter = PlaylistAdapter(playlists)
        }

        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE
            //binding.createPlaylistTitleInputText.text = Editable.Factory.getInstance().newEditable("")
            binding.createPlaylistTitleInputText.setText("")
            binding.createPlaylistView.visibility = View.VISIBLE
        }

        binding.addButton.setOnClickListener {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistView.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistView.visibility = View.GONE
        }

        setupPage()

        return binding.root
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PLAYLISTS"

        //binding.displayRecyclerview.adapter = PlaylistAdapter(listOf(MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
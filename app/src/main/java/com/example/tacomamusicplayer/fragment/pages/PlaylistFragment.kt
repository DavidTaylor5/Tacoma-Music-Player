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

    //TODO I need to make sure that I can't make multiple playlists with the same name...

    //TODO Create a function that can turn songData into a MediaItem and albumData into a MediaItem...

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

        parentViewModel.availablePlaylists.observe(viewLifecycleOwner) { playlists ->
            //set the rv adapter here... or modify the rv here?...
            binding.displayRecyclerview.adapter = PlaylistAdapter(playlists, this::onPlaylistClick)
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
            parentViewModel.createNamedPlaylist(binding.createPlaylistTitleInputText.text.toString())
        }

        binding.cancelButton.setOnClickListener {
            binding.fab.visibility = View.VISIBLE
            binding.createPlaylistView.visibility = View.GONE
        }

        setupPage()

        return binding.root
    }

    private fun onPlaylistClick(playlistTitle: String) { //TODO I need to use this function just like AlbumListFragment...
        //parentViewModel.querySongsFromAlbum(albumTitle)
        //TODO I want to query songs from the database... like parentViewModel.querySongsFromPlaylist(playlistTitle)
        parentViewModel.querySongsFromPlaylist(playlistTitle) //TODO is the playlist name the id?
        parentViewModel.setPage(PageType.SONG_PAGE)
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PLAYLISTS"

        //binding.displayRecyclerview.adapter = PlaylistAdapter(listOf(MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
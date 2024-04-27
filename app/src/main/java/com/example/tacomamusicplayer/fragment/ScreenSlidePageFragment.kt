package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.AlbumListAdapter
import com.example.tacomamusicplayer.adapter.PlaylistAdapter
import com.example.tacomamusicplayer.adapter.SongListAdapter
import com.example.tacomamusicplayer.databinding.FragmentScreenSlidePageBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class ScreenSlidePageFragment(val position: Int): Fragment() {

    private lateinit var binding: FragmentScreenSlidePageBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentScreenSlidePageBinding.inflate(inflater)

        val observer: Observer<List<MediaItem>> = object : Observer<List<MediaItem>> {
            override fun onChanged(value: List<MediaItem>) {
                Timber.d("onCreateView: found albumList.size=${value.size}")
            }
        }

        //Now I just need to create different fragments for each type?
        parentViewModel.albumMediaItemList.observe(viewLifecycleOwner, observer)

        setupPage()

        return binding.root
    }

    private fun setupPage() {
        Timber.d("setupPage: ")

        when(position) {
            PageType.PLAYLIST_PAGE.type() -> {
                setupPlaylistPage()
            }
            PageType.ALBUM_PAGE.type() -> {
                setupAlbumPage()
            }
            PageType.SONG_PAGE.type() -> {
                setupSongPage()
            }
            else -> {
                Timber.d("setupPage: position doesn't match case in setupPage()")
            }
        }
    }

    private fun setupPlaylistPage() {
        binding.sectionTitle.text = "PLAYLISTS"

        binding.displayRecyclerview.adapter = PlaylistAdapter(listOf("What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip"))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    private fun setupAlbumPage() {
        binding.sectionTitle.text = "ALBUMS"

        binding.displayRecyclerview.adapter = AlbumListAdapter(listOf("What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip"))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    private fun setupSongPage() {
        binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

        binding.displayRecyclerview.adapter = SongListAdapter(listOf("What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip"))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

}
package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.AlbumListAdapter
import com.example.tacomamusicplayer.databinding.FragmentAlbumlistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.example.tacomamusicplayer.viewmodel.MusicChooserViewModel
import timber.log.Timber

class AlbumListFragment(

): Fragment() {

    private lateinit var binding: FragmentAlbumlistBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    //private val parentFragViewModel: MusicChooserViewModel by viewModels({requireParentFragment()})

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAlbumlistBinding.inflate(inflater)

        val observer: Observer<List<MediaItem>> =
            Observer { mediaList ->
                Timber.d("onCreateView: found albumList.size=${mediaList.size}")
                binding.displayRecyclerview.adapter = AlbumListAdapter(mediaList, this::onAlbumClick)
            }

        //Now I just need to create different fragments for each type?
        parentViewModel.albumMediaItemList.observe(viewLifecycleOwner, observer)

        setupPage()

        return binding.root
    }

    private fun onAlbumClick(albumTitle: String) {
        parentViewModel.querySongsFromAlbum(albumTitle)
        parentViewModel.setPage(PageType.SONG_PAGE)
    }

    private fun setupPage() {
        binding.sectionTitle.text = "ALBUMS"
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
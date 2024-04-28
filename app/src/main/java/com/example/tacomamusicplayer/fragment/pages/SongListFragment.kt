package com.example.tacomamusicplayer.fragment.pages

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
import com.example.tacomamusicplayer.databinding.FragmentAlbumlistBinding
import com.example.tacomamusicplayer.databinding.FragmentScreenSlidePageBinding
import com.example.tacomamusicplayer.databinding.FragmentSonglistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class SongListFragment(): Fragment() {

    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSonglistBinding.inflate(inflater)

//        val observer: Observer<List<MediaItem>> = object : Observer<List<MediaItem>> {
//            override fun onChanged(value: List<MediaItem>) {
//                Timber.d("onCreateView: found albumList.size=${value.size}")
//            }
//        }
//
//        //Now I just need to create different fragments for each type?
//        parentViewModel.albumMediaItemList.observe(viewLifecycleOwner, observer)

        //TODO I'll instead query the current mediaItem list -> this can be a playlist or an album of songs

        setupPage()

        return binding.root
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

        binding.displayRecyclerview.adapter = SongListAdapter(listOf("What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip","What", "The", "Flip"))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
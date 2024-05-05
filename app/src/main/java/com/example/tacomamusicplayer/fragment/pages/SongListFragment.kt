package com.example.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.adapter.SongListAdapter
import com.example.tacomamusicplayer.databinding.FragmentSonglistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.viewmodel.MainViewModel

class SongListFragment(
    val navigationCallback: (PageType) -> Unit
): Fragment() {

    private lateinit var binding: FragmentSonglistBinding
    private val parentViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSonglistBinding.inflate(inflater)

        //TODO I'll instead query the current mediaItem list -> this can be a playlist or an album of songs

        setupPage()

        return binding.root
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

        binding.displayRecyclerview.adapter = SongListAdapter(listOf(MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, MediaItem.EMPTY, ))
        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        binding.songListInformationScreen.setInformationText("THIS IS INFORMATION TEXT")
        binding.songListInformationScreen.setImageIcon(resources.getDrawable(R.drawable.browse_album_icon)) //TODO add theme here?
        binding.songListInformationScreen.setImageClickCallback { navigationCallback(PageType.ALBUM_PAGE) }
    }
}
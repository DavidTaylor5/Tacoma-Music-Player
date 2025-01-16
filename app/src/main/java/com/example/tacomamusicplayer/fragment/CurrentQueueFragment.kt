package com.example.tacomamusicplayer.fragment

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
import com.example.tacomamusicplayer.databinding.FragmentCurrentQueueBinding
import com.example.tacomamusicplayer.databinding.FragmentSonglistBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.util.SongSettingsUtil
import com.example.tacomamusicplayer.viewmodel.CurrentQueueViewModel
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class CurrentQueueFragment: Fragment() {

    //TODO what to do if the current song list is empty?

    private lateinit var binding: FragmentCurrentQueueBinding
    private val parentViewModel: MainViewModel by activityViewModels()
    private val viewModel: CurrentQueueViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCurrentQueueBinding.inflate(inflater)

        //TODO I'll instead query the current mediaItem list -> this can be a playlist or an album of songs
        parentViewModel.songQueue.observe(viewLifecycleOwner) {songs ->
            Timber.d("onCreateView: songs.size=${songs.size}")
            binding.displayRecyclerview.adapter = SongListAdapter(
                songs,
                this::handleSongSetting
            )
            determineIfShowingEmptyPlaylistScreen(songs)
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
    private fun determineIfShowingEmptyPlaylistScreen(songs: List<MediaItem>) {
        if(songs.isEmpty()){
            binding.noMusicAddedText.visibility = View.VISIBLE
        } else {
            binding.noMusicAddedText.visibility = View.GONE
        }
    }

    //TODO update this later...
    private fun handleSongSetting(setting: SongSettingsUtil.Setting, mediaItems: List<MediaItem> = listOf()) {
//        when (setting) {
//            SongSettingsUtil.Setting.ADD_TO_PLAYLIST -> handleAddToPlaylist()
//            SongSettingsUtil.Setting.ADD_TO_QUEUE -> handleAddToQueue(mediaItem)
//            SongSettingsUtil.Setting.CHECK_STATS -> handleCheckStats()
//            SongSettingsUtil.Setting.UNKNOWN -> { Timber.d("handleSongSetting: UNKNOWN SETTING") }
//        }
    }

    private fun setupPage() {
        binding.sectionTitle.text = "PARTICULAR ALBUM - ARTIST"

        binding.displayRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}
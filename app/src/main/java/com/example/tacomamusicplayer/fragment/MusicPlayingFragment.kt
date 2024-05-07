package com.example.tacomamusicplayer.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber


class MusicPlayingFragment: Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentMusicPlayingBinding

    private var controller: MediaController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView: ")
        binding = FragmentMusicPlayingBinding.inflate(inflater)
        return binding.root
    }

    @OptIn(UnstableApi::class) override fun onStart() {
        super.onStart()

        parentViewModel.mediaController.observe(this) { controller ->
            binding.playerView.player = controller
            this.controller = controller
            binding.playerView.showController()
        }

        parentViewModel.currentSongList.observe(this) { songs ->
            Timber.d("onStart: CHANGING controller songs to be songs.size=${songs.size}")

            //TODO remove this and change with song queue
            controller?.clearMediaItems()
            controller?.addMediaItems(songs)
        }


        binding.psychoButton.setOnClickListener {
            findNavController().navigate(ScreenType.PERMISSION_DENIED_SCREEN.route())
        }

        binding.navigateChooseMusic.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
        }
    }
}
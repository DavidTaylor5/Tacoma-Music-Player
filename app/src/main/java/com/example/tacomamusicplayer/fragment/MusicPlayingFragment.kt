package com.example.tacomamusicplayer.fragment

import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.annotation.OptIn
import androidx.core.view.GestureDetectorCompat
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
import com.example.tacomamusicplayer.util.MusicGestureDetector
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

        setupLibraryButtonAnimation(binding)

        return binding.root
    }

    private fun setupLibraryButtonAnimation(binding:FragmentMusicPlayingBinding) {
        binding.libraryButton!!.setBackgroundResource(R.drawable.library_button_animation)
        val frameAnimation = binding.libraryButton.background as AnimationDrawable
        frameAnimation.start()
    }

    @OptIn(UnstableApi::class) override fun onStart() {
        super.onStart()

        parentViewModel.mediaController.observe(this) { controller ->
            binding.playerView.player = controller
            this.controller = controller
            binding.playerView.showController()
        }

        parentViewModel.songQueue.observe(this) { songs ->
            controller?.addMediaItems(songs)
        }

        binding.psychoButton.setOnClickListener {
            findNavController().navigate(ScreenType.PERMISSION_DENIED_SCREEN.route())
        }

        binding.navigateChooseMusic.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
        }

        binding.libraryButton?.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
        }
    }

}
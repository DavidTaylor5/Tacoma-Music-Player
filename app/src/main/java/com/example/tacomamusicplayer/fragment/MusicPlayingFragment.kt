package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber


class MusicPlayingFragment: Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentMusicPlayingBinding

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
            binding.playerView.showController()
        }


        binding.psychoButton.setOnClickListener {
            findNavController().navigate(ScreenType.PERMISSION_DENIED_SCREEN.route())
        }

        binding.navigateChooseMusic.setOnClickListener {
            findNavController().navigate(ScreenType.MUSIC_CHOOSER_SCREEN.route())
        }
    }
}
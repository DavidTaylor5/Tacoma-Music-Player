package com.andaagii.tacomamusicplayer.fragment.pages

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MusicPlayingFragment: Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentMusicPlayingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: ")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView: ")
        binding = FragmentMusicPlayingBinding.inflate(inflater)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED ) {
                parentViewModel.playbackStateFlow.collect { state ->

                    if(state.controlState.isPlaying) {
                        binding.playButton?.setBackgroundResource(R.drawable.baseline_pause_24)
                    } else {
                        binding.playButton?.setBackgroundResource(R.drawable.baseline_play_arrow_24)
                    }

                    state.positionState.currentlyPlayingSong?.let {
                        updateUIForCurrentSong(it)
                    }

                    showActivePlayer(
                        show = !SongData.isNullSong(state.positionState.currentlyPlayingSong)
                    )

                    when(state.controlState.loopMode) {
                        Player.REPEAT_MODE_OFF -> {  binding.loopToggle?.setBackgroundResource(R.drawable.one_x) }
                        Player.REPEAT_MODE_ONE -> {  binding.loopToggle?.setBackgroundResource(R.drawable.repeat_one) }
                        Player.REPEAT_MODE_ALL -> {  binding.loopToggle?.setBackgroundResource(R.drawable.repeat) }
                    }

                    if(state.controlState.shuffleMode == ShuffleType.SHUFFLED) {
                        binding.shuffleToggle?.setBackgroundResource(R.drawable.shuffle)
                    } else {
                        binding.shuffleToggle?.setBackgroundResource(R.drawable.right_arrow)
                    }
                }
            }
        }

        binding.prevButton?.setOnClickListener {
            parentViewModel.seekPreviousSong()
        }

        binding.nextButton?.setOnClickListener {
            parentViewModel.seekNextSong()
        }

        binding.playButton?.setOnClickListener {
            parentViewModel.flipPlayingState()
        }

        binding.loopToggle?.setOnClickListener {
            parentViewModel.flipLoopMode()
        }

        binding.shuffleToggle?.setOnClickListener {
            parentViewModel.flipShuffleState()
        }

        binding.seekBack?.setOnClickListener {
            parentViewModel.seekBackward()
        }

        binding.seekForward?.setOnClickListener {
            parentViewModel.seekForward()
        }

        return binding.root
    }

    override fun onResume() {
        Timber.d("onResume: ")
        super.onResume()
    }

    override fun onPause() {
        Timber.d("onPause: ")
        super.onPause()
    }

    private fun updateUIForCurrentSong(songData: SongData) {
        updateCurrentSongArt(songData.albumTitle, songData.artworkUri)
        updateCurrentSongTitle(songData.songTitle)
        updateCurrentSongArtist(songData.artist)
        updateCurrentAlbumTitle(songData.albumTitle)
    }

    private fun updateCurrentSongArt(albumTitle: String, artworkUri: String) {
        val customImage = "album_${albumTitle}"
        UtilImpl.drawMediaItemArt(
            binding.songArt!!,
            artworkUri.toUri(),
            Size(500, 500),
            customImage,
            synchronous = true
        )
    }

    private fun updateCurrentSongTitle(songTitle: String) {
        binding.songTitleTextview?.text  = songTitle
    }

    private fun updateCurrentSongArtist(artist: String) {
        binding.artistNameTextview?.text  = artist
    }

    private fun updateCurrentAlbumTitle(albumTitle: String) {
        binding.albumTitleTextview?.text  = albumTitle
    }

    /**
     * Determines whether to show the default (no music playing) or active player.
     */
    private fun showActivePlayer(show: Boolean) {
        if(show) {
            binding.chopperDefault?.visibility = View.GONE
            binding.activePlayerContent?.visibility =View.VISIBLE
        } else {
            binding.chopperDefault?.visibility = View.VISIBLE
            binding.activePlayerContent?.visibility =View.GONE
        }
    }
}
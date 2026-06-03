package com.andaagii.tacomamusicplayer.fragment.pages

import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.launch
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.andaagii.tacomamusicplayer.enumtype.ShuffleType
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Full-screen player page hosted at index 1 in `PlayerDisplayFragment`'s `ViewPager2`.
 *
 * Collects [MainViewModel] StateFlows for shuffle mode, loop mode, play/pause state, and song
 * metadata in [onCreateView] with `repeatOnLifecycle(STARTED)`, which restarts collection each
 * time the page becomes visible in the ViewPager2.
 *
 * Swipe/double-tap gestures to collapse the player are handled by the parent
 * `PlayerDisplayFragment`, not here.
 */
@AndroidEntryPoint
class MusicPlayingFragment : Fragment() {

    private val parentViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentMusicPlayingBinding

    /**
     * Cached reference to the active [MediaController]. Stored here so click listeners can
     * invoke playback commands without hitting the ViewModel on every tap.
     */
    private var controller: MediaController? = null

    /**
     * The song metadata most recently rendered to the UI. Compared against incoming
     * [MainViewModel.currentPlayingSongInfo] emissions to skip redundant UI redraws when the
     * same song data re-emits without an actual track change.
     */
    private var currentSongInfo: SongData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: ")
        super.onCreate(savedInstanceState)
    }

    @OptIn(UnstableApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView: ")
        binding = FragmentMusicPlayingBinding.inflate(inflater)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.mediaController.collect { controller ->
                    controller ?: return@collect
                    binding.playerView.player = controller
                    this@MusicPlayingFragment.controller = controller
                    binding.playerView.showController()
                    updateUIForCurrentSong()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.currentPlayingSongInfo.collect { currentSong ->
                    showActivePlayer(show = currentSong != null && !SongData.isNullSong(currentSong))
                    if (currentSong != null && currentSong != currentSongInfo) {
                        currentSongInfo = currentSong
                        updateUIForCurrentSong()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.loopMode.collect { repeatMode ->
                    Timber.d("onCreateView: repeatMode=$repeatMode")
                    when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> { binding.loopToggle?.setBackgroundResource(R.drawable.one_x) }
                        Player.REPEAT_MODE_ONE -> { binding.loopToggle?.setBackgroundResource(R.drawable.repeat_one) }
                        Player.REPEAT_MODE_ALL -> { binding.loopToggle?.setBackgroundResource(R.drawable.repeat) }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.shuffleMode.collect { isShuffled ->
                    Timber.d("onCreateView: isShuffled=$isShuffled")
                    if (isShuffled == ShuffleType.SHUFFLED) {
                        binding.shuffleToggle?.setBackgroundResource(R.drawable.shuffle)
                    } else {
                        binding.shuffleToggle?.setBackgroundResource(R.drawable.right_arrow)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.isPlaying.collect { isPlaying ->
                    Timber.d("onCreateView: isPlaying=$isPlaying")
                    if (isPlaying) {
                        binding.playButton?.setBackgroundResource(R.drawable.baseline_pause_24)
                    } else {
                        binding.playButton?.setBackgroundResource(R.drawable.white_play_arrow)
                    }
                }
            }
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

    /**
     * Refreshes all player UI elements from the current [MediaController] state.
     *
     * Called whenever the controller is first bound or the active song changes.
     */
    private fun updateUIForCurrentSong() {
        updateCurrentSongArt()
        updateCurrentSongTitle()
        updateCurrentSongArtist()
        updateCurrentAlbumTitle()
    }

    /** Loads and displays the artwork for the currently playing track. */
    private fun updateCurrentSongArt() {
        this.context?.resources?.let { res ->
            controller?.mediaMetadata?.let { metadata ->
                val customImage = "album_${metadata.albumTitle}"
                UtilImpl.drawMediaItemArt(
                    binding.songArt!!,
                    metadata.artworkUri ?: Uri.EMPTY,
                    Size(500, 500),
                    customImage,
                    synchronous = true
                )
            }
        }
    }

    /** Updates the song title text view from the controller's current metadata. */
    private fun updateCurrentSongTitle() {
        controller?.mediaMetadata?.title?.let { title ->
            binding.songTitleTextview?.text = title
        }
    }

    /** Updates the artist name text view from the controller's current metadata. */
    private fun updateCurrentSongArtist() {
        controller?.mediaMetadata?.artist?.let { artist ->
            binding.artistNameTextview?.text = artist
        }
    }

    /** Updates the album title text view from the controller's current metadata. */
    private fun updateCurrentAlbumTitle() {
        controller?.mediaMetadata?.albumTitle?.let { albumTitle ->
            binding.albumTitleTextview?.text = albumTitle
        }
    }

    override fun onStart() {
        super.onStart()

        binding.prevButton?.setOnClickListener {
            Timber.d("prevButton_onClick: ")
            controller?.seekToPrevious()
        }

        binding.playButton?.setOnClickListener { button ->
            Timber.d("playButton_onClick: ")
            controller?.let {
                if (!it.isPlaying) {
                    button.setBackgroundResource(R.drawable.baseline_pause_24)
                    it.play()
                } else {
                    button.setBackgroundResource(R.drawable.baseline_play_arrow_24)
                    it.pause()
                }
            }
        }

        binding.loopToggle?.setOnClickListener {
            parentViewModel.flipLoopMode()
        }

        binding.shuffleToggle?.setOnClickListener {
            parentViewModel.flipShuffleState()
        }

        binding.seekBack?.setOnClickListener {
            controller?.seekBack()
        }

        binding.seekForward?.setOnClickListener {
            controller?.seekForward()
        }

        binding.nextButton?.setOnClickListener {
            Timber.d("nextButton_onClick: ")
            controller?.seekToNextMediaItem()
        }
    }

    /**
     * Toggles between the default (no-song) placeholder and the active player UI.
     *
     * When [show] is `true`, hides `chopperDefault` and reveals `activePlayerContent`.
     * When `false`, does the reverse so the placeholder fills the screen.
     *
     * @param show `true` to display the active player controls; `false` to show the placeholder.
     */
    private fun showActivePlayer(show: Boolean) {
        if (show) {
            binding.chopperDefault?.visibility = View.GONE
            binding.activePlayerContent?.visibility = View.VISIBLE
        } else {
            binding.chopperDefault?.visibility = View.VISIBLE
            binding.activePlayerContent?.visibility = View.GONE
        }
    }
}

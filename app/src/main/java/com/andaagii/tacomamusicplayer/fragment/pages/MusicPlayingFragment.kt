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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
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
 * Observes [MainViewModel] for shuffle mode, loop mode, play/pause state, and song
 * metadata. The `MediaController` reference is obtained in [onStart] rather than
 * [onCreateView] because [onStart] fires each time the fragment becomes visible in the
 * pager, ensuring the controller binding is refreshed after process restoration.
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView: ")
        binding = FragmentMusicPlayingBinding.inflate(inflater)
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

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()

        // Observe the controller here in onStart rather than onCreateView so that the
        // binding is refreshed every time this page becomes visible in the ViewPager2.
        parentViewModel.mediaController.observe(this) { controller ->
            binding.playerView.player = controller
            this.controller = controller
            binding.playerView.showController()

            // Sync UI with whatever is already loaded in the controller on first attach.
            updateUIForCurrentSong()

            if (controller.isPlaying) {
                binding.playButton?.setBackgroundResource(R.drawable.baseline_pause_24)
            } else {
                binding.playButton?.setBackgroundResource(R.drawable.baseline_play_arrow_24)
            }
        }

        parentViewModel.currentPlayingSongInfo.observe(this) { currentSong ->
            // Hide the active player controls when no song has been loaded yet.
            showActivePlayer(show = !SongData.isNullSong(currentSong))

            // Guard against redundant redraws when metadata re-emits for the same song.
            if (currentSong != currentSongInfo) {
                currentSongInfo = currentSong
                updateUIForCurrentSong()
            }
        }

        parentViewModel.loopMode.observe(this) { repeatMode ->
            Timber.d("onStart: repeatMode=$repeatMode")
            when (repeatMode) {
                Player.REPEAT_MODE_OFF -> { binding.loopToggle?.setBackgroundResource(R.drawable.one_x) }
                Player.REPEAT_MODE_ONE -> { binding.loopToggle?.setBackgroundResource(R.drawable.repeat_one) }
                Player.REPEAT_MODE_ALL -> { binding.loopToggle?.setBackgroundResource(R.drawable.repeat) }
            }
        }

        parentViewModel.shuffleMode.observe(this) { isShuffled ->
            Timber.d("onStart: isShuffled=$isShuffled")
            if (isShuffled == ShuffleType.SHUFFLED) {
                binding.shuffleToggle?.setBackgroundResource(R.drawable.shuffle)
            } else {
                binding.shuffleToggle?.setBackgroundResource(R.drawable.right_arrow)
            }
        }

        parentViewModel.isPlaying.observe(this) { isPlaying ->
            Timber.d("onStart: isPlaying=$isPlaying")
            if (isPlaying) {
                binding.playButton?.setBackgroundResource(R.drawable.baseline_pause_24)
            } else {
                binding.playButton?.setBackgroundResource(R.drawable.white_play_arrow)
            }
        }

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

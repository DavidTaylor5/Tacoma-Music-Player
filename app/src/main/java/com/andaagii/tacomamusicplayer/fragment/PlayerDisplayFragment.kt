package com.andaagii.tacomamusicplayer.fragment

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.andaagii.tacomamusicplayer.composables.NavigationControl
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.PlayerDisplayFragmentBinding
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Host fragment for the main `ViewPager2` swipe layout.
 *
 * Wires [ScreenSlidePagerAdapter], [com.andaagii.tacomamusicplayer.composables.NavigationControl], edge-to-edge window insets,
 * mini-player controls, and page-change callbacks. Observes [MainViewModel] for playback
 * state (to drive the mini-player) and navigation events (to programmatically scroll the pager).
 *
 * The mini-player overlay is shown on all pages except [PageType.PLAYER_PAGE] and when no song
 * is currently loaded. Tapping the mini-player scrolls to [PageType.PLAYER_PAGE].
 */
@AndroidEntryPoint
class PlayerDisplayFragment : Fragment() {
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var binding: PlayerDisplayFragmentBinding

    private val parentViewModel: MainViewModel by activityViewModels()

    /**
     * The currently visible ViewPager2 page index. Cached here so [updateMiniPlayerForCurrentSong]
     * can decide whether to show the mini-player without querying the pager on every metadata emit.
     */
    private var currPage: Int? = null

    /** Drives the active-tab highlight in [NavigationControl]. Updated on every page change. */
    private var currentNavPage by mutableStateOf(PageType.PLAYER_PAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: ")
        super.onCreate(savedInstanceState)
        pagerAdapter = ScreenSlidePagerAdapter(requireActivity())
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayerDisplayFragmentBinding.inflate(inflater)

        // Apply status-bar inset as a top margin on the inset spacer view so the pager
        // content sits below the system status bar. Returning CONSUMED prevents descendant
        // views from applying the same inset a second time.
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarInset!!) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }

        // Apply navigation-bar inset as a bottom margin on the tab control so it clears
        // the gesture handle / home indicator on edge-to-edge displays.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationControl) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        binding.pager.adapter = pagerAdapter
        // Keep all 5 pages alive simultaneously so swipes between non-adjacent pages don't
        // trigger Fragment recreation and lose transient UI state.
        binding.pager.offscreenPageLimit = 4

        // Land on the player page at app launch.
        currentNavPage = PageType.PLAYER_PAGE
        navigateToPlayerPage()

        val onPageChangedCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Timber.d("onPageSelected: position=$position")
                super.onPageSelected(position)

                currPage = position

                // Show the mini-player on every page except the full player, and only when
                // a song is actually loaded.
                val songInfo = parentViewModel.currentPlayingSongInfo.value
                if (position != PageType.PLAYER_PAGE.type() && songInfo != null && !SongData.isNullSong(songInfo)) {
                    binding.miniPlayerControls?.visibility = View.VISIBLE
                } else {
                    binding.miniPlayerControls?.visibility = View.GONE
                }

                parentViewModel.observeCurrentPage(PageType.determinePageFromPosition(position))
                currentNavPage = PageType.determinePageFromPosition(position)
            }
        }

        binding.pager.registerOnPageChangeCallback(onPageChangedCallback)

        // Seed the cached page so mini-player visibility is correct before the first user swipe.
        currPage = binding.pager.currentItem
        parentViewModel.observeCurrentPage(PageType.PLAYER_PAGE)

        binding.navigationControl.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NavigationControl(
                    currentPage = currentNavPage,
                    queueIconRes = R.drawable.queue_icon,
                    playerIconRes = R.drawable.play_circle_outline,
                    playlistIconRes = R.drawable.playlist_icon,
                    albumIconRes = R.drawable.browse_album_icon,
                    songIconRes = R.drawable.album_icon,
                    onPageSelected = { page -> parentViewModel.setPage(page) }
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.navigateToPage.collect { page ->
                    binding.pager.currentItem = page.type()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.isPlaying.collect { isPlaying ->
                    if (isPlaying) {
                        binding.miniPlayerPlayButton?.setBackgroundResource(R.drawable.baseline_pause_24)
                    } else {
                        binding.miniPlayerPlayButton?.setBackgroundResource(R.drawable.white_play_arrow)
                    }
                }
            }
        }

        binding.miniPlayerPlayButton?.setOnClickListener {
            parentViewModel.flipPlayingState()
        }

        binding.miniPlayerPrevButton?.setOnClickListener {
            parentViewModel.mediaController.value?.seekToPrevious()
        }

        binding.miniPlayerNextButton?.setOnClickListener {
            parentViewModel.mediaController.value?.seekToNextMediaItem()
        }

        binding.miniPlayerControls?.setOnClickListener {
            navigateToPlayerPage()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.currentPlayingSongInfo.collect { currentSong ->
                    currentSong?.let { updateMiniPlayerForCurrentSong(it) }
                }
            }
        }

        return binding.root
    }

    /** Scrolls the pager to the player page (index 1). */
    private fun navigateToPlayerPage() {
        binding.pager.currentItem = 1
    }

    /**
     * Updates the mini-player strip for [song].
     *
     * First resolves whether the container should be visible — hidden when [song] is null/empty,
     * or when the user is already on the player page. Then loads the artwork thumbnail and
     * sets the title + artist description text.
     *
     * @param song The currently playing track's metadata snapshot.
     */
    private fun updateMiniPlayerForCurrentSong(song: SongData) {
        val miniPlayerShowing = binding.miniPlayerControls?.visibility ?: View.GONE
        if (SongData.isNullSong(song)) {
            binding.miniPlayerControls?.visibility = View.GONE
        } else if (miniPlayerShowing == View.GONE && currPage != null && currPage != PageType.PLAYER_PAGE.type()) {
            binding.miniPlayerControls?.visibility = View.VISIBLE
        }

        // Load the album artwork thumbnail into the mini-player image view.
        val customImage = "album_${song.albumTitle}"
        UtilImpl.drawMediaItemArt(
            binding.miniPlayerImage!!,
            song.artworkUri.toUri(),
            Size(300, 300),
            customImage,
            synchronous = true
        )

        // Compose the "Song Title - Artist" description line.
        val songDescription = "${song.songTitle} - ${song.artist}"
        binding.miniPlayerDescription?.text = songDescription
    }
}

package com.andaagii.tacomamusicplayer.fragment

import android.os.Bundle
import android.util.Size
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.PlayerDisplayFragmentBinding
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Host fragment for the main `ViewPager2` swipe layout.
 *
 * Wires [ScreenSlidePagerAdapter], `CustomNavigationControl`, edge-to-edge window insets,
 * mini-player controls, and page-change callbacks. Observes [MainViewModel] for playback
 * state (to drive the mini-player) and navigation events (to programmatically scroll the pager).
 *
 * The mini-player overlay is shown on all pages except [PageType.PLAYER_PAGE] and when no song
 * is currently loaded. Tapping the mini-player scrolls to [PageType.PLAYER_PAGE].
 *
 * Double-tap or swipe-down on the player area navigates to [ScreenType.MUSIC_PLAYING_SCREEN].
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

    /**
     * Gesture detector that handles two interactions on the player area:
     * - **Double-tap** — navigates to [ScreenType.MUSIC_PLAYING_SCREEN].
     * - **Swipe-down** (vertical velocity > 500 px/s) — navigates to [ScreenType.MUSIC_PLAYING_SCREEN].
     */
    private val detector = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Timber.d("onDoubleTap: navigate to the music playing screen!")
            findNavController().navigate(ScreenType.MUSIC_PLAYING_SCREEN.route())
            return super.onDoubleTap(e)
        }

        override fun onDown(e: MotionEvent): Boolean {
            Timber.d("onDown: ")
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            Timber.d("onFling: e1=$e1, e2=$e2, velocityX=$velocityX, velocityY=$velocityY")

            // Threshold of 500 px/s distinguishes an intentional downward swipe from an
            // accidental brush while scrolling horizontally between pages.
            if (velocityY > 500) {
                Timber.d("onFling: navigate to the music playing screen!")
                findNavController().navigate(ScreenType.MUSIC_PLAYING_SCREEN.route())
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

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
        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYER_PAGE)
        navigateToPlayerPage()

        val onPageChangedCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Timber.d("onPageSelected: position=$position")
                super.onPageSelected(position)

                currPage = position

                // Show the mini-player on every page except the full player, and only when
                // a song is actually loaded.
                if (position != PageType.PLAYER_PAGE.type() && !SongData.isNullSong(parentViewModel.currentPlayingSongInfo.value)) {
                    binding.miniPlayerControls?.visibility = View.VISIBLE
                } else {
                    binding.miniPlayerControls?.visibility = View.GONE
                }

                parentViewModel.observeCurrentPage(PageType.determinePageFromPosition(position))

                when (position) {
                    PageType.QUEUE_PAGE.type() -> binding.navigationControl.setFocusOnNavigationButton(PageType.QUEUE_PAGE)
                    PageType.PLAYER_PAGE.type() -> binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYER_PAGE)
                    PageType.PLAYLIST_PAGE.type() -> binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYLIST_PAGE)
                    PageType.ALBUM_PAGE.type() -> binding.navigationControl.setFocusOnNavigationButton(PageType.ALBUM_PAGE)
                    PageType.SONG_PAGE.type() -> binding.navigationControl.setFocusOnNavigationButton(PageType.SONG_PAGE)
                }
            }
        }

        binding.pager.registerOnPageChangeCallback(onPageChangedCallback)

        binding.navigationControl.setQueueButtonOnClick { parentViewModel.setPage(PageType.QUEUE_PAGE) }
        binding.navigationControl.setPlayerButtonOnClick { parentViewModel.setPage(PageType.PLAYER_PAGE) }
        binding.navigationControl.setPlaylistButtonOnClick { parentViewModel.setPage(PageType.PLAYLIST_PAGE) }
        binding.navigationControl.setBrowseAlbumButtonOnClick { parentViewModel.setPage(PageType.ALBUM_PAGE) }
        binding.navigationControl.setAlbumButtonOnClick { parentViewModel.setPage(PageType.SONG_PAGE) }

        // Observe on requireActivity() rather than viewLifecycleOwner so navigation events
        // posted while the Fragment's view is being recreated are not missed.
        parentViewModel.navigateToPage.observe(requireActivity()) { page ->
            binding.pager.currentItem = page.type()
        }

        parentViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if (isPlaying) {
                binding.miniPlayerPlayButton?.setBackgroundResource(R.drawable.baseline_pause_24)
            } else {
                binding.miniPlayerPlayButton?.setBackgroundResource(R.drawable.white_play_arrow)
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

        // Observe on requireActivity() so artwork and title update even if the Fragment's
        // view is briefly torn down during a configuration change.
        parentViewModel.currentPlayingSongInfo.observe(requireActivity()) { currentSong ->
            updateMiniPlayerForCurrentSong(currentSong)
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

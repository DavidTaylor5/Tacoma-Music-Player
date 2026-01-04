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

@AndroidEntryPoint
class PlayerDisplayFragment: Fragment() {
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var binding: PlayerDisplayFragmentBinding

    private val parentViewModel: MainViewModel by activityViewModels()

    private val detector = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Timber.d("onDoubleTap: navigate to the music chooser screen!")

            //navigate to the music chooser fragment...
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

            if(velocityY > 500) {
                Timber.d("onFling: navigate to the music chooser screen!")

                //navigate to the music chooser fragment...
                findNavController().navigate(ScreenType.MUSIC_PLAYING_SCREEN.route())
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: ")
        super.onCreate(savedInstanceState)
        pagerAdapter =  ScreenSlidePagerAdapter(requireActivity())
    }

    override fun onStart() {
        super.onStart()
        //TODO add code to add to controller new music...
    }



//    private fun setupPlayingAnimation(binding: FragmentMusicChooserBinding) {
//        binding.playingAnimation!!.setBackgroundResource(R.drawable.playing_animation)
//        val frameAnimation = binding.playingAnimation.background as AnimationDrawable
//        frameAnimation.start()
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayerDisplayFragmentBinding.inflate(inflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarInset!!) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }

            //Return consumed if you don't want the window insets to keep passing down to descendant views
            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationControl) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
            }

            //Return consumed if you don't want the window insets to keep passing down to descendant views
            WindowInsetsCompat.CONSUMED
        }

        //val gesture = GestureDetector(container!!.context, detector)

        //setupPlayingAnimation(binding)

//        binding.playingAnimation!!.setOnTouchListener { v, event ->
//            gesture.onTouchEvent(event)
//        }

        binding.pager.adapter = pagerAdapter
        binding.pager.offscreenPageLimit = 4

        //Start app on player page
        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYER_PAGE)
        navigateToPlayerPage()

        val onPageChangedCallback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Timber.d("onPageSelected: position=$position")
                super.onPageSelected(position)

                // Don't show the mini player on the player page
                if(position != PageType.PLAYER_PAGE.type()) {
                    binding.miniPlayerControls?.visibility = View.VISIBLE
                } else {
                    binding.miniPlayerControls?.visibility = View.GONE
                }

                //observe the current page
                parentViewModel.observeCurrentPage(PageType.determinePageFromPosition(position))

                when (position) {
                    PageType.QUEUE_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.QUEUE_PAGE)
                    }

                    PageType.PLAYER_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYER_PAGE)
                    }

                    PageType.PLAYLIST_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYLIST_PAGE)
                    }

                    PageType.ALBUM_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.ALBUM_PAGE)
                    }

                    PageType.SONG_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.SONG_PAGE)
                    }
                }
            }
        }

        binding.pager.registerOnPageChangeCallback(onPageChangedCallback)

        binding.navigationControl.setQueueButtonOnClick {
            parentViewModel.setPage(PageType.QUEUE_PAGE)
        }

        binding.navigationControl.setPlayerButtonOnClick {
            parentViewModel.setPage(PageType.PLAYER_PAGE)
        }

        binding.navigationControl.setPlaylistButtonOnClick {
            parentViewModel.setPage(PageType.PLAYLIST_PAGE)
        }
        binding.navigationControl.setBrowseAlbumButtonOnClick {
            parentViewModel.setPage(PageType.ALBUM_PAGE)
        }
        binding.navigationControl.setAlbumButtonOnClick {
            parentViewModel.setPage(PageType.SONG_PAGE)
        }
        parentViewModel.navigateToPage.observe(requireActivity()) { page -> //todo test this, odd that activity instead of fragment is passed here...
            binding.pager.currentItem = page.type()
        }

        parentViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if(isPlaying) {
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

        parentViewModel.currentPlayingSongInfo.observe(requireActivity()) { currentSong ->
            updateMiniPlayerForCurrentSong(currentSong)
        }

        return binding.root
    }

    private fun navigateToPlayerPage() {
        binding.pager.currentItem = 1
    }

    private fun updateMiniPlayerForCurrentSong(song: SongData) {
        //Set mini player song image
        val customImage = "album_${song.albumTitle}"
        UtilImpl.drawMediaItemArt(
            binding.miniPlayerImage!!,
            song.artworkUri.toUri(),
            Size(300, 300),
            customImage,
            synchronous = true
        )

        //Set mini player description
        val songDescription = "${song.songTitle} - ${song.artist}"
        binding.miniPlayerDescription?.text = songDescription
    }
}
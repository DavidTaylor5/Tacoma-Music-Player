package com.example.tacomamusicplayer.fragment

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.FragmentMusicChooserBinding
import com.example.tacomamusicplayer.databinding.FragmentMusicPlayingBinding
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.enum.ScreenType
import com.example.tacomamusicplayer.viewmodel.MainViewModel
import com.example.tacomamusicplayer.viewmodel.MusicChooserViewModel
import timber.log.Timber

class MusicChooserFragment: Fragment() {
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var binding: FragmentMusicChooserBinding

    private val viewModel: MusicChooserViewModel by viewModels()
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
        super.onCreate(savedInstanceState)
        pagerAdapter =  ScreenSlidePagerAdapter(requireActivity())

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_up)
    }

    override fun onStart() {
        super.onStart()
        //TODO add code to add to controller new music...
    }

    private fun setupPlayingAnimation(binding: FragmentMusicChooserBinding) {
        binding.playingAnimation!!.setBackgroundResource(R.drawable.playing_animation)
        val frameAnimation = binding.playingAnimation.background as AnimationDrawable
        frameAnimation.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicChooserBinding.inflate(inflater)

        val gesture = GestureDetector(container!!.context, detector)

        setupPlayingAnimation(binding)

        binding.playingAnimation!!.setOnTouchListener { v, event ->
            gesture.onTouchEvent(event)
        }

        binding.pager.adapter = pagerAdapter

        val onPageChangedCallback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                when (position) {
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

        binding.navigationControl.setPlaylistButtonOnClick {
//            setPage(PageType.PLAYLIST_PAGE)
            parentViewModel.setPage(PageType.PLAYLIST_PAGE)
        }
        binding.navigationControl.setBrowseAlbumButtonOnClick {
//            setPage(PageType.ALBUM_PAGE)
            parentViewModel.setPage(PageType.ALBUM_PAGE)
        }
        binding.navigationControl.setAlbumButtonOnClick {
//            setPage(PageType.SONG_PAGE)
            parentViewModel.setPage(PageType.SONG_PAGE)
        }

        parentViewModel.currentpage.observe(requireActivity()) { page -> //todo test this, odd that activity instead of fragment is passed here...
            binding.pager.currentItem = page.type()
        }

        return binding.root
    }

//    private fun setPage(page: PageType) {
//        binding.pager.currentItem = page.type()
//    }
}
package com.andaagii.tacomamusicplayer.fragment

import android.graphics.pdf.PdfDocument.Page
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.andaagii.tacomamusicplayer.databinding.FragmentMusicChooserBinding
import com.andaagii.tacomamusicplayer.enum.PageType
import com.andaagii.tacomamusicplayer.enum.ScreenType
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import timber.log.Timber

class MusicChooserFragment: Fragment() {
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var binding: FragmentMusicChooserBinding

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
        binding = FragmentMusicChooserBinding.inflate(inflater)

        val gesture = GestureDetector(container!!.context, detector)

        //setupPlayingAnimation(binding)

//        binding.playingAnimation!!.setOnTouchListener { v, event ->
//            gesture.onTouchEvent(event)
//        }

        binding.playerSection?.setOnClickListener {
            //navigate to the music chooser fragment...
            findNavController().navigate(ScreenType.MUSIC_PLAYING_SCREEN.route())
        }

        //TODO If there are more options to add later on I will replace popupmenu with MenuView...
//        binding.sortingButton?.setOnClickListener {
//            binding.sortingPrompt?.visibility = View.VISIBLE
//        }

        binding.sortingButton?.setOnClickListener {
            val menu = PopupMenu(this.context, binding.sortingButton)

            parentViewModel.getCurrentPage()?.let {page ->
                if(page == PageType.PLAYLIST_PAGE) {
                    menu.menuInflater.inflate(R.menu.sorting_options_playlist, menu.menu)
                } else if (page == PageType.ALBUM_PAGE) {
                    menu.menuInflater.inflate(R.menu.sorting_options_album, menu.menu)
                } else {
                    Timber.d("onCreateView: not setting sortingButton, currentPage unknown")
                }
            }

            menu.setOnMenuItemClickListener {
                Toast.makeText(this.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()

                //Update the Sorting for the tab.
                val chosenSortingOption = SortingUtil.determineSortingOptionFromTitle(it.title.toString())
                parentViewModel.updateSortingForPage(chosenSortingOption)

                parentViewModel.getCurrentPage()?.let { page ->
                    if(page == PageType.PLAYLIST_PAGE) {
                        parentViewModel.savePlaylistSorting(requireContext(), chosenSortingOption)
                    } else if(page == PageType.ALBUM_PAGE) {
                        parentViewModel.saveAlbumSorting(requireContext(), chosenSortingOption)
                    }
                }

                return@setOnMenuItemClickListener true
            }
            menu.show()
        }

        binding.searchButton?.setOnClickListener {
            Toast.makeText(this.context, "Search Icon Pressed!", Toast.LENGTH_SHORT).show()
            //TODO add the search screen functionality...
        }

        binding.pager.adapter = pagerAdapter

        val onPageChangedCallback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                //observe the current page
                parentViewModel.observeCurrentPage(PageType.determinePageFromPosition(position))

                when (position) {
                    PageType.PLAYLIST_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYLIST_PAGE)
                        adjustForPlaylistPage()
                    }

                    PageType.ALBUM_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.ALBUM_PAGE)
                        adjustForAlbumPage()
                    }

                    PageType.SONG_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.SONG_PAGE)
                        adjustForSongPage()
                    }
                }
            }
        }

        binding.pager.registerOnPageChangeCallback(onPageChangedCallback)

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

        parentViewModel.isShowingSearchMode.observe(requireActivity()) { isShowing ->
            //TODO I need to show the cancel_search_button here to deactivate search mode.
        }

        return binding.root
    }

    private fun adjustForPlaylistPage() {
        binding.sortingButton?.visibility = View.VISIBLE
        binding.searchButton?.visibility = View.GONE
    }

    private fun adjustForAlbumPage() {
        binding.sortingButton?.visibility = View.VISIBLE
        binding.searchButton?.visibility = View.GONE
    }

    private fun adjustForSongPage() {
        binding.sortingButton?.visibility = View.INVISIBLE
        binding.searchButton?.visibility = View.VISIBLE
    }


}
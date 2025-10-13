package com.andaagii.tacomamusicplayer.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.PopupMenu
import android.widget.Toast
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
import com.andaagii.tacomamusicplayer.enumtype.LayoutType
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.enumtype.ScreenType
import com.andaagii.tacomamusicplayer.util.SortingUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import com.andaagii.tacomamusicplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PlayerDisplayFragment: Fragment() {
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var binding: PlayerDisplayFragmentBinding

    private var playlistPageCurrentLayout: LayoutType? = null
    private var playlistPageCurrentIcon: Int? = null
    private var albumPageCurrentLayout: LayoutType? = null
    private var albumPageCurrentIcon: Int? = null

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


        binding.sortingButton?.setOnClickListener {
            val menu = PopupMenu(
                this.context,
                binding.sortingButton,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )

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
            parentViewModel.handleSearchButtonClick()
        }



        binding.cancelSearchButton?.setOnClickListener {
            Toast.makeText(this.context, "Cancel Search Pressed!", Toast.LENGTH_SHORT).show()
            parentViewModel.handleCancelSearchButtonClick()
        }

        binding.pager.adapter = pagerAdapter
        binding.pager.offscreenPageLimit = 4

        //Start app on player page
        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYER_PAGE)
        navigateToPlayerPage()
        adjustForPlayerPage()

        val onPageChangedCallback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Timber.d("onPageSelected: position=$position")
                super.onPageSelected(position)

                //observe the current page
                parentViewModel.observeCurrentPage(PageType.determinePageFromPosition(position))

                when (position) {

                    PageType.QUEUE_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.QUEUE_PAGE)
                        adjustForQueuePage()
                    }

                    PageType.PLAYER_PAGE.type() -> {
                        binding.navigationControl.setFocusOnNavigationButton(PageType.PLAYER_PAGE)
                        adjustForPlayerPage()
                    }

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

        parentViewModel.layoutForPlaylistTab.observe(viewLifecycleOwner) { layout ->
            playlistPageCurrentLayout = layout
            playlistPageCurrentIcon = if(layout == LayoutType.TWO_GRID_LAYOUT) {
                R.drawable.baseline_grid_view_24
            } else {
                R.drawable.baseline_table_rows_24
            }
            binding.layoutButton?.setBackgroundResource(playlistPageCurrentIcon ?: 0)
        }

        parentViewModel.layoutForAlbumTab.observe(viewLifecycleOwner) { layout ->
            albumPageCurrentLayout = layout
            albumPageCurrentIcon = if(layout == LayoutType.TWO_GRID_LAYOUT) {
                R.drawable.baseline_grid_view_24
            } else {
                R.drawable.baseline_table_rows_24
            }
            binding.layoutButton?.setBackgroundResource(albumPageCurrentIcon ?: 0)
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

        parentViewModel.isShowingSearchMode.observe(requireActivity()) { isShowing ->
            if(isShowing) {
                binding.cancelSearchButton?.visibility = View.VISIBLE
                binding.searchButton?.visibility = View.GONE
            } else {
                binding.searchButton?.visibility = View.VISIBLE
                binding.cancelSearchButton?.visibility = View.GONE
            }
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
        UtilImpl.drawSongArt(
            binding.miniPlayerImage!!,
            Uri.parse(song.artworkUri),
            Size(300, 300),
            customImage,
            synchronous = true
        )

        //Set mini player description
        val songDescription = "${song.songTitle} - ${song.artist}"
        binding.miniPlayerDescription?.text = songDescription
    }

    private fun determineWhichSearchIconToShow() {
        if(parentViewModel.isShowingSearchMode.value == true) {
            binding.cancelSearchButton?.visibility = View.VISIBLE
            binding.searchButton?.visibility = View.GONE
        } else {
            binding.searchButton?.visibility = View.VISIBLE
            binding.cancelSearchButton?.visibility = View.GONE
        }
    }

    private fun removeSearchIcons() {
        binding.searchButton?.visibility = View.GONE
        binding.cancelSearchButton?.visibility = View.GONE
    }

    private fun adjustForQueuePage() {
        removeSearchIcons()
        binding.miniPlayerControls?.visibility = View.VISIBLE
        binding.pageTitle?.visibility = View.VISIBLE
        binding.pageTitle?.text = "Queue"
        binding.pageAction?.visibility = View.VISIBLE
        binding.pageAction?.text = "Clear"
        binding.pageAction?.setOnClickListener {
            parentViewModel.clearQueue()
        }

        binding.layoutButton?.visibility = View.GONE

        binding.buttonContainer?.visibility = View.GONE
    }

    private fun adjustForPlayerPage() {
        binding.pageTitle?.visibility = View.GONE
        binding.pageAction?.visibility = View.GONE
        removeSearchIcons()
        binding.miniPlayerControls?.visibility = View.GONE

        binding.layoutButton?.visibility = View.GONE

        binding.buttonContainer?.visibility = View.GONE
    }

    private fun adjustForPlaylistPage() {
        binding.pageTitle?.visibility = View.VISIBLE
        binding.pageTitle?.text = "Playlists"
        binding.pageAction?.visibility = View.VISIBLE
        binding.pageAction?.text = "Add Playlist"
        binding.pageAction?.setOnClickListener {
            parentViewModel.showAddPlaylistPromptOnPlaylistPage(true)
        }

        binding.sortingButton?.visibility = View.VISIBLE
        removeSearchIcons()
        binding.miniPlayerControls?.visibility = View.VISIBLE

        binding.layoutButton?.visibility = View.VISIBLE
        binding.layoutButton?.setOnClickListener {
            if(playlistPageCurrentLayout == LayoutType.LINEAR_LAYOUT) {
                //Update Layout State / Save to datastore
                parentViewModel.savePlaylistLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                //Update Layout State / Save to datastore
                parentViewModel.savePlaylistLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
            }
        }
        playlistPageCurrentIcon?.let { iconResId ->
            binding.layoutButton?.setBackgroundResource(iconResId)
        }

        binding.buttonContainer?.visibility = View.VISIBLE
    }

    private fun adjustForAlbumPage() {
        binding.pageTitle?.visibility = View.VISIBLE
        binding.pageTitle?.text = "Albums"
        binding.pageAction?.visibility = View.INVISIBLE
        binding.sortingButton?.visibility = View.VISIBLE
        removeSearchIcons()
        binding.miniPlayerControls?.visibility = View.VISIBLE

        binding.layoutButton?.visibility = View.VISIBLE
        binding.layoutButton?.setOnClickListener {
            if(albumPageCurrentLayout == LayoutType.LINEAR_LAYOUT) {
                //Update Layout State / Save to datastore
                parentViewModel.saveAlbumLayout(requireContext(), LayoutType.TWO_GRID_LAYOUT)
            } else {
                //Update Layout State / Save to datastore
                parentViewModel.saveAlbumLayout(requireContext(), LayoutType.LINEAR_LAYOUT)
            }
        }
        albumPageCurrentIcon?.let { iconResId ->
            binding.layoutButton?.setBackgroundResource(iconResId)
        }

        binding.buttonContainer?.visibility = View.VISIBLE
    }

    private fun adjustForSongPage() {
        binding.pageTitle?.visibility = View.VISIBLE
        binding.pageTitle?.text = "Songs"
        binding.pageAction?.visibility = View.INVISIBLE
        binding.sortingButton?.visibility = View.INVISIBLE
        determineWhichSearchIconToShow()
        binding.miniPlayerControls?.visibility = View.VISIBLE
        binding.layoutButton?.visibility = View.GONE

        binding.buttonContainer?.visibility = View.VISIBLE
    }
}
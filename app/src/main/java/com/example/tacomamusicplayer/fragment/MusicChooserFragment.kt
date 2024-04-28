package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.FragmentMusicChooserBinding
import com.example.tacomamusicplayer.enum.PageType

class MusicChooserFragment: Fragment() {

    private val PLAYLIST_FRAGMENT = 0
    private val BROWSE_ALBUMS_FRAGMENT = 1
    private val ALBUM_FRAGMENT = 2

    private lateinit var pagerAdapter: ScreenSlidePagerAdapter


    private lateinit var binding: FragmentMusicChooserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pagerAdapter =  ScreenSlidePagerAdapter(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicChooserBinding.inflate(inflater)

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
            binding.pager.currentItem = PLAYLIST_FRAGMENT
        }
        binding.navigationControl.setBrowseAlbumButtonOnClick {
            binding.pager.currentItem = BROWSE_ALBUMS_FRAGMENT
        }
        binding.navigationControl.setAlbumButtonOnClick {
            binding.pager.currentItem = ALBUM_FRAGMENT
        }

        return binding.root
    }
}
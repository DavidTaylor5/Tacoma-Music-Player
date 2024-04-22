package com.example.tacomamusicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.FragmentMusicChooserBinding

class MusicChooserFragment: Fragment() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private lateinit var viewPager: ViewPager2

    private val PLAYLIST_FRAGMENT = 0
    private val BROWSE_ALBUMS_FRAGMENT = 1
    private val ALBUM_FRAGMENT = 2


    private lateinit var binding: FragmentMusicChooserBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicChooserBinding.inflate(inflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        //Instantiate a ViewPager2 and a PagerAdapter
        viewPager = binding.pager
        val pagerAdapter = ScreenSlidePagerAdapter(requireActivity())
        viewPager.adapter = pagerAdapter

        binding.navigationControl.setPlaylistButtonOnClick {
            viewPager.currentItem = PLAYLIST_FRAGMENT
        }
        binding.navigationControl.setBrowseAlbumButtonOnClick {
            viewPager.currentItem = BROWSE_ALBUMS_FRAGMENT
        }
        binding.navigationControl.setAlbumButtonOnClick {
            viewPager.currentItem = ALBUM_FRAGMENT
        }
    }
}
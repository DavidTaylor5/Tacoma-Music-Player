package com.example.tacomamusicplayer.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.ActivityChooseMusicBinding
import com.example.tacomamusicplayer.util.UtilImpl
import com.example.tacomamusicplayer.viewmodel.ChooseMusicViewModel

//TODO this should actually be a fragment??...
class ChooseMusicActivity: FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private lateinit var viewPager: ViewPager2


    private lateinit var binding: ActivityChooseMusicBinding

    //TODO maybe these should be completely seperate fragments with their own personalized logic?
    //but also they dispaly similar logic and should be clickable...

    /*
    * What happens on a click?
    * A click on a playlist item should start the music player activity and load in the playlist from that point
    * A click on an album should bring up it's songs in a different tab
    * A click on an song in the album should start the music player from that point in the album...
    *
    * */


    private val PLAYLIST_FRAGMENT = 0
    private val BROWSE_ALBUMS_FRAGMENT = 1
    private val ALBUM_FRAGMENT = 2

    val viewModel: ChooseMusicViewModel by viewModels()


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup view binding in the project
        binding = ActivityChooseMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Instantiate a ViewPager2 and a PagerAdapter
        viewPager = binding.pager
        val pagerAdapter = ScreenSlidePagerAdapter(this)
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

    override fun onResume() {
        super.onResume()
        UtilImpl.hideNavigationUI(window)
    }

    override fun onBackPressed() {
        if(viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            //otherwise, select the pervious step
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }
}
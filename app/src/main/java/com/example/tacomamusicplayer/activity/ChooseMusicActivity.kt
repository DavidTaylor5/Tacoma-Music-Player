package com.example.tacomamusicplayer.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.ActivityChooseMusicBinding
import com.example.tacomamusicplayer.util.UtilImpl


class ChooseMusicActivity: FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private lateinit var viewPager: ViewPager2


    private lateinit var binding: ActivityChooseMusicBinding

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
            viewPager.currentItem = 0
        }
        binding.navigationControl.setBrowseAlbumButtonOnClick {
            viewPager.currentItem = 1
        }
        binding.navigationControl.setAlbumButtonOnClick {
            viewPager.currentItem = 2
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
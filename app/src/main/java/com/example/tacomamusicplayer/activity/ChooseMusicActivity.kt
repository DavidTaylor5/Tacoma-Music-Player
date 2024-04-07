package com.example.tacomamusicplayer.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.tacomamusicplayer.AlbumAdapter
import com.example.tacomamusicplayer.adapter.ScreenSlidePagerAdapter
import com.example.tacomamusicplayer.databinding.ActivityChooseMusicBinding


class ChooseMusicActivity: FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private lateinit var viewPager: ViewPager2


    private lateinit var binding: ActivityChooseMusicBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup view binding in the project
        binding = ActivityChooseMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //Instantiate a ViewPager2 and a PagerAdapter
        viewPager = binding.pager
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

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
package com.example.tacomamusicplayer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tacomamusicplayer.fragment.ScreenSlidePageFragment

/**
 * The number of pages to show.
 */
private const val NUM_PAGES = 3
class ScreenSlidePagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount(): Int  = NUM_PAGES
    override fun createFragment(position: Int): Fragment = ScreenSlidePageFragment(position)
}
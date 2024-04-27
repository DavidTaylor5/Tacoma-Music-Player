package com.example.tacomamusicplayer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tacomamusicplayer.enum.PageType
import com.example.tacomamusicplayer.fragment.ScreenSlidePageFragment
import com.example.tacomamusicplayer.fragment.pages.AlbumListFragment
import com.example.tacomamusicplayer.fragment.pages.PlaylistFragment
import com.example.tacomamusicplayer.fragment.pages.SongListFragment

/**
 * The number of pages to show.
 */
private const val NUM_PAGES = 3
class ScreenSlidePagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount(): Int  = NUM_PAGES
    override fun createFragment(position: Int): Fragment{
        return when(position) {
            PageType.PLAYLIST_PAGE.type() -> PlaylistFragment()
            PageType.ALBUM_PAGE.type() -> AlbumListFragment()
            PageType.SONG_PAGE.type() -> SongListFragment()
            else -> SongListFragment()
        }
    }
}
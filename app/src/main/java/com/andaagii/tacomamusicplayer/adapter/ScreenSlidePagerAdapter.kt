package com.andaagii.tacomamusicplayer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.andaagii.tacomamusicplayer.enumtype.PageType
import com.andaagii.tacomamusicplayer.fragment.pages.CurrentQueueFragment
import com.andaagii.tacomamusicplayer.fragment.pages.MusicPlayingFragment
import com.andaagii.tacomamusicplayer.fragment.pages.AlbumListFragment
import com.andaagii.tacomamusicplayer.fragment.pages.PlaylistFragment
import com.andaagii.tacomamusicplayer.fragment.pages.SongListFragment

private const val NUM_PAGES = 5
class ScreenSlidePagerAdapter(
    fa: FragmentActivity
): FragmentStateAdapter(fa) {
    override fun getItemCount(): Int  = NUM_PAGES
    override fun createFragment(position: Int): Fragment{
        return when(position) {
            PageType.QUEUE_PAGE.type() -> CurrentQueueFragment()
            PageType.PLAYER_PAGE.type() -> MusicPlayingFragment()
            PageType.PLAYLIST_PAGE.type() -> PlaylistFragment()
            PageType.ALBUM_PAGE.type() -> AlbumListFragment()
            PageType.SONG_PAGE.type() -> SongListFragment()
            else -> SongListFragment()
        }
    }
}
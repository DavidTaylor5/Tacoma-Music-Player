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

/**
 * [FragmentStateAdapter] that backs the [androidx.viewpager2.widget.ViewPager2] in
 * [com.andaagii.tacomamusicplayer.fragment.PlayerDisplayFragment].
 *
 * Maps each page index to its corresponding [Fragment] using [PageType] ordinals. The page
 * order is: Queue → Player → Playlists → Albums → Songs.
 */
class ScreenSlidePagerAdapter(
    fa: FragmentActivity
): FragmentStateAdapter(fa) {

    override fun getItemCount(): Int  = NUM_PAGES

    /**
     * Returns the [Fragment] for the given [position].
     *
     * Falls back to [SongListFragment] for any unrecognised position to prevent a crash,
     * though in practice [getItemCount] ensures the pager never requests an out-of-range index.
     */
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
package com.andaagii.tacomamusicplayer.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.andaagii.tacomamusicplayer.data.DisplaySong

object DisplaySongDiffCallback: DiffUtil.ItemCallback<DisplaySong>() {
    override fun areItemsTheSame(
        oldItem: DisplaySong,
        newItem: DisplaySong
    ): Boolean {
        return oldItem.mediaItem == newItem.mediaItem
    }

    override fun areContentsTheSame(
        oldItem: DisplaySong,
        newItem: DisplaySong
    ): Boolean {
        return oldItem == newItem
    }
}
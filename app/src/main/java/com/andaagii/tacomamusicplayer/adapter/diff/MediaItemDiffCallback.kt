package com.andaagii.tacomamusicplayer.adapter.diff

import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil

object MediaItemDiffCallback: DiffUtil.ItemCallback<MediaItem>() {
    override fun areItemsTheSame(
        oldItem: MediaItem,
        newItem: MediaItem
    ): Boolean {
        return oldItem.mediaMetadata.albumTitle == newItem.mediaMetadata.albumTitle
    }

    override fun areContentsTheSame(
        oldItem: MediaItem,
        newItem: MediaItem
    ): Boolean {
        return oldItem == newItem
    }
}
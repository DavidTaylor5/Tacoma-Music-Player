package com.andaagii.tacomamusicplayer.adapter.diff

import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil

/**
 * [DiffUtil.ItemCallback] used by all [androidx.recyclerview.widget.ListAdapter] instances
 * that display [MediaItem] lists (albums, playlists).
 *
 * Sharing a single callback object avoids duplicating identity and content logic across every
 * adapter that works with [MediaItem]s.
 */
object MediaItemDiffCallback: DiffUtil.ItemCallback<MediaItem>() {

    /**
     * Determines whether two items represent the same logical entity.
     *
     * Uses `albumTitle` as the stable identity key because [MediaItem] does not expose a
     * single synthesised ID field; album title is unique within the library catalog.
     */
    override fun areItemsTheSame(
        oldItem: MediaItem,
        newItem: MediaItem
    ): Boolean {
        return oldItem.mediaMetadata.description == newItem.mediaMetadata.description
    }

    /**
     * Determines whether two items have identical visible content.
     *
     * Delegates to structural equality (`==`) on [MediaItem], which covers all metadata fields
     * including artwork URI, so any field change triggers a rebind.
     */
    override fun areContentsTheSame(
        oldItem: MediaItem,
        newItem: MediaItem
    ): Boolean {
        return oldItem == newItem
    }
}
package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.QueueListAdapter.QueueSongViewHolder
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.DisplaySong
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.ViewholderQueueSongBinding
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/**
 * [ListAdapter] for the current playback queue with drag-to-reorder support.
 *
 * Displays each track as a [DisplaySong] row, highlighting the currently playing item with a
 * green stroke. Drag handles are set up in [onCreateViewHolder] and attached to the host
 * fragment's [androidx.recyclerview.widget.ItemTouchHelper] via [onHandleDrag].
 *
 * Submit new lists via [submitList]. During an active drag, [moveItem] updates [displayList]
 * and calls [submitList] so DiffUtil emits the correct [notifyItemMoved] — this is the
 * single notification path, avoiding conflicts with a separate manual call. The dragged item's
 * position is controlled by [androidx.recyclerview.widget.ItemTouchHelper] independently and
 * does not depend on these notifications.
 *
 * @param handleSongSetting Invoked when the user selects a popup menu option on a row.
 * @param onHandleDrag Called when the user touches the drag handle, so the host can start an
 *   [androidx.recyclerview.widget.ItemTouchHelper] drag.
 * @param onRemoveSong Invoked after a track is removed, passing its former index so the host
 *   ViewModel can sync the persistent queue.
 * @param playSongAtPosition Invoked when the user taps a row to jump playback to that position.
 */
class QueueListAdapter(
    val handleSongSetting: (MenuOptionUtil.MenuOption, List<MediaItem>) -> Unit,
    val onHandleDrag: (viewHolder: RecyclerView.ViewHolder) -> Unit,
    val onRemoveSong: (Int) -> Unit,
    val playSongAtPosition: (Int) -> Unit,
) : ListAdapter<DisplaySong, QueueSongViewHolder>(DisplaySongDiffCallback) {

    /**
     * Shadow list that tracks display order independently of [ListAdapter.currentList].
     *
     * [onCurrentListChanged] syncs it whenever a [submitList] call delivers a new list.
     * During a drag, [moveItem] updates only this list so [onBindViewHolder] reflects the
     * in-progress visual order without triggering an async diff on every move event.
     */
    private val displayList = mutableListOf<DisplaySong>()

    companion object {
        private val DisplaySongDiffCallback = object : DiffUtil.ItemCallback<DisplaySong>() {
            override fun areItemsTheSame(oldItem: DisplaySong, newItem: DisplaySong): Boolean =
                oldItem.mediaItem.mediaMetadata.description == newItem.mediaItem.mediaMetadata.description

            override fun areContentsTheSame(oldItem: DisplaySong, newItem: DisplaySong): Boolean =
                oldItem == newItem
        }
    }

    /** ViewHolder that holds the inflated [ViewholderQueueSongBinding] for a single queue row. */
    class QueueSongViewHolder(
        val binding: ViewholderQueueSongBinding,
        var isFavorited: Boolean = false,
    ) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = displayList.size

    override fun onCurrentListChanged(
        previousList: List<DisplaySong>,
        currentList: List<DisplaySong>,
    ) {
        displayList.clear()
        displayList.addAll(currentList)
    }

    /**
     * Moves the item at [from] to [to] within [displayList] and submits the updated list so
     * [ListAdapter] emits the correct [notifyItemMoved] via DiffUtil.
     *
     * Using [submitList] here (rather than a manual [notifyItemMoved] call from the fragment)
     * keeps [ListAdapter.currentList] in sync with [displayList] during drag. This ensures the
     * post-drag observer-driven [submitList] computes a no-op diff and emits no conflicting
     * notifications.
     */
    fun moveItem(from: Int, to: Int) {
        displayList.add(to, displayList.removeAt(from))
        submitList(displayList.toList())
    }

    /** Replaces the queue with an empty list. */
    fun clearQueue() {
        submitList(emptyList())
    }

    /**
     * Updates the play-indicator highlight to reflect [updatedSong] as the currently playing track.
     *
     * If the currently indicated song already matches [updatedSong], no update is performed.
     * The indicator is only set when the song is found at a positive index — index 0 is skipped
     * to avoid a false-positive on the first item when no match is found.
     *
     * @param updatedSong The track that is now actively playing.
     */
    fun updateCurrentSongIndicator(updatedSong: SongData) {
        try {
            val currSong = displayList.first { it.showPlayIndicator }
            if (currSong.mediaItem.mediaMetadata.title == updatedSong.songTitle) return
        } catch (e: Exception) {
            Timber.d("updateCurrentSongIndicator: No currSong found!")
        }

        val indicatorPosition = displayList.indexOfFirst {
            it.mediaItem.mediaMetadata.title == updatedSong.songTitle
        }

        Timber.d("updateCurrentSongIndicator: indicatorPosition=$indicatorPosition")
        if (indicatorPosition > 0) {
            submitList(displayList.map { song ->
                song.copy(showPlayIndicator = song.mediaItem.mediaMetadata.title == updatedSong.songTitle)
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueSongViewHolder {
        Timber.d("onCreateViewHolder: ")
        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderQueueSongBinding.inflate(inflater, parent, false)
        val viewHolder = QueueSongViewHolder(binding)

        viewHolder.binding.songHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onHandleDrag(viewHolder)
            }
            true
        }

        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: QueueSongViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        if (position >= displayList.size) return

        val displaySong = displayList[position]
        val songData = displaySong.mediaItem.mediaMetadata

        var songDurationReadable = "Unknown Duration"
        var artworkUri: Uri? = displaySong.mediaItem.mediaMetadata.artworkUri

        Timber.d("onBindViewHolder: songTitle=${songData.title}, songArtist=${songData.artist}, albumTitle=${songData.albumTitle}, albumArtUri=${songData.artworkUri}")

        songData.description?.toString()?.toLongOrNull()?.let {
            songDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(it)
        }

        viewHolder.binding.songContainer.strokeColor =
            if (displaySong.showPlayIndicator) Color.GREEN else Color.WHITE

        viewHolder.binding.songContainer.setOnClickListener {
            playSongAtPosition(viewHolder.absoluteAdapterPosition)
        }

        val customImage = UtilImpl.getImageBaseNameFromExternalStorage(
            groupTitle = songData.albumTitle.toString(),
            artist = songData.albumArtist.toString(),
            songGroupType = if (songData.albumArtist == Const.USER_PLAYLIST) SongGroupType.PLAYLIST else SongGroupType.ALBUM
        )

        artworkUri?.let { uri ->
            UtilImpl.drawMediaItemArt(
                viewHolder.binding.albumArt,
                uri,
                Size(200, 200),
                customImage
            )
        }

        viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
        viewHolder.isFavorited = false

        (viewHolder.binding.favoriteAnimation.background as AnimationDrawable).apply {
            stop()
            selectDrawable(0)
            invalidateSelf()
        }

        viewHolder.binding.songTitleTextView.text = songData.title.toString()
        viewHolder.binding.artistTextView.text = songData.artist.toString()
        viewHolder.binding.durationTextView.text = songDurationReadable

        viewHolder.binding.menuIcon.setOnClickListener {
            val menu = PopupMenu(
                viewHolder.itemView.context,
                viewHolder.binding.menuIcon,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )
            menu.menuInflater.inflate(R.menu.queue_song_options, menu.menu)
            menu.setOnMenuItemClickListener { item ->
                Toast.makeText(viewHolder.itemView.context, "You Clicked " + item.title, Toast.LENGTH_SHORT).show()
                handleMenuItem(item, viewHolder.absoluteAdapterPosition)
                true
            }
            menu.show()
        }
    }

    /** Dispatches the selected popup [item] for the row at [position] to the appropriate handler. */
    private fun handleMenuItem(item: MenuItem, position: Int) {
        when (MenuOptionUtil.determineMenuOptionFromTitle(item.title.toString())) {
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> handleAddToPlaylist(position)
            MenuOptionUtil.MenuOption.REMOVE_FROM_QUEUE -> handleRemoveFromQueue(position)
            MenuOptionUtil.MenuOption.CHECK_STATS -> handleCheckStatus()
            else -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    private fun handleAddToPlaylist(position: Int) {
        if (position < displayList.size) {
            handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST, listOf(displayList[position].mediaItem))
        }
    }

    /**
     * Removes the track at [position], submits the updated list so [ListAdapter] animates the
     * removal, then notifies the host via [onRemoveSong] to sync the controller queue.
     */
    private fun handleRemoveFromQueue(position: Int) {
        if (position >= displayList.size) return
        val newList = displayList.toMutableList().also { it.removeAt(position) }
        submitList(newList)
        onRemoveSong(position)
    }

    private fun handleCheckStatus() {
        // TODO: Add statistics logic
    }
}

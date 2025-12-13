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
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.DisplaySong
import com.andaagii.tacomamusicplayer.data.SongData
import com.andaagii.tacomamusicplayer.databinding.ViewholderQueueSongBinding
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

class QueueListAdapter(
    private var dataSet:  List<DisplaySong>,
    val handleSongSetting: (MenuOptionUtil.MenuOption, List<MediaItem>) -> Unit,
    val onHandleDrag: (viewHolder: RecyclerView.ViewHolder) -> Unit,
    val onRemoveSong: (Int) -> Unit,
    val playSongAtPosition: (Int) -> Unit,
): RecyclerView.Adapter<QueueListAdapter.QueueSongViewHolder>() {

    private var favoriteList: MutableList<Boolean> = dataSet.map { false }.toMutableList()

    class QueueSongViewHolder(val binding: ViewholderQueueSongBinding, var isFavorited: Boolean = false): RecyclerView.ViewHolder(binding.root)

    /**
     * Move Items in the recyclerview to adjacent positions
     */
    fun moveItem(from: Int, to: Int) {
        val modData = dataSet.toMutableList()
        val temp = dataSet[to]

        modData[to] = dataSet[from]
        modData[from] = temp

        dataSet = modData
    }

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueSongViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderQueueSongBinding.inflate(inflater, parent, false)

        val viewHolder = QueueSongViewHolder(binding)

        //This code allows for the songHandle for dragging songs inside of the queue
        viewHolder.binding.songHandle.setOnTouchListener { v, event ->
            if(event.actionMasked == MotionEvent.ACTION_DOWN) {
                onHandleDrag(viewHolder)
            }
            return@setOnTouchListener true
        }

        return viewHolder
    }

    private fun clearPreviousSongIndicator() {
        val currSongPos = dataSet.indexOfFirst {song ->
            song.showPlayIndicator
        }

        if(currSongPos >= 0) {
            Timber.d("clearPreviousSongIndicator: currSongPos=$currSongPos")
            dataSet[currSongPos].showPlayIndicator = false
            this.notifyItemChanged(currSongPos)
        }
    }

    /**
     * Clear all songs out of recyclerview.
     */
    fun clearQueue() {
        dataSet = listOf()
        this.notifyDataSetChanged()
    }

    fun updateCurrentSongIndicator(updatedSong: SongData) {
        try {
            val currSong = dataSet.first {  song ->
                song.showPlayIndicator
            }

            if(currSong.mediaItem.mediaMetadata.title == updatedSong.songTitle ) {
                return
            }

        } catch(e: Exception) {
            Timber.d("updateCurrentSongIndicator: No currSong found!")
        }

        clearPreviousSongIndicator()

        val indicatorPosition = dataSet.indexOfFirst {
            it.mediaItem.mediaMetadata.title == updatedSong.songTitle
        }

        Timber.d("updateCurrentSongIndicator: indicatorPosition$indicatorPosition")
        if(indicatorPosition > 0) {
            dataSet[indicatorPosition].showPlayIndicator = true
            this.notifyItemChanged(indicatorPosition)
        }
    }

    override fun onBindViewHolder(viewHolder: QueueSongViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var songTitle = "DEFAULT SONG TITLE"
        var songArtist = "DEFAULT SONG ARTIST"
        //var albumTitle = "DEFAULT ALBUM TITLE"
        var songDuration = "DEFAULT SONG DURATION"
        var artworkUri = Uri.EMPTY
        var songDurationReadable = "Unknown Duration"

        //First check that dataSet has a value for position
        if(position < dataSet.size) {
            val songData = dataSet[position].mediaItem.mediaMetadata
            Timber.d("onBindViewHolder: CHECKING VALUES songTitle=${songData.title},  songArtist=${songData.artist}, albumTitle=${songData.albumTitle}, albumArtUri=${songData.artworkUri}")

            songTitle = songData.title.toString()
            songArtist = songData.artist.toString()
            //albumTitle = dataSet[position].mediaItem.mediaMetadata.albumTitle.toString()
            artworkUri = dataSet[position].mediaItem.mediaMetadata.artworkUri
            songDuration = dataSet[position].mediaItem.mediaMetadata.description.toString()

            val songDurationInLong = songDuration.toLongOrNull()
            songDurationInLong?.let {
                songDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(songDurationInLong)
            }

            if(dataSet[position].showPlayIndicator) {
                Timber.d("onBindViewHolder: songTitle=$songTitle, is showing play indicator!")

                viewHolder.binding.songContainer.strokeColor = Color.GREEN
            } else {
                viewHolder.binding.songContainer.strokeColor = Color.WHITE
            }

            viewHolder.binding.songContainer.setOnClickListener {
                playSongAtPosition(viewHolder.absoluteAdapterPosition)
            }

            val customImage = UtilImpl.getImageBaseNameFromExternalStorage(
                groupTitle = songData.albumTitle.toString(),
                artist = songData.albumArtist.toString(),
                songGroupType = if(songData.albumArtist == Const.USER_PLAYLIST) SongGroupType.PLAYLIST else SongGroupType.ALBUM
            )
            UtilImpl.drawMediaItemArt(
                viewHolder.binding.albumArt,
                artworkUri,
                Size(200, 200),
                customImage
            )

            viewHolder.binding.favoriteAnimation.setBackgroundDrawable(null)
//                viewHolder.binding.favoriteAnimation.background as AnimationDrawable).stop()
            viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
//                viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
            viewHolder.isFavorited = false

            if(favoriteList[position]) {
                viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.unfavorite_animation)
            } else {
                viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
            }

            (viewHolder.binding.favoriteAnimation.background as AnimationDrawable).stop()
            (viewHolder.binding.favoriteAnimation.background as AnimationDrawable).selectDrawable(0)
            (viewHolder.binding.favoriteAnimation.background as AnimationDrawable).invalidateSelf()

            //TODO Add back song selection in the queue, currently disabled.
//            viewHolder.binding.albumArt.setOnClickListener {
//
//                if(favoriteList[position]) { //currently favorited so, ontap turn to un favorited...
//                    viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.unfavorite_animation)
//                    viewHolder.isFavorited = false
//                    favoriteList[position] = false
//                } else { //currently un favorited, turn to favorited...
//                    viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
//                    viewHolder.isFavorited = true
//                    favoriteList[position] = true
//                }
//                val frameAnimation = viewHolder.binding.favoriteAnimation.background as AnimationDrawable
//                frameAnimation.start()
//            }
        }

        viewHolder.binding.songTitleTextView.text = songTitle
        viewHolder.binding.artistTextView.text = songArtist
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
            menu.setOnMenuItemClickListener {
                Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleMenuItem(it, viewHolder.absoluteAdapterPosition) //TODO not done yet
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }
    }

    private fun handleMenuItem(item: MenuItem, position: Int) {
        when(MenuOptionUtil.determineMenuOptionFromTitle(item.title.toString())) {
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> handleAddToPlaylist(position)
            MenuOptionUtil.MenuOption.REMOVE_FROM_QUEUE -> handleRemoveFromQueue(position)
            MenuOptionUtil.MenuOption.CHECK_STATS -> handleCheckStatus()
            else -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    private fun handleAddToPlaylist(position: Int) {
        handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST, listOf(dataSet[position].mediaItem))
    }

    private fun handleRemoveFromQueue(position: Int) {
        val modData = dataSet.toMutableList()
        modData.removeAt(position)
        dataSet = modData

        onRemoveSong(position)
    }

    private fun handleCheckStatus() {
        //TODO Add statistics logic...
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

}
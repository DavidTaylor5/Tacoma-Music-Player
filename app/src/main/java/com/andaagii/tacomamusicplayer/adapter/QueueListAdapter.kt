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
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.adapter.diff.DisplaySongDiffCallback
import com.andaagii.tacomamusicplayer.constants.Const
import com.andaagii.tacomamusicplayer.data.DisplaySong
import com.andaagii.tacomamusicplayer.databinding.ViewholderQueueSongBinding
import com.andaagii.tacomamusicplayer.enumtype.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

class QueueListAdapter(
    val handleSongSetting: (MenuOptionUtil.MenuOption, List<MediaItem>) -> Unit,
    val onHandleDrag: (viewHolder: RecyclerView.ViewHolder) -> Unit,
    val playSongAtPosition: (Int) -> Unit,
): ListAdapter<DisplaySong, QueueListAdapter.QueueSongViewHolder>(DisplaySongDiffCallback) {
    class QueueSongViewHolder(val binding: ViewholderQueueSongBinding, var isFavorited: Boolean = false): RecyclerView.ViewHolder(binding.root)

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

    override fun onBindViewHolder(viewHolder: QueueSongViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var songTitle = "DEFAULT SONG TITLE"
        var songArtist = "DEFAULT SONG ARTIST"
        //var albumTitle = "DEFAULT ALBUM TITLE"
        var songDuration = "DEFAULT SONG DURATION"
        var artworkUri = Uri.EMPTY
        var songDurationReadable = "Unknown Duration"

        //First check that dataSet has a value for position
        if(position < currentList.size) {
            val songData = currentList[position].mediaItem.mediaMetadata
            Timber.d("onBindViewHolder: CHECKING VALUES songTitle=${songData.title},  songArtist=${songData.artist}, albumTitle=${songData.albumTitle}, albumArtUri=${songData.artworkUri}")

            songTitle = songData.title.toString()
            songArtist = songData.artist.toString()
            //albumTitle = dataSet[position].mediaItem.mediaMetadata.albumTitle.toString()
            artworkUri = currentList[position].mediaItem.mediaMetadata.artworkUri
            songDuration = currentList[position].mediaItem.mediaMetadata.description.toString()

            val songDurationInLong = songDuration.toLongOrNull()
            songDurationInLong?.let {
                songDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(songDurationInLong)
            }

            if(currentList[position].showPlayIndicator) {
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

            artworkUri?.let { uri ->
                UtilImpl.drawMediaItemArt(
                    viewHolder.binding.albumArt,
                    uri,
                    Size(200, 200),
                    customImage
                )
            }

            viewHolder.binding.favoriteAnimation.setBackgroundDrawable(null)
//                viewHolder.binding.favoriteAnimation.background as AnimationDrawable).stop()
            viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
//                viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
            viewHolder.isFavorited = false

//            if(favoriteList[position]) {
//                viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.unfavorite_animation)
//            } else {
//                viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
//            }

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
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> {
                handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST, listOf(currentList[position].mediaItem))
            }
            MenuOptionUtil.MenuOption.REMOVE_FROM_QUEUE -> {
                handleSongSetting(MenuOptionUtil.MenuOption.REMOVE_FROM_QUEUE, listOf(currentList[position].mediaItem))
            }
            else -> Timber.d("handleMenuItem: UNKNOWN $item...")
        }
    }
}

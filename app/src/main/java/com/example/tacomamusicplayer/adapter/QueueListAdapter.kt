package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.ViewholderQueueSongBinding
import com.example.tacomamusicplayer.util.MenuOptionUtil
import com.example.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/*TODO I want to use my new MediaItemUtil.createSongDataFromMediaItem
*  to add individual songs into specific playlists next!
* */

//TODO SAVE THE SONG QUEUE AS A HIDDEN PLAYLIST, I CAN KEEP IT IN THE DATABASE when I leave the app?
//TODO I ALSO WANT TO SAVE MY POSITION IN THE QUEUE

class QueueListAdapter(
    private var dataSet:  List<MediaItem>,
    val handleSongSetting: (MenuOptionUtil.MenuOption, List<MediaItem>) -> Unit,
    val onHandleDrag: (viewHolder: RecyclerView.ViewHolder) -> Unit,
    val onRemoveSong: (Int) -> Unit
): RecyclerView.Adapter<QueueListAdapter.QueueSongViewHolder>() {

    private var favoriteList: MutableList<Boolean> = dataSet.map { false }.toMutableList() //TODO I just need to make this persistent pass this data in as well...

    class QueueSongViewHolder(val binding: ViewholderQueueSongBinding, var isFavorited: Boolean = false): RecyclerView.ViewHolder(binding.root)

    //TODO START REMOVING THE SONGQUEUE variable....




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

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: QueueSongViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var songTitle = "DEFAULT SONG TITLE"
        var songArtist = "DEFAULT SONG ARTIST"
        var albumTitle = "DEFAULT ALBUM TITLE"
        var songDuration = "DEFUALT SONG DURATION"
        var artworkUri = Uri.EMPTY
        var songDurationReadable = "Unknown Duration"

        //First check that dataSet has a value for position
        if(position < dataSet.size) {

            val songData = dataSet[position].mediaMetadata
            Timber.d("onBindViewHolder: CHECKING VALUES songTitle=${songData.title},  songArtist=${songData.artist}, albumTitle=${songData.albumTitle}, albumArtUri=${songData.artworkUri}")

            songTitle = songData.title.toString()
            songArtist = songData.artist.toString()
            albumTitle = dataSet[position].mediaMetadata.albumTitle.toString()
            artworkUri = dataSet[position].mediaMetadata.artworkUri
            songDuration = dataSet[position].mediaMetadata.description.toString()

            val songDurationInLong = songDuration.toLongOrNull()
            songDurationInLong?.let {
                songDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(songDurationInLong)
            }

            //TODO what to do when a song is clicked?
            //viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            UtilImpl.drawUriOntoImageView(
                viewHolder.binding.albumArt,
                artworkUri,
                Size(100, 100)
            )

            //TEST CODE FOR LIKE ANIMATION...
            //ISSUE -> It shouldn't be the viewholder but the data which determines what should be shown...
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

            viewHolder.binding.albumArt.setOnClickListener {

                if(favoriteList[position]) { //currently favorited so, ontap turn to un favorited...
                    viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.unfavorite_animation)
                    viewHolder.isFavorited = false
                    favoriteList[position] = false
                } else { //currently un favorited, turn to favorited...
                    viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
                    viewHolder.isFavorited = true
                    favoriteList[position] = true
                }
                val frameAnimation = viewHolder.binding.favoriteAnimation.background as AnimationDrawable
                frameAnimation.start()
            }
//                val frameAnimation = binding.libraryAnimation.background as AnimationDrawable
//                frameAnimation.start()

        }

        viewHolder.binding.songTitleTextView.text = songTitle
        viewHolder.binding.artistTextView.text = songArtist
        viewHolder.binding.durationTextView.text = songDurationReadable

        viewHolder.binding.menuIcon.setOnClickListener {

            val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

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
        handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST, listOf(dataSet[position]))
    }

    private fun handleRemoveFromQueue(position: Int) {
        onRemoveSong(position)
    }

    private fun handleCheckStatus() {
        //TODO Add statistics logic...
    }

    private fun openAddToPlaylistPrompt() {
        //TODO I should have a floating prompt that I can scroll to many playlists with...
        //I need to add a recyclerview with all of the avilable playlists...
        //TODO I should flag the above fragment that I want to show the playlist settings option...
        //should also have an option to create a new playlist? [add later...]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
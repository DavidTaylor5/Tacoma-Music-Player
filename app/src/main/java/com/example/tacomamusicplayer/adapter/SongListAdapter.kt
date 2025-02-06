package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.ViewholderSongBinding
import com.example.tacomamusicplayer.enum.SongGroupType
import com.example.tacomamusicplayer.util.SongSettingsUtil
import com.example.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/*TODO I want to use my new MediaItemUtil.createSongDataFromMediaItem
*  to add individual songs into specific playlists next!
* */

class SongListAdapter(
    private var dataSet:  List<MediaItem>,
    val handleSongSetting: (SongSettingsUtil.Setting, List<MediaItem>) -> Unit,
    val songGroupType: SongGroupType,
    val onHandleDrag: (viewHolder: RecyclerView.ViewHolder) -> Unit
): RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    private var favoriteList: MutableList<Boolean> = dataSet.map { false }.toMutableList() //TODO I just need to make this persistent pass this data in as well...

    class SongViewHolder(val binding: ViewholderSongBinding, var isFavorited: Boolean = false): RecyclerView.ViewHolder(binding.root)


    fun moveItem(from: Int, to: Int) {
        if(from == to) return

        val dataCopy = dataSet.toMutableList()
        val dataCopyIndexes = dataCopy.mapIndexed { index, mediaItem ->
            index
        }

        val front = dataCopyIndexes.subList(0, to).toMutableList()
        val end = dataCopyIndexes.subList(to, dataCopyIndexes.size).toMutableList()
        front.removeIf { index ->
            index == from
        }
        end.removeIf { index ->
            index == from
        }

        val newIndexes = mutableListOf<Int>()
        newIndexes.addAll(front)
        newIndexes.add(from)
        newIndexes.addAll(end)

        Timber.d("moveItem: newIndexes=$newIndexes")

        val newData = newIndexes.map {index ->
            dataSet[index]
        }

        dataSet = newData
    }

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderSongBinding.inflate(inflater, parent, false)

        val viewHolder = SongViewHolder(binding)

        //This code allows for the songHandle for dragging songs inside of the queue
        viewHolder.binding.songHandle.setOnTouchListener { v, event ->
            if(event.actionMasked == MotionEvent.ACTION_DOWN) {
                onHandleDrag(viewHolder)
            }
            return@setOnTouchListener true
        }

        if(songGroupType.equals(SongGroupType.PLAYLIST)) {
            viewHolder.binding.songHandle.visibility = View.VISIBLE
        } else {
            //User shouldn't be able to update order of album songs which have intrinsic ordering
            viewHolder.binding.songHandle.visibility = View.GONE
        }

        return viewHolder
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: SongViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var songTitle = "DEFAULT SONG TITLE"
        var songArtist = "DEFAULT SONG ARTIST"
        var albumTitle = "DEFAULT ALBUM TITLE"
        var songDuration = "DEFAULT SONG DURATION"
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

        //TODO allow songs to be unfavorited...
//        viewHolder.binding.favoriteIcon.setOnClickListener {
//            viewHolder.binding.favoriteIcon.background = ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.baseline_star_24_green)
//        }

        viewHolder.binding.addIcon.setOnClickListener {
            Toast.makeText(viewHolder.itemView.context, "Added $songTitle to the queue!", Toast.LENGTH_SHORT).show()
            handleSongSetting(SongSettingsUtil.Setting.ADD_TO_QUEUE, listOf(dataSet[position]))
        }

        viewHolder.binding.menuIcon.setOnClickListener {

            val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

            menu.menuInflater.inflate(R.menu.menu_song_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                handleMenuItem(it, position)
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }
    }

    private fun handleMenuItem(item: MenuItem, position: Int) {
        when(SongSettingsUtil.determineSettingFromTitle(item.title.toString())) {
            SongSettingsUtil.Setting.ADD_TO_PLAYLIST -> handleAddToPlaylist(position)
            SongSettingsUtil.Setting.ADD_TO_QUEUE -> handleAddToQueue(position)
            SongSettingsUtil.Setting.CHECK_STATS -> handleCheckStatus()
            SongSettingsUtil.Setting.UNKNOWN -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    private fun handleAddToPlaylist(position: Int) {
        handleSongSetting(SongSettingsUtil.Setting.ADD_TO_PLAYLIST, listOf(dataSet[position]))
    }

    private fun handleAddToQueue(position: Int) {
        handleSongSetting(SongSettingsUtil.Setting.ADD_TO_QUEUE, listOf(dataSet[position]))
    }

    private fun handleCheckStatus() {
        //TODO Add statistics logic...
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
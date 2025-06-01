package com.andaagii.tacomamusicplayer.adapter

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
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.databinding.ViewholderSongBinding
import com.andaagii.tacomamusicplayer.enum.SongGroupType
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

class SongListAdapter(
    private var dataSet:  List<MediaItem>,
    val handleSongSetting: (MenuOptionUtil.MenuOption, List<MediaItem>) -> Unit,
    val handleSongClick: (position:Int) -> Unit,
    val handleAlbumClick: (albumTitle: String) -> Unit,
    val handleSongSelected: (mediaItem:MediaItem, isSelected: Boolean) -> Unit,
    var songGroupType: SongGroupType,
    val onHandleDrag: (viewHolder: RecyclerView.ViewHolder) -> Unit
): RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    private var favoriteList: MutableList<Boolean> = dataSet.map { false }.toMutableList()

    class SongViewHolder(val binding: ViewholderSongBinding, var isFavorited: Boolean = false): RecyclerView.ViewHolder(binding.root)

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

    fun clearAllSelected() {
        favoriteList = dataSet.map { false }.toMutableList()
        this.notifyDataSetChanged()
    }

    fun setSongs(searchItems: List<MediaItem>, songGroupType: SongGroupType) {
        Timber.d("setSongs: searchItems=$searchItems, songGroupType=$songGroupType")
        this.dataSet = searchItems
        this.songGroupType = songGroupType
        this.notifyDataSetChanged()
    }

    fun removeSong(songTitle: String): Int {
        val posOfRemovedItem = dataSet.indexOfFirst { song ->
            song.mediaId == songTitle
        }

        val modDataSet = dataSet.toMutableList()
        modDataSet.removeAt(posOfRemovedItem)
        dataSet = modDataSet
        return posOfRemovedItem
    }

    fun getSongOrder(): List<MediaItem> {
        return dataSet
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

    /**
     * Certain expectations for songs displayed for albums and playlists.
     */
    private fun bindSongHolder(viewHolder: SongViewHolder, position: Int) {
        Timber.d("bindSongHolder: ")
        val songMetadata = dataSet[position].mediaMetadata

        val songTitle = songMetadata.title.toString()
        val songArtist = songMetadata.artist.toString()
        val albumTitle = songMetadata.albumTitle.toString()
        var songDurationReadable = ""

        val songDuration = songMetadata.description.toString()

        val songDurationInLong = songDuration.toLongOrNull()
        songDurationInLong?.let {
            songDurationReadable = UtilImpl.calculateHumanReadableTimeFromMilliseconds(songDurationInLong)
        }

        //When I click the text of a song, it should add songgroup to the queue and start playing at that song.
        viewHolder.binding.textVerticalContainer.setOnClickListener {
            handleSongClick(viewHolder.absoluteAdapterPosition)
        }

        viewHolder.binding.songTitleTextView.text = songTitle
        viewHolder.binding.artistTextView.text = songArtist
        viewHolder.binding.durationTextView.text = songDurationReadable
    }

    /**
     * Certain expectations for songs displayed as search data.
     */
    private fun bindSearchHolder(viewHolder: SongViewHolder, position: Int) {
        Timber.d("bindSearchHolder: ")
        val searchMetadata = dataSet[position].mediaMetadata

        val songTitle = searchMetadata.title.toString()
        val songArtist = searchMetadata.artist.toString()
        val albumTitle = searchMetadata.albumTitle.toString()

        val searchDescription = searchMetadata.description.toString()
        val searchType = if(searchMetadata.isPlayable == true) "SONG" else "ALBUM"

        viewHolder.binding.songTitleTextView.text = searchDescription
        viewHolder.binding.artistTextView.text = songArtist
        viewHolder.binding.durationTextView.text = searchType

        if(searchMetadata.isPlayable == true) {
            viewHolder.binding.textVerticalContainer.setOnClickListener {
                handleSongClick(viewHolder.absoluteAdapterPosition)
            }
        } else {
            viewHolder.binding.textVerticalContainer.setOnClickListener {
                Timber.d("bindSearchHolder: handleAlbumClick -> ${albumTitle}")
                handleAlbumClick(albumTitle)
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: SongViewHolder, position: Int) {
        //Timber.d("onBindViewHolder: ")

        var songTitle = "DEFAULT SONG TITLE"
        var songArtist = "DEFAULT SONG ARTIST"
        var albumTitle = "DEFAULT ALBUM TITLE"
        var songDuration = "DEFAULT SONG DURATION"
        var artworkUri = Uri.EMPTY
        var songDurationReadable = "Unknown Duration"

        //Common additions to both search results and album/playlist display
        if(position < dataSet.size) {

            val songMetadata = dataSet[position].mediaMetadata
            artworkUri = songMetadata.artworkUri

            val customImage = "album_${dataSet[position].mediaMetadata.albumTitle}"
            UtilImpl.drawSongArt(
                viewHolder.binding.albumArt,
                artworkUri,
                Size(200, 200),
                customImage
            )

            //Setup multi select...
            viewHolder.binding.favoriteAnimation.setBackgroundDrawable(null)
            viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
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
                    handleSongSelected(
                        dataSet[position],
                        false
                    )
                } else { //currently un favorited, turn to favorited...
                    viewHolder.binding.favoriteAnimation.setBackgroundResource(R.drawable.favorite_animation)
                    viewHolder.isFavorited = true
                    favoriteList[position] = true
                    handleSongSelected(
                        dataSet[position],
                        true
                    )
                }
                val frameAnimation = viewHolder.binding.favoriteAnimation.background as AnimationDrawable
                frameAnimation.start()
            }

            viewHolder.binding.menuIcon.setOnClickListener {

                val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

                if (songGroupType == SongGroupType.PLAYLIST) {
                    menu.menuInflater.inflate(R.menu.songlist_playlist_options, menu.menu)
                } else {
                    menu.menuInflater.inflate(R.menu.songlist_album_options, menu.menu)
                }

                menu.setOnMenuItemClickListener {
                    Toast.makeText(
                        viewHolder.itemView.context,
                        "You Clicked " + it.title,
                        Toast.LENGTH_SHORT
                    ).show()
                    handleMenuItem(it, dataSet[viewHolder.absoluteAdapterPosition])
                    return@setOnMenuItemClickListener true
                }
                menu.show()
            }

            viewHolder.binding.addIcon.setOnClickListener {
                Toast.makeText(viewHolder.itemView.context, "Added $songTitle to the queue!", Toast.LENGTH_SHORT).show()
                handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_QUEUE, listOf(dataSet[position]))
            }


            //HANDLE SPECIFICS RELATED TO DISPLAY SONG VERSUS DISPLAY SEARCH RESULT

            if(songGroupType == SongGroupType.SEARCH_LIST) {
                //Timber.d("onBindViewHolder: bind search holder!")
                bindSearchHolder(viewHolder, position)
            } else {
                //Timber.d("onBindViewHolder: bind song holder!")
                bindSongHolder(viewHolder, position)
            }
        }
    }

    //TODO move out of adapters?
    private fun handleMenuItem(item: MenuItem, mediaItem: MediaItem) {
        when(MenuOptionUtil.determineMenuOptionFromTitle(item.title.toString())) {
            MenuOptionUtil.MenuOption.REMOVE_FROM_PLAYLIST -> {
                handleSongSetting(MenuOptionUtil.MenuOption.REMOVE_FROM_PLAYLIST, listOf(mediaItem))
            }
            MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST -> handleAddToPlaylist(mediaItem)
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> handleAddToQueue(mediaItem)
            MenuOptionUtil.MenuOption.CHECK_STATS -> handleCheckStatus()
            else -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    private fun handleAddToPlaylist(mediaItem: MediaItem) {
        handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_PLAYLIST, listOf(mediaItem))
    }

    private fun handleAddToQueue(mediaItem: MediaItem) {
        handleSongSetting(MenuOptionUtil.MenuOption.ADD_TO_QUEUE, listOf(mediaItem))
    }

    private fun handleCheckStatus() {
        //TODO Add statistics logic...
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
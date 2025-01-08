package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.databinding.ViewholderSongBinding
import com.example.tacomamusicplayer.util.SongSettingsUtil
import timber.log.Timber

/*TODO I want to use my new MediaItemUtil.createSongDataFromMediaItem
*  to add individual songs into specific playlists next!
* */

class SongListAdapter(
    private val dataSet:  List<MediaItem>,
    val handleSongSetting: (SongSettingsUtil.Setting, MediaItem?) -> Unit
): RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    private var favoriteList: MutableList<Boolean> = dataSet.map { false }.toMutableList() //TODO I just need to make this persistent pass this data in as well...

    class SongViewHolder(val binding: ViewholderSongBinding, var isFavorited: Boolean = false): RecyclerView.ViewHolder(binding.root)

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderSongBinding.inflate(inflater, parent, false)

        return SongViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: SongViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var songTitle = "DEFAULT SONG TITLE"
        var songArtist = "DEFAULT SONG ARTIST"
        var albumTitle = "DEFAULT ALBUM TITLE"
        var songDuration = "DEFUALT SONG DURATION"
        var artworkUri = Uri.EMPTY

        //First check that dataSet has a value for position
        if(position < dataSet.size) {

            val songData = dataSet[position].mediaMetadata
            Timber.d("onBindViewHolder: CHECKING VALUES songTitle=${songData.title},  songArtist=${songData.artist}, albumTitle=${songData.albumTitle}, albumArtUri=${songData.artworkUri}")

            songTitle = songData.title.toString()
            songArtist = songData.artist.toString()
            albumTitle = dataSet[position].mediaMetadata.albumTitle.toString()
            artworkUri = dataSet[position].mediaMetadata.artworkUri

            //TODO what to do when a song is clicked?
            //viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            val resolver = viewHolder.itemView.context.contentResolver

            try {

                Timber.d("queryAllMediaItems: Getting album art from URI=${artworkUri}")

                //Album art as a bitmap, I need to work on what to do when this is blank / null?
                val albumArt = resolver.loadThumbnail(artworkUri, Size(100, 100), null)
                val albumDrawable = BitmapDrawable(viewHolder.itemView.context.resources, albumArt)

                viewHolder.binding.albumArt.setImageDrawable(albumDrawable)


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

                Timber.d("queryAllMediaItems: SUCCESSFUL! ALBUM ART FOUND!")

            } catch (e: Exception) {
                Timber.d("queryAllMediaItems: ERROR ON LOADING ALBUM ART e=$e")
            }
        }

        viewHolder.binding.songTitleTextView.text = songTitle
        viewHolder.binding.artistTextView.text = songArtist
        viewHolder.binding.durationTextView.text = "DEFAULT DURATION"

        //TODO allow songs to be unfavorited...
//        viewHolder.binding.favoriteIcon.setOnClickListener {
//            viewHolder.binding.favoriteIcon.background = ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.baseline_star_24_green)
//        }

        viewHolder.binding.addIcon.setOnClickListener {
            Toast.makeText(viewHolder.itemView.context, "Added $songTitle to the queue!", Toast.LENGTH_SHORT).show()
            handleSongSetting(SongSettingsUtil.Setting.ADD_TO_QUEUE, dataSet[position])
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
            SongSettingsUtil.Setting.ADD_TO_PLAYLIST -> handleAddToPlaylist()
            SongSettingsUtil.Setting.ADD_TO_QUEUE -> handleAddToQueue(position)
            SongSettingsUtil.Setting.CHECK_STATS -> handleCheckStatus()
            SongSettingsUtil.Setting.UNKNOWN -> Timber.d("handleMenuItem: UNKNOWN menuitem...")
        }
    }

    private fun handleAddToPlaylist() {
        handleSongSetting(SongSettingsUtil.Setting.ADD_TO_PLAYLIST, null)
    }

    private fun handleAddToQueue(position: Int) {
        handleSongSetting(SongSettingsUtil.Setting.ADD_TO_QUEUE, dataSet[position])
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
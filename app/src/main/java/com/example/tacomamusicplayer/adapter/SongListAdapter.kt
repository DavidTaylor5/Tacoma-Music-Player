package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.ViewholderSongBinding
import timber.log.Timber

class SongListAdapter(
    private val dataSet:  List<MediaItem>,
    val onAddIconClick: (MediaItem) -> Unit,
    val onMenuIconClick: () -> Unit
): RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    class SongViewHolder(val binding: ViewholderSongBinding): RecyclerView.ViewHolder(binding.root)

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

                Timber.d("queryAllMediaItems: SUCCESSFUL! ALBUM ART FOUND!")

            } catch (e: Exception) {
                Timber.d("queryAllMediaItems: ERROR ON LOADING ALBUM ART e=$e")
            }
        }

        viewHolder.binding.songTitleTextView.text = songTitle
        viewHolder.binding.artistTextView.text = songArtist
        viewHolder.binding.durationTextView.text = "DEFAULT DURATION"

        viewHolder.binding.favoriteIcon.setOnClickListener {
            viewHolder.binding.favoriteIcon.background = ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.baseline_star_24_green)
        }

        viewHolder.binding.addIcon.setOnClickListener {
            Toast.makeText(viewHolder.itemView.context, "Added $songTitle to the queue!", Toast.LENGTH_SHORT).show()
            onAddIconClick(dataSet[position])
            //todo add this to current queue or add to a playlist?
        }

        viewHolder.binding.menuIcon.setOnClickListener {

            val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

            menu.menuInflater.inflate(R.menu.menu_song_options, menu.menu)
            menu.setOnMenuItemClickListener {
                Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()
                return@setOnMenuItemClickListener true
                //TODO set actual options here....
                //TODO create a custom component for these song components...
            }
            menu.show()
        }

    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
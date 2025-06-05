package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.databinding.ViewholderAlbumBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/**
 * A recyclerview adapter that is able to take a list of Album Media Items and display them.
 */
class AlbumListAdapter(
    private var albums: List<MediaItem>,
    private val onAlbumClick: (String) -> Unit,
    private val onPlayIconClick: (String) -> Unit,
    private val handleAlbumOption: (MenuOptionUtil.MenuOption, String, String?) -> Unit,
): RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumViewHolder(val binding : ViewholderAlbumBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        Timber.d("onCreateViewHolder: ")
        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderAlbumBinding.inflate(inflater, parent, false)
        return AlbumViewHolder(binding)
    }

    fun updateData(albums: List<MediaItem>) {
        this.albums = albums
        this.notifyDataSetChanged()
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: AlbumViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        //First check that dataSet has a value for position
        if(position < albums.size) {
            val album = albums[position]
            val albumMetadata = album.mediaMetadata
            val customImage = "album_${albumMetadata.albumTitle}"
            Timber.d("onBindViewHolder: CHECKING VALUES albumTitle=${albumMetadata.albumTitle}, albumArtist=${albumMetadata.albumArtist}, albumArtUri=${albumMetadata.artworkUri}")

            val albumTitle = albumMetadata.albumTitle.toString()
            val albumArtist = albumMetadata.albumArtist.toString()
            val albumUri = albumMetadata.artworkUri ?: Uri.EMPTY

            viewHolder.binding.playButton.setOnClickListener {
                onPlayIconClick(albumTitle)
            }

            viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            UtilImpl.drawImageAssociatedWithAlbum(
                viewHolder.binding.albumArt,
                albumUri,
                Size(400, 400),
                customImage
            )

            viewHolder.binding.menuIcon.setOnClickListener {
                val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

                menu.menuInflater.inflate(R.menu.album_options, menu.menu)
                menu.setOnMenuItemClickListener {
                    Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()

                    //Handle Album Option
                    val customImageName = "album_${albums[position].mediaMetadata.albumTitle}"
                    handleAlbumOption(
                        MenuOptionUtil.determineMenuOptionFromTitle(it.title.toString()),
                        albums[position].mediaId,
                        customImageName
                    )

                    return@setOnMenuItemClickListener true
                }
                menu.show()
            }

            viewHolder.binding.albumName.text = "$albumTitle \n $albumArtist"
            albumMetadata.releaseYear?.let { year ->
                if(year > 0) {
                    viewHolder.binding.releaseYear.text = year.toString()
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return albums.size
    }
}
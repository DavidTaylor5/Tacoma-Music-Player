package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.database.entity.SongGroupEntity
import com.andaagii.tacomamusicplayer.databinding.ViewholderAlbumGridLayoutBinding
import com.andaagii.tacomamusicplayer.util.MenuOptionUtil
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/**
 * A recyclerview adapter that is able to take a list of Album Media Items and display them.
 */
class AlbumGridAdapter(
    private var albums: List<MediaItem>,
    private val onAlbumClick: (String) -> Unit,
    private val onPlayIconClick: (String) -> Unit,
    private val handleAlbumOption: (MenuOptionUtil.MenuOption, String, String?) -> Unit,
): RecyclerView.Adapter<AlbumGridAdapter.AlbumGridViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumGridViewHolder(val binding : ViewholderAlbumGridLayoutBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumGridViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        //TODO I will also need an if statement if I want to switch between bindings
        val binding = ViewholderAlbumGridLayoutBinding.inflate(inflater, parent, false)

        return AlbumGridViewHolder(binding)
    }

    fun updateData(albums: List<MediaItem>) {
        this.albums = albums
        this.notifyDataSetChanged()
    }

    // Replace the contents of a view (invoked by the layout manager)
    @OptIn(UnstableApi::class) override fun onBindViewHolder(viewHolder: AlbumGridViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        //First check that dataSet has a value for position
        if(position < albums.size) {
            val album = albums[position]
            //val albumMetadata = album.mediaMetadata
            val customImage = "album_${album.mediaMetadata.albumTitle}"
            Timber.d("onBindViewHolder: CHECKING VALUES albumTitle=${album.mediaMetadata.albumTitle}, albumArtist=${album.mediaMetadata.albumArtist}, albumArtUri=${album.mediaMetadata.artworkUri}")

            val albumTitle = album.mediaId
            val albumArtist = album.mediaMetadata.albumArtist
            val albumUri = album.mediaMetadata.artworkUri ?: Uri.EMPTY

            viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            UtilImpl.drawImageAssociatedWithAlbum(
                viewHolder.binding.albumArt,
                albumUri,
                Size(400, 400),
                customImage
            )

            viewHolder.binding.albumName.text = albumTitle

            album.mediaMetadata.releaseYear?.let { year ->
                if(year > 0) {
                    viewHolder.binding.descriptionText.text = "$year | $albumArtist"
                }
            }
        }

        viewHolder.binding.itemContainer.setOnLongClickListener {
            val menu = PopupMenu(
                viewHolder.itemView.context,
                viewHolder.binding.itemContainer,
                Gravity.START,
                0,
                R.style.PopupMenuBlack
            )

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

            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return albums.size
    }
}
package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
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
    private val handleAlbumOption: (MenuOptionUtil.MenuOption, String) -> Unit,
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
    @OptIn(UnstableApi::class) override fun onBindViewHolder(viewHolder: AlbumViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var albumTitle = "Default ALBUM"
        var albumArtist = ""
        var albumUri = Uri.EMPTY

        //First check that dataSet has a value for position
        if(position < albums.size) {

            Timber.d("onBindViewHolder: CHECKING VALUES albumTitle=${albums[0].mediaMetadata.albumTitle}, albumArtist=${albums[0].mediaMetadata.albumArtist}, albumArtUri=${albums[0].mediaMetadata.artworkUri}")

            albumTitle = albums[position].mediaMetadata.albumTitle.toString()
            albumArtist = albums[position].mediaMetadata.albumArtist.toString()
            albumUri = albums[position].mediaMetadata.artworkUri

            viewHolder.binding.playButton.setOnClickListener {
                onPlayIconClick(albumTitle)
            }

            viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            UtilImpl.drawUriOntoImageViewCoil(
                viewHolder.binding.albumArt,
                albumUri,
                Size(400, 400)
            )

            viewHolder.binding.menuIcon.setOnClickListener {
                val menu = PopupMenu(viewHolder.itemView.context, viewHolder.binding.menuIcon)

                menu.menuInflater.inflate(R.menu.album_options, menu.menu)
                menu.setOnMenuItemClickListener {
                    Toast.makeText(viewHolder.itemView.context, "You Clicked " + it.title, Toast.LENGTH_SHORT).show()

                    handleMenuOption(it.title.toString(), position)

                    return@setOnMenuItemClickListener true
                }
                menu.show()
            }
        }

        viewHolder.binding.albumName.text = "$albumTitle \n $albumArtist"

        albums[position].mediaMetadata.releaseYear?.let { year ->
            if(year > 0) {
                viewHolder.binding.releaseYear.text = year.toString()
            }
        }
    }

    private fun handleMenuOption(menuOptionTitle: String, position: Int) {
        Timber.d("handleMenuOption: menuOptionTitle=$menuOptionTitle, position=$position")
        when(MenuOptionUtil.determineMenuOptionFromTitle(menuOptionTitle)) {
            MenuOptionUtil.MenuOption.PLAY_ALBUM -> {
                handleAlbumOption(
                    MenuOptionUtil.MenuOption.PLAY_ALBUM,
                    albums[position].mediaId
                )
            }
            MenuOptionUtil.MenuOption.ADD_TO_QUEUE -> {
                handleAlbumOption(
                    MenuOptionUtil.MenuOption.ADD_TO_QUEUE,
                    albums[position].mediaId
                )
            }
            else -> {
                Timber.d("handleMenuOption: Album Menu Option not recognized.")
            }
        }
    }

    override fun getItemCount(): Int {
        return albums.size
    }
}
package com.andaagii.tacomamusicplayer.adapter

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.andaagii.tacomamusicplayer.databinding.ViewholderAlbumBinding
import com.andaagii.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/**
 * A recyclerview adapter that is able to take a list of Album Media Items and display them.
 */
class AlbumListAdapter(
    private val albums: List<MediaItem>,
    private val onAlbumClick: (String) -> Unit,
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

    // Replace the contents of a view (invoked by the layout manager)
    @OptIn(UnstableApi::class) override fun onBindViewHolder(viewHolder: AlbumViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")

        var albumTitle = "Default ALBUM"
        var albumArtist = ""
        var albumDuration = ""
        var albumUri = Uri.EMPTY

        //First check that dataSet has a value for position
        if(position < albums.size) {

            Timber.d("onBindViewHolder: CHECKING VALUES albumTitle=${albums[0].mediaMetadata.albumTitle}, albumArtist=${albums[0].mediaMetadata.albumArtist}, albumArtUri=${albums[0].mediaMetadata.artworkUri}")

            albumTitle = albums[position].mediaMetadata.albumTitle.toString()
            albumArtist = albums[position].mediaMetadata.albumArtist.toString()
            albumUri = albums[position].mediaMetadata.artworkUri

            viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            UtilImpl.drawUriOntoImageView(
                viewHolder.binding.albumArt,
                albumUri,
                Size(100, 100)
            )
        }

        viewHolder.binding.albumName.text = "$albumTitle \n $albumArtist"

        albums[position].mediaMetadata.releaseYear?.let { year ->
            if(year > 0) {
                viewHolder.binding.releaseYear.text = year.toString()
            }
        }
    }

    override fun getItemCount(): Int {
        return albums.size
    }
}
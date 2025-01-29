package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.databinding.ViewholderAlbumBinding
import com.example.tacomamusicplayer.util.UtilImpl
import timber.log.Timber

/**
 * A recyclerview adapter that is able to take a list of Album Media Items and display them.
 */
class AlbumListAdapter(
    private val dataSet: List<MediaItem>,
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
        var albumUri = Uri.EMPTY //TODO I'll have to get a solution  for this... else default picture

        //First check that dataSet has a value for position
        if(position < dataSet.size) {

            Timber.d("onBindViewHolder: CHECKING VALUES albumTitle=${dataSet[0].mediaMetadata.albumTitle}, albumArtist=${dataSet[0].mediaMetadata.albumArtist}, albumArtUri=${dataSet[0].mediaMetadata.artworkUri}")

            albumTitle = dataSet[position].mediaMetadata.albumTitle.toString()
            albumArtist = dataSet[position].mediaMetadata.albumArtist.toString()
            albumUri = dataSet[position].mediaMetadata.artworkUri

            viewHolder.binding.itemContainer.setOnClickListener { onAlbumClick(albumTitle) }

            UtilImpl.drawUriOntoImageView(
                viewHolder.binding.albumArt,
                albumUri,
                Size(100, 100)
            )
        }

        // Get element from  your dataset at this position and replace the contents of the view with that element
        //viewHolder.albumInfo.text = "Default ALBUM \n Default Artist \n Default Duration"//dataSet[position]
        viewHolder.binding.albumInfo.text = "$albumTitle \n $albumArtist"
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
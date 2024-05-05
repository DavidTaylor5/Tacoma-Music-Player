package com.example.tacomamusicplayer.adapter

import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.databinding.ViewholderAlbumBinding
import timber.log.Timber
import java.lang.Exception

/**
 * A recyclerview adapter that is able to take a list of Album Media Items and display them.
 */
class AlbumListAdapter(private val dataSet: List<MediaItem>): RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumViewHolder(val binding : ViewholderAlbumBinding): RecyclerView.ViewHolder(binding.root)

    //Create new views (invoked by the layout manager)
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

            val resolver = viewHolder.itemView.context.contentResolver

            try {

                Timber.d("queryAllMediaItems: Getting album art from URI=${albumUri.toString()}")

                //Album art as a bitmap, I need to work on what to do when this is blank / null?
                val albumArt = resolver.loadThumbnail(albumUri, Size(100, 100), null)
                val albumDrawable = BitmapDrawable(viewHolder.itemView.context.resources, albumArt)

                viewHolder.binding.albumArt.setImageDrawable(albumDrawable)

                Timber.d("queryAllMediaItems: SUCCESSFUL! ALBUM ART FOUND!")

            } catch (e: Exception) {
                Timber.d("queryAllMediaItems: ERROR ON LOADING ALBUM ART e=$e")
            }
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
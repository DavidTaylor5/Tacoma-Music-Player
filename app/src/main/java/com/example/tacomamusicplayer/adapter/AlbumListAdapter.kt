package com.example.tacomamusicplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R
import com.example.tacomamusicplayer.databinding.ViewholderAlbumBinding
import timber.log.Timber

//I'm going to use this recyclerview to show all available albums...
class AlbumListAdapter(private val dataSet: List<MediaItem>): RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumViewHolder(val binding : ViewholderAlbumBinding): RecyclerView.ViewHolder(binding.root) {

    }

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        Timber.d("onCreateViewHolder: ")

        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = ViewholderAlbumBinding.inflate(inflater, parent, false)

        return AlbumViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: AlbumViewHolder, position: Int) {
        Timber.d("onBindViewHolder: ")


        // Get element from  your dataset at this position and replace the contents of the view with that element
        //viewHolder.albumInfo.text = "Default ALBUM \n Default Artist \n Default Duration"//dataSet[position]
        viewHolder.binding.albumInfo.text = "Default ALBUM \n Default Artist \n Default Duration"
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
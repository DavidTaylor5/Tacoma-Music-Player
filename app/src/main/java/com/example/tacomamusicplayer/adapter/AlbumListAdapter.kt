package com.example.tacomamusicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tacomamusicplayer.R

//I'm going to use this recyclerview to show all available albums...
class AlbumListAdapter(private val dataSet: List<String>): RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class AlbumViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val albumInfo: TextView

        init {
            albumInfo = view.findViewById(R.id.album_info)
        }
    }

    //Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        //Create a new view, which defines the UI of teh list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_album, parent, false)

        return AlbumViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: AlbumViewHolder, position: Int) {

        // Get element from  your dataset at this position and replace the contents of the view with that element
        viewHolder.albumInfo.text = "Default ALBUM \n Default Artist \n Default Duration"//dataSet[position]
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }

}
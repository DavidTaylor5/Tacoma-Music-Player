package com.example.tacomamusicplayer.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tacomamusicplayer.adapter.PlaylistPromptAdapter
import com.example.tacomamusicplayer.data.Playlist
import com.example.tacomamusicplayer.data.PlaylistData
import com.example.tacomamusicplayer.databinding.ViewCustomInformationScreenBinding
import com.example.tacomamusicplayer.databinding.ViewCustomPlaylistPromptBinding

class CustomPlaylistPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomPlaylistPromptBinding

    init {
        attrs?.let {

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            //I gotta remember to actually attach to the view
            binding = ViewCustomPlaylistPromptBinding.inflate(inflater, this, true)

            //Note don't forget the layoutManager
            binding.displayRecyclerview.layoutManager = LinearLayoutManager(this.context)
        }
    }

    /**
     * Show all available playlists in the prompt.
     */
    fun setPlaylistData(playlists: List<Playlist>) {
        binding.displayRecyclerview.adapter = PlaylistPromptAdapter(
            playlists
        ) {  /* TODO */ }

        //Now that data has changed -> notify the rv to update!
        val rv = binding.displayRecyclerview
        val rvAdapter = rv.adapter
        rvAdapter!!.notifyDataSetChanged()
    }
}
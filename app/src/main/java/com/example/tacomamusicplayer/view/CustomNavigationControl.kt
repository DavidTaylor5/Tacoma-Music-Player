package com.example.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tacomamusicplayer.R

class CustomNavigationControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    //views inside of the custom layout
    private lateinit var playlistButton: ImageView
    private lateinit var browseAlbumButton: ImageView
    private lateinit var albumButton: ImageView

    init {

        attrs?.let {

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val typedArray = context.obtainStyledAttributes(it, R.styleable.CustomNavigationControl)

            //possible xml custom attributes
            val playlistImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_playlistImg, 0)
            val albumBrowseImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_albumBrowseImg, 0)
            val albumImg = typedArray.getResourceId(R.styleable.CustomNavigationControl_albumImg, 0)

            //finalte default layout
            inflater.inflate(R.layout.view_custom_navigation_control, this)

            //get references to views in default layout
            playlistButton = findViewById(R.id.playlist_button)
            browseAlbumButton = findViewById(R.id.album_browse_button)
            albumButton = findViewById(R.id.album_button)

            playlistButton.setImageDrawable(ContextCompat.getDrawable(context, playlistImg))
            browseAlbumButton.setImageDrawable(ContextCompat.getDrawable(context, albumBrowseImg))
            albumButton.setImageDrawable(ContextCompat.getDrawable(context, albumImg))
        }

    }

    fun setPlaylistButtonOnClick(callback: () -> Unit) {
        playlistButton.setOnClickListener { callback() }
    }

    fun setBrowseAlbumButtonOnClick(callback: () -> Unit) {
        browseAlbumButton.setOnClickListener { callback() }
    }

    fun setAlbumButtonOnClick(callback: () -> Unit) {
        albumButton.setOnClickListener { callback() }
    }
}
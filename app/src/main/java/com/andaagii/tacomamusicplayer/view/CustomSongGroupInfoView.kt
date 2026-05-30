package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.andaagii.tacomamusicplayer.databinding.CustomSongGroupInfoViewBinding

/**
 * Header view displayed above the track list when the user drills into an album or playlist.
 *
 * Shows album art, the song-group title, a play button, and a menu icon. Click callbacks for
 * the play and menu buttons are registered via [setOnPlayIconPressed] and [setOnMenuIconPressed].
 * The artwork [ImageView] is exposed via [getSongGroupImage] so the host can load an image into it.
 */
class CustomSongGroupInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: CustomSongGroupInfoViewBinding

    private var onMenuIconPressed : () -> Unit = {}
    private var onPlayIconPressed : () -> Unit = {}


    init {
        attrs?.let {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = CustomSongGroupInfoViewBinding.inflate(inflater, this, true)

            binding.playButton.setOnClickListener {
                onPlayIconPressed()
            }

            binding.menuIcon.setOnClickListener {
                onMenuIconPressed()
            }
        }
    }

    /** Registers [onPress] as the callback invoked when the menu icon is tapped. */
    fun setOnMenuIconPressed(onPress: () -> Unit) {
        onMenuIconPressed = onPress
    }

    /** Registers [onPress] as the callback invoked when the play button is tapped. */
    fun setOnPlayIconPressed(onPress: () -> Unit) {
        onPlayIconPressed = onPress
    }

    /**
     * Sets the title text displayed in the header.
     *
     * @param title The album or playlist name to display.
     */
    fun setSongGroupTitleText(title: String) {
        binding.songGroupTitle.text = title
        binding.songGroupImage
    }

    /** Returns the menu icon [View] so the host can anchor a [android.widget.PopupMenu] to it. */
    fun getMenuIconView(): View { return binding.menuIcon }

    /** Returns the artwork [ImageView] so the host can load album or playlist art into it. */
    fun getSongGroupImage(): ImageView { return binding.songGroupImage }
}
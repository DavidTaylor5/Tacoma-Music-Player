package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomInformationScreenBinding

/**
 * Empty-state / informational screen displayed when a list has no content to show.
 *
 * Supports up to two icon+label pairs, each with an optional click callback. The host fragment
 * configures the icons, labels, and callbacks via the `set*` functions based on the specific
 * empty-state context (e.g., no songs, no playlists).
 */
class CustomInformationScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomInformationScreenBinding

    init {
        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomInformationScreenBinding.inflate(inflater, this, true)
        }
    }

    /** Sets the drawable for the first icon slot. */
    fun setFirstIcon(imageDrawable: Drawable) {
        binding.firstIcon.setImageDrawable(imageDrawable)
    }

    /** Registers [callback] as the click listener for the first icon's tappable area. */
    fun setFirstIconCallback(callback: () -> Unit) {
        binding.firstClickable.setOnClickListener {
            callback()
        }
    }

    /** Sets the drawable for the second icon slot. */
    fun setSecondIcon(imageDrawable: Drawable) {
        binding.secondIcon.setImageDrawable(imageDrawable)
    }

    /** Registers [callback] as the click listener for the second icon's tappable area. */
    fun setSecondIconCallback(callback: () -> Unit) {
        binding.secondClickable.setOnClickListener {
            callback()
        }
    }

    /** Sets the label text below the first icon. */
    fun setFirstInfo(info: String) {
        binding.firstInfo.text = info
    }

    /** Sets the label text below the second icon. */
    fun setSecondInfo(info: String) {
        binding.secondInfo.text = info
    }
}
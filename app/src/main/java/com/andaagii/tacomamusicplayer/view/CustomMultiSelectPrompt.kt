package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomMultiSelectPromptBinding

/**
 * Toolbar overlay displayed during multi-select mode in [com.andaagii.tacomamusicplayer.fragment.pages.SongListFragment].
 *
 * Contains a description text, a confirm (menu) icon to act on the selection, and a close icon
 * to exit multi-select mode. Callbacks are registered via [setOnMenuIconClick] and [setOnCloseIconClick].
 */
class CustomMultiSelectPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomMultiSelectPromptBinding

    private var menuIconOnClick : () -> Unit = {}
    private var closeIconOnClick: () -> Unit = {}

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewCustomMultiSelectPromptBinding.inflate(inflater, this, true)

        binding.menuIcon.setOnClickListener {
            menuIconOnClick()
        }

        binding.closeIcon.setOnClickListener {
            closeIconOnClick()
        }
    }

    /**
     * Updates the description text shown in the toolbar, typically to reflect the selection count.
     *
     * @param text The text to display (e.g., "3 songs selected").
     */
    fun setPromptText(text: String) {
        binding.descriptionText.text = text
    }

    /** Registers [onClick] as the callback invoked when the confirm/menu icon is tapped. */
    fun setOnMenuIconClick(onClick: () -> Unit) {
        menuIconOnClick = onClick
    }

    /** Registers [onClick] as the callback invoked when the close icon is tapped. */
    fun setOnCloseIconClick(onClick: () -> Unit) {
        closeIconOnClick = onClick
    }
}
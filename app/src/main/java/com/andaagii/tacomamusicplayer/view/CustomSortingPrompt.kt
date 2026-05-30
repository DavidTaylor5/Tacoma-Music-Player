package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomSortingPromptBinding

/**
 * Dialog overlay for choosing a sort order (title, artist, release year, etc.).
 *
 * Currently inflates `view_custom_sorting_prompt.xml` with no additional logic. Sort option
 * selection is handled by the host fragment via a separate [android.widget.PopupMenu] built
 * from the sort options XML menus.
 */
class CustomSortingPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomSortingPromptBinding

    init {
        attrs?.let {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomSortingPromptBinding.inflate(inflater, this, true)
        }
    }
}
package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomSortingPromptBinding

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
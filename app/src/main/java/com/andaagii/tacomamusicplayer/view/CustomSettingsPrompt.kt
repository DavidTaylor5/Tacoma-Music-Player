package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomSettingsPromptBinding

/**
 * Settings / options dialog overlay.
 *
 * Currently inflates `view_custom_settings_prompt.xml` with no additional logic. This view is
 * a shell reserved for future settings options and has no interactive elements at present.
 */
class CustomSettingsPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomSettingsPromptBinding

    init {
        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomSettingsPromptBinding.inflate(inflater, this, true)
        }
    }
}
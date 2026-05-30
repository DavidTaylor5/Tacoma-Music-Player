package com.andaagii.tacomamusicplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.andaagii.tacomamusicplayer.databinding.ViewCustomInputTextPromptBinding

/**
 * Generic two-button dialog with a text input field.
 *
 * Used for actions that require a short string from the user, such as naming a new playlist
 * or renaming an existing one. Button labels and click callbacks are configured via the
 * `set*` functions after inflation. The entered text is retrieved with [getUserInputtedText]
 * and cleared with [resetUserInput].
 */
class CustomInputTextPrompt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewCustomInputTextPromptBinding

    private var option1ButtonOnClick : () -> Unit = {}
    private var option2ButtonOnClick : () -> Unit = {}

    init {
        attrs?.let {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewCustomInputTextPromptBinding.inflate(inflater, this, true)

            binding.option1Button.setOnClickListener { option1ButtonOnClick() }

            binding.option2Button.setOnClickListener { option2ButtonOnClick() }
        }
    }

    /**
     * Sets the floating hint label on the text input field.
     *
     * @param hint Hint text displayed when the field is empty.
     */
    fun setTextInputHint(hint: String) {
        binding.textPromptLayout.hint = hint
    }

    /** Sets the label for the primary (confirm) button. */
    fun setOption1ButtonText(b1Text: String) {
        binding.option1Button.text = b1Text
    }

    /** Sets the label for the secondary (cancel) button. */
    fun setOption2ButtonText(b2Text: String) {
        binding.option2Button.text = b2Text
    }

    /** Hides the prompt. */
    fun closePrompt() {
        this.visibility = View.GONE
    }

    /** Makes the prompt visible. */
    fun showPrompt() {
        this.visibility = View.VISIBLE
    }

    /** Registers [onClick] as the callback invoked when the primary button is tapped. */
    fun setOption1ButtonOnClick(onClick: () -> Unit) {
        option1ButtonOnClick = onClick
    }

    /** Registers [onClick] as the callback invoked when the secondary button is tapped. */
    fun setOption2ButtonOnClick(onClick: () -> Unit) {
        option2ButtonOnClick = onClick
    }

    /**
     * Returns the current text entered by the user, trimmed of surrounding whitespace.
     *
     * @return The text field's current value as a [String], or an empty string if blank.
     */
    fun getUserInputtedText(): String {
        return binding.promptInput.text.toString()
    }

    /** Clears the text input field. Called after the user confirms or cancels the prompt. */
    fun resetUserInput() {
        binding.promptInput.setText("")
    }
}
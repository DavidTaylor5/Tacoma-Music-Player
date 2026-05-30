package com.andaagii.tacomamusicplayer.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andaagii.tacomamusicplayer.R
import com.andaagii.tacomamusicplayer.util.SortingUtil

/** Warm gold color matching the original `#A2942B` footer background. */
private val SortingFooterColor = Color(0xFFA2942B)

/**
 * Dialog for choosing a sort order from a caller-supplied list of options.
 *
 * Stateful — the currently selected option is tracked internally and initialised from
 * [currentSorting]. The caller supplies [availableOptions] so that album-specific options
 * (title / artist / release year) and playlist-specific options (title / creation date /
 * modification date) can each be shown in the correct context.
 *
 * @param modifier Modifier applied to the root [Column].
 * @param availableOptions The subset of [SortingUtil.SortingOption] values to display as
 *   radio choices.
 * @param currentSorting The option that should appear pre-selected when the prompt opens.
 * @param onApplyClick Called with the user's chosen [SortingUtil.SortingOption] when APPLY
 *   is tapped.
 * @param onCloseClick Called when the user taps CLOSE without applying a change.
 */
@Composable
fun SortingPrompt(
    modifier: Modifier = Modifier,
    availableOptions: List<SortingUtil.SortingOption>,
    currentSorting: SortingUtil.SortingOption,
    onApplyClick: (SortingUtil.SortingOption) -> Unit,
    onCloseClick: () -> Unit
) {
    // Pre-select the current sort order; changes are local until the user taps APPLY
    var selected by remember { mutableStateOf(currentSorting) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sorting_options_title),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        // One radio row per available option
        availableOptions.forEach { option ->
            SortingOptionRow(
                option = option,
                isSelected = option == selected,
                onSelected = { selected = option }
            )
        }

        HorizontalDivider(color = Color.DarkGray)

        // Footer with close and apply actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCloseClick) {
                Text(text = stringResource(R.string.close_button), color = Color.White)
            }
            TextButton(onClick = { onApplyClick(selected) }) {
                Text(text = stringResource(R.string.apply_button), color = Color.White)
            }
        }
    }
}

/**
 * Single radio button row inside [SortingPrompt].
 *
 * Stateless — the parent [SortingPrompt] owns the selection state.
 *
 * @param option The [SortingUtil.SortingOption] this row represents.
 * @param isSelected Whether this row's radio button should appear selected.
 * @param onSelected Called when the user taps this row to select it.
 */
@Composable
private fun SortingOptionRow(
    option: SortingUtil.SortingOption,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Make the entire row tappable, not just the radio button
            .clickable(onClick = onSelected)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(selectedColor = Color.White)
        )
        Text(
            text = sortingOptionLabel(option),
            color = Color.White,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * Maps a [SortingUtil.SortingOption] to its human-readable string resource label.
 *
 * @param option The sort option to look up.
 * @return The localised label string for display in the radio row.
 */
@Composable
private fun sortingOptionLabel(option: SortingUtil.SortingOption): String {
    return when (option) {
        SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL ->
            stringResource(R.string.order_by_title_alphabetical)
        SortingUtil.SortingOption.SORTING_ARTIST_ALPHABETICAL ->
            stringResource(R.string.order_by_artist_alphabetical)
        SortingUtil.SortingOption.SORTING_NEWEST_RELEASE ->
            stringResource(R.string.order_by_newest_release)
        SortingUtil.SortingOption.SORTING_OLDEST_RELEASE ->
            stringResource(R.string.order_by_oldest_release)
        SortingUtil.SortingOption.SORTING_BY_CREATION_DATE ->
            stringResource(R.string.order_by_creation_date)
        SortingUtil.SortingOption.SORTING_BY_MODIFICATION_DATE ->
            stringResource(R.string.order_by_recently_modified)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun SortingPromptPreview() {
    SortingPrompt(
        availableOptions = listOf(
            SortingUtil.SortingOption.SORTING_TITLE_ALPHABETICAL,
            SortingUtil.SortingOption.SORTING_ARTIST_ALPHABETICAL,
            SortingUtil.SortingOption.SORTING_NEWEST_RELEASE,
            SortingUtil.SortingOption.SORTING_OLDEST_RELEASE
        ),
        currentSorting = SortingUtil.SortingOption.SORTING_NEWEST_RELEASE,
        onApplyClick = {},
        onCloseClick = {}
    )
}

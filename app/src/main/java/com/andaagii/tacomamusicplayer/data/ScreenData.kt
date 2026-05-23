package com.andaagii.tacomamusicplayer.data

import com.andaagii.tacomamusicplayer.enumtype.ScreenType

/**
 * Navigation event wrapper posted through `LiveData` to trigger a screen transition.
 *
 * Wrapping [ScreenType] in a data class enables `MainViewModel` to emit a distinct object
 * each time navigation is requested. Without this wrapper, posting the same [ScreenType]
 * value twice would not re-trigger observers because `LiveData` skips emission when the
 * new value equals the current one.
 *
 * @param currentScreen The destination screen to navigate to.
 */
data class ScreenData(
    val currentScreen: ScreenType
)

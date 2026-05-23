package com.andaagii.tacomamusicplayer.data

/**
 * Artwork selection state for a song group (album or playlist).
 *
 * Consumers should use `MediaItemUtil.determineArtUri` rather than reading these fields
 * directly — it applies the [useCustomArt] flag and falls back gracefully when the
 * custom file is missing or empty.
 *
 * @param artFileOriginal Absolute file-system path to the artwork extracted from MediaStore
 *   during library cataloging. Empty string if no embedded art was found for this group.
 * @param artFileCustom Absolute file-system path to an image uploaded by the user
 *   (cropped via uCrop). Empty string when no custom art has been set.
 * @param useCustomArt `true` to display [artFileCustom] instead of [artFileOriginal].
 *   Mirrors the value stored in `SongEntity.useCustomArt` and persisted in the Room database.
 */
data class ArtInfo(
    val artFileOriginal: String,
    val artFileCustom: String,
    val useCustomArt: Boolean
)

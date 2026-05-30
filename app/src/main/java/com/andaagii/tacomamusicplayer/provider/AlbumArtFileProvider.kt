package com.andaagii.tacomamusicplayer.provider

import androidx.core.content.FileProvider

/**
 * [FileProvider] subclass that exposes local album-art files as `content://` URIs.
 *
 * Android Auto cannot access raw `file://` URIs due to its cross-process security model,
 * so artwork must be shared through a [FileProvider]. This subclass exists solely to give
 * the provider a unique authority string (`com.andaagii.tacomamusicplayer.provider`) declared
 * in [AndroidManifest.xml]; no additional logic is required beyond the inherited implementation.
 *
 * Shareable paths are configured in `res/xml/file_paths.xml`.
 */
class AlbumArtFileProvider(): FileProvider() {



}
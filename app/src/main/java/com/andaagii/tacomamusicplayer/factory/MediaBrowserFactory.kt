package com.andaagii.tacomamusicplayer.factory

import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Injectable factory that creates [MediaBrowser] instances for connecting to [MusicService].
 *
 * Wrapping construction in a factory keeps [MainViewModel] testable — tests can substitute
 * a fake factory without needing a real [android.content.Context] or service binding.
 */
class MediaBrowserFactory @Inject constructor(
    @ApplicationContext val context: Context
) {
    /**
     * Builds a [MediaBrowser] connected to the session identified by [sessionToken].
     *
     * Returns a [ListenableFuture] that completes once the browser has successfully
     * connected to the [com.andaagii.tacomamusicplayer.service.MusicService] session.
     *
     * @param sessionToken Token identifying the target [androidx.media3.session.MediaSessionService].
     * @return A future that resolves to the connected [MediaBrowser].
     */
    fun create(sessionToken: SessionToken): ListenableFuture<MediaBrowser> {
        return MediaBrowser.Builder(context, sessionToken).buildAsync()
    }
}
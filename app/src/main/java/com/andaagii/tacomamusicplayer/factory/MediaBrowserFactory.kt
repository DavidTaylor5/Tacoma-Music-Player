package com.andaagii.tacomamusicplayer.factory

import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaBrowserFactory @Inject constructor(
    @ApplicationContext val context: Context
) {
    fun create(sessionToken: SessionToken): ListenableFuture<MediaBrowser> {
        return MediaBrowser.Builder(context, sessionToken).buildAsync()
    }
}
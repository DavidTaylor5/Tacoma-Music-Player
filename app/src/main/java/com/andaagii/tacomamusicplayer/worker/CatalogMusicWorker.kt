package com.andaagii.tacomamusicplayer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.media3.session.MediaBrowser
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andaagii.tacomamusicplayer.database.dao.SongDao
import com.andaagii.tacomamusicplayer.database.dao.SongGroupDao
import com.andaagii.tacomamusicplayer.util.MediaStoreUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * A class that queries the medialibraryservice, saving found songs and album art into a database.
 */
@HiltWorker
class CatalogMusicWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaStoreUtil: MediaStoreUtil,
    private val songDao: SongDao,
    private val songGroupDao: SongGroupDao
): CoroutineWorker(appContext, workerParams) { //TODO find another way to pass in these dependencies...

    override suspend fun doWork(): Result {
        Timber.d("doWork: Catalog user music!")

        // Start cataloging the music
        catalogMusic()

        Timber.d("doWork: return success!")
        return Result.success()
    }

    /**
     * Catalog all of the music on a user's phone
     */
    private fun catalogMusic() {
        Timber.d("catalogMusic: ")
        //TODO query the albums with the MediaStore...
    }


    /**
     * Takes an album and adds all of it's songs to the
     */
    private fun catalogAlbumSongs(albumId: String) {
        Timber.d("catalogAlbumSongs: ")
        //TODO Query a certain album using mediaStore...
    }
}

/*
One work is defined, it must be scheduled with the WorkManager service in order to run.
WorkManager offers a lot of flexibility in how you schedule your work. You can schedule it to run
periodically over an interval of time, or you can schedule it to run only one time.

However you choose to schedule it, you will always use a WorkRequest. While a Worker defines the unit
of work, a WorkRequest (and its subclasses) define how and when it should be run. In the simplest case,
you can use a OneTimeWorkRequest.
ex.
val uploadWorkRequest: WorkRequest =
  OneTimeWorkRequestBuilder<UploadWorker>()
    .build()

Finally, you need to submit your WorkRequest to WorkManager using the enqueue() method.
ex.
WorkManager
  .getInstance(myContext)
  .enqueue(uploadWorkRequest)

 */
package com.andaagii.tacomamusicplayer.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

/**
 * A class that queries the medialibraryservice, saving found songs and album art into a database.
 */
class CheckMusicWorker(
    appContext: Context,
    workerParams: WorkerParameters
): Worker(appContext, workerParams) {

    override fun doWork(): Result {

        Timber.d("doWork: Work Done!")
        //TODO replace this with actual work...

        return Result.success()
        //Result.failure()
        //Result.retry()
    }


}

//TODO add hilt...


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
package app.studydesk.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.content.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Daily background check for new content: one small request when nothing changed.
 * WorkManager spreads the start time over a 6-hour window so phones don't all ask at once,
 * and backs off after failures.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ContentRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (val result = repository.sync(force = false)) {
        is SyncResult.Failed -> when (result.failure) {
            SyncFailure.Offline, SyncFailure.Timeout, is SyncFailure.Server -> Result.retry()
            else -> Result.success()   // retrying won't fix a full disk or a bad file today
        }
        else -> Result.success()
    }

    companion object {
        private const val NAME = "content-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS, 6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

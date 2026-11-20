package app.studydesk.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.ui.platform.LocalContext
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.content.SyncResult
import app.studydesk.core.designsystem.R

/** A plain sentence for a failed update. The saved content is always still there. */
@Composable
fun syncFailureMessage(failure: SyncFailure): String {
    val context = LocalContext.current
    return when (failure) {
        SyncFailure.Offline -> stringResource(R.string.sync_offline)
        SyncFailure.Timeout -> stringResource(R.string.sync_timeout)
        is SyncFailure.Server, is SyncFailure.Corrupt, is SyncFailure.Unknown -> stringResource(R.string.sync_failed_kept)
        is SyncFailure.StorageFull -> stringResource(
            R.string.sync_storage_full,
            Formatter.formatShortFileSize(context, maxOf(failure.bytesNeeded, 1L)),
        )
    }
}

/** A short sentence after a sync finished, for a snackbar. Null when there is nothing to say. */
@Composable
fun syncResultMessage(result: SyncResult): String? {
    val context = LocalContext.current
    return when (result) {
        SyncResult.Skipped, SyncResult.UpToDate -> stringResource(R.string.sync_up_to_date)
        is SyncResult.Updated -> pluralStringResource(
            R.plurals.sync_updated,
            result.files.size,
            result.files.size,
            Formatter.formatShortFileSize(context, result.bytes),
        )
        SyncResult.UpdateRequired -> stringResource(R.string.sync_update_app)
        is SyncResult.Failed -> syncFailureMessage(result.failure)
    }
}

/** "5 minutes ago", "Yesterday", or "never". */
@Composable
fun relativeTime(epochMillis: Long?): String =
    if (epochMillis == null) stringResource(R.string.time_never)
    else DateUtils.getRelativeTimeSpanString(epochMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

package app.studydesk.core.content

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Why an update did not happen. The phone's saved content is never touched when one occurs. */
sealed interface SyncFailure {
    /** No connection at all. */
    data object Offline : SyncFailure

    /** The connection worked but the server took too long. */
    data object Timeout : SyncFailure

    /** The server answered with an error status (after trying every address). */
    data class Server(val status: Int) : SyncFailure

    /** A file arrived with the wrong size or checksum, or could not be read. */
    data class Corrupt(val path: String) : SyncFailure

    /** Not enough free space on the phone. */
    data class StorageFull(val bytesNeeded: Long) : SyncFailure

    data class Unknown(val message: String?) : SyncFailure
}

/** Thrown inside the content layer, carrying the classified failure. */
class SyncException(val failure: SyncFailure, cause: Throwable? = null) : IOException(failure.toString(), cause)

internal fun Throwable.toSyncFailure(): SyncFailure {
    if (this is SyncException) return failure
    var t: Throwable? = this
    while (t != null) {
        when {
            t is UnknownHostException || t is ConnectException || t is NoRouteToHostException -> return SyncFailure.Offline
            t is SocketTimeoutException -> return SyncFailure.Timeout
            // Ktor's own timeout types, matched by name so this file does not depend on Ktor.
            t::class.java.simpleName.contains("Timeout") -> return SyncFailure.Timeout
            t is IOException && (t.message ?: "").contains("ENOSPC") -> return SyncFailure.StorageFull(0)
        }
        t = t.cause
    }
    return SyncFailure.Unknown(message)
}

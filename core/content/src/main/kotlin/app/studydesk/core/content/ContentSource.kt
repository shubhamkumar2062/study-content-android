package app.studydesk.core.content

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

/** Reads published files from the network. */
interface ContentSource {
    /** Conditional GET of `manifest.json`: [ManifestResponse.NotModified] when the ETag still matches. */
    suspend fun fetchManifest(etag: String?): ManifestResponse

    /** GET of any published file, by its path relative to `v1/`. */
    suspend fun fetchFile(path: String): ByteArray
}

sealed interface ManifestResponse {
    data object NotModified : ManifestResponse

    class Fetched(val bytes: ByteArray, val etag: String?) : ManifestResponse
}

/**
 * Tries the main address first and the backups only when it fails (no connection
 * problem on the phone's side, but an error or timeout from that server).
 * A phone that is offline fails fast instead of trying every address.
 */
class HttpContentSource(
    private val client: HttpClient,
    private val baseUrls: List<String>,
    private val log: (String) -> Unit = {},
) : ContentSource {

    private var requestCount = 0

    override suspend fun fetchManifest(etag: String?): ManifestResponse =
        withFallback("manifest.json") { base, isPrimary ->
            val response = client.get(base + "manifest.json") {
                // ETags differ between hosts, so only send the main host's one to the main host.
                if (etag != null && isPrimary) header(HttpHeaders.IfNoneMatch, etag)
            }
            count(base + "manifest.json", response.status.value)
            when (response.status) {
                HttpStatusCode.NotModified -> ManifestResponse.NotModified
                HttpStatusCode.OK -> ManifestResponse.Fetched(
                    bytes = response.body(),
                    etag = if (isPrimary) response.headers[HttpHeaders.ETag] else null,
                )
                else -> throw SyncException(SyncFailure.Server(response.status.value))
            }
        }

    override suspend fun fetchFile(path: String): ByteArray =
        withFallback(path) { base, _ ->
            val response = client.get(base + path)
            count(base + path, response.status.value)
            if (response.status != HttpStatusCode.OK) throw SyncException(SyncFailure.Server(response.status.value))
            response.body()
        }

    private suspend fun <T> withFallback(what: String, block: suspend (base: String, isPrimary: Boolean) -> T): T {
        var last: Throwable? = null
        for ((index, base) in baseUrls.withIndex()) {
            try {
                return block(base, index == 0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val failure = e.toSyncFailure()
                last = if (e is SyncException) e else SyncException(failure, e)
                // The phone has no connection: another address will not help.
                if (failure is SyncFailure.Offline) {
                    log("$what failed at $base: offline, not trying backups")
                    break
                }
                log("$what failed at $base ($failure); ${if (index < baseUrls.lastIndex) "trying backup" else "no more addresses"}")
            }
        }
        throw last ?: SyncException(SyncFailure.Unknown("no content address"))
    }

    private fun count(url: String, status: Int) {
        requestCount++
        log("request #$requestCount GET $url -> $status")
    }
}

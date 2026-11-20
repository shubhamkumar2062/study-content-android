package app.studydesk.core.content

import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.ContentManifest
import app.studydesk.core.model.FileEntry
import app.studydesk.core.model.PackEntry
import app.studydesk.core.model.SUPPORTED_CONTENT_FORMAT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Which subject packs the student needs kept up to date automatically (their own semester). */
fun interface WantedPacks {
    /** Picks from the pack ids the server lists. */
    suspend fun select(available: Collection<String>): Set<String>
}

sealed interface SyncResult {
    /** Checked too recently; no request was made. */
    data object Skipped : SyncResult

    /** The server has nothing newer (usually a 304 with no body). */
    data object UpToDate : SyncResult

    data class Updated(val files: List<FileEntry>, val bytes: Long) : SyncResult

    /** The server's content needs a newer app; the saved content is kept. */
    data object UpdateRequired : SyncResult

    data class Failed(val failure: SyncFailure) : SyncResult
}

/**
 * Keeps the phone's content in step with the server using as few requests as possible:
 * one conditional manifest request at most every [ContentConfig.minCheckIntervalMillis],
 * then only the files whose checksum changed and that the student actually needs.
 *
 * Nothing on the phone changes unless every needed file arrived and passed its checks.
 */
class ContentSyncer(
    private val source: ContentSource,
    private val storage: ContentStorage,
    private val bundled: BundledContent,
    private val config: ContentConfig,
    private val wantedPacks: WantedPacks,
    private val clock: Clock = Clock.System,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    /**
     * What the app reads now: the saved state, or the bundled copy on a fresh install. After an app
     * update whose bundled copy is newer than what the phone saved, the bundled copy is used, so new
     * subjects appear even if the phone never goes online.
     */
    fun currentState(): LocalState? {
        val saved = storage.readState()
        val shipped = bundled.manifest() ?: return saved
        return when {
            saved == null -> LocalState(active = shipped)
            isNewer(shipped, saved.active) -> saved.copy(active = shipped)
            else -> saved
        }
    }

    /** The bytes of a text file in use, from the downloaded copy or the bundled one. */
    fun read(entry: FileEntry): ByteArray? =
        storage.readObject(entry.sha256) ?: entry.takeIf(::isBundled)?.let { bundled.read(it.path) }

    /** A downloaded image, or null if it has not been downloaded yet. */
    fun mediaFile(entry: FileEntry): File? = storage.objectFile(entry.sha256)

    fun isOnPhone(entry: FileEntry): Boolean = storage.hasObject(entry.sha256) || isBundled(entry)

    /** Bytes used by downloaded files (the bundled copy inside the app is not counted). */
    fun downloadedBytes(): Long = storage.objectsSize()

    suspend fun sync(force: Boolean = false): SyncResult = mutex.withLock { withContext(io) { syncLocked(force) } }

    /** Brings one subject's text onto the phone (newest version) and keeps it updated from now on. */
    suspend fun downloadPack(packId: String): SyncResult = mutex.withLock {
        withContext(io) {
            val state = currentState() ?: return@withContext SyncResult.Failed(SyncFailure.Unknown("no content"))
            val newest = (state.remote ?: state.active).packs.firstOrNull { it.id == packId }
                ?: return@withContext SyncResult.Failed(SyncFailure.Unknown("unknown subject $packId"))
            val missing = listOf(newest.file).filterNot(::isOnPhone)
            try {
                download(missing)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return@withContext SyncResult.Failed(e.toSyncFailure())
            }
            val packs = state.active.packs.filterNot { it.id == packId } + newest
            storage.writeState(
                state.copy(
                    active = state.active.copy(packs = packs.sortedBy { it.id }),
                    pinned = state.pinned + packId,
                    lastUpdatedAt = if (missing.isEmpty()) state.lastUpdatedAt else clock.now(),
                ),
            )
            if (missing.isEmpty()) SyncResult.UpToDate else SyncResult.Updated(missing, missing.sumOf { it.bytes })
        }
    }

    /** Downloads a subject's images that are not on the phone yet. */
    suspend fun downloadMedia(packId: String): SyncResult = mutex.withLock {
        withContext(io) {
            val pack = currentState()?.active?.packs?.firstOrNull { it.id == packId }
                ?: return@withContext SyncResult.Failed(SyncFailure.Unknown("subject not on phone"))
            val missing = pack.media.filterNot { storage.hasObject(it.sha256) }
            try {
                download(missing)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return@withContext SyncResult.Failed(e.toSyncFailure())
            }
            if (missing.isEmpty()) SyncResult.UpToDate else SyncResult.Updated(missing, missing.sumOf { it.bytes })
        }
    }

    /** Stops a subject from updating automatically; its saved copy stays readable. */
    suspend fun unpin(packId: String) = mutex.withLock {
        withContext(io) {
            currentState()?.let { storage.writeState(it.copy(pinned = it.pinned - packId)) }
        }
    }

    private suspend fun syncLocked(force: Boolean): SyncResult {
        val state = currentState()
        val now = clock.now()
        val last = state?.lastCheckedAt
        if (!force && last != null && last <= now && now - last < config.minCheckIntervalMillis) {
            return SyncResult.Skipped
        }

        val response = try {
            source.fetchManifest(state?.etag)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return SyncResult.Failed(e.toSyncFailure())
        }

        val (remote, etag) = when (response) {
            ManifestResponse.NotModified ->
                (state?.remote ?: state?.active ?: return SyncResult.Failed(SyncFailure.Unknown("no content"))) to state?.etag
            is ManifestResponse.Fetched -> {
                val parsed = runCatching { ContentJson.decodeFromString<ContentManifest>(response.bytes.decodeToString()) }
                    .getOrElse { return SyncResult.Failed(SyncFailure.Corrupt("manifest.json")) }
                parsed to response.etag
            }
        }

        if (remote.contentFormat != SUPPORTED_CONTENT_FORMAT || remote.minAppVersion > config.appContentVersion) {
            if (state != null) {
                storage.writeState(state.copy(remote = remote, etag = etag, lastCheckedAt = now, appUpdateRequired = true))
            }
            return SyncResult.UpdateRequired
        }

        val wanted = wantedPacks.select(remote.packs.map { it.id }) + state?.pinned.orEmpty()
        val target = plan(state?.active, remote, wanted)
        val missing = target.textFiles.filterNot(::isOnPhone).distinctBy { it.sha256 }

        try {
            download(missing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return SyncResult.Failed(e.toSyncFailure())
        }

        val oldFiles = state?.active?.textFiles.orEmpty().map { it.path to it.sha256 }.toSet()
        val changed = target.textFiles.filter { (it.path to it.sha256) !in oldFiles }
        storage.writeState(
            LocalState(
                active = target,
                remote = remote,
                etag = etag,
                lastCheckedAt = now,
                lastUpdatedAt = if (changed.isEmpty()) state?.lastUpdatedAt else now,
                appUpdateRequired = false,
                pinned = state?.pinned.orEmpty(),
            ),
        )
        removeUnused(target)
        return if (changed.isEmpty()) SyncResult.UpToDate else SyncResult.Updated(changed, changed.sumOf { it.bytes })
    }

    /**
     * The files the phone should use next. Catalog and date sheets always follow the server.
     * A changed pack is downloaded only if the student needs it; otherwise the phone keeps
     * the copy it has (shown with "update available") until the subject is opened.
     */
    private fun plan(active: ContentManifest?, remote: ContentManifest, wanted: Set<String>): ContentManifest {
        val current = active?.packs.orEmpty().associateBy { it.id }
        val packs = remote.packs.mapNotNull { newest ->
            val old = current[newest.id]
            when {
                old != null && old.sha256 == newest.sha256 -> newest
                newest.id in wanted -> newest
                old != null -> old
                isOnPhone(newest.file) -> newest
                else -> null
            }
        }
        return remote.copy(packs = packs)
    }

    private suspend fun download(files: List<FileEntry>) {
        if (files.isEmpty()) return
        val needed = files.sumOf { it.bytes }
        if (storage.freeSpace() < needed + SPACE_MARGIN) throw SyncException(SyncFailure.StorageFull(needed))
        for (file in files) {
            val bytes = source.fetchFile(file.path)
            if (bytes.size.toLong() != file.bytes || sha256(bytes) != file.sha256) {
                throw SyncException(SyncFailure.Corrupt(file.path))
            }
            storage.writeObject(file.sha256, bytes)
        }
    }

    private fun removeUnused(active: ContentManifest) {
        val keep = active.textFiles.map { it.sha256 }.toSet() + active.packs.flatMap { p -> p.media.map { it.sha256 } }
        storage.deleteObjectsExcept(keep)
    }

    private fun isNewer(a: ContentManifest, b: ContentManifest): Boolean {
        val ta = runCatching { java.time.OffsetDateTime.parse(a.generatedAt).toInstant() }.getOrNull()
        val tb = runCatching { java.time.OffsetDateTime.parse(b.generatedAt).toInstant() }.getOrNull()
        return ta != null && tb != null && ta.isAfter(tb)
    }

    private fun isBundled(entry: FileEntry): Boolean =
        bundled.manifest()?.let { m -> m.textFiles.any { it.path == entry.path && it.sha256 == entry.sha256 } } == true

    private companion object {
        const val SPACE_MARGIN = 5L * 1024 * 1024
    }
}

internal fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** The newest version of a pack on the server, when it is newer than the copy on the phone. */
fun LocalState.newerPack(packId: String): PackEntry? {
    val mine = active.packs.firstOrNull { it.id == packId }
    val theirs = remote?.packs?.firstOrNull { it.id == packId } ?: return null
    return theirs.takeIf { mine == null || mine.sha256 != it.sha256 }
}

package app.studydesk.core.content

import app.studydesk.core.model.Catalog
import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.ExamSchedule
import app.studydesk.core.model.FileEntry
import app.studydesk.core.model.PackEntry
import app.studydesk.core.model.SubjectPack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import java.io.File

/** What the app knows about syncing, for banners and Settings. */
data class SyncStatus(
    val running: Boolean = false,
    val lastResult: SyncResult? = null,
    val lastCheckedAt: Long? = null,
    val lastUpdatedAt: Long? = null,
    val appUpdateRequired: Boolean = false,
    val contentGeneratedAt: String? = null,
)

/** One subject, as far as the phone has it. */
sealed interface PackResult {
    data class Ready(val pack: SubjectPack, val entry: PackEntry, val newerVersion: PackEntry?) : PackResult

    /** Listed on the server but not on the phone yet. */
    data class NotDownloaded(val entry: PackEntry) : PackResult

    /** The saved file could not be read (should not happen; offer a fresh download). */
    data object Unreadable : PackResult

    data object NotFound : PackResult
}

/**
 * The one entry point the app uses for content. Everything is read from the phone;
 * [sync] and the download functions are the only things that use the network.
 */
class ContentRepository(
    private val syncer: ContentSyncer,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val state = MutableStateFlow(syncer.currentState())
    private val status = MutableStateFlow(state.value.toStatus(running = false, last = null))
    private val parsed = object : LinkedHashMap<String, Any>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>?) = size > 12
    }

    val localState: StateFlow<LocalState?> = state.asStateFlow()
    val syncStatus: StateFlow<SyncStatus> = status.asStateFlow()

    val catalog: Flow<Catalog?> = state
        .map { it?.active?.catalog }
        .distinctUntilChanged()
        .map { entry -> entry?.let { load(it, Catalog.serializer()) } }
        .flowOn(io)

    val exams: Flow<List<ExamSchedule>> = state
        .map { it?.active?.exams.orEmpty() }
        .distinctUntilChanged()
        .map { entries -> entries.mapNotNull { load(it.file, ExamSchedule.serializer()) } }
        .flowOn(io)

    fun pack(packId: String): Flow<PackResult> = state
        .map { s -> Triple(s?.active?.packs?.firstOrNull { it.id == packId }, s?.newerPack(packId), s) }
        .distinctUntilChanged { a, b -> a.first == b.first && a.second == b.second }
        .map { (entry, newer, s) -> resolvePack(packId, entry, newer, s) }
        .flowOn(io)

    /** All subjects whose text is on the phone, for search indexing. */
    suspend fun loadAllPacks(): List<SubjectPack> = withContext(io) {
        state.value?.active?.packs.orEmpty().mapNotNull { load(it.file, SubjectPack.serializer()) }
    }

    fun mediaFile(packId: String, name: String): File? {
        val entry = state.value?.active?.packs?.firstOrNull { it.id == packId }
            ?.media?.firstOrNull { it.path.substringAfterLast('/') == name } ?: return null
        return syncer.mediaFile(entry)
    }

    suspend fun downloadedBytes(): Long = withContext(io) { syncer.downloadedBytes() }

    /** Asks the server for updates. Without [force] it does nothing if it asked recently. */
    suspend fun sync(force: Boolean = false): SyncResult = track { syncer.sync(force) }

    suspend fun downloadPack(packId: String): SyncResult = track { syncer.downloadPack(packId) }

    suspend fun downloadMedia(packId: String): SyncResult = track { syncer.downloadMedia(packId) }

    suspend fun removeDownload(packId: String) {
        syncer.unpin(packId)
        refresh()
    }

    private suspend fun track(block: suspend () -> SyncResult): SyncResult {
        status.update { it.copy(running = true) }
        val result = try {
            block()
        } finally {
            refresh()
        }
        status.value = state.value.toStatus(running = false, last = result)
        return result
    }

    private fun refresh() {
        state.value = syncer.currentState()
        status.update { state.value.toStatus(it.running, it.lastResult) }
    }

    private fun resolvePack(packId: String, entry: PackEntry?, newer: PackEntry?, s: LocalState?): PackResult {
        if (entry == null) {
            val remote = s?.remote?.packs?.firstOrNull { it.id == packId } ?: s?.active?.packs?.firstOrNull { it.id == packId }
            return if (remote != null) PackResult.NotDownloaded(remote) else PackResult.NotFound
        }
        val pack = load(entry.file, SubjectPack.serializer()) ?: return PackResult.Unreadable
        return PackResult.Ready(pack, entry, newer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> load(entry: FileEntry, serializer: DeserializationStrategy<T>): T? {
        synchronized(parsed) { parsed[entry.sha256]?.let { return it as T } }
        val bytes = syncer.read(entry) ?: return null
        val value = runCatching { ContentJson.decodeFromString(serializer, bytes.decodeToString()) }.getOrNull() ?: return null
        synchronized(parsed) { parsed[entry.sha256] = value }
        return value
    }
}

private fun LocalState?.toStatus(running: Boolean, last: SyncResult?) = SyncStatus(
    running = running,
    lastResult = last,
    lastCheckedAt = this?.lastCheckedAt,
    lastUpdatedAt = this?.lastUpdatedAt,
    appUpdateRequired = this?.appUpdateRequired == true,
    contentGeneratedAt = this?.active?.generatedAt,
)

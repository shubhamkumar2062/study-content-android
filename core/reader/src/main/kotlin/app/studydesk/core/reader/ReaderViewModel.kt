package app.studydesk.core.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.PackResult
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.content.SyncResult
import app.studydesk.core.database.BookmarkDao
import app.studydesk.core.database.BookmarkEntity
import app.studydesk.core.database.HighlightDao
import app.studydesk.core.database.HighlightEntity
import app.studydesk.core.database.ProgressDao
import app.studydesk.core.database.ReadingProgressEntity
import app.studydesk.core.database.UserPreferences
import app.studydesk.core.model.Question
import app.studydesk.core.model.ReaderSettings
import app.studydesk.core.model.SubjectPack
import app.studydesk.core.reader.markdown.AskYourself
import app.studydesk.core.reader.markdown.Block
import app.studydesk.core.reader.markdown.MarkdownParser
import app.studydesk.core.reader.markdown.ReaderDoc
import app.studydesk.core.reader.markdown.ReaderDocs
import app.studydesk.core.reader.navigation.ReaderRoute
import app.studydesk.core.reader.readaloud.ReadAloudController
import app.studydesk.core.reader.readaloud.ReadAloudState
import app.studydesk.core.reader.readaloud.SpeechSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class OutlineEntry(val blockIndex: Int, val level: Int, val title: String, val anchor: String)

data class SearchMatch(val blockIndex: Int, val start: Int, val end: Int)

data class FindState(val open: Boolean = false, val query: String = "", val matches: List<SearchMatch> = emptyList(), val current: Int = 0)

/** A parsed document, kept together so a doc switch swaps everything at once. */
data class LoadedDoc(
    val doc: ReaderDoc,
    val blocks: List<Block>,
    val group: List<ReaderDoc>,
    val outline: List<OutlineEntry>,
    val ask: Map<String, List<Question>>,
) {
    val prev: ReaderDoc? get() = group.getOrNull(group.indexOf(doc) - 1)
    val next: ReaderDoc? get() = group.getOrNull(group.indexOf(doc) + 1)
}

sealed interface ReaderContent {
    data object Loading : ReaderContent
    data class Ready(val pack: SubjectPack, val loaded: LoadedDoc, val restore: ReadingProgressEntity?, val newerAvailable: Boolean) : ReaderContent
    data class NotDownloaded(val bytes: Long) : ReaderContent
    data object NotFound : ReaderContent
    data object Unreadable : ReaderContent
}

data class ReaderUiState(
    val content: ReaderContent = ReaderContent.Loading,
    val settings: ReaderSettings = ReaderSettings(),
    val highlights: Map<Int, List<HighlightEntity>> = emptyMap(),
    val docBookmarked: Boolean = false,
    val bookmarkedAnchors: Set<String> = emptySet(),
    val done: Boolean = false,
    val find: FindState = FindState(),
    val downloading: Boolean = false,
    val readAloud: ReadAloudState = ReadAloudState(),
)

sealed interface ReaderEvent {
    data object MarkedDone : ReaderEvent
    data class HighlightDeleted(val highlight: HighlightEntity) : ReaderEvent
    data class BookmarkChanged(val added: Boolean) : ReaderEvent
    data class DownloadFailed(val failure: SyncFailure) : ReaderEvent
    data object Downloaded : ReaderEvent
    data object ReadAloudUnavailable : ReaderEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val preferences: UserPreferences,
    private val progress: ProgressDao,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val readAloud: ReadAloudController,
) : ViewModel() {
    private val route = savedState.toRoute<ReaderRoute>()
    val packId = route.packId
    private val docId = MutableStateFlow(route.docId)
    /** Block to jump to on first open (from search or a bookmark); -1 = the saved position. */
    var startBlock = route.blockIndex
        private set

    private val find = MutableStateFlow(FindState())
    private val downloading = MutableStateFlow(false)
    private val doneOverride = MutableStateFlow<Pair<String, Boolean>?>(null)
    private val _events = Channel<ReaderEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loaded = combine(content.pack(packId), docId) { result, id -> result to id }
        .map { (result, id) ->
            when (result) {
                is PackResult.Ready -> {
                    val doc = withContext(Dispatchers.Default) { load(result.pack, id) }
                    if (doc == null) ReaderContent.NotFound
                    else ReaderContent.Ready(result.pack, doc, progress.get(packId, id), result.newerVersion != null)
                }
                is PackResult.NotDownloaded -> ReaderContent.NotDownloaded(result.entry.bytes)
                PackResult.NotFound -> ReaderContent.NotFound
                PackResult.Unreadable -> ReaderContent.Unreadable
            }
        }

    private val userData = docId.flatMapLatest { id ->
        combine(
            highlightDao.forDoc(packId, id),
            bookmarkDao.forPack(packId),
            progress.forPack(packId),
        ) { highlights, bookmarks, progressRows ->
            Triple(
                highlights.groupBy { it.blockIndex },
                bookmarks,
                progressRows.firstOrNull { it.docId == id }?.done == true,
            )
        }
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        loaded,
        preferences.settings.map { it.reader },
        userData,
        combine(find, downloading, doneOverride, readAloud.state) { a, b, c, d -> listOf(a, b, c, d) },
        docId,
    ) { c, settings, (highlights, bookmarks, done), extra, id ->
        @Suppress("UNCHECKED_CAST")
        val override = extra[2] as Pair<String, Boolean>?
        ReaderUiState(
            content = c,
            settings = settings,
            highlights = highlights,
            docBookmarked = bookmarks.any { it.kind == "doc" && it.targetId == id },
            bookmarkedAnchors = bookmarks.filter { it.kind == "section" && it.docId == id }.map { it.targetId.substringAfter('#') }.toSet(),
            done = override?.takeIf { it.first == id }?.second ?: done,
            find = extra[0] as FindState,
            downloading = extra[1] as Boolean,
            readAloud = (extra[3] as ReadAloudState).takeIf { it.docKey == "$packId/$id" } ?: ReadAloudState(rate = settings.speechRate),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    private fun load(pack: SubjectPack, id: String): LoadedDoc? {
        val (doc, group) = ReaderDocs.find(pack, id) ?: return null
        val blocks = MarkdownParser.parse(doc.markdown)
        val outline = blocks.mapIndexedNotNull { i, b ->
            (b as? Block.Heading)?.takeIf { it.level in 1..3 }?.let { OutlineEntry(i, it.level, it.plainText, it.anchor) }
        }
        val ask = if (doc.unit != null) AskYourself.place(blocks, pack.questions.filter { it.unit == doc.unit }) else emptyMap()
        return LoadedDoc(doc, blocks, group, outline, ask)
    }

    fun openDoc(id: String) {
        stopReadingIfOtherDoc()
        startBlock = -1
        find.value = FindState()
        docId.value = id
    }

    // --- progress ---------------------------------------------------------------------------

    private var saveJob: Job? = null

    /** Called while scrolling; saved shortly after the student stops. */
    fun onScrolled(blockIndex: Int, offset: Int, fraction: Float) {
        val ready = uiState.value.content as? ReaderContent.Ready ?: return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            save(ready, blockIndex, offset, fraction, uiState.value.done)
        }
    }

    /** Called once when the student reaches the end of the document. */
    fun onReachedEnd() {
        val state = uiState.value
        val ready = state.content as? ReaderContent.Ready ?: return
        if (state.done) return
        setDone(true)
        _events.trySend(ReaderEvent.MarkedDone)
        viewModelScope.launch { save(ready, ready.loaded.blocks.lastIndex, 0, 1f, true) }
    }

    fun setDone(done: Boolean) {
        val ready = uiState.value.content as? ReaderContent.Ready ?: return
        doneOverride.value = docId.value to done
        viewModelScope.launch {
            val current = progress.get(packId, docId.value)
            save(ready, current?.blockIndex ?: 0, current?.blockOffset ?: 0, current?.fraction ?: 0f, done)
        }
    }

    private suspend fun save(ready: ReaderContent.Ready, blockIndex: Int, offset: Int, fraction: Float, done: Boolean) {
        progress.upsert(
            ReadingProgressEntity(
                packId = packId,
                docId = ready.loaded.doc.id,
                blockIndex = blockIndex,
                blockOffset = offset,
                fraction = fraction.coerceIn(0f, 1f),
                done = done,
                updatedAt = System.currentTimeMillis(),
                title = ready.loaded.doc.title,
                subjectTitle = ready.pack.title,
            ),
        )
    }

    // --- settings -------------------------------------------------------------------------

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch { preferences.updateReader(transform) }
    }

    // --- bookmarks ------------------------------------------------------------------------

    fun toggleDocBookmark() {
        val ready = uiState.value.content as? ReaderContent.Ready ?: return
        val id = ready.loaded.doc.id
        viewModelScope.launch {
            val removed = bookmarkDao.delete(packId, "doc", id) > 0
            if (!removed) {
                bookmarkDao.insert(BookmarkEntity(0, packId, "doc", id, id, null, ready.loaded.doc.title, ready.pack.title, System.currentTimeMillis()))
            }
            _events.send(ReaderEvent.BookmarkChanged(added = !removed))
        }
    }

    fun toggleSectionBookmark(entry: OutlineEntry) {
        val ready = uiState.value.content as? ReaderContent.Ready ?: return
        val target = "${ready.loaded.doc.id}#${entry.anchor}"
        viewModelScope.launch {
            val removed = bookmarkDao.delete(packId, "section", target) > 0
            if (!removed) {
                bookmarkDao.insert(
                    BookmarkEntity(0, packId, "section", target, ready.loaded.doc.id, entry.blockIndex, entry.title, ready.pack.title, System.currentTimeMillis()),
                )
            }
            _events.send(ReaderEvent.BookmarkChanged(added = !removed))
        }
    }

    // --- highlights -----------------------------------------------------------------------

    fun addHighlight(blockIndex: Int, start: Int, end: Int, colorIndex: Int, note: String?) {
        val ready = uiState.value.content as? ReaderContent.Ready ?: return
        val text = ready.loaded.blocks.getOrNull(blockIndex)?.plainText ?: return
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        if (e <= s) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            highlightDao.insert(
                HighlightEntity(0, packId, ready.loaded.doc.id, blockIndex, s, e, colorIndex, text.substring(s, e), note?.takeIf { it.isNotBlank() }, ready.loaded.doc.title, ready.pack.title, now, now),
            )
        }
    }

    fun updateHighlight(id: Long, colorIndex: Int, note: String?) {
        viewModelScope.launch { highlightDao.update(id, note?.takeIf { it.isNotBlank() }, colorIndex, System.currentTimeMillis()) }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            val h = highlightDao.get(id) ?: return@launch
            highlightDao.delete(id)
            _events.send(ReaderEvent.HighlightDeleted(h))
        }
    }

    fun restoreHighlight(h: HighlightEntity) {
        viewModelScope.launch { highlightDao.restore(h) }
    }

    // --- find in note ---------------------------------------------------------------------

    fun openFind(open: Boolean) {
        find.value = if (open) find.value.copy(open = true) else FindState()
    }

    fun findQuery(query: String) {
        val ready = uiState.value.content as? ReaderContent.Ready
        val matches = if (ready == null || query.isBlank()) emptyList() else buildList {
            val q = query.trim()
            ready.loaded.blocks.forEachIndexed { i, b ->
                var from = 0
                val text = b.plainText
                while (true) {
                    val at = text.indexOf(q, from, ignoreCase = true)
                    if (at < 0) break
                    add(SearchMatch(i, at, at + q.length))
                    from = at + q.length
                }
            }
        }
        find.update { it.copy(query = query, matches = matches, current = 0) }
    }

    fun findStep(by: Int) {
        find.update { s -> if (s.matches.isEmpty()) s else s.copy(current = (s.current + by + s.matches.size) % s.matches.size) }
    }

    // --- downloads ------------------------------------------------------------------------

    fun download() {
        if (downloading.value) return
        viewModelScope.launch {
            downloading.value = true
            val result = content.downloadPack(packId)
            downloading.value = false
            when (result) {
                is SyncResult.Failed -> _events.send(ReaderEvent.DownloadFailed(result.failure))
                is SyncResult.Updated -> _events.send(ReaderEvent.Downloaded)
                else -> Unit
            }
        }
    }

    fun mediaFile(name: String) = content.mediaFile(packId, name)

    fun downloadImages() {
        viewModelScope.launch {
            val result = content.downloadMedia(packId)
            if (result is SyncResult.Failed) _events.send(ReaderEvent.DownloadFailed(result.failure))
        }
    }

    // --- read aloud -----------------------------------------------------------------------

    fun readAloud(fromBlock: Int = 0) {
        val ready = uiState.value.content as? ReaderContent.Ready ?: return
        val segments = ready.loaded.blocks.flatMapIndexed { i, b ->
            when (b) {
                is Block.Code, is Block.Image, Block.Rule -> emptyList()
                is Block.Table -> listOf(SpeechSegment(i, 0, 0, b.plainText.replace(" | ", ", ")))
                else -> ReadAloudController.sentences(i, b.plainText)
            }
        }
        val start = segments.indexOfFirst { it.blockIndex >= fromBlock }.coerceAtLeast(0)
        readAloud.setRate(uiState.value.settings.speechRate)
        readAloud.start("$packId/${ready.loaded.doc.id}", ready.loaded.doc.title, segments, start)
        viewModelScope.launch {
            delay(1500)
            if (readAloud.state.value.status == ReadAloudState.Status.Unavailable) _events.send(ReaderEvent.ReadAloudUnavailable)
        }
    }

    fun pauseReading() = readAloud.pause()
    fun resumeReading() = readAloud.resume()
    fun stopReading() = readAloud.stop()
    fun skipSentence(by: Int) = readAloud.skip(by)
    fun setSpeechRate(rate: Float) {
        readAloud.setRate(rate)
        updateSettings { it.copy(speechRate = rate) }
    }

    private fun stopReadingIfOtherDoc() {
        if (readAloud.state.value.docKey?.startsWith("$packId/") == true) readAloud.stop()
    }
}

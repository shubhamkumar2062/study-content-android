package app.studydesk.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.newerPack
import app.studydesk.core.database.BookmarkDao
import app.studydesk.core.database.BookmarkEntity
import app.studydesk.core.database.HighlightDao
import app.studydesk.core.database.HighlightEntity
import app.studydesk.core.database.PracticeDao
import app.studydesk.core.database.ProgressDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadRow(val packId: String, val title: String, val bytes: Long, val pinned: Boolean, val updateAvailable: Boolean)

data class ProgressRow(val packId: String, val title: String, val unitsDone: Int, val units: Int, val mcqAccuracy: Int?, val mocks: Int)

data class LibraryUiState(
    val loading: Boolean = true,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    val downloads: List<DownloadRow> = emptyList(),
    val downloadedBytes: Long = 0,
    val progress: List<ProgressRow> = emptyList(),
)

sealed interface LibraryEvent {
    data class BookmarkRemoved(val bookmark: BookmarkEntity) : LibraryEvent
    data class HighlightRemoved(val highlight: HighlightEntity) : LibraryEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val content: ContentRepository,
    private val bookmarks: BookmarkDao,
    private val highlights: HighlightDao,
    progress: ProgressDao,
    practice: PracticeDao,
) : ViewModel() {
    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val downloads = combine(content.localState, content.catalog) { state, catalog ->
        val bytes = content.downloadedBytes()
        val rows = state?.active?.packs.orEmpty().map { p ->
            DownloadRow(
                packId = p.id,
                title = catalog?.subject(p.id)?.subject?.let { "${it.code} · ${it.title}" } ?: p.id,
                bytes = p.bytes + p.media.sumOf { it.bytes },
                pinned = p.id in state?.pinned.orEmpty(),
                updateAvailable = state?.newerPack(p.id) != null,
            )
        }
        rows to bytes
    }

    private val progressRows = combine(progress.all(), practice.accuracyByPack(), practice.mockCounts(), content.localState) { rows, acc, mocks, _ ->
        val packs = content.loadAllPacks().associateBy { it.subjectId }
        val byPack = rows.groupBy { it.packId }
        (byPack.keys + acc.map { it.packId } + mocks.map { it.packId }).distinct().mapNotNull { id ->
            val pack = packs[id] ?: return@mapNotNull null
            val a = acc.firstOrNull { it.packId == id }
            ProgressRow(
                packId = id,
                title = "${pack.code} · ${pack.title}",
                unitsDone = byPack[id].orEmpty().count { it.done && it.docId.startsWith("unit-") },
                units = pack.units.size,
                mcqAccuracy = a?.takeIf { it.attempts > 0 }?.let { it.correct * 100 / it.attempts },
                mocks = mocks.firstOrNull { it.packId == id }?.count ?: 0,
            )
        }
    }

    val uiState: StateFlow<LibraryUiState> = combine(bookmarks.all(), highlights.all(), downloads, progressRows) { b, h, (d, bytes), p ->
        LibraryUiState(false, b, h, d, bytes, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun removeBookmark(b: BookmarkEntity) {
        viewModelScope.launch {
            bookmarks.delete(b.packId, b.kind, b.targetId)
            _events.send(LibraryEvent.BookmarkRemoved(b))
        }
    }

    fun restoreBookmark(b: BookmarkEntity) {
        viewModelScope.launch { bookmarks.insert(b.copy(id = 0)) }
    }

    fun removeHighlight(h: HighlightEntity) {
        viewModelScope.launch {
            highlights.delete(h.id)
            _events.send(LibraryEvent.HighlightRemoved(h))
        }
    }

    fun restoreHighlight(h: HighlightEntity) {
        viewModelScope.launch { highlights.restore(h) }
    }

    fun stopUpdating(packId: String) {
        viewModelScope.launch { content.removeDownload(packId) }
    }

}

package app.studydesk.feature.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.content.PackResult
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.content.SyncResult
import app.studydesk.core.database.BookmarkDao
import app.studydesk.core.database.BookmarkEntity
import app.studydesk.core.database.CardDao
import app.studydesk.core.database.CardStateEntity
import app.studydesk.core.database.MockResultEntity
import app.studydesk.core.database.PracticeDao
import app.studydesk.core.database.ProgressDao
import app.studydesk.core.database.ReadingProgressEntity
import app.studydesk.core.database.UnitAccuracy
import app.studydesk.core.model.SubjectPack
import app.studydesk.core.model.SubjectRef
import app.studydesk.feature.subject.navigation.SubjectRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionFilter(
    val unit: Int? = null,
    val type: String? = null,
    val probability: String? = null,
    val savedOnly: Boolean = false,
)

sealed interface SubjectContent {
    data object Loading : SubjectContent
    data class Ready(val pack: SubjectPack, val newerAvailable: Boolean, val pinned: Boolean, val imagesMissing: Boolean) : SubjectContent {
        /** Everything is on the phone and current: no download to offer. */
        val complete: Boolean get() = !newerAvailable && !imagesMissing
    }
    data class NotDownloaded(val bytes: Long) : SubjectContent
    /** Listed in the catalog but no notes yet (or not reviewed, in a release build). */
    data object ComingSoon : SubjectContent
    data object Unreadable : SubjectContent
}

data class SubjectUiState(
    val ref: SubjectRef? = null,
    val content: SubjectContent = SubjectContent.Loading,
    val progress: Map<String, ReadingProgressEntity> = emptyMap(),
    val savedQuestions: Set<String> = emptySet(),
    val revising: Set<String> = emptySet(),
    val accuracy: List<UnitAccuracy> = emptyList(),
    val wrongMcqs: Int = 0,
    val results: List<MockResultEntity> = emptyList(),
    val filter: QuestionFilter = QuestionFilter(),
    val downloading: Boolean = false,
)

sealed interface SubjectEvent {
    data class Failed(val failure: SyncFailure) : SubjectEvent
    data object Downloaded : SubjectEvent
}

@HiltViewModel
class SubjectViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val visibility: ContentVisibility,
    progress: ProgressDao,
    private val bookmarks: BookmarkDao,
    private val cards: CardDao,
    practice: PracticeDao,
) : ViewModel() {
    private val route = savedState.toRoute<SubjectRoute>()
    val subjectId = route.subjectId
    val initialTab = route.tab

    private val filter = MutableStateFlow(QuestionFilter())
    private val downloading = MutableStateFlow(false)
    private val _events = Channel<SubjectEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val main = combine(content.catalog, content.pack(subjectId), content.localState) { catalog, pack, local ->
        val ref = catalog?.subject(subjectId)
        val c = when (pack) {
            is PackResult.Ready ->
                if (visibility.canOpen(pack.pack.reviewed)) {
                    SubjectContent.Ready(
                        pack.pack,
                        pack.newerVersion != null,
                        subjectId in local?.pinned.orEmpty(),
                        pack.pack.media.any { content.mediaFile(subjectId, it) == null },
                    )
                }
                else SubjectContent.ComingSoon
            is PackResult.NotDownloaded -> if (visibility.canOpen(pack.entry.reviewed)) SubjectContent.NotDownloaded(pack.entry.bytes) else SubjectContent.ComingSoon
            PackResult.NotFound -> SubjectContent.ComingSoon
            PackResult.Unreadable -> SubjectContent.Unreadable
        }
        ref to c
    }

    private val user = combine(
        progress.forPack(subjectId),
        bookmarks.forPack(subjectId),
        cards.forPack(subjectId),
        practice.accuracyByUnit(subjectId),
        combine(practice.wrongMcqIds(subjectId), practice.results(subjectId)) { a, b -> a to b },
    ) { p, b, c, acc, (wrong, results) ->
        SubjectUiState(
            progress = p.associateBy { it.docId },
            savedQuestions = b.filter { it.kind == "question" }.map { it.targetId }.toSet(),
            revising = c.filter { it.box <= 2 }.map { it.questionId }.toSet(),
            accuracy = acc,
            wrongMcqs = wrong.size,
            results = results,
        )
    }

    val uiState: StateFlow<SubjectUiState> = combine(main, user, filter, downloading) { (ref, c), u, f, d ->
        u.copy(ref = ref, content = c, filter = f, downloading = d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectUiState())

    fun setFilter(transform: (QuestionFilter) -> QuestionFilter) {
        filter.value = transform(filter.value)
    }

    fun toggleSaved(pack: SubjectPack, questionId: String) {
        viewModelScope.launch {
            if (bookmarks.delete(subjectId, "question", questionId) == 0) {
                val q = pack.questions.firstOrNull { it.id == questionId } ?: return@launch
                bookmarks.insert(BookmarkEntity(0, subjectId, "question", questionId, null, null, q.question, pack.title, System.currentTimeMillis()))
            }
        }
    }

    /** "Revise again": the question becomes a flashcard due now; tapping again removes it. */
    fun toggleRevise(questionId: String) {
        viewModelScope.launch {
            if (questionId in uiState.value.revising) {
                cards.delete(subjectId, questionId)
            } else {
                val now = System.currentTimeMillis()
                cards.upsert(CardStateEntity(subjectId, questionId, box = 1, dueAt = now, reviewedAt = now))
            }
        }
    }

    fun download() {
        if (downloading.value) return
        viewModelScope.launch {
            downloading.value = true
            val text = content.downloadPack(subjectId)
            val images = if (text !is SyncResult.Failed) content.downloadMedia(subjectId) else text
            downloading.value = false
            when {
                text is SyncResult.Failed -> _events.send(SubjectEvent.Failed(text.failure))
                images is SyncResult.Failed -> _events.send(SubjectEvent.Failed(images.failure))
                else -> _events.send(SubjectEvent.Downloaded)
            }
        }
    }
}

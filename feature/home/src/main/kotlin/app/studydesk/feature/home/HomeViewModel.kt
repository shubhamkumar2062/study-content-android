package app.studydesk.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.content.SyncResult
import app.studydesk.core.content.SyncStatus
import app.studydesk.core.database.CardDao
import app.studydesk.core.database.ProgressDao
import app.studydesk.core.database.ReadingProgressEntity
import app.studydesk.core.database.UserPreferences
import app.studydesk.core.model.Catalog
import app.studydesk.core.model.ExamPaper
import app.studydesk.core.model.ExamSchedule
import app.studydesk.core.model.ExamSession
import app.studydesk.core.model.Subject
import app.studydesk.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class UpcomingPaper(val paper: ExamPaper, val session: ExamSession, val daysLeft: Long)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NoCourse : HomeUiState
    data class Ready(
        val courseLabel: String,
        val subjects: List<Subject>,
        val upcoming: List<UpcomingPaper>,
        val recent: List<ReadingProgressEntity>,
        val dueCards: Int,
        val quizSubject: Subject?,
        val sync: SyncStatus,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferences: UserPreferences,
    private val content: ContentRepository,
    progress: ProgressDao,
    cards: CardDao,
    val visibility: ContentVisibility,
) : ViewModel() {
    val refreshing = MutableStateFlow(false)
    private var lastPull = 0L

    val uiState: StateFlow<HomeUiState> = combine(
        preferences.settings,
        content.catalog,
        content.exams,
        content.syncStatus,
        combine(progress.recent(4), cards.dueCount(System.currentTimeMillis())) { a, b -> a to b },
    ) { settings, catalog, exams, sync, (recent, due) ->
        build(settings, catalog, exams, sync, recent, due)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Pull to refresh: asks the server, at most once a minute. */
    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastPull < 60_000) return
        lastPull = now
        viewModelScope.launch {
            refreshing.value = true
            try {
                content.sync(force = true)
            } finally {
                refreshing.value = false
            }
        }
    }

    private fun build(
        settings: UserSettings,
        catalog: Catalog?,
        exams: List<ExamSchedule>,
        sync: SyncStatus,
        recent: List<ReadingProgressEntity>,
        due: Int,
    ): HomeUiState {
        val course = settings.course ?: return HomeUiState.NoCourse
        if (catalog == null) return HomeUiState.Loading
        val programme = catalog.programme(course.streamId, course.programmeId)
        val term = catalog.term(course) ?: return HomeUiState.NoCourse
        val subjects = term.subjects.filterNot { it.isPractical }
            .sortedByDescending { visibility.canOpen(it) }
        val today = LocalDate.now()
        val upcoming = exams.flatMap { schedule ->
            schedule.sessions.flatMap { session ->
                session.papers
                    .filter { it.programmeId == course.programmeId && it.schemeId == course.schemeId && it.termId == course.termId }
                    .mapNotNull { paper ->
                        val date = runCatching { LocalDate.parse(paper.date) }.getOrNull() ?: return@mapNotNull null
                        val days = ChronoUnit.DAYS.between(today, date)
                        if (days < 0) null else UpcomingPaper(paper, session, days)
                    }
            }
        }.sortedWith(compareBy({ it.paper.date }, { it.paper.start })).take(4)
        return HomeUiState.Ready(
            courseLabel = listOfNotNull(programme?.name, term.name).joinToString(" · "),
            subjects = subjects,
            upcoming = upcoming,
            recent = recent,
            dueCards = due,
            quizSubject = subjects.firstOrNull { visibility.canOpen(it) },
            sync = sync,
        )
    }
}

/** True when the last update attempt failed because the phone has no connection. */
internal val SyncStatus.offline: Boolean
    get() = (lastResult as? SyncResult.Failed)?.failure == SyncFailure.Offline

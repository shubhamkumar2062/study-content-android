package app.studydesk.feature.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.database.UserPreferences
import app.studydesk.core.model.CourseSelection
import app.studydesk.core.model.ExamBody
import app.studydesk.core.model.ExamPaper
import app.studydesk.core.model.ExamSchedule
import app.studydesk.core.model.ExamSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class PaperEntry(val paper: ExamPaper, val session: ExamSession, val body: ExamBody?, val date: LocalDate)

sealed interface ExamsUiState {
    data object Loading : ExamsUiState
    data class Ready(
        val course: CourseSelection?,
        val mine: List<PaperEntry>,
        val schedules: List<Pair<ExamBody?, ExamSchedule>>,
        val showPast: Boolean,
        val today: LocalDate,
    ) : ExamsUiState
}

@HiltViewModel
class ExamsViewModel @Inject constructor(content: ContentRepository, preferences: UserPreferences) : ViewModel() {
    val showPast = MutableStateFlow(false)

    val uiState: StateFlow<ExamsUiState> = combine(content.exams, content.catalog, preferences.settings, showPast) { exams, catalog, settings, past ->
        val today = LocalDate.now()
        val course = settings.course
        val mine = if (course == null) emptyList() else exams.flatMap { schedule ->
            val body = catalog?.examBody(schedule.bodyId)
            schedule.sessions.flatMap { session ->
                session.papers
                    .filter { it.programmeId == course.programmeId && it.schemeId == course.schemeId && it.termId == course.termId }
                    .mapNotNull { p -> runCatching { LocalDate.parse(p.date) }.getOrNull()?.let { PaperEntry(p, session, body, it) } }
            }
        }.filter { past || !it.date.isBefore(today) }.sortedWith(compareBy({ it.date }, { it.paper.start }))
        ExamsUiState.Ready(course, mine, exams.map { catalog?.examBody(it.bodyId) to it }, past, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExamsUiState.Loading)
}

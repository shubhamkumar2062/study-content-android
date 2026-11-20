package app.studydesk.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.PackResult
import app.studydesk.core.database.MockResultEntity
import app.studydesk.core.database.PracticeDao
import app.studydesk.feature.practice.navigation.MockTestRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class MockPhase { Loading, Missing, Answering, Marking }

data class MockUiState(
    val phase: MockPhase = MockPhase.Loading,
    val subjectTitle: String = "",
    val paper: Paper? = null,
    val section: Int = 0,
    val remainingSeconds: Int = 0,
    val paused: Boolean = false,
    val timeUp: Boolean = false,
) {
    val unansweredMcqs: Int get() = paper?.sections?.filter { it.isMcq }?.sumOf { s -> s.items.count { it.chosen == null } } ?: 0
}

@HiltViewModel
class MockTestViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val practice: PracticeDao,
) : ViewModel() {
    private val route = savedState.toRoute<MockTestRoute>()
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(MockUiState())
    val state: StateFlow<MockUiState> = _state.asStateFlow()
    private val _finished = Channel<Long>(Channel.BUFFERED)
    val finished = _finished.receiveAsFlow()
    private var ticker: Job? = null
    private var startedAt = savedState.get<Long>("startedAt") ?: System.currentTimeMillis().also { savedState["startedAt"] = it }

    init {
        viewModelScope.launch {
            val pack = (content.pack(route.packId).first() as? PackResult.Ready)?.pack
            val test = pack?.mockTests?.firstOrNull { it.id == route.testId }
            if (pack == null || test == null) {
                _state.value = MockUiState(phase = MockPhase.Missing)
                return@launch
            }
            val saved = savedState.get<String>("paper")?.let { runCatching { json.decodeFromString<Paper>(it) }.getOrNull() }
            val seed = savedState.get<Long>("seed") ?: System.nanoTime().also { savedState["seed"] = it }
            val paper = saved ?: PaperBuilder.build(pack, test, seed)
            val phase = savedState.get<String>("phase")?.let { MockPhase.valueOf(it) } ?: MockPhase.Answering
            _state.value = MockUiState(
                phase = phase,
                subjectTitle = pack.title,
                paper = paper,
                remainingSeconds = savedState.get<Int>("remaining") ?: (test.durationMinutes * 60),
                paused = savedState.get<Boolean>("paused") ?: false,
            )
            if (phase == MockPhase.Answering) startTimer()
        }
    }

    private fun startTimer() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (s.phase != MockPhase.Answering) break
                if (s.paused) continue
                val left = s.remainingSeconds - 1
                _state.update { it.copy(remainingSeconds = left) }
                savedState["remaining"] = left
                if (left <= 0) {
                    _state.update { it.copy(timeUp = true) }
                    finishAnswering()
                    break
                }
            }
        }
    }

    fun setPaused(paused: Boolean) {
        _state.update { it.copy(paused = paused) }
        savedState["paused"] = paused
    }

    fun selectSection(index: Int) = _state.update { it.copy(section = index) }

    fun choose(sectionId: String, itemId: String, option: Int) = editItem(sectionId, itemId) { it.copy(chosen = option) }

    fun toggleFlag(sectionId: String, itemId: String) = editItem(sectionId, itemId) { it.copy(flagged = !it.flagged) }

    fun setSelfMarks(sectionId: String, itemId: String, marks: Int) = editItem(sectionId, itemId) { it.copy(selfMarks = marks) }

    private fun editItem(sectionId: String, itemId: String, change: (PaperItem) -> PaperItem) {
        _state.update { s ->
            val paper = s.paper ?: return@update s
            val updated = paper.copy(
                sections = paper.sections.map { sec ->
                    if (sec.id != sectionId) sec else sec.copy(items = sec.items.map { if (it.id == itemId) change(it) else it })
                },
            )
            savedState["paper"] = json.encodeToString(Paper.serializer(), updated)
            s.copy(paper = updated)
        }
    }

    /** Stops the clock; MCQs are now fixed and the student marks the written answers. */
    fun finishAnswering() {
        ticker?.cancel()
        _state.update { it.copy(phase = MockPhase.Marking, paused = false) }
        savedState["phase"] = MockPhase.Marking.name
        // If there are no written sections, there is nothing to mark.
        if (_state.value.paper?.sections?.all { it.isMcq } == true) save()
    }

    fun save() {
        val paper = _state.value.paper ?: return
        viewModelScope.launch {
            val id = practice.insertResult(
                MockResultEntity(
                    packId = route.packId,
                    testId = paper.testId,
                    testTitle = paper.title,
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    autoMarks = paper.autoMarks,
                    autoMax = paper.autoMax,
                    selfMarks = paper.selfMarks,
                    selfMax = paper.selfMax,
                    totalMarks = paper.totalMarks,
                    detailJson = json.encodeToString(Paper.serializer(), paper),
                ),
            )
            _finished.send(id)
        }
    }
}

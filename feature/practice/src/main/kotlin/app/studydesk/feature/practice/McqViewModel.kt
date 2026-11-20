package app.studydesk.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.PackResult
import app.studydesk.core.database.McqAttemptEntity
import app.studydesk.core.database.PracticeDao
import app.studydesk.core.model.Mcq
import app.studydesk.feature.practice.navigation.McqRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class McqUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val subjectTitle: String = "",
    val questions: List<Mcq> = emptyList(),
    val index: Int = 0,
    /** Answer chosen per question id. */
    val answers: Map<String, Int> = emptyMap(),
    val finished: Boolean = false,
) {
    val current: Mcq? get() = questions.getOrNull(index)
    val score: Int get() = questions.count { answers[it.id] == it.correct }
    val wrong: List<Mcq> get() = questions.filter { it.id in answers && answers[it.id] != it.correct }
}

@HiltViewModel
class McqViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val practice: PracticeDao,
) : ViewModel() {
    val route = savedState.toRoute<McqRoute>()
    private val _state = MutableStateFlow(McqUiState())
    val state: StateFlow<McqUiState> = _state.asStateFlow()

    // The same order survives rotation and process death.
    private val seed: Long = savedState.get<Long>("seed") ?: System.nanoTime().also { savedState["seed"] = it }

    init {
        viewModelScope.launch {
            val pack = (content.pack(route.packId).first() as? PackResult.Ready)?.pack
            if (pack == null) {
                _state.value = McqUiState(loading = false, missing = true)
                return@launch
            }
            val wrong = if (route.wrongOnly) practice.wrongMcqIds(route.packId).first().toSet() else null
            val questions = McqPicker.pick(pack.mcqs, route.unit, wrong, route.quick, seed)
            val index = savedState.get<Int>("index") ?: 0
            @Suppress("UNCHECKED_CAST")
            val answers = (savedState.get<HashMap<String, Int>>("answers") ?: HashMap())
            _state.value = McqUiState(false, questions.isEmpty(), pack.title, questions, index.coerceAtMost((questions.size - 1).coerceAtLeast(0)), answers)
        }
    }

    fun choose(option: Int) {
        val s = _state.value
        val q = s.current ?: return
        if (q.id in s.answers) return
        val answers = s.answers + (q.id to option)
        _state.update { it.copy(answers = answers) }
        savedState["answers"] = HashMap(answers)
        viewModelScope.launch {
            practice.insertAttempt(McqAttemptEntity(0, route.packId, q.id, q.unit, option, option == q.correct, System.currentTimeMillis()))
        }
    }

    fun next() {
        _state.update {
            if (it.index >= it.questions.lastIndex) it.copy(finished = true)
            else it.copy(index = it.index + 1).also { s -> savedState["index"] = s.index }
        }
    }
}

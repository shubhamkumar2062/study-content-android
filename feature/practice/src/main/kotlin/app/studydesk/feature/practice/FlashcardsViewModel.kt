package app.studydesk.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.PackResult
import app.studydesk.core.database.CardDao
import app.studydesk.core.database.CardStateEntity
import app.studydesk.core.model.Question
import app.studydesk.feature.practice.navigation.FlashcardsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Flashcard(val packId: String, val subjectTitle: String, val question: Question)

data class FlashcardsUiState(
    val loading: Boolean = true,
    val queue: List<Flashcard> = emptyList(),
    val known: Int = 0,
    val again: Int = 0,
    val started: Boolean = false,
) {
    val current: Flashcard? get() = queue.firstOrNull()
    val finished: Boolean get() = started && queue.isEmpty()
}

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val cards: CardDao,
) : ViewModel() {
    val route = savedState.toRoute<FlashcardsRoute>()
    private val _state = MutableStateFlow(FlashcardsUiState())
    val state: StateFlow<FlashcardsUiState> = _state.asStateFlow()
    private val states = mutableMapOf<Pair<String, String>, CardStateEntity>()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val deck = if (route.packId != null) {
                val pack = (content.pack(route.packId).first() as? PackResult.Ready)?.pack
                val saved = cards.forPack(route.packId).first().associateBy { it.questionId }
                saved.values.forEach { states[it.packId to it.questionId] = it }
                pack?.questions.orEmpty()
                    .filter { q -> saved[q.id]?.let { it.dueAt <= now } ?: true }
                    // Cards marked "revise again" first, then new ones, high-probability first.
                    .sortedWith(compareBy({ saved[it.id]?.box ?: 9 }, { it.rating.rank }))
                    .map { Flashcard(route.packId, pack!!.title, it) }
            } else {
                val due = cards.due(now)
                due.forEach { states[it.packId to it.questionId] = it }
                due.groupBy { it.packId }.flatMap { (packId, list) ->
                    val pack = (content.pack(packId).first() as? PackResult.Ready)?.pack ?: return@flatMap emptyList()
                    list.mapNotNull { s -> pack.questions.firstOrNull { it.id == s.questionId }?.let { Flashcard(packId, pack.title, it) } }
                }
            }
            _state.value = FlashcardsUiState(loading = false, queue = deck, started = deck.isNotEmpty())
        }
    }

    fun answer(knew: Boolean) {
        val card = _state.value.current ?: return
        val key = card.packId to card.question.id
        val next = Leitner.next(card.packId, card.question.id, states[key], knew, System.currentTimeMillis())
        states[key] = next
        viewModelScope.launch { cards.upsert(next) }
        _state.update {
            val rest = it.queue.drop(1)
            // "Revise again" sends the card to the back of today's pile.
            if (knew) it.copy(queue = rest, known = it.known + 1) else it.copy(queue = rest + card, again = it.again + 1)
        }
    }
}

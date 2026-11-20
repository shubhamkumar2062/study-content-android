package app.studydesk.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.model.Catalog
import app.studydesk.core.model.SubjectRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BrowseUiState {
    data object Loading : BrowseUiState
    data object Unreadable : BrowseUiState
    data class Ready(val catalog: Catalog, val query: String, val matches: List<SubjectRef>) : BrowseUiState
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val content: ContentRepository,
    val visibility: ContentVisibility,
) : ViewModel() {
    val query = MutableStateFlow("")

    val uiState: StateFlow<BrowseUiState> = combine(content.catalog, query.debounce(150).onStart { emit("") }) { catalog, q ->
        if (catalog == null) {
            BrowseUiState.Unreadable
        } else {
            BrowseUiState.Ready(catalog, q, if (q.isBlank()) emptyList() else match(catalog, q))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState.Loading)

    fun retry() {
        viewModelScope.launch { content.sync(force = true) }
    }

    private fun match(catalog: Catalog, q: String): List<SubjectRef> {
        val words = q.trim().lowercase().split(Regex("\\s+"))
        return catalog.allSubjects
            .filter { ref ->
                val text = "${ref.subject.code} ${ref.subject.title} ${ref.programme.name}".lowercase()
                words.all { it in text }
            }
            .sortedWith(compareByDescending<SubjectRef> { visibility.canOpen(it.subject) }.thenBy { it.subject.code })
            .take(100)
    }
}

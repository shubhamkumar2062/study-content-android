package app.studydesk.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.database.SearchDao
import app.studydesk.core.database.SearchHit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Preparing : SearchUiState
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Results(val hits: List<SearchHit>) : SearchUiState
    data object NoResults : SearchUiState
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val indexer: SearchIndexer, private val dao: SearchDao) : ViewModel() {
    val query = MutableStateFlow("")
    private val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            runCatching { indexer.ensureIndexed() }
            ready.value = true
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(query.debounce(200), ready) { q, r -> q to r }
        .mapLatest { (q, r) ->
            val fts = SearchIndexer.query(q)
            when {
                fts == null -> if (r) SearchUiState.Idle else SearchUiState.Preparing
                !r -> SearchUiState.Preparing
                else -> {
                    val hits = runCatching { dao.search(fts) }.getOrDefault(emptyList())
                    if (hits.isEmpty()) SearchUiState.NoResults else SearchUiState.Results(hits)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Preparing)
}

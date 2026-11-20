package app.studydesk.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.database.UserPreferences
import app.studydesk.core.model.Catalog
import app.studydesk.core.model.CourseSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Ready(val catalog: Catalog) : CatalogUiState
    data object Unreadable : CatalogUiState
}

/** Shared by first launch and "Change course". */
@HiltViewModel
class CoursePickerViewModel @Inject constructor(
    private val content: ContentRepository,
    private val preferences: UserPreferences,
    val visibility: ContentVisibility,
) : ViewModel() {

    val catalog: StateFlow<CatalogUiState> = content.catalog
        .map { if (it == null) CatalogUiState.Unreadable else CatalogUiState.Ready(it) }
        .catch { emit(CatalogUiState.Unreadable) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState.Loading)

    fun save(course: CourseSelection, finishOnboarding: Boolean, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setCourse(course)
            if (finishOnboarding) preferences.setOnboarded()
            onSaved()
            // Fetch the new semester's subjects if the server has newer copies (no-op if checked recently).
            content.sync(force = false)
        }
    }

    fun retry() {
        viewModelScope.launch { content.sync(force = true) }
    }
}

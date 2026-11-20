package app.studydesk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.database.UserPreferences
import app.studydesk.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val settings: UserSettings) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferences.settings
        .map<UserSettings, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}

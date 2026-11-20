package app.studydesk.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.SyncResult
import app.studydesk.core.content.SyncStatus
import app.studydesk.core.database.UserPreferences
import app.studydesk.core.model.AppTheme
import app.studydesk.core.model.Catalog
import app.studydesk.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val courseLabel: String? = null,
    val sync: SyncStatus = SyncStatus(),
)

sealed interface SettingsEvent {
    data class Synced(val result: SyncResult) : SettingsEvent
    data object TooSoon : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val content: ContentRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(preferences.settings, content.catalog, content.syncStatus) { s, catalog, sync ->
        SettingsUiState(s, courseLabel(s, catalog), sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var lastManualCheck = 0L

    /** "Check now": always asks the server, but at most once a minute. */
    fun checkNow() {
        val now = System.currentTimeMillis()
        if (now - lastManualCheck < 60_000) {
            _events.trySend(SettingsEvent.TooSoon)
            return
        }
        lastManualCheck = now
        viewModelScope.launch { _events.send(SettingsEvent.Synced(content.sync(force = true))) }
    }

    fun setTheme(theme: AppTheme) = launch { preferences.setTheme(theme) }
    fun setDynamicColor(on: Boolean) = launch { preferences.setDynamicColor(on) }
    fun setImagesWifiOnly(on: Boolean) = launch { preferences.setImagesWifiOnly(on) }
    fun setTextScale(scale: Float) = launch { preferences.updateReader { it.copy(textScale = scale) } }
    fun setSerif(on: Boolean) = launch { preferences.updateReader { it.copy(serif = on) } }
    fun setKeepScreenOn(on: Boolean) = launch { preferences.updateReader { it.copy(keepScreenOn = on) } }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun courseLabel(s: UserSettings, catalog: Catalog?): String? {
        val course = s.course ?: return null
        val programme = catalog?.programme(course.streamId, course.programmeId) ?: return null
        val term = catalog.term(course) ?: return programme.name
        return "${programme.name} · ${term.name}"
    }
}

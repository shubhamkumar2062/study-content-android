package app.studydesk.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.studydesk.core.content.WantedPacks
import app.studydesk.core.model.AppTheme
import app.studydesk.core.model.CourseSelection
import app.studydesk.core.model.ReaderSettings
import app.studydesk.core.model.ReaderTheme
import app.studydesk.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** The student's choices, kept on the phone only. */
class UserPreferences(private val store: DataStore<Preferences>) : WantedPacks {

    val settings: Flow<UserSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    suspend fun setCourse(course: CourseSelection) = store.edit {
        it[STREAM] = course.streamId
        it[PROGRAMME] = course.programmeId
        it[SCHEME] = course.schemeId
        it[TERM] = course.termId
    }

    suspend fun setOnboarded() = store.edit { it[ONBOARDED] = true }
    suspend fun setTheme(theme: AppTheme) = store.edit { it[THEME] = theme.name }
    suspend fun setDynamicColor(on: Boolean) = store.edit { it[DYNAMIC] = on }
    suspend fun setImagesWifiOnly(on: Boolean) = store.edit { it[WIFI_IMAGES] = on }

    suspend fun updateReader(transform: (ReaderSettings) -> ReaderSettings) = store.edit { prefs ->
        val next = transform(prefs.toSettings().reader)
        prefs[TEXT_SCALE] = next.textScale.coerceIn(ReaderSettings.MIN_SCALE, ReaderSettings.MAX_SCALE)
        prefs[LINE_SPACING] = next.lineSpacing
        prefs[SERIF] = next.serif
        prefs[READER_THEME] = next.theme.name
        prefs[KEEP_SCREEN_ON] = next.keepScreenOn
        prefs[VOLUME_KEYS] = next.volumeKeysZoom
        prefs[SPEECH_RATE] = next.speechRate.coerceIn(0.5f, 2f)
    }

    override suspend fun select(available: Collection<String>): Set<String> {
        val prefix = settings.first().coursePrefix ?: return emptySet()
        return available.filter { it.startsWith(prefix) }.toSet()
    }

    private fun Preferences.toSettings(): UserSettings {
        val course = if (this[STREAM] != null && this[PROGRAMME] != null && this[SCHEME] != null && this[TERM] != null) {
            CourseSelection(this[STREAM]!!, this[PROGRAMME]!!, this[SCHEME]!!, this[TERM]!!)
        } else {
            null
        }
        return UserSettings(
            course = course,
            onboarded = this[ONBOARDED] ?: false,
            theme = enumOr(this[THEME], AppTheme.System),
            dynamicColor = this[DYNAMIC] ?: false,
            imagesWifiOnly = this[WIFI_IMAGES] ?: false,
            reader = ReaderSettings(
                textScale = this[TEXT_SCALE] ?: 1f,
                lineSpacing = this[LINE_SPACING] ?: 1.5f,
                serif = this[SERIF] ?: false,
                theme = enumOr(this[READER_THEME], ReaderTheme.Auto),
                keepScreenOn = this[KEEP_SCREEN_ON] ?: true,
                volumeKeysZoom = this[VOLUME_KEYS] ?: false,
                speechRate = this[SPEECH_RATE] ?: 1f,
            ),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        val STREAM = stringPreferencesKey("course_stream")
        val PROGRAMME = stringPreferencesKey("course_programme")
        val SCHEME = stringPreferencesKey("course_scheme")
        val TERM = stringPreferencesKey("course_term")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val WIFI_IMAGES = booleanPreferencesKey("images_wifi_only")
        val TEXT_SCALE = floatPreferencesKey("reader_text_scale")
        val LINE_SPACING = floatPreferencesKey("reader_line_spacing")
        val SERIF = booleanPreferencesKey("reader_serif")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val KEEP_SCREEN_ON = booleanPreferencesKey("reader_keep_screen_on")
        val VOLUME_KEYS = booleanPreferencesKey("reader_volume_keys_zoom")
        val SPEECH_RATE = floatPreferencesKey("reader_speech_rate")
    }
}

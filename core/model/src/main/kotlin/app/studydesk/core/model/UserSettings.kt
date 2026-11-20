package app.studydesk.core.model

enum class AppTheme { System, Light, Dark }

enum class ReaderTheme { Auto, Light, Sepia, Dark, Black }

data class ReaderSettings(
    /** 0.8 to 2.0; pinch-to-zoom in the reader changes it. */
    val textScale: Float = 1f,
    val lineSpacing: Float = 1.5f,
    val serif: Boolean = false,
    val theme: ReaderTheme = ReaderTheme.Auto,
    val keepScreenOn: Boolean = true,
    /** Volume keys change the text size in the reader (an accessibility option). */
    val volumeKeysZoom: Boolean = false,
    /** Read-aloud speed, 0.5 to 2.0. */
    val speechRate: Float = 1f,
) {
    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 2.0f
    }
}

data class UserSettings(
    val course: CourseSelection? = null,
    val onboarded: Boolean = false,
    val theme: AppTheme = AppTheme.System,
    val dynamicColor: Boolean = false,
    val imagesWifiOnly: Boolean = false,
    val reader: ReaderSettings = ReaderSettings(),
) {
    /** Pack ids for the student's own semester start with this. */
    val coursePrefix: String?
        get() = course?.let { "${it.streamId}.${it.programmeId}.${it.schemeId}.${it.termId}." }
}

package app.studydesk.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// StudyDesk brand palette: electric indigo for focus, teal as the calm second colour,
// warm amber for highlights. Used by default; wallpaper colours are an opt-in setting.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF4338CA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF111322),
    surface = Color(0xFFF7F8FC),
    onSurface = Color(0xFF111322),
    surfaceVariant = Color(0xFFE6E8F2),
    onSurfaceVariant = Color(0xFF4A4E63),
    outline = Color(0xFF7A7F96),
    outlineVariant = Color(0xFFD5D8E5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FA),
    surfaceContainer = Color(0xFFEBEEF7),
    surfaceContainerHigh = Color(0xFFE5E8F3),
    surfaceContainerHighest = Color(0xFFDEE2EF),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C0E1A),
    onBackground = Color(0xFFE6E8F5),
    surface = Color(0xFF0C0E1A),
    onSurface = Color(0xFFE6E8F5),
    surfaceVariant = Color(0xFF2A2E42),
    onSurfaceVariant = Color(0xFFC3C7DA),
    outline = Color(0xFF8B90A8),
    outlineVariant = Color(0xFF353A52),
    surfaceContainerLowest = Color(0xFF080A14),
    surfaceContainerLow = Color(0xFF131628),
    surfaceContainer = Color(0xFF171A2E),
    surfaceContainerHigh = Color(0xFF1F2338),
    surfaceContainerHighest = Color(0xFF2A2E44),
)

/** Colours that mean the same thing everywhere (probability, right/wrong). */
data class StudyColors(
    val high: Color,
    val onHigh: Color,
    val medium: Color,
    val onMedium: Color,
    val low: Color,
    val onLow: Color,
    val correct: Color,
    val correctContainer: Color,
    val wrong: Color,
    val wrongContainer: Color,
    val highlights: List<Color>,
)

internal val LightStudyColors = StudyColors(
    high = Color(0xFFFFDAD6), onHigh = Color(0xFF8C1D18),
    medium = Color(0xFFFFE0B2), onMedium = Color(0xFF6B3B00),
    low = Color(0xFFE3E7EC), onLow = Color(0xFF3B4752),
    correct = Color(0xFF1B6E3A), correctContainer = Color(0xFFC6F0CF),
    wrong = Color(0xFFB3261E), wrongContainer = Color(0xFFFFDAD6),
    highlights = listOf(Color(0x66FFE066), Color(0x6680E0A0), Color(0x6680C8FF), Color(0x66FF9FC8)),
)

internal val DarkStudyColors = StudyColors(
    high = Color(0xFF5C1A15), onHigh = Color(0xFFFFDAD6),
    medium = Color(0xFF4F2D00), onMedium = Color(0xFFFFDCBE),
    low = Color(0xFF2E353C), onLow = Color(0xFFD5DCE3),
    correct = Color(0xFF8FD9A4), correctContainer = Color(0xFF0E4A25),
    wrong = Color(0xFFFFB4AB), wrongContainer = Color(0xFF6B1612),
    highlights = listOf(Color(0x66B39A1F), Color(0x663E8F5C), Color(0x663C77A6), Color(0x66A6466F)),
)

package app.studydesk.core.designsystem.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector

/** A colour pair for a gradient, plus a solid colour for text and icons on light surfaces. */
data class Accent(val start: Color, val end: Color, val icon: ImageVector) {
    val solid: Color get() = start
    val gradient: Brush get() = Brush.linearGradient(listOf(start, end))
    /** Soft tint for backgrounds behind the accent (cards, icon circles). */
    fun tint(alpha: Float = 0.14f): Color = start.copy(alpha = alpha)
}

object Brand {
    /** Hero headers (Home, onboarding). Darker in dark mode so white text keeps its contrast. */
    val heroGradient: Brush
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF155E75)))
        } else {
            Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF0E7490)))
        }

    private val pharmacy = Accent(Color(0xFF0D9488), Color(0xFF059669), Icons.Rounded.Science)
    private val nursing = Accent(Color(0xFFE11D48), Color(0xFFF43F5E), Icons.Rounded.HealthAndSafety)
    private val education = Accent(Color(0xFFD97706), Color(0xFFEA580C), Icons.Rounded.School)
    private val degree = Accent(Color(0xFF7C3AED), Color(0xFF6366F1), Icons.Rounded.AutoStories)
    private val other = Accent(Color(0xFF4338CA), Color(0xFF0E7490), Icons.AutoMirrored.Rounded.MenuBook)

    /** Colour and icon of a stream, by its id in the catalog. */
    fun stream(streamId: String): Accent = when (streamId.substringBefore('.')) {
        "pharmacy" -> pharmacy
        "nursing" -> nursing
        "education" -> education
        "degree" -> degree
        else -> other
    }

    private val subjectHues = listOf(
        Color(0xFF0D9488) to Color(0xFF14B8A6),
        Color(0xFF4F46E5) to Color(0xFF6366F1),
        Color(0xFFE11D48) to Color(0xFFF43F5E),
        Color(0xFFD97706) to Color(0xFFF59E0B),
        Color(0xFF7C3AED) to Color(0xFF8B5CF6),
        Color(0xFF0284C7) to Color(0xFF0EA5E9),
        Color(0xFF059669) to Color(0xFF10B981),
        Color(0xFFEA580C) to Color(0xFFF97316),
    )

    /** A stable colour per subject (same subject, same colour, on every screen and phone). */
    fun subject(subjectId: String): Accent {
        val (a, b) = subjectHues[Math.floorMod(subjectId.hashCode(), subjectHues.size)]
        return Accent(a, b, stream(subjectId).icon)
    }
}

/** True in dark mode, whichever way it was chosen. */
val isDarkTheme: Boolean
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Tints a subject's screens with its colour: tabs, progress bars, labels and buttons follow it.
 * Text uses a darker shade in light mode and a lighter one in dark mode so it stays readable.
 */
@Composable
fun SubjectTheme(accent: Accent, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = base.background.luminance() < 0.5f
    val strong = if (dark) lerp(accent.start, Color.White, 0.35f) else lerp(accent.start, Color.Black, 0.22f)
    val container = lerp(base.surface, accent.start, if (dark) 0.28f else 0.14f)
    val scheme = base.copy(
        primary = strong,
        onPrimary = if (dark) Color(0xFF111322) else Color.White,
        primaryContainer = container,
        onPrimaryContainer = if (dark) lerp(accent.start, Color.White, 0.75f) else lerp(accent.start, Color.Black, 0.55f),
        surfaceTint = strong,
        // Progress-bar tracks and tonal chips: a softer tint of the same colour.
        secondaryContainer = lerp(base.surface, accent.start, if (dark) 0.22f else 0.10f),
        onSecondaryContainer = if (dark) lerp(accent.start, Color.White, 0.75f) else lerp(accent.start, Color.Black, 0.55f),
    )
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content)
}

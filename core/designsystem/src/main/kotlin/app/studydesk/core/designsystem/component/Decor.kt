package app.studydesk.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.studydesk.core.designsystem.theme.isDarkTheme

/** Two soft circles in a gradient header's corners, for depth. Purely decorative. */
@Composable
fun DecorativeCircles(modifier: Modifier = Modifier, alpha: Float = 0.12f) {
    Canvas(modifier) {
        val c = Color.White.copy(alpha = alpha)
        drawCircle(c, radius = size.height * 0.9f, center = Offset(size.width * 1.02f, -size.height * 0.15f))
        drawCircle(c.copy(alpha = alpha * 0.7f), radius = size.height * 0.55f, center = Offset(size.width * 0.78f, size.height * 1.05f))
    }
}

/**
 * Light status-bar icons while a screen with a dark header is shown; the theme's normal
 * icons come back when it leaves.
 */
@Composable
fun StatusBarIconsOnDark() {
    val view = LocalView.current
    val dark = isDarkTheme
    if (view.isInEditMode) return
    DisposableEffect(view, dark) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = !dark }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

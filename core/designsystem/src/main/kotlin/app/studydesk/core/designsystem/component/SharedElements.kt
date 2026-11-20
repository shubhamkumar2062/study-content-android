package app.studydesk.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import app.studydesk.core.designsystem.theme.LocalReduceMotion

/** Set by the app around its navigation so screens can share elements across a transition. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The animated scope of the current navigation destination. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Lets an element (a subject card, an image) grow into the next screen. A no-op when
 * the screen is shown without a transition or the phone's animations are off.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsIfAvailable(key: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    if (LocalReduceMotion.current) return this
    return with(shared) {
        this@sharedBoundsIfAvailable.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}

/** The shared-element key that links a subject card or row to the subject screen header. */
fun subjectSharedKey(subjectId: String) = "subject-$subjectId"

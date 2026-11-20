package app.studydesk.core.designsystem.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/** Material 3 motion tokens; everything snaps when the phone's animations are off. */
object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500

    @Composable
    @ReadOnlyComposable
    fun <T> tweenOrSnap(durationMillis: Int = MEDIUM): AnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else tween(durationMillis, easing = Emphasized)

    @Composable
    @ReadOnlyComposable
    fun <T> bouncy(): AnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
}

package app.studydesk.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.studydesk.core.designsystem.R
import app.studydesk.core.designsystem.theme.LocalReduceMotion

/** A moving light sweep over placeholder shapes; a still tint when animations are off. */
fun Modifier.shimmer(shape: Shape? = null): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val light = MaterialTheme.colorScheme.surfaceContainerLow
    val reduce = LocalReduceMotion.current
    val brush = if (reduce) {
        Brush.linearGradient(listOf(base, base))
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val x by transition.animateFloat(
            initialValue = -600f,
            targetValue = 1600f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmer-x",
        )
        Brush.linearGradient(listOf(base, light, base), start = Offset(x, 0f), end = Offset(x + 600f, 300f))
    }
    (if (shape != null) clip(shape) else this).background(brush)
}

@Composable
fun SkeletonLine(width: Dp? = null, height: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .shimmer(MaterialTheme.shapes.extraSmall),
    )
}

/** Placeholder rows shaped like a list of cards (icon, title, subtitle). */
@Composable
fun SkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.state_loading)
    Column(
        modifier.padding(16.dp).clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).shimmer(MaterialTheme.shapes.medium))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonLine(height = 16.dp, modifier = Modifier.fillMaxWidth(0.7f))
                    SkeletonLine(height = 12.dp, modifier = Modifier.fillMaxWidth(0.45f))
                }
            }
        }
    }
}

/** Placeholder shaped like a page of text (heading, paragraphs). */
@Composable
fun SkeletonDocument(modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.state_loading)
    Column(
        modifier.padding(20.dp).clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonLine(height = 28.dp, modifier = Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(8.dp))
        repeat(3) {
            repeat(4) { i -> SkeletonLine(modifier = Modifier.fillMaxWidth(if (i == 3) 0.55f else 1f)) }
            Spacer(Modifier.height(10.dp))
        }
        Box(Modifier.fillMaxWidth().height(140.dp).shimmer(MaterialTheme.shapes.medium))
    }
}

/** Placeholder for a horizontal row of cards. */
@Composable
fun SkeletonCards(count: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Column(Modifier.width(160.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(96.dp).shimmer(MaterialTheme.shapes.large))
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.8f))
            }
        }
    }
}

@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).shimmer(CircleShape))
}

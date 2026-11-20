package app.studydesk.core.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ZoomInMap
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.reader.markdown.Block
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

/**
 * Full-screen view of an image, table or text diagram: pinch to zoom up to 5×, drag to pan,
 * double-tap to zoom in or out, and swipe down (at normal size) or Back to close.
 */
@Composable
internal fun ZoomOverlay(
    block: Block,
    type: ReaderType,
    marks: TextMarks,
    mediaFile: (String) -> File?,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val dismissDrag = remember { Animatable(0f) }
    var container by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = maxOf(0f, (contentSize.width * s - container.width) / 2f)
        val maxY = maxOf(0f, (contentSize.height * s - container.height) / 2f)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    val background = MaterialTheme.colorScheme.scrim
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - (abs(dismissDrag.value) / 900f).coerceIn(0f, 0.6f) }
            .background(background.copy(alpha = 0.94f))
            .onSizeChanged { container = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        scope.launch {
                            if (scale.value > 1.05f) {
                                launch { scale.animateTo(1f) }
                                offset.animateTo(Offset.Zero)
                            } else {
                                val target = 2.5f
                                val center = Offset(container.width / 2f, container.height / 2f)
                                val wanted = clamp((center - tap) * (target - 1f), target)
                                if (reduce) {
                                    scale.snapTo(target)
                                    offset.snapTo(wanted)
                                } else {
                                    launch { scale.animateTo(target) }
                                    offset.animateTo(wanted)
                                }
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pinched = false
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (event.changes.size > 1 || zoom != 1f) pinched = true
                        if (pinched || scale.value > 1.01f) {
                            val s = (scale.value * zoom).coerceIn(1f, 5f)
                            scope.launch {
                                scale.snapTo(s)
                                offset.snapTo(clamp(offset.value + pan, s))
                            }
                        } else {
                            scope.launch { dismissDrag.snapTo(dismissDrag.value + pan.y) }
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (abs(dismissDrag.value) > 220f) {
                        onDismiss()
                    } else {
                        scope.launch { dismissDrag.animateTo(0f) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                // Only an image may exceed the screen (shown at natural size and panned). A table or
                // text diagram scrolls horizontally inside itself, and a horizontal scroller measured
                // with an unbounded width crashes, so keep those bounded to the container width.
                .then(if (block is Block.Image) Modifier.wrapContentSize(unbounded = true) else Modifier.fillMaxWidth())
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.value.x
                    translationY = offset.value.y + dismissDrag.value
                },
        ) {
            when (block) {
                is Block.Image -> {
                    val file = mediaFile(block.source.substringAfterLast('/'))
                    AsyncImage(
                        model = file,
                        contentDescription = block.alt,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(8.dp).background(androidx.compose.ui.graphics.Color.White),
                    )
                }
                is Block.Table -> Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                    TableView(block, type, marks, Modifier.padding(4.dp), onZoom = null)
                }
                is Block.Code -> CodeView(block, type, marks, Modifier.padding(4.dp), onZoom = null)
                else -> Text(block.plainText, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.Close, stringResource(R.string.reader_zoom_close))
            }
            if (scale.value > 1.05f) {
                FilledTonalIconButton(
                    onClick = {
                        scope.launch {
                            launch { scale.animateTo(1f) }
                            offset.animateTo(Offset.Zero)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) { Icon(Icons.Outlined.ZoomInMap, stringResource(R.string.reader_zoom_reset)) }
            }
        }
    }
}

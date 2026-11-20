package app.studydesk.core.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studydesk.core.designsystem.component.shimmer
import app.studydesk.core.reader.markdown.Block
import app.studydesk.core.reader.markdown.Inline
import app.studydesk.core.reader.markdown.plain
import coil3.compose.SubcomposeAsyncImage
import java.io.File

/** Text sizes for the current zoom; everything in the reader scales together. */
data class ReaderType(val scale: Float, val lineSpacing: Float, val serif: Boolean) {
    private val family = if (serif) FontFamily.Serif else FontFamily.Default
    fun body(): TextStyle = TextStyle(fontFamily = family, fontSize = (17 * scale).sp, lineHeight = (17 * scale * lineSpacing).sp)
    fun heading(level: Int): TextStyle {
        val size = when (level) {
            1 -> 26f
            2 -> 22f
            3 -> 19f
            else -> 17f
        } * scale
        return TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = size.sp, lineHeight = (size * 1.3f).sp)
    }
    fun mono(): TextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = (13.5f * scale).sp, lineHeight = (13.5f * scale * 1.35f).sp)
    fun table(): TextStyle = TextStyle(fontFamily = family, fontSize = (15 * scale).sp, lineHeight = (15 * scale * 1.35f).sp)
    val small: TextUnit get() = (13 * scale).sp
}

/** What the screen needs to know when the student touches a block. */
class BlockCallbacks(
    val onLongPress: (blockIndex: Int) -> Unit,
    val onTapHighlight: (highlightId: Long) -> Unit,
    val onZoom: (blockIndex: Int) -> Unit,
    val onHeadingMenu: (blockIndex: Int) -> Unit,
    val mediaFile: (name: String) -> File?,
    val onDownloadImages: () -> Unit,
)

@Composable
fun ReaderBlock(
    index: Int,
    block: Block,
    type: ReaderType,
    marks: TextMarks,
    highlightIds: List<Pair<IntRange, Long>>,
    callbacks: BlockCallbacks,
    imagesWaitForWifi: Boolean,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is Block.Heading -> InteractiveText(
            text = buildBlockText(block.runs, marks),
            style = type.heading(block.level),
            highlightIds = highlightIds,
            onLongPress = { callbacks.onHeadingMenu(index) },
            onTapHighlight = callbacks.onTapHighlight,
            modifier = modifier
                .padding(top = if (block.level <= 2) 28.dp else 18.dp, bottom = 8.dp)
                .semantics { heading() },
            color = if (block.level == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        is Block.Paragraph -> InteractiveText(
            text = buildBlockText(block.runs, marks),
            style = type.body(),
            highlightIds = highlightIds,
            onLongPress = { callbacks.onLongPress(index) },
            onTapHighlight = callbacks.onTapHighlight,
            modifier = modifier.padding(vertical = 6.dp),
        )
        is Block.ListItem -> Row(modifier.padding(start = (block.depth * 20).dp, top = 3.dp, bottom = 3.dp)) {
            Text(
                block.marker,
                style = type.body(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width((26 * type.scale).dp).clearAndSetSemantics { },
            )
            InteractiveText(
                text = buildBlockText(block.runs, marks),
                style = type.body(),
                highlightIds = highlightIds,
                onLongPress = { callbacks.onLongPress(index) },
                onTapHighlight = callbacks.onTapHighlight,
                modifier = Modifier.weight(1f),
            )
        }
        is Block.Quote -> Row(modifier.padding(vertical = 8.dp).height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).heightIn(min = 24.dp).background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.extraSmall))
            InteractiveText(
                text = buildBlockText(block.runs, marks),
                style = type.body().copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                highlightIds = highlightIds,
                onLongPress = { callbacks.onLongPress(index) },
                onTapHighlight = callbacks.onTapHighlight,
                modifier = Modifier.padding(start = 14.dp).weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is Block.Code -> CodeView(block, type, marks, modifier.padding(vertical = 8.dp), onZoom = { callbacks.onZoom(index) })
        is Block.Table -> TableView(block, type, marks, modifier.padding(vertical = 8.dp), onZoom = { callbacks.onZoom(index) })
        is Block.Image -> ImageView(block, callbacks, imagesWaitForWifi, modifier.padding(vertical = 10.dp), onZoom = { callbacks.onZoom(index) })
        Block.Rule -> HorizontalDivider(modifier.padding(vertical = 16.dp))
    }
}

/** Text that reports long presses, and taps on a highlight (to edit it). */
@Composable
private fun InteractiveText(
    text: androidx.compose.ui.text.AnnotatedString,
    style: TextStyle,
    highlightIds: List<Pair<IntRange, Long>>,
    onLongPress: () -> Unit,
    onTapHighlight: (Long) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val longPressLabel = stringResource(R.string.reader_highlight)
    Text(
        text = text,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(highlightIds) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { pos ->
                        val offset = layout?.getOffsetForPosition(pos) ?: return@detectTapGestures
                        highlightIds.firstOrNull { offset in it.first }?.let { onTapHighlight(it.second) }
                    },
                )
            }
            .semantics { onLongClick(label = longPressLabel) { onLongPress(); true } },
    )
}

@Composable
internal fun CodeView(block: Block.Code, type: ReaderType, marks: TextMarks, modifier: Modifier = Modifier, onZoom: (() -> Unit)?) {
    val scroll = rememberScrollState()
    val zoomLabel = stringResource(R.string.reader_zoom_hint)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onZoom != null) Modifier.clickable(onClickLabel = zoomLabel, onClick = onZoom) else Modifier),
    ) {
        Column {
            Text(
                plainAnnotated(block.text, marks),
                style = type.mono(),
                softWrap = false,
                modifier = Modifier.horizontalFade(scroll).horizontalScroll(scroll).padding(14.dp),
            )
            if (onZoom != null) ZoomHint(Modifier.align(Alignment.End))
        }
    }
}

@Composable
internal fun TableView(block: Block.Table, type: ReaderType, marks: TextMarks, modifier: Modifier = Modifier, onZoom: (() -> Unit)?) {
    val scroll = rememberScrollState()
    val columns = maxOf(block.header.size, block.rows.maxOfOrNull { it.size } ?: 0)
    // Column width from its longest cell, within reason, so a phone shows 2-3 columns and scrolls for the rest.
    val widths = (0 until columns).map { c ->
        val longest = (listOf(block.header) + block.rows).maxOf { row -> row.getOrNull(c)?.plain()?.length ?: 0 }
        ((longest * 7.2f + 24f).coerceIn(96f, 280f) * type.scale).dp
    }
    val outline = MaterialTheme.colorScheme.outlineVariant
    val zoomLabel = stringResource(R.string.reader_zoom_hint)
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onZoom != null) Modifier.clickable(onClickLabel = zoomLabel, onClick = onZoom) else Modifier),
    ) {
        Column(
            Modifier
                .border(1.dp, outline, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .horizontalFade(scroll)
                .horizontalScroll(scroll),
        ) {
            TableRow(block.header, widths, type, marks, header = true, rowNumber = 0, headerNames = block.header)
            block.rows.forEachIndexed { i, row ->
                HorizontalDivider(color = outline, modifier = Modifier.width(widths.fold(0.dp) { a, b -> a + b }))
                TableRow(row, widths, type, marks, header = false, rowNumber = i + 1, headerNames = block.header, zebra = i % 2 == 1)
            }
        }
        if (onZoom != null) ZoomHint(Modifier.align(Alignment.End))
    }
}

@Composable
private fun TableRow(
    cells: List<List<Inline>>,
    widths: List<androidx.compose.ui.unit.Dp>,
    type: ReaderType,
    marks: TextMarks,
    header: Boolean,
    rowNumber: Int,
    headerNames: List<List<Inline>>,
    zebra: Boolean = false,
) {
    val description = if (header) {
        headerNames.joinToString(", ") { it.plain() }
    } else {
        stringResource(
            R.string.reader_table_row,
            rowNumber,
            cells.mapIndexed { i, c -> "${headerNames.getOrNull(i)?.plain().orEmpty()}: ${c.plain()}" }.joinToString(", "),
        )
    }
    val bg = when {
        header -> MaterialTheme.colorScheme.secondaryContainer
        zebra -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> Color.Transparent
    }
    Row(Modifier.background(bg).height(IntrinsicSize.Min).clearAndSetSemantics { contentDescription = description }) {
        widths.forEachIndexed { i, w ->
            Text(
                buildBlockText(cells.getOrNull(i).orEmpty(), marks),
                style = type.table().copy(fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal),
                color = if (header) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(w).padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ImageView(block: Block.Image, callbacks: BlockCallbacks, waitForWifi: Boolean, modifier: Modifier, onZoom: () -> Unit) {
    val name = block.source.substringAfterLast('/')
    val file = callbacks.mediaFile(name)
    if (file == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CloudDownload, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text(block.alt.ifBlank { name }, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(if (waitForWifi) R.string.reader_image_wifi else R.string.reader_image_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = callbacks.onDownloadImages) { Text(stringResource(R.string.reader_download)) }
            }
        }
        return
    }
    SubcomposeAsyncImage(
        model = file,
        contentDescription = block.alt,
        contentScale = ContentScale.FillWidth,
        loading = { Box(Modifier.fillMaxWidth().height(180.dp).shimmer(MaterialTheme.shapes.medium)) },
        error = {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BrokenImage, null)
                Text(stringResource(R.string.reader_image_failed), style = MaterialTheme.typography.bodySmall)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, MaterialTheme.shapes.medium)
            .clickable(onClickLabel = stringResource(R.string.reader_zoom_hint), onClick = onZoom)
            .semantics { onClick { onZoom(); true } },
    )
}

/** "Tap to zoom" under a table or diagram, so nothing covers its content. */
@Composable
private fun ZoomHint(modifier: Modifier) {
    Row(modifier.padding(top = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Outlined.ZoomOutMap, null, Modifier.width(14.dp).height(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.reader_zoom_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Fades the right (or left) edge while there's more to scroll, so cut-off content is obvious. */
fun Modifier.horizontalFade(scroll: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = 32.dp.toPx()
        if (scroll.value < scroll.maxValue) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width),
                topLeft = Offset(size.width - fade, 0f),
                blendMode = BlendMode.DstIn,
            )
        }
        if (scroll.value > 0) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
                size = androidx.compose.ui.geometry.Size(fade, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
    }

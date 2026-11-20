package app.studydesk.core.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.studydesk.core.designsystem.theme.StudyTheme
import app.studydesk.core.model.ReaderSettings
import app.studydesk.core.model.ReaderTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(settings: ReaderSettings, onChange: ((ReaderSettings) -> ReaderSettings) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var scale by remember(settings.textScale) { mutableFloatStateOf(settings.textScale) }
            Text(stringResource(R.string.reader_text_size), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChange { it.copy(textScale = (it.textScale - 0.1f).coerceAtLeast(ReaderSettings.MIN_SCALE)) } }) {
                    Icon(Icons.Outlined.TextDecrease, stringResource(R.string.reader_text_size) + " −")
                }
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    onValueChangeFinished = { onChange { s -> s.copy(textScale = scale) } },
                    valueRange = ReaderSettings.MIN_SCALE..ReaderSettings.MAX_SCALE,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onChange { it.copy(textScale = (it.textScale + 0.1f).coerceAtMost(ReaderSettings.MAX_SCALE)) } }) {
                    Icon(Icons.Outlined.TextIncrease, stringResource(R.string.reader_text_size) + " +")
                }
                Text("${(scale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
            }

            Text(stringResource(R.string.reader_line_spacing), style = MaterialTheme.typography.titleSmall)
            val spacings = listOf(1.3f, 1.5f, 1.8f)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                spacings.forEachIndexed { i, v ->
                    SegmentedButton(
                        selected = abs(settings.lineSpacing - v) < 0.05f,
                        onClick = { onChange { it.copy(lineSpacing = v) } },
                        shape = SegmentedButtonDefaults.itemShape(i, spacings.size),
                    ) { Text(stringResource(listOf(R.string.reader_spacing_compact, R.string.reader_spacing_normal, R.string.reader_spacing_relaxed)[i])) }
                }
            }

            Text(stringResource(R.string.reader_font), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !settings.serif, onClick = { onChange { it.copy(serif = false) } }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(stringResource(R.string.reader_font_sans))
                }
                SegmentedButton(selected = settings.serif, onClick = { onChange { it.copy(serif = true) } }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text(stringResource(R.string.reader_font_serif))
                }
            }

            Text(stringResource(R.string.reader_theme), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReaderTheme.entries.forEach { theme ->
                    val (bg, fg) = themeSwatch(theme)
                    val label = stringResource(themeLabel(theme))
                    val selected = settings.theme == theme
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(bg, CircleShape)
                                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onChange { it.copy(theme = theme) } }
                                .semantics {
                                    contentDescription = label
                                    role = Role.RadioButton
                                    this.selected = selected
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = fg) else Text("Aa", color = fg, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            SwitchLine(stringResource(R.string.reader_keep_screen_on), settings.keepScreenOn) { v -> onChange { it.copy(keepScreenOn = v) } }
            SwitchLine(stringResource(R.string.reader_volume_keys), settings.volumeKeysZoom) { v -> onChange { it.copy(volumeKeysZoom = v) } }
        }
    }
}

private fun abs(f: Float) = kotlin.math.abs(f)

@Composable
private fun SwitchLine(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

internal fun themeLabel(theme: ReaderTheme) = when (theme) {
    ReaderTheme.Auto -> R.string.reader_theme_auto
    ReaderTheme.Light -> R.string.reader_theme_light
    ReaderTheme.Sepia -> R.string.reader_theme_sepia
    ReaderTheme.Dark -> R.string.reader_theme_dark
    ReaderTheme.Black -> R.string.reader_theme_black
}

@Composable
private fun themeSwatch(theme: ReaderTheme): Pair<Color, Color> = when (theme) {
    ReaderTheme.Auto -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    ReaderTheme.Light -> Color(0xFFFFFFFF) to Color(0xFF1B1B21)
    ReaderTheme.Sepia -> Color(0xFFF4ECD8) to Color(0xFF3B2F1E)
    ReaderTheme.Dark -> Color(0xFF1F1F25) to Color(0xFFE4E1EA)
    ReaderTheme.Black -> Color(0xFF000000) to Color(0xFFD6D6D6)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OutlineSheet(
    outline: List<OutlineEntry>,
    currentBlock: Int,
    bookmarked: Set<String>,
    onPick: (OutlineEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.reader_contents),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        OutlineList(outline, currentBlock, bookmarked, onPick, Modifier.navigationBarsPadding())
    }
}

/** Headings of the document; the section being read is highlighted. Used in a sheet and beside the text on tablets. */
@Composable
internal fun OutlineList(
    outline: List<OutlineEntry>,
    currentBlock: Int,
    bookmarked: Set<String>,
    onPick: (OutlineEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = outline.lastOrNull { it.blockIndex <= currentBlock }
    LazyColumn(modifier) {
        items(outline, key = { it.blockIndex }) { entry ->
            val selected = entry == current
            ListItem(
                headlineContent = {
                    Text(
                        entry.title,
                        style = if (entry.level <= 2) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                },
                trailingContent = if (entry.anchor in bookmarked) {
                    { Icon(Icons.Outlined.Bookmark, stringResource(R.string.reader_bookmark_added), tint = MaterialTheme.colorScheme.primary) }
                } else {
                    null
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                ),
                modifier = Modifier
                    .padding(start = ((entry.level - 1).coerceAtLeast(0) * 16).dp)
                    .clickable { onPick(entry) }
                    .semantics { this.selected = selected },
            )
        }
    }
}

/** Choosing text in a block, then highlighting, noting, copying, sharing or reading it. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SelectionSheet(
    text: String,
    onHighlight: (start: Int, end: Int, color: Int, note: String?) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onReadFromHere: () -> Unit,
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(text, TextRange(0, text.length))) }
    var color by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    val selection = value.selection.let { if (it.collapsed) TextRange(0, text.length) else TextRange(it.min, it.max) }
    val chosen = text.substring(selection.start, selection.end)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.reader_selection_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.reader_selection_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = { value = it.copy(text = text) },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            )
            ColorDots(color) { color = it }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.reader_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { onCopy(chosen) }, label = { Text(stringResource(R.string.reader_copy)) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                AssistChip(onClick = { onShare(chosen) }, label = { Text(stringResource(R.string.reader_share)) }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                AssistChip(onClick = onReadFromHere, label = { Text(stringResource(R.string.reader_read_from_here)) }, leadingIcon = { Icon(Icons.Outlined.RecordVoiceOver, null) })
                AssistChip(onClick = { onFind(chosen) }, label = { Text(stringResource(R.string.reader_find_this)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            }
            Button(
                onClick = { onHighlight(selection.start, selection.end, color, note) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.reader_highlight)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HighlightSheet(
    quote: String,
    initialColor: Int,
    initialNote: String?,
    onSave: (color: Int, note: String?) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    var color by remember { mutableIntStateOf(initialColor) }
    var note by remember { mutableStateOf(initialNote.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.reader_edit_highlight), style = MaterialTheme.typography.titleMedium)
            SelectionContainer {
                Text(
                    "“$quote”",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudyTheme.colors.highlights[color.coerceIn(0, 3)], MaterialTheme.shapes.small)
                        .padding(12.dp),
                )
            }
            ColorDots(color) { color = it }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.reader_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.reader_delete))
                }
                IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, stringResource(R.string.reader_share)) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(color, note) }) { Text(stringResource(R.string.reader_save)) }
            }
        }
    }
}

@Composable
private fun ColorDots(selected: Int, onPick: (Int) -> Unit) {
    val colors = StudyTheme.colors.highlights
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        colors.forEachIndexed { i, c ->
            val label = stringResource(R.string.reader_highlight_color, i + 1)
            Box(
                Modifier
                    .size(40.dp)
                    .background(c.copy(alpha = 1f), CircleShape)
                    .border(if (i == selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onPick(i) }
                    .semantics {
                        contentDescription = label
                        role = Role.RadioButton
                        this.selected = i == selected
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (i == selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeadingMenuSheet(
    title: String,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onReadFromHere: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            MenuLine(
                if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                stringResource(if (bookmarked) R.string.reader_unbookmark_section else R.string.reader_bookmark_section),
                onToggleBookmark,
            )
            MenuLine(Icons.Outlined.ContentCopy, stringResource(R.string.reader_copy_section), onCopy)
            MenuLine(Icons.Outlined.Share, stringResource(R.string.reader_share), onShare)
            MenuLine(Icons.Outlined.RecordVoiceOver, stringResource(R.string.reader_read_from_here), onReadFromHere)
        }
    }
}

@Composable
private fun MenuLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}


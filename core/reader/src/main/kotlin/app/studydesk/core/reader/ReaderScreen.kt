package app.studydesk.core.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.database.HighlightEntity
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.ErrorState
import app.studydesk.core.designsystem.component.InfoBanner
import app.studydesk.core.designsystem.component.ProbabilityChip
import app.studydesk.core.designsystem.component.Sharing
import app.studydesk.core.designsystem.component.SkeletonDocument
import app.studydesk.core.designsystem.theme.Brand
import app.studydesk.core.designsystem.theme.SubjectTheme
import app.studydesk.core.designsystem.component.UnreviewedNotice
import app.studydesk.core.designsystem.component.syncFailureMessage
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.designsystem.theme.StudyTheme
import app.studydesk.core.model.Question
import app.studydesk.core.model.ReaderSettings
import app.studydesk.core.model.ReaderTheme
import app.studydesk.core.reader.export.ContentPrinter
import app.studydesk.core.reader.markdown.Block
import app.studydesk.core.reader.markdown.ReaderDocs
import app.studydesk.core.reader.readaloud.ReadAloudState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import android.text.format.Formatter

@Composable
internal fun ReaderScreen(onBack: () -> Unit, onOpenQuestions: (String) -> Unit, viewModel: ReaderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ReaderEvents(viewModel, snackbar)

    ReaderThemeWrapper(state.settings.theme) {
        when (val content = state.content) {
            ReaderContent.Loading -> Scaffold(topBar = { SimpleTopBar(onBack) }) { p -> SkeletonDocument(Modifier.padding(p)) }
            is ReaderContent.NotDownloaded -> Scaffold(topBar = { SimpleTopBar(onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { p ->
                val context = LocalContext.current
                EmptyState(
                    icon = Icons.Outlined.CloudDownload,
                    title = stringResource(R.string.reader_not_downloaded_title),
                    message = stringResource(R.string.reader_not_downloaded_body, Formatter.formatShortFileSize(context, content.bytes)),
                    actionLabel = stringResource(if (state.downloading) R.string.reader_downloading else R.string.reader_download),
                    onAction = viewModel::download,
                    modifier = Modifier.padding(p),
                )
            }
            ReaderContent.NotFound -> Scaffold(topBar = { SimpleTopBar(onBack) }) { p ->
                ErrorState(
                    title = stringResource(R.string.reader_not_found_title),
                    message = stringResource(R.string.reader_not_found_body),
                    actionLabel = stringResource(R.string.reader_back),
                    onAction = onBack,
                    modifier = Modifier.padding(p),
                )
            }
            ReaderContent.Unreadable -> Scaffold(topBar = { SimpleTopBar(onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { p ->
                ErrorState(
                    title = stringResource(R.string.reader_unreadable_title),
                    message = stringResource(R.string.reader_unreadable_body),
                    actionLabel = stringResource(R.string.reader_download),
                    onAction = viewModel::download,
                    modifier = Modifier.padding(p),
                )
            }
            is ReaderContent.Ready -> SubjectTheme(Brand.subject(content.pack.subjectId)) {
                ReaderBody(content, state, viewModel, snackbar, onBack, onOpenQuestions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopBar(onBack: () -> Unit) {
    TopAppBar(title = {}, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.reader_back)) } })
}

/** Page colours for Light / Sepia / Dark / Black; Auto follows the app theme. */
@Composable
private fun ReaderThemeWrapper(theme: ReaderTheme, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val scheme = when (theme) {
        ReaderTheme.Auto -> base
        ReaderTheme.Light -> lightColorScheme(primary = base.primary, secondary = base.secondary, secondaryContainer = Color(0xFFE3E7F7), onSecondaryContainer = Color(0xFF1B1B21))
        ReaderTheme.Sepia -> lightColorScheme(
            primary = Color(0xFF8A4B14), secondary = Color(0xFF7A5C2E),
            background = Color(0xFFF4ECD8), surface = Color(0xFFF4ECD8), onSurface = Color(0xFF3B2F1E), onBackground = Color(0xFF3B2F1E),
            surfaceContainer = Color(0xFFEDE3CA), surfaceContainerHigh = Color(0xFFE8DCC0), surfaceContainerLow = Color(0xFFF0E6CF),
            surfaceContainerHighest = Color(0xFFE2D5B6), onSurfaceVariant = Color(0xFF5E4E35), outlineVariant = Color(0xFFD3C3A0),
            secondaryContainer = Color(0xFFE6D3AE), onSecondaryContainer = Color(0xFF3B2F1E),
        )
        ReaderTheme.Dark -> darkColorScheme(primary = Color(0xFFB9C3FF), secondary = Color(0xFF82D3D8))
        ReaderTheme.Black -> darkColorScheme(
            primary = Color(0xFFB9C3FF), secondary = Color(0xFF82D3D8),
            background = Color.Black, surface = Color.Black, onSurface = Color(0xFFD6D6D6),
            surfaceContainer = Color(0xFF111111), surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainerHighest = Color(0xFF222222), secondaryContainer = Color(0xFF1E2A2B),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content)
}

/** What a lazy-list row is: a block of the note, an "Ask yourself" card, or the footer. */
private sealed interface ReaderItem {
    /** "Not checked by a teacher yet", above the first block of an unreviewed subject's note. */
    data object Unreviewed : ReaderItem
    data class BlockItem(val index: Int) : ReaderItem
    data class AskItem(val anchor: String, val questions: List<Question>) : ReaderItem
    data object Footer : ReaderItem
}

private fun buildItems(loaded: LoadedDoc, reviewed: Boolean): List<ReaderItem> {
    val headings = loaded.blocks.withIndex().filter { it.value is Block.Heading }
    // Each section's questions go just before the next heading of the same or higher level.
    val insertAt = mutableMapOf<Int, MutableList<ReaderItem.AskItem>>()
    for ((i, b) in headings) {
        val h = b as Block.Heading
        val questions = loaded.ask[h.anchor] ?: continue
        val end = headings.firstOrNull { it.index > i && (it.value as Block.Heading).level <= h.level }?.index ?: loaded.blocks.size
        insertAt.getOrPut(end) { mutableListOf() } += ReaderItem.AskItem(h.anchor, questions)
    }
    return buildList {
        if (!reviewed) add(ReaderItem.Unreviewed)
        loaded.blocks.indices.forEach { i ->
            insertAt[i]?.let { addAll(it) }
            add(ReaderItem.BlockItem(i))
        }
        insertAt[loaded.blocks.size]?.let { addAll(it) }
        add(ReaderItem.Footer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderBody(
    ready: ReaderContent.Ready,
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpenQuestions: (String) -> Unit,
) {
    val loaded = ready.loaded
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val items = remember(loaded, ready.pack.reviewed) { buildItems(loaded, ready.pack.reviewed) }
    val itemOfBlock = remember(items) { items.withIndex().filter { it.value is ReaderItem.BlockItem }.associate { (it.value as ReaderItem.BlockItem).index to it.index } }

    // Where to open: a requested block, else the saved place.
    val listState = remember(loaded.doc.id) {
        val target = viewModel.startBlock.takeIf { it >= 0 } ?: ready.restore?.blockIndex ?: 0
        val offset = if (viewModel.startBlock >= 0) 0 else ready.restore?.blockOffset ?: 0
        // At the very start, open at the top of the list so anything above the first block shows.
        val index = if (target == 0 && offset == 0) 0 else itemOfBlock[target] ?: 0
        LazyListState(index, offset)
    }
    val currentBlock by remember(items) {
        derivedStateOf { (items.getOrNull(listState.firstVisibleItemIndex) as? ReaderItem.BlockItem)?.index ?: 0 }
    }
    val fraction by remember(items) {
        derivedStateOf {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()
            if (first == null || items.size <= 1) 0f
            else ((first.index + (-first.offset).toFloat() / maxOf(first.size, 1)) / (items.size - 1)).coerceIn(0f, 1f)
        }
    }

    // Save the reading position as the student scrolls; mark done at the end.
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (item, offset) ->
                val block = (items.getOrNull(item) as? ReaderItem.BlockItem)?.index ?: return@collect
                viewModel.onScrolled(block, offset, fraction)
            }
    }
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == items.lastIndex && listState.canScrollBackward }
            .distinctUntilChanged()
            .filter { it }
            .collect { viewModel.onReachedEnd() }
    }
    // Follow find and read aloud.
    LaunchedEffect(state.find.current, state.find.matches) {
        val match = state.find.matches.getOrNull(state.find.current) ?: return@LaunchedEffect
        itemOfBlock[match.blockIndex]?.let { if (reduce) listState.scrollToItem(it) else listState.animateScrollToItem(it) }
    }
    val speakingBlock = state.readAloud.current?.blockIndex
    LaunchedEffect(speakingBlock) {
        val item = speakingBlock?.let { itemOfBlock[it] } ?: return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (item !in visible.dropLast(1)) listState.animateScrollToItem(item)
    }

    // Live text zoom: pinch with two fingers anywhere; saved when the fingers lift.
    var liveScale by remember { mutableFloatStateOf(state.settings.textScale) }
    LaunchedEffect(state.settings.textScale) { liveScale = state.settings.textScale }
    val type = ReaderType(liveScale, state.settings.lineSpacing, state.settings.serif)

    val view = LocalView.current
    DisposableEffect(state.settings.keepScreenOn) {
        view.keepScreenOn = state.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var selectionBlock by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<HighlightEntity?>(null) }
    var headingMenu by remember { mutableStateOf<Int?>(null) }
    var zoomBlock by rememberSaveable(loaded.doc.id) { mutableStateOf<Int?>(null) }
    val blockBounds = remember(loaded.doc.id) { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    var rootSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1, 1)) }

    val c = StudyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val baseMarks = TextMarks(
        matchColor = scheme.tertiaryContainer,
        currentMatchColor = scheme.tertiary.copy(alpha = 0.55f),
        speakingColor = scheme.primary.copy(alpha = 0.18f),
        codeBackground = scheme.surfaceContainerHigh,
        linkColor = scheme.primary,
    )
    fun marksFor(index: Int, length: Int): Pair<TextMarks, List<Pair<IntRange, Long>>> {
        val hs = state.highlights[index].orEmpty().mapNotNull { h -> (h.start until h.end).clampTo(length)?.let { it to h } }
        val matches = state.find.matches.filter { it.blockIndex == index }.mapNotNull { (it.start until it.end).clampTo(length) }
        val current = state.find.matches.getOrNull(state.find.current)?.takeIf { it.blockIndex == index }?.let { (it.start until it.end).clampTo(length) }
        val speaking = state.readAloud.current?.takeIf { it.blockIndex == index && it.end > it.start }?.let { (it.start until it.end).clampTo(length) }
        return baseMarks.copy(
            highlights = hs.map { (r, h) -> r to c.highlights[h.colorIndex.coerceIn(0, 3)] },
            matches = matches,
            currentMatch = current,
            speaking = speaking,
        ) to hs.map { (r, h) -> r to h.id }
    }

    val callbacks = BlockCallbacks(
        onLongPress = { selectionBlock = it },
        onTapHighlight = { id -> editing = state.highlights.values.flatten().firstOrNull { it.id == id } },
        onZoom = { zoomBlock = it },
        onHeadingMenu = { headingMenu = it },
        mediaFile = viewModel::mediaFile,
        onDownloadImages = viewModel::downloadImages,
    )

    val focus = remember { FocusRequester() }
    LaunchedEffect(state.settings.volumeKeysZoom) { if (state.settings.volumeKeysZoom) runCatching { focus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSize = it.size }
            .then(
                if (state.settings.volumeKeysZoom) {
                    Modifier
                        .focusRequester(focus)
                        .focusable()
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val delta = when (e.key.nativeKeyCode) {
                                AndroidKeyEvent.KEYCODE_VOLUME_UP -> 0.1f
                                AndroidKeyEvent.KEYCODE_VOLUME_DOWN -> -0.1f
                                else -> return@onPreviewKeyEvent false
                            }
                            viewModel.updateSettings { it.copy(textScale = (it.textScale + delta).coerceIn(ReaderSettings.MIN_SCALE, ReaderSettings.MAX_SCALE)) }
                            true
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Column {
                    AnimatedContent(state.find.open, label = "topbar") { finding ->
                        if (finding) {
                            FindBar(state.find, viewModel)
                        } else {
                            ReaderTopBar(ready, state, viewModel, onBack, onOpenQuestions, onOutline = { showOutline = true }, onSettings = { showSettings = true }, context = context)
                        }
                    }
                    val progress by animateFloatAsState(fraction, label = "progress")
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().heightIn(max = 3.dp), drawStopIndicator = {})
                    InfoBanner(
                        visible = ready.newerAvailable,
                        message = stringResource(R.string.reader_newer),
                        icon = Icons.Outlined.TipsAndUpdates,
                        actionLabel = stringResource(R.string.reader_update),
                        onAction = viewModel::download,
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    state.readAloud.active,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    ReadAloudBar(
                        state = state.readAloud,
                        onPlayPause = { if (state.readAloud.status == ReadAloudState.Status.Speaking) viewModel.pauseReading() else viewModel.resumeReading() },
                        onSkip = viewModel::skipSentence,
                        onRate = viewModel::setSpeechRate,
                        onStop = viewModel::stopReading,
                    )
                }
            },
        ) { padding ->
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val wide = maxWidth >= 840.dp
                Row(Modifier.fillMaxSize()) {
                    if (wide && loaded.outline.isNotEmpty()) {
                        Surface(Modifier.width(300.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            OutlineList(loaded.outline, currentBlock, state.bookmarkedAnchors, onPick = { e ->
                                scope.launch { itemOfBlock[e.blockIndex]?.let { listState.animateScrollToItem(it) } }
                            })
                        }
                        VerticalDivider()
                    }
                    AnimatedContent(
                        targetState = loaded.doc.id,
                        transitionSpec = {
                            val group = loaded.group.map { it.id }
                            val forward = group.indexOf(targetState) > group.indexOf(initialState)
                            if (reduce) fadeIn() togetherWith fadeOut()
                            else (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                        },
                        label = "doc",
                        modifier = Modifier.weight(1f),
                    ) { _ ->
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .pinchToZoom(
                                    onZoom = { z -> liveScale = (liveScale * z).coerceIn(ReaderSettings.MIN_SCALE, ReaderSettings.MAX_SCALE) },
                                    onEnd = { val s = liveScale; viewModel.updateSettings { it.copy(textScale = s) } },
                                ),
                        ) {
                            items(items, key = { item ->
                                when (item) {
                                    is ReaderItem.BlockItem -> "b${item.index}"
                                    is ReaderItem.AskItem -> "a${item.anchor}"
                                    ReaderItem.Unreviewed -> "unreviewed"
                                    ReaderItem.Footer -> "footer"
                                }
                            }) { item ->
                                Box(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                                    when (item) {
                                        is ReaderItem.BlockItem -> {
                                            val block = loaded.blocks[item.index]
                                            val (marks, ids) = marksFor(item.index, block.plainText.length)
                                            ReaderBlock(
                                                index = item.index,
                                                block = block,
                                                type = type,
                                                marks = marks,
                                                highlightIds = ids,
                                                callbacks = callbacks,
                                                imagesWaitForWifi = false,
                                                modifier = Modifier.onGloballyPositioned { blockBounds[item.index] = it.boundsInRoot() },
                                            )
                                        }
                                        is ReaderItem.AskItem -> AskYourselfCard(item.questions)
                                        ReaderItem.Unreviewed -> UnreviewedNotice(Modifier.padding(bottom = 12.dp))
                                        ReaderItem.Footer -> Footer(loaded, state.done, onToggleDone = { viewModel.setDone(!state.done) }, onOpen = viewModel::openDoc)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-screen zoom, growing out of the block that was tapped.
        val zoomed = zoomBlock?.let { loaded.blocks.getOrNull(it) }
        val origin = zoomBlock?.let { blockBounds[it] }?.center?.let {
            TransformOrigin(it.x / rootSize.width.coerceAtLeast(1), it.y / rootSize.height.coerceAtLeast(1))
        } ?: TransformOrigin.Center
        AnimatedVisibility(
            visible = zoomed != null,
            enter = if (reduce) fadeIn() else scaleIn(initialScale = 0.3f, transformOrigin = origin) + fadeIn(),
            exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.3f, transformOrigin = origin) + fadeOut(),
        ) {
            var last by remember { mutableStateOf<Block?>(null) }
            if (zoomed != null) last = zoomed
            last?.let { ZoomOverlay(it, type.copy(scale = 1f), baseMarks, viewModel::mediaFile, onDismiss = { zoomBlock = null }) }
        }
    }

    if (showSettings) ReaderSettingsSheet(state.settings, viewModel::updateSettings) { showSettings = false }
    if (showOutline) {
        OutlineSheet(loaded.outline, currentBlock, state.bookmarkedAnchors, onPick = { e ->
            showOutline = false
            scope.launch { itemOfBlock[e.blockIndex]?.let { if (reduce) listState.scrollToItem(it) else listState.animateScrollToItem(it) } }
        }) { showOutline = false }
    }
    val copiedMessage = stringResource(R.string.reader_copied)
    val source = stringResource(R.string.reader_share_source, loaded.doc.title, ready.pack.title)
    selectionBlock?.let { index ->
        SelectionSheet(
            text = loaded.blocks[index].plainText,
            onHighlight = { s, e, color, note ->
                viewModel.addHighlight(index, s, e, color, note)
                selectionBlock = null
            },
            onCopy = { text ->
                copy(context, text)
                selectionBlock = null
                scope.launch { snackbar.showSnackbar(copiedMessage) }
            },
            onShare = { text -> Sharing.shareText(context, "“$text”\n\n$source", loaded.doc.title) },
            onReadFromHere = {
                selectionBlock = null
                viewModel.readAloud(index)
            },
            onFind = { text ->
                selectionBlock = null
                viewModel.openFind(true)
                viewModel.findQuery(text)
            },
            onDismiss = { selectionBlock = null },
        )
    }
    editing?.let { h ->
        HighlightSheet(
            quote = h.quote,
            initialColor = h.colorIndex,
            initialNote = h.note,
            onSave = { color, note ->
                viewModel.updateHighlight(h.id, color, note)
                editing = null
            },
            onDelete = {
                viewModel.deleteHighlight(h.id)
                editing = null
            },
            onShare = { Sharing.shareText(context, "“${h.quote}”" + (h.note?.let { "\n\n$it" } ?: "") + "\n\n$source", loaded.doc.title) },
            onDismiss = { editing = null },
        )
    }
    headingMenu?.let { index ->
        val heading = loaded.blocks[index] as? Block.Heading
        val entry = loaded.outline.firstOrNull { it.blockIndex == index }
        if (heading != null) {
            val sectionText = sectionText(loaded.blocks, index)
            HeadingMenuSheet(
                title = heading.plainText,
                bookmarked = heading.anchor in state.bookmarkedAnchors,
                onToggleBookmark = {
                    entry?.let(viewModel::toggleSectionBookmark)
                    headingMenu = null
                },
                onCopy = {
                    copy(context, sectionText)
                    headingMenu = null
                    scope.launch { snackbar.showSnackbar(copiedMessage) }
                },
                onShare = { Sharing.shareText(context, "$sectionText\n\n$source", heading.plainText) },
                onReadFromHere = {
                    headingMenu = null
                    viewModel.readAloud(index)
                },
                onDismiss = { headingMenu = null },
            )
        } else {
            headingMenu = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    ready: ReaderContent.Ready,
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onOpenQuestions: (String) -> Unit,
    onOutline: () -> Unit,
    onSettings: () -> Unit,
    context: Context,
) {
    var menu by remember { mutableStateOf(false) }
    val loaded = ready.loaded
    val printNotice = if (ready.pack.reviewed) null else
        stringResource(app.studydesk.core.designsystem.R.string.unreviewed_title) + ". " +
            stringResource(app.studydesk.core.designsystem.R.string.unreviewed_body)
    val probabilityLabels = listOf(stringResource(R.string.probability_high), stringResource(R.string.probability_medium), stringResource(R.string.probability_low))
    TopAppBar(
        title = {
            Column {
                Text(loaded.doc.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ready.pack.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.reader_back)) } },
        actions = {
            IconButton(onClick = { viewModel.openFind(true) }) { Icon(Icons.Outlined.Search, stringResource(R.string.reader_find)) }
            val scale by animateFloatAsState(if (state.docBookmarked) 1.15f else 1f, app.studydesk.core.designsystem.theme.Motion.bouncy(), label = "bm")
            IconButton(onClick = viewModel::toggleDocBookmark) {
                Icon(
                    if (state.docBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    stringResource(if (state.docBookmarked) R.string.reader_bookmark_remove else R.string.reader_bookmark_add),
                    tint = if (state.docBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size((24 * scale).dp),
                )
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.FormatSize, stringResource(R.string.reader_settings)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.reader_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (loaded.outline.isNotEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.reader_contents)) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.List, null) }, onClick = {
                            menu = false
                            onOutline()
                        })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.reader_read_aloud)) }, leadingIcon = { Icon(Icons.Outlined.RecordVoiceOver, null) }, onClick = {
                        menu = false
                        viewModel.readAloud(0)
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.reader_print_unit)) }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }, onClick = {
                        menu = false
                        ContentPrinter.printNotes(context, loaded.doc.title, "${ready.pack.code} · ${ready.pack.title}", listOf(loaded.doc.title to loaded.doc.markdown), printNotice)
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.reader_print_subject)) }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }, onClick = {
                        menu = false
                        val docs = ReaderDocs.all(ready.pack).flatten()
                        ContentPrinter.printNotes(context, "${ready.pack.code} ${ready.pack.title}", ready.pack.code, docs.map { it.title to it.markdown }, printNotice)
                    })
                    if (ready.pack.questions.isNotEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.reader_print_questions)) }, leadingIcon = { Icon(Icons.Outlined.Quiz, null) }, onClick = {
                            menu = false
                            ContentPrinter.printQuestions(context, "${ready.pack.code} question bank", ready.pack.title, ready.pack.questions, printNotice) { q ->
                                probabilityLabels[q.rating.rank]
                            }
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.reader_ask_title)) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) }, onClick = {
                            menu = false
                            onOpenQuestions(ready.pack.subjectId)
                        })
                    }
                    if (ready.pack.media.isNotEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.reader_download_images)) }, leadingIcon = { Icon(Icons.Outlined.Image, null) }, onClick = {
                            menu = false
                            viewModel.downloadImages()
                        })
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindBar(find: FindState, viewModel: ReaderViewModel) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    TopAppBar(
        navigationIcon = { IconButton(onClick = { viewModel.openFind(false) }) { Icon(Icons.Rounded.Close, stringResource(R.string.reader_find_close)) } },
        title = {
            TextField(
                value = find.query,
                onValueChange = viewModel::findQuery,
                placeholder = { Text(stringResource(R.string.reader_find_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.findStep(1) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        actions = {
            if (find.query.isNotBlank()) {
                Text(
                    if (find.matches.isEmpty()) stringResource(R.string.reader_find_none)
                    else stringResource(R.string.reader_find_count, find.current + 1, find.matches.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { viewModel.findStep(-1) }, enabled = find.matches.isNotEmpty()) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.reader_find_prev)) }
            IconButton(onClick = { viewModel.findStep(1) }, enabled = find.matches.isNotEmpty()) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.reader_find_next)) }
        },
    )
}

@Composable
private fun AskYourselfCard(questions: List<Question>) {
    OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Quiz, null, tint = MaterialTheme.colorScheme.tertiary)
                Text(stringResource(R.string.reader_ask_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            }
            Text(stringResource(R.string.reader_ask_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            questions.forEach { q ->
                var open by rememberSaveable(q.id) { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(q.question, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        ProbabilityChip(q.rating)
                    }
                    FilterChip(
                        selected = open,
                        onClick = { open = !open },
                        label = { Text(stringResource(if (open) R.string.reader_hide_answer else R.string.reader_show_answer)) },
                    )
                    AnimatedVisibility(open) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(q.answer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer(loaded: LoadedDoc, done: Boolean, onToggleDone: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FilterChip(
            selected = done,
            onClick = onToggleDone,
            label = { Text(stringResource(if (done) R.string.reader_done else R.string.reader_mark_done)) },
            leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            loaded.prev?.let { prev ->
                OutlinedButton(onClick = { onOpen(prev.id) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.reader_previous), style = MaterialTheme.typography.labelSmall)
                        Text(prev.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } ?: Spacer(Modifier.weight(1f))
            loaded.next?.let { next ->
                Button(onClick = { onOpen(next.id) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.reader_next), style = MaterialTheme.typography.labelSmall)
                        Text(next.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            } ?: Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReaderEvents(viewModel: ReaderViewModel, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val markedDone = stringResource(R.string.reader_marked_done)
    val undo = stringResource(R.string.reader_undo)
    val deleted = stringResource(R.string.reader_highlight_deleted)
    val added = stringResource(R.string.reader_bookmark_added)
    val removed = stringResource(R.string.reader_bookmark_removed)
    val downloaded = stringResource(R.string.reader_downloaded)
    val ttsUnavailable = stringResource(R.string.reader_tts_unavailable)
    val failureTexts = mapOf(
        SyncFailure.Offline to stringResource(app.studydesk.core.designsystem.R.string.sync_offline),
        SyncFailure.Timeout to stringResource(app.studydesk.core.designsystem.R.string.sync_timeout),
    )
    val genericFailure = stringResource(app.studydesk.core.designsystem.R.string.sync_failed_kept)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReaderEvent.MarkedDone -> {
                    if (snackbar.showSnackbar(markedDone, undo, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) viewModel.setDone(false)
                }
                is ReaderEvent.HighlightDeleted -> {
                    if (snackbar.showSnackbar(deleted, undo) == SnackbarResult.ActionPerformed) viewModel.restoreHighlight(event.highlight)
                }
                is ReaderEvent.BookmarkChanged -> snackbar.showSnackbar(if (event.added) added else removed)
                is ReaderEvent.DownloadFailed -> snackbar.showSnackbar(
                    when (val f = event.failure) {
                        is SyncFailure.StorageFull -> context.getString(app.studydesk.core.designsystem.R.string.sync_storage_full, Formatter.formatShortFileSize(context, maxOf(f.bytesNeeded, 1)))
                        else -> failureTexts[f] ?: genericFailure
                    },
                )
                ReaderEvent.Downloaded -> snackbar.showSnackbar(downloaded)
                ReaderEvent.ReadAloudUnavailable -> snackbar.showSnackbar(ttsUnavailable, duration = SnackbarDuration.Long)
            }
        }
    }
}

/** Two-finger pinch changes the text size; one finger is left alone so the list still scrolls. */
private fun Modifier.pinchToZoom(onZoom: (Float) -> Unit, onEnd: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var zoomed = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    zoomed = true
                    onZoom(zoom)
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (zoomed) onEnd()
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("StudyDesk", text))
}

/** The heading and everything under it until the next heading of the same or higher level. */
private fun sectionText(blocks: List<Block>, headingIndex: Int): String {
    val level = (blocks[headingIndex] as Block.Heading).level
    val end = (headingIndex + 1 until blocks.size).firstOrNull { (blocks[it] as? Block.Heading)?.level?.let { l -> l <= level } == true } ?: blocks.size
    return blocks.subList(headingIndex, end).joinToString("\n\n") { it.plainText }.trim()
}


package app.studydesk.feature.library

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.database.BookmarkEntity
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.designsystem.component.Tag
import app.studydesk.core.designsystem.theme.StudyTheme
import kotlinx.coroutines.launch

class LibraryActions(
    val onOpenReader: (packId: String, docId: String, blockIndex: Int) -> Unit,
    val onOpenSubject: (packId: String, tab: String?) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreen(actions: LibraryActions, viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbar = remember { SnackbarHostState() }
    val removed = stringResource(R.string.library_removed)
    val undo = stringResource(R.string.library_undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            if (snackbar.showSnackbar(removed, undo) == SnackbarResult.ActionPerformed) {
                when (e) {
                    is LibraryEvent.BookmarkRemoved -> viewModel.restoreBookmark(e.bookmark)
                    is LibraryEvent.HighlightRemoved -> viewModel.restoreHighlight(e.highlight)
                }
            }
        }
    }
    val tabs = listOf(R.string.library_bookmarks, R.string.library_highlights, R.string.library_downloads, R.string.library_progress)
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { LargeTopAppBar(title = { Text(stringResource(R.string.library_title)) }, scrollBehavior = scroll) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = pager.currentPage) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = pager.currentPage == i, onClick = { scope.launch { pager.animateScrollToPage(i) } }, text = { Text(stringResource(t), maxLines = 1) })
                }
            }
            if (state.loading) {
                SkeletonList()
            } else {
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> Bookmarks(state, viewModel, actions)
                        1 -> Highlights(state, viewModel, actions)
                        2 -> Downloads(state, viewModel, actions)
                        else -> Progress(state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun Bookmarks(state: LibraryUiState, viewModel: LibraryViewModel, actions: LibraryActions) {
    if (state.bookmarks.isEmpty()) {
        EmptyState(Icons.Outlined.BookmarkBorder, stringResource(R.string.library_no_bookmarks_title), stringResource(R.string.library_no_bookmarks_body))
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(state.bookmarks, key = { it.id }) { b ->
            ListItem(
                overlineContent = { Text("${kindLabel(b)} · ${b.subjectTitle}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                headlineContent = { Text(b.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Icon(if (b.kind == "question") Icons.Outlined.Quiz else Icons.AutoMirrored.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { IconButton(onClick = { viewModel.removeBookmark(b) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.library_remove)) } },
                modifier = Modifier.clickable {
                    when (b.kind) {
                        "question" -> actions.onOpenSubject(b.packId, "questions")
                        else -> actions.onOpenReader(b.packId, b.docId ?: b.targetId, b.blockIndex ?: -1)
                    }
                },
            )
        }
    }
}

@Composable
private fun kindLabel(b: BookmarkEntity) = stringResource(
    when (b.kind) {
        "section" -> R.string.library_kind_section
        "question" -> R.string.library_kind_question
        else -> R.string.library_kind_doc
    },
)

@Composable
private fun Highlights(state: LibraryUiState, viewModel: LibraryViewModel, actions: LibraryActions) {
    if (state.highlights.isEmpty()) {
        EmptyState(Icons.Outlined.BorderColor, stringResource(R.string.library_no_highlights_title), stringResource(R.string.library_no_highlights_body))
        return
    }
    val colors = StudyTheme.colors.highlights
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.highlights, key = { it.id }) { h ->
            ElevatedCard(onClick = { actions.onOpenReader(h.packId, h.docId, h.blockIndex) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(6.dp).fillMaxHeight().background(colors[h.colorIndex.coerceIn(0, 3)].copy(alpha = 1f)))
                    Column(Modifier.padding(14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${h.docTitle} · ${h.subjectTitle}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("“${h.quote}”", style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        h.note?.let { Text(stringResource(R.string.library_note, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                    IconButton(onClick = { viewModel.removeHighlight(h) }, modifier = Modifier.align(Alignment.Top)) { Icon(Icons.Outlined.Delete, stringResource(R.string.library_remove)) }
                }
            }
        }
    }
}

@Composable
private fun Downloads(state: LibraryUiState, viewModel: LibraryViewModel, actions: LibraryActions) {
    val context = LocalContext.current
    if (state.downloads.isEmpty()) {
        EmptyState(Icons.Outlined.CloudOff, stringResource(R.string.library_no_downloads_title), stringResource(R.string.library_no_downloads_body))
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Text(
                stringResource(R.string.library_space, Formatter.formatShortFileSize(context, state.downloadedBytes)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        items(state.downloads, key = { it.packId }) { d ->
            ListItem(
                headlineContent = { Text(d.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Formatter.formatShortFileSize(context, d.bytes))
                        if (d.updateAvailable) Tag(stringResource(R.string.library_update_available), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        else Tag(stringResource(if (d.pinned) R.string.library_kept_updated else R.string.library_with_app), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                trailingContent = if (d.pinned) {
                    { TextButton(onClick = { viewModel.stopUpdating(d.packId) }) { Text(stringResource(R.string.library_remove)) } }
                } else {
                    null
                },
                modifier = Modifier.clickable { actions.onOpenSubject(d.packId, null) },
            )
        }
    }
}

@Composable
private fun Progress(state: LibraryUiState, actions: LibraryActions) {
    if (state.progress.isEmpty()) {
        EmptyState(Icons.Outlined.Insights, stringResource(R.string.library_no_progress_title), stringResource(R.string.library_no_progress_body))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.progress, key = { it.packId }) { p ->
            ElevatedCard(onClick = { actions.onOpenSubject(p.packId, null) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.title, style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = { if (p.units == 0) 0f else p.unitsDone / p.units.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.library_units_done, p.unitsDone, p.units), style = MaterialTheme.typography.bodySmall)
                    p.mcqAccuracy?.let { Text(stringResource(R.string.library_mcq_accuracy, it), style = MaterialTheme.typography.bodySmall) }
                    if (p.mocks > 0) Text(stringResource(R.string.library_mocks, p.mocks), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

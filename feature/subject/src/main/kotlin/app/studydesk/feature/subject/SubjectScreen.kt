package app.studydesk.feature.subject

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.content.SyncFailure
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.DecorativeCircles
import app.studydesk.core.designsystem.component.StatusBarIconsOnDark
import app.studydesk.core.designsystem.theme.Brand
import app.studydesk.core.designsystem.theme.SubjectTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.studydesk.core.designsystem.component.ErrorState
import app.studydesk.core.designsystem.component.InfoBanner
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.designsystem.component.UnreviewedNotice
import app.studydesk.core.designsystem.component.sharedBoundsIfAvailable
import app.studydesk.core.designsystem.component.subjectSharedKey
import kotlinx.coroutines.launch

class SubjectActions(
    val onBack: () -> Unit,
    val onOpenReader: (packId: String, docId: String) -> Unit,
    val onPractice: (packId: String, unit: Int, wrongOnly: Boolean, quick: Boolean) -> Unit,
    val onFlashcards: (packId: String) -> Unit,
    val onMockTest: (packId: String, testId: String) -> Unit,
    val onMockResult: (resultId: Long) -> Unit,
)

internal enum class SubjectTab(val key: String, val label: Int) {
    Notes("notes", R.string.tab_notes),
    Questions("questions", R.string.tab_questions),
    Mcqs("mcqs", R.string.tab_mcqs),
    Tests("tests", R.string.tab_tests),
    Practicals("practicals", R.string.tab_practicals),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubjectScreen(actions: SubjectActions, viewModel: SubjectViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val downloadedText = stringResource(R.string.subject_downloaded)
    val offline = stringResource(app.studydesk.core.designsystem.R.string.sync_offline)
    val failed = stringResource(app.studydesk.core.designsystem.R.string.sync_failed_kept)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                SubjectEvent.Downloaded -> snackbar.showSnackbar(downloadedText)
                is SubjectEvent.Failed -> snackbar.showSnackbar(
                    when (val f = e.failure) {
                        SyncFailure.Offline -> offline
                        is SyncFailure.StorageFull -> context.getString(app.studydesk.core.designsystem.R.string.sync_storage_full, Formatter.formatShortFileSize(context, maxOf(f.bytesNeeded, 1)))
                        else -> failed
                    },
                )
            }
        }
    }

    val subject = state.ref?.subject
    val accent = Brand.subject(viewModel.subjectId)
    StatusBarIconsOnDark()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Header in the subject's colour; grows from the card or row that was tapped.
            Box(
                Modifier
                    .fillMaxWidth()
                    .sharedBoundsIfAvailable(subjectSharedKey(viewModel.subjectId))
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(accent.gradient),
            ) {
                DecorativeCircles(Modifier.matchParentSize(), alpha = 0.14f)
                Column(Modifier.statusBarsPadding().padding(bottom = 18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = actions.onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.subject_back), tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        val ready = state.content as? SubjectContent.Ready
                        if (ready != null) DownloadChip(ready, state.downloading, viewModel::download)
                    }
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center,
                        ) { Icon(accent.icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(
                                subject?.code ?: (state.content as? SubjectContent.Ready)?.pack?.code.orEmpty(),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            Text(
                                subject?.title ?: (state.content as? SubjectContent.Ready)?.pack?.title.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                            )
                        }
                    }
                    state.ref?.let {
                        Text(
                            "${it.programme.name} · ${it.term.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                        )
                    }
                }
            }
            when (val content = state.content) {
                SubjectContent.Loading -> SkeletonList()
                SubjectContent.ComingSoon -> EmptyState(
                    icon = Icons.Outlined.HourglassEmpty,
                    title = stringResource(R.string.subject_coming_title),
                    message = stringResource(R.string.subject_coming_body, subject?.title ?: viewModel.subjectId),
                )
                is SubjectContent.NotDownloaded -> EmptyState(
                    icon = Icons.Outlined.CloudDownload,
                    title = stringResource(R.string.subject_not_downloaded_title),
                    message = stringResource(R.string.subject_not_downloaded_body, Formatter.formatShortFileSize(context, content.bytes)),
                    actionLabel = stringResource(if (state.downloading) R.string.subject_downloading else R.string.subject_download),
                    onAction = viewModel::download,
                )
                SubjectContent.Unreadable -> ErrorState(
                    title = stringResource(R.string.subject_error_title),
                    message = stringResource(R.string.subject_error_body),
                    actionLabel = stringResource(R.string.subject_download),
                    onAction = viewModel::download,
                )
                is SubjectContent.Ready -> {
                    InfoBanner(
                        visible = content.newerAvailable,
                        message = stringResource(R.string.subject_update),
                        icon = Icons.Outlined.TipsAndUpdates,
                        actionLabel = stringResource(R.string.subject_download),
                        onAction = viewModel::download,
                    )
                    if (!content.pack.reviewed) {
                        UnreviewedNotice(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    SubjectTheme(accent) { SubjectTabs(content, state, viewModel, actions) }
                }
            }
        }
    }
}

@Composable
private fun DownloadChip(ready: SubjectContent.Ready, downloading: Boolean, onDownload: () -> Unit) {
    when {
        downloading -> CircularProgressIndicator(Modifier.padding(end = 16.dp).size(20.dp), strokeWidth = 2.dp, color = Color.White)
        ready.complete -> HeaderPill(Icons.Outlined.CloudDone, stringResource(R.string.subject_downloaded), onClick = null)
        else -> HeaderPill(Icons.Outlined.CloudDownload, stringResource(R.string.subject_download), onClick = onDownload)
    }
}

/** Translucent white pill for actions on the coloured header. */
@Composable
private fun HeaderPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
    val color = Color.White.copy(alpha = 0.2f)
    if (onClick != null) {
        Surface(onClick = onClick, color = color, contentColor = Color.White, shape = CircleShape, modifier = Modifier.padding(end = 12.dp)) { content() }
    } else {
        Surface(color = color, contentColor = Color.White, shape = CircleShape, modifier = Modifier.padding(end = 12.dp)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectTabs(content: SubjectContent.Ready, state: SubjectUiState, viewModel: SubjectViewModel, actions: SubjectActions) {
    val pack = content.pack
    val tabs = remember(pack) {
        SubjectTab.entries.filter {
            when (it) {
                SubjectTab.Notes -> pack.units.isNotEmpty() || pack.extras.isNotEmpty()
                SubjectTab.Questions -> pack.questions.isNotEmpty()
                SubjectTab.Mcqs -> pack.mcqs.isNotEmpty()
                SubjectTab.Tests -> pack.mockTests.isNotEmpty()
                SubjectTab.Practicals -> pack.practicals.isNotEmpty()
            }
        }
    }
    val start = tabs.indexOfFirst { it.key == viewModel.initialTab }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { tabs.size }
    val scope = rememberCoroutineScope()
    PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 12.dp) {
        tabs.forEachIndexed { i, tab ->
            Tab(
                selected = pager.currentPage == i,
                onClick = { scope.launch { pager.animateScrollToPage(i) } },
                text = { Text(stringResource(tab.label)) },
            )
        }
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
        when (tabs[page]) {
            SubjectTab.Notes -> NotesTab(pack, state, actions)
            SubjectTab.Questions -> QuestionsTab(pack, state, viewModel)
            SubjectTab.Mcqs -> McqsTab(pack, state, actions)
            SubjectTab.Tests -> TestsTab(pack, state, actions)
            SubjectTab.Practicals -> PracticalsTab(pack, state, actions)
        }
    }
}


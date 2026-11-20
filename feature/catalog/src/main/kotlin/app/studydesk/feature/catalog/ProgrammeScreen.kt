package app.studydesk.feature.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.ErrorState
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.model.Programme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ProgrammeScreen(
    streamId: String,
    programmeId: String,
    onBack: () -> Unit,
    onOpenSubject: (subjectId: String, tab: String?) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val programme: Programme? = (state as? BrowseUiState.Ready)?.catalog?.programme(streamId, programmeId)
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(programme?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.picker_back)) }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        when {
            state is BrowseUiState.Loading -> SkeletonList(modifier = Modifier.padding(padding))
            state is BrowseUiState.Unreadable || programme == null -> ErrorState(
                title = stringResource(R.string.loading_error_title),
                message = stringResource(R.string.loading_error_body),
                actionLabel = stringResource(R.string.action_retry),
                onAction = viewModel::retry,
                modifier = Modifier.padding(padding),
            )
            programme.pickableSchemes().isEmpty() -> EmptyState(
                icon = Icons.Outlined.PendingActions,
                title = stringResource(R.string.course_not_listed_title),
                message = stringResource(R.string.course_not_listed_body),
                modifier = Modifier.padding(padding),
            )
            else -> {
                val schemes = programme.pickableSchemes()
                var schemeId by rememberSaveable { mutableStateOf(schemes.first().id) }
                val scheme = schemes.firstOrNull { it.id == schemeId } ?: schemes.first()
                Column(Modifier.padding(padding).fillMaxSize()) {
                    if (schemes.size > 1) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            schemes.forEachIndexed { i, s ->
                                SegmentedButton(
                                    selected = s.id == scheme.id,
                                    onClick = { schemeId = s.id },
                                    shape = SegmentedButtonDefaults.itemShape(i, schemes.size),
                                ) { Text(s.name, maxLines = 1) }
                            }
                        }
                    }
                    AnimatedContent(scheme, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it.id }, label = "scheme") { sc ->
                        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.fillMaxSize()) {
                            if (sc.status == "to-confirm") {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.padding(16.dp),
                                    ) {
                                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(stringResource(R.string.course_status_to_confirm), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            sc.terms.forEach { term ->
                                stickyHeader(key = "h-${term.id}") {
                                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            term.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).semantics { heading() },
                                        )
                                    }
                                }
                                items(term.subjects, key = { "${term.id}-${it.code}" }) { subject ->
                                    SubjectRow(
                                        subject = subject,
                                        visibility = viewModel.visibility,
                                        onClick = { subject.destination().let { (id, tab) -> onOpenSubject(id, tab) } },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

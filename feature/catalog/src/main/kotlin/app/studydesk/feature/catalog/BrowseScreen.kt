package app.studydesk.feature.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.theme.Brand
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.ErrorState
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.model.Catalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseScreen(
    onOpenCourse: (streamId: String, programmeId: String) -> Unit,
    onOpenSubject: (subjectId: String, tab: String?) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text(stringResource(R.string.browse_title)) }, scrollBehavior = scroll) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text(stringResource(R.string.browse_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.query.value = "" }) { Icon(Icons.Outlined.Close, stringResource(android.R.string.cancel)) }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AnimatedContent(
                targetState = state,
                contentKey = { s -> if (s is BrowseUiState.Ready) (if (s.query.isBlank()) "list" else "search") else s::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "browse",
            ) { s ->
                when (s) {
                    BrowseUiState.Loading -> SkeletonList()
                    BrowseUiState.Unreadable -> ErrorState(
                        title = stringResource(R.string.loading_error_title),
                        message = stringResource(R.string.loading_error_body),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = viewModel::retry,
                    )
                    is BrowseUiState.Ready -> if (s.query.isBlank()) {
                        StreamList(s.catalog, viewModel, onOpenCourse)
                    } else if (s.matches.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = stringResource(R.string.browse_no_match_title),
                            message = stringResource(R.string.browse_no_match_body),
                        )
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(s.matches, key = { it.subject.id }) { ref ->
                                SubjectRow(
                                    subject = ref.subject,
                                    visibility = viewModel.visibility,
                                    supporting = "${ref.programme.name} · ${ref.term.name}",
                                    onClick = { ref.subject.destination().let { (id, tab) -> onOpenSubject(id, tab) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamList(catalog: Catalog, viewModel: BrowseViewModel, onOpenCourse: (String, String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        catalog.streamsAvailableFirst().forEach { stream ->
            item(key = "h-${stream.id}") {
                Text(
                    stream.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Brand.stream(stream.id).solid,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp, start = 4.dp).semantics { heading() },
                )
            }
            items(stream.programmes, key = { "${stream.id}-${it.id}" }) { programme ->
                val schemes = programme.pickableSchemes()
                val terms = schemes.flatMap { it.terms }
                val ready = terms.sumOf { it.readyCount(viewModel.visibility) }
                val total = terms.sumOf { it.theoryCount() }
                OptionCard(
                    title = programme.name,
                    subtitle = if (schemes.isEmpty()) null else stringResource(R.string.picker_subjects_ready, ready, total),
                    accent = Brand.stream(stream.id),
                    enabled = schemes.isNotEmpty(),
                    progress = if (total > 0) ready.toFloat() / total else null,
                    onClick = { onOpenCourse(stream.id, programme.id) },
                )
            }
        }
    }
}

package app.studydesk.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.database.SearchHit
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.SkeletonList

class SearchActions(
    val onBack: () -> Unit,
    val onOpenReader: (packId: String, docId: String, blockIndex: Int) -> Unit,
    val onOpenSubject: (packId: String, tab: String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(actions: SearchActions, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.search_back)) } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.query.value = it },
                        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { viewModel.query.value = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.search_clear)) }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "search",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { s ->
            when (s) {
                SearchUiState.Preparing -> Column {
                    Text(stringResource(R.string.search_preparing), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                    SkeletonList(rows = 4)
                }
                SearchUiState.Searching -> SkeletonList(rows = 4)
                SearchUiState.Idle -> EmptyState(Icons.Outlined.ManageSearch, stringResource(R.string.search_start_title), stringResource(R.string.search_start_body))
                SearchUiState.NoResults -> EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.search_none_title), stringResource(R.string.search_none_body))
                is SearchUiState.Results -> Results(s.hits, actions)
            }
        }
    }
}

private val kindOrder = listOf("note", "question", "mcq", "practical")

@Composable
private fun Results(hits: List<SearchHit>, actions: SearchActions) {
    val groups = hits.groupBy { it.kind }.toList().sortedBy { kindOrder.indexOf(it.first) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        groups.forEach { (kind, list) ->
            item(key = "h-$kind") {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Text(
                        stringResource(
                            when (kind) {
                                "question" -> R.string.search_kind_question
                                "mcq" -> R.string.search_kind_mcq
                                "practical" -> R.string.search_kind_practical
                                else -> R.string.search_kind_note
                            },
                        ) + " · ${list.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).semantics { heading() },
                    )
                }
            }
            items(list, key = { "${it.packId}/${it.kind}/${it.refId}" }) { hit ->
                ListItem(
                    overlineContent = { Text(hit.subjectTitle, maxLines = 1) },
                    headlineContent = { Text(hit.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(snippet(hit.snippet), maxLines = 3, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.clickable {
                        when (hit.kind) {
                            "question" -> actions.onOpenSubject(hit.packId, "questions")
                            "mcq" -> actions.onOpenSubject(hit.packId, "mcqs")
                            else -> {
                                val docId = hit.refId.substringBefore('#')
                                val block = hit.refId.substringAfter('#', "0").toIntOrNull() ?: 0
                                actions.onOpenReader(hit.packId, docId, block)
                            }
                        }
                    },
                )
            }
        }
    }
}

/** The FTS snippet marks matches with the control characters 2 and 3; show them in bold. */
private fun snippet(raw: String) = buildAnnotatedString {
    var bold = false
    raw.forEach { ch ->
        when (ch) {
            '\u0002' -> bold = true
            '\u0003' -> bold = false
            else -> if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(ch) } else append(ch)
        }
    }
}

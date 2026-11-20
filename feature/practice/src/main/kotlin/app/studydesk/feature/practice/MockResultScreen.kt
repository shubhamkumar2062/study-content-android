package app.studydesk.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import app.studydesk.core.database.MockResultEntity
import app.studydesk.core.database.PracticeDao
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.SkeletonDocument
import app.studydesk.core.designsystem.theme.StudyTheme
import app.studydesk.feature.practice.navigation.MockResultRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MockResultViewModel @Inject constructor(savedState: SavedStateHandle, private val practice: PracticeDao) : ViewModel() {
    val resultId = savedState.toRoute<MockResultRoute>().resultId
    suspend fun load(): Pair<MockResultEntity, Paper?>? {
        val r = practice.result(resultId) ?: return null
        return r to runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<Paper>(r.detailJson) }.getOrNull()
    }
}

private sealed interface ResultLoad {
    data object Loading : ResultLoad
    data object Missing : ResultLoad
    data class Ready(val result: MockResultEntity, val paper: Paper?) : ResultLoad
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MockResultScreen(onBack: () -> Unit, viewModel: MockResultViewModel = hiltViewModel()) {
    val load by produceState<ResultLoad>(ResultLoad.Loading, viewModel) {
        value = viewModel.load()?.let { ResultLoad.Ready(it.first, it.second) } ?: ResultLoad.Missing
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mock_result_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.practice_back)) } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val l = load) {
                ResultLoad.Loading -> SkeletonDocument()
                ResultLoad.Missing -> EmptyState(Icons.Outlined.TaskAlt, stringResource(R.string.not_found), "", actionLabel = stringResource(R.string.practice_back), onAction = onBack)
                is ResultLoad.Ready -> {
                    val r = l.result
                    val total = r.autoMarks + r.selfMarks
                    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(r.testTitle, style = MaterialTheme.typography.titleMedium)
                                ScoreRing(total / r.totalMarks.coerceAtLeast(1).toFloat(), stringResource(R.string.result_score, total, r.totalMarks))
                                if (r.autoMax > 0) Text("${stringResource(R.string.mock_result_auto)}: ${r.autoMarks} / ${r.autoMax}", style = MaterialTheme.typography.bodyMedium)
                                if (r.selfMax > 0) Text("${stringResource(R.string.mock_result_self)}: ${r.selfMarks} / ${r.selfMax}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        l.paper?.let { paper ->
                            item { Text(stringResource(R.string.mock_result_breakdown), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                            items(paper.sections, key = { it.id }) { s ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.mock_result_section, s.title, s.marks, s.maxMarks), style = MaterialTheme.typography.bodyLarge)
                                    LinearProgressIndicator(progress = { s.marks / s.maxMarks.coerceAtLeast(1).toFloat() }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            val wrong = paper.sections.filter { it.isMcq }.flatMap { s -> s.items.filter { it.chosen != it.correct } }
                            if (wrong.isNotEmpty()) {
                                item { Text(stringResource(R.string.result_review), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                                items(wrong, key = { "w-${it.id}" }) { item ->
                                    OutlinedCard(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(item.question, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                item.chosen?.let { stringResource(R.string.result_your_answer, item.options[it]) } ?: stringResource(R.string.result_skipped),
                                                color = StudyTheme.colors.wrong,
                                            )
                                            Text("✓ ${item.answer}", color = StudyTheme.colors.correct)
                                            Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

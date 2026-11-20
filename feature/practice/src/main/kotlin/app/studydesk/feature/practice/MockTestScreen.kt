package app.studydesk.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.OutlinedFlag
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.SkeletonDocument
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MockTestScreen(onBack: () -> Unit, onFinished: (Long) -> Unit, viewModel: MockTestViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val view = LocalView.current
    LaunchedEffect(viewModel) { viewModel.finished.collect(onFinished) }
    val timeUpText = stringResource(R.string.test_time_up)
    LaunchedEffect(state.timeUp) {
        if (state.timeUp) {
            view.answerHaptic(false)
            snackbar.showSnackbar(timeUpText)
        }
    }
    BackHandler(enabled = state.phase == MockPhase.Answering || state.phase == MockPhase.Marking) { confirmLeave = true }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.paper?.title.orEmpty(), maxLines = 1)
                        Text(state.subjectTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                },
                navigationIcon = { IconButton(onClick = { confirmLeave = true }) { Icon(Icons.Rounded.Close, stringResource(R.string.practice_close)) } },
                actions = {
                    if (state.phase == MockPhase.Answering) {
                        TimerChip(state.remainingSeconds)
                        IconButton(onClick = { viewModel.setPaused(!state.paused) }) {
                            Icon(
                                if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                stringResource(if (state.paused) R.string.test_resume else R.string.test_pause),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.phase == MockPhase.Answering) {
                Button(
                    onClick = { confirmFinish = true },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.test_submit)) }
            } else if (state.phase == MockPhase.Marking) {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.test_save)) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.phase) {
                MockPhase.Loading -> SkeletonDocument()
                MockPhase.Missing -> EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = stringResource(R.string.not_found),
                    message = "",
                    actionLabel = stringResource(R.string.practice_back),
                    onAction = onBack,
                )
                MockPhase.Answering -> {
                    Answering(state, viewModel, Modifier.then(if (state.paused) Modifier.blur(16.dp).alpha(0.2f) else Modifier))
                    if (state.paused) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.test_paused), style = MaterialTheme.typography.headlineMedium)
                            Button(onClick = { viewModel.setPaused(false) }, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.test_resume)) }
                        }
                    }
                }
                MockPhase.Marking -> Marking(state, viewModel)
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.test_leave_title)) },
            text = { Text(stringResource(R.string.test_leave_body)) },
            confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.test_leave)) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.test_keep_going)) } },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.test_submit_confirm_title)) },
            text = {
                val unanswered = state.unansweredMcqs
                Text(
                    if (unanswered > 0) pluralStringResource(R.plurals.test_submit_confirm_unanswered, unanswered, unanswered)
                    else stringResource(R.string.test_submit_confirm_body),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    viewModel.finishAnswering()
                }) { Text(stringResource(R.string.test_submit_yes)) }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.test_keep_going)) } },
        )
    }
}

@Composable
private fun TimerChip(seconds: Int) {
    val low = seconds <= 5 * 60
    val color by animateColorAsState(if (low) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, label = "timer")
    val text = "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    val description = stringResource(R.string.test_time_left, text)
    AssistChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color),
        border = null,
        modifier = Modifier.semantics { stateDescription = description },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Answering(state: MockUiState, viewModel: MockTestViewModel, modifier: Modifier) {
    val paper = state.paper ?: return
    Column(modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = state.section, edgePadding = 12.dp) {
            paper.sections.forEachIndexed { i, s ->
                Tab(selected = state.section == i, onClick = { viewModel.selectSection(i) }, text = { Text(s.title.substringBefore(":"), maxLines = 1) })
            }
        }
        AnimatedContent(state.section, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "section") { index ->
            val section = paper.sections[index]
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(stringResource(R.string.test_section, section.title, section.attempt, section.items.size), style = MaterialTheme.typography.titleSmall)
                    if (!section.isMcq) Text(stringResource(R.string.test_written_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                itemsIndexed(section.items, key = { _, it -> it.id }) { n, item ->
                    OutlinedCard(Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text("${n + 1}. ${item.question}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                IconToggleButton(checked = item.flagged, onCheckedChange = { viewModel.toggleFlag(section.id, item.id) }) {
                                    Icon(
                                        if (item.flagged) Icons.Outlined.Flag else Icons.Outlined.OutlinedFlag,
                                        stringResource(if (item.flagged) R.string.test_unflag else R.string.test_flag),
                                        tint = if (item.flagged) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (section.isMcq) {
                                item.options.forEachIndexed { i, option ->
                                    val selected = item.chosen == i
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .selectable(selected = selected, role = Role.RadioButton) { viewModel.choose(section.id, item.id, i) },
                                    ) {
                                        Text("${"ABCDEF"[i]}. $option", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
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

@Composable
private fun Marking(state: MockUiState, viewModel: MockTestViewModel) {
    val paper = state.paper ?: return
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.test_mark_title), style = MaterialTheme.typography.titleLarge)
                if (paper.autoMax > 0) Text(stringResource(R.string.test_mcq_marked, paper.autoMarks, paper.autoMax), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        paper.sections.filterNot { it.isMcq }.forEach { section ->
            item(key = "h-${section.id}") {
                Column {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.test_mark_hint, section.attempt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            itemsIndexed(section.items, key = { _, it -> "${section.id}-${it.id}" }) { n, item ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${n + 1}. ${item.question}", style = MaterialTheme.typography.titleSmall)
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(stringResource(R.string.test_model_answer), style = MaterialTheme.typography.labelMedium)
                                Text(item.answer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        var marks by rememberSaveable(item.id) { mutableStateOf((item.selfMarks ?: 0).toFloat()) }
                        Text(stringResource(R.string.test_marks_given, marks.roundToInt(), section.marksEach), style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = marks,
                            onValueChange = { marks = it },
                            onValueChangeFinished = { viewModel.setSelfMarks(section.id, item.id, marks.roundToInt()) },
                            valueRange = 0f..section.marksEach.toFloat(),
                            steps = (section.marksEach - 1).coerceAtLeast(0),
                        )
                    }
                }
            }
        }
    }
}

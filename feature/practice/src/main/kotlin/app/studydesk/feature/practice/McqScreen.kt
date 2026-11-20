package app.studydesk.feature.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.SkeletonDocument
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.designsystem.theme.StudyTheme
import app.studydesk.core.model.Mcq

private val letters = listOf("A", "B", "C", "D", "E", "F")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun McqScreen(onBack: () -> Unit, onRetryWrong: (String) -> Unit, viewModel: McqViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val route = viewModel.route
    val title = when {
        route.quick -> stringResource(R.string.mcq_title_quick)
        route.wrongOnly -> stringResource(R.string.mcq_title_wrong)
        route.unit > 0 -> stringResource(R.string.mcq_title_unit, route.unit)
        else -> stringResource(R.string.mcq_title)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title)
                        if (state.subjectTitle.isNotEmpty()) Text(state.subjectTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.practice_back)) } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> SkeletonDocument()
                state.missing -> EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = stringResource(R.string.mcq_none_title),
                    message = stringResource(R.string.mcq_none_body),
                    actionLabel = stringResource(R.string.result_done),
                    onAction = onBack,
                )
                state.finished -> McqResults(state, onBack, onRetry = { onRetryWrong(route.packId) })
                else -> McqQuestion(state, viewModel)
            }
        }
    }
}

@Composable
private fun McqQuestion(state: McqUiState, viewModel: McqViewModel) {
    val reduce = LocalReduceMotion.current
    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (state.index + 1) / state.questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Text(
            stringResource(R.string.mcq_progress, state.index + 1, state.questions.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        AnimatedContent(
            targetState = state.index,
            transitionSpec = {
                if (reduce) fadeIn() togetherWith androidx.compose.animation.fadeOut()
                else (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + androidx.compose.animation.fadeOut())
            },
            label = "mcq",
            modifier = Modifier.weight(1f),
        ) { index ->
            val q = state.questions[index]
            McqCard(q, state.answers[q.id], onChoose = viewModel::choose)
        }
        val answered = state.current?.id in state.answers
        AnimatedVisibility(answered, enter = expandVertically() + fadeIn()) {
            Button(
                onClick = viewModel::next,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).heightIn(min = 52.dp),
            ) { Text(stringResource(if (state.index >= state.questions.lastIndex) R.string.mcq_finish else R.string.mcq_next)) }
        }
    }
}

@Composable
private fun McqCard(q: Mcq, chosen: Int?, onChoose: (Int) -> Unit) {
    val view = LocalView.current
    LaunchedEffect(chosen) { if (chosen != null) view.answerHaptic(chosen == q.correct) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(q.question, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        q.options.forEachIndexed { i, option ->
            OptionRow(letters[i], option, i, q.correct, chosen, onChoose)
        }
        if (chosen != null) {
            val right = chosen == q.correct
            val c = StudyTheme.colors
            Card(
                colors = CardDefaults.cardColors(containerColor = if (right) c.correctContainer else c.wrongContainer),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (right) stringResource(R.string.mcq_correct) else stringResource(R.string.mcq_wrong, "${letters[q.correct]}. ${q.options[q.correct]}"),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (right) c.correct else c.wrong,
                    )
                    Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionRow(letter: String, text: String, index: Int, correct: Int, chosen: Int?, onChoose: (Int) -> Unit) {
    val c = StudyTheme.colors
    val answered = chosen != null
    val target = when {
        answered && index == correct -> c.correctContainer
        answered && index == chosen -> c.wrongContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val bg by animateColorAsState(target, label = "option")
    val border = when {
        answered && index == correct -> c.correct
        answered && index == chosen -> c.wrong
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val label = stringResource(R.string.mcq_option, letter, text)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(if (answered && (index == correct || index == chosen)) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = chosen == index, enabled = !answered, role = Role.RadioButton) { onChoose(index) }
            .semantics { contentDescription = label },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text(letter, style = MaterialTheme.typography.labelLarge) }
            }
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (answered && index == correct) Icon(Icons.Rounded.CheckCircle, null, tint = c.correct)
            if (answered && index == chosen && index != correct) Icon(Icons.Rounded.Cancel, null, tint = c.wrong)
        }
    }
}

@Composable
private fun McqResults(state: McqUiState, onDone: () -> Unit, onRetry: () -> Unit) {
    val total = state.questions.size
    val pct = if (total == 0) 0f else state.score / total.toFloat()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleMedium)
                ScoreRing(pct, stringResource(R.string.result_score, state.score, total))
                Text(
                    stringResource(
                        when {
                            pct >= 0.8f -> R.string.result_great
                            pct >= 0.5f -> R.string.result_good
                            else -> R.string.result_keep
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.wrong.isNotEmpty()) OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.result_retry_wrong)) }
                    Button(onClick = onDone) { Text(stringResource(R.string.result_done)) }
                }
            }
        }
        if (state.wrong.isNotEmpty()) {
            item { Text(stringResource(R.string.result_review), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(state.wrong, key = { it.id }) { q ->
                OutlinedCard(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(q.question, style = MaterialTheme.typography.titleSmall)
                        val chosen = state.answers[q.id]
                        Text(
                            chosen?.let { stringResource(R.string.result_your_answer, q.options[it]) } ?: stringResource(R.string.result_skipped),
                            color = StudyTheme.colors.wrong,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text("✓ ${q.options[q.correct]}", color = StudyTheme.colors.correct, style = MaterialTheme.typography.bodyMedium)
                        Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** A ring that fills to the score with a short animation. */
@Composable
internal fun ScoreRing(fraction: Float, label: String) {
    val progress = remember { Animatable(0f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(fraction) { if (reduce) progress.snapTo(fraction) else progress.animateTo(fraction, androidx.compose.animation.core.tween(900)) }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val color = when {
        fraction >= 0.8f -> StudyTheme.colors.correct
        fraction >= 0.5f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Box(Modifier.size(160.dp).semantics(mergeDescendants = true) { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(160.dp)) {
            val stroke = 14.dp.toPx()
            val d = size.minDimension - stroke
            drawArc(track, 0f, 360f, false, Offset(stroke / 2, stroke / 2), Size(d, d), style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress.value, false, Offset(stroke / 2, stroke / 2), Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}

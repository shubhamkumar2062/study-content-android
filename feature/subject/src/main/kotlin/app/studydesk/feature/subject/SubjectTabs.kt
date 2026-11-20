package app.studydesk.feature.subject

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.ProbabilityChip
import app.studydesk.core.designsystem.component.Tag
import app.studydesk.core.model.Question
import app.studydesk.core.model.SubjectPack
import app.studydesk.core.reader.markdown.ReaderDocs
import java.text.DateFormat
import java.util.Date

@Composable
internal fun NotesTab(pack: SubjectPack, state: SubjectUiState, actions: SubjectActions) {
    val units = pack.units.sortedBy { it.number }
    val done = units.count { state.progress[ReaderDocs.unitId(it.number)]?.done == true }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(stringResource(R.string.subject_units_done, done, units.size), style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { if (units.isEmpty()) 0f else done / units.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
        items(units, key = { it.number }) { unit ->
            val id = ReaderDocs.unitId(unit.number)
            val p = state.progress[id]
            DocCard(
                overline = stringResource(R.string.subject_unit, unit.number),
                title = unit.title,
                fraction = p?.fraction ?: 0f,
                done = p?.done == true,
                onClick = { actions.onOpenReader(pack.subjectId, id) },
            )
        }
        if (pack.extras.isNotEmpty()) {
            item { Text(stringResource(R.string.subject_extras), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp).semantics { heading() }) }
            items(pack.extras, key = { "x-${it.id}" }) { extra ->
                val id = ReaderDocs.extraId(extra.id)
                DocCard(null, extra.title, state.progress[id]?.fraction ?: 0f, state.progress[id]?.done == true) { actions.onOpenReader(pack.subjectId, id) }
            }
        }
    }
}

@Composable
private fun DocCard(overline: String?, title: String, fraction: Float, done: Boolean, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(
                if (done) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = if (done) stringResource(R.string.subject_done) else null,
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (overline != null) Text(overline, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!done && fraction > 0.02f) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            if (!done && fraction > 0.02f) Text(stringResource(R.string.subject_percent, (fraction * 100).toInt()), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun QuestionsTab(pack: SubjectPack, state: SubjectUiState, viewModel: SubjectViewModel) {
    val f = state.filter
    val shown = remember(pack, f, state.savedQuestions) {
        pack.questions.filter { q ->
            (f.unit == null || q.unit == f.unit) &&
                (f.type == null || q.type == f.type) &&
                (f.probability == null || q.probability == f.probability) &&
                (!f.savedOnly || q.id in state.savedQuestions)
        }.sortedWith(compareBy<Question>({ it.unit }, { it.rating.rank }, { it.id }))
    }
    var sharing by remember { mutableStateOf<Question?>(null) }
    Column(Modifier.fillMaxSize()) {
        val units = pack.units.map { it.number }.sorted()
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(f.unit == null, { viewModel.setFilter { it.copy(unit = null) } }, { Text(stringResource(R.string.filter_all_units)) }) }
            items(units) { u -> FilterChip(f.unit == u, { viewModel.setFilter { it.copy(unit = if (it.unit == u) null else u) } }, { Text(stringResource(R.string.filter_unit, u)) }) }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(f.type == "short", { viewModel.setFilter { it.copy(type = if (it.type == "short") null else "short") } }, { Text(stringResource(R.string.filter_short)) }) }
            item { FilterChip(f.type == "long", { viewModel.setFilter { it.copy(type = if (it.type == "long") null else "long") } }, { Text(stringResource(R.string.filter_long)) }) }
            listOf("high" to R.string.filter_high, "medium" to R.string.filter_medium, "low" to R.string.filter_low).forEach { (p, label) ->
                item { FilterChip(f.probability == p, { viewModel.setFilter { it.copy(probability = if (it.probability == p) null else p) } }, { Text(stringResource(label)) }) }
            }
            item {
                FilterChip(f.savedOnly, { viewModel.setFilter { it.copy(savedOnly = !it.savedOnly) } }, { Text(stringResource(R.string.filter_saved)) }, leadingIcon = { Icon(Icons.Outlined.Bookmark, null, Modifier.size(18.dp)) })
            }
        }
        if (shown.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FilterAltOff,
                title = stringResource(R.string.questions_none_title),
                message = stringResource(R.string.questions_none_body),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(stringResource(R.string.questions_count, shown.size), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.probability_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(shown, key = { it.id }) { q ->
                    QuestionCard(
                        q = q,
                        saved = q.id in state.savedQuestions,
                        revising = q.id in state.revising,
                        onSave = { viewModel.toggleSaved(pack, q.id) },
                        onRevise = { viewModel.toggleRevise(q.id) },
                        onShare = { sharing = q },
                    )
                }
            }
        }
    }
    sharing?.let { q -> ShareQuestionDialog(q, pack, onDismiss = { sharing = null }) }
}

@Composable
private fun QuestionCard(q: Question, saved: Boolean, revising: Boolean, onSave: () -> Unit, onRevise: () -> Unit, onShare: () -> Unit) {
    var open by rememberSaveable(q.id) { mutableStateOf(false) }
    OutlinedCard(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Tag(stringResource(R.string.filter_unit, q.unit), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                Tag(stringResource(R.string.question_marks, q.marks), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                ProbabilityChip(q.rating)
            }
            Text(q.question, style = MaterialTheme.typography.titleSmall)
            AnimatedVisibility(open) {
                Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(q.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(12.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { open = !open }) { Text(stringResource(if (open) R.string.question_hide else R.string.question_show)) }
                Spacer(Modifier.weight(1f))
                IconToggleButton(checked = revising, onCheckedChange = { onRevise() }) {
                    Icon(Icons.Outlined.Style, stringResource(if (revising) R.string.question_revising else R.string.question_revise), tint = if (revising) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconToggleButton(checked = saved, onCheckedChange = { onSave() }) {
                    Icon(
                        if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        stringResource(if (saved) R.string.question_unbookmark else R.string.question_bookmark),
                        tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShare) { Icon(Icons.Outlined.Image, stringResource(R.string.question_share)) }
            }
        }
    }
}

@Composable
internal fun McqsTab(pack: SubjectPack, state: SubjectUiState, actions: SubjectActions) {
    val units = pack.mcqs.groupBy { it.unit }.toSortedMap()
    val acc = state.accuracy.associateBy { it.unit }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { actions.onPractice(pack.subjectId, 0, false, false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.mcq_practise_all)) }
                FilledTonalButton(onClick = { actions.onPractice(pack.subjectId, 0, false, true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.mcq_quick)) }
                if (state.wrongMcqs > 0) {
                    OutlinedButton(onClick = { actions.onPractice(pack.subjectId, 0, true, false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.Replay, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.mcq_retry_wrong, state.wrongMcqs))
                    }
                }
                OutlinedButton(onClick = { actions.onFlashcards(pack.subjectId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Style, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mcq_flashcards))
                }
            }
        }
        items(units.entries.toList(), key = { it.key }) { (unit, list) ->
            val a = acc[unit]
            ElevatedCard(onClick = { actions.onPractice(pack.subjectId, unit, false, false) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.mcq_unit, unit, list.size), style = MaterialTheme.typography.titleSmall)
                    Text(pack.units.firstOrNull { it.number == unit }?.title.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (a != null && a.attempts > 0) {
                        val pct = a.correct * 100 / a.attempts
                        LinearProgressIndicator(progress = { a.correct / a.attempts.toFloat() }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.mcq_accuracy, pct, a.attempts), style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text(stringResource(R.string.mcq_not_tried), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TestsTab(pack: SubjectPack, state: SubjectUiState, actions: SubjectActions) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(pack.mockTests, key = { it.id }) { test ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary)
                        Text(test.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.tests_pattern, test.totalMarks, test.durationMinutes), style = MaterialTheme.typography.bodyMedium)
                    test.patternSource?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Button(onClick = { actions.onMockTest(pack.subjectId, test.id) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.tests_start)) }
                }
            }
        }
        if (state.results.isNotEmpty()) {
            item { Text(stringResource(R.string.tests_history), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp).semantics { heading() }) }
            items(state.results, key = { it.id }) { r ->
                Card(onClick = { actions.onMockResult(r.id) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(r.testTitle) },
                        supportingContent = { Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.finishedAt))) },
                        trailingContent = { Text(stringResource(R.string.tests_result, r.autoMarks + r.selfMarks, r.totalMarks), style = MaterialTheme.typography.titleMedium) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun PracticalsTab(pack: SubjectPack, state: SubjectUiState, actions: SubjectActions) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.practicals_hint, pack.practicalCodes.joinToString().ifEmpty { pack.code }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(pack.practicals, key = { it.id }) { p ->
            val id = ReaderDocs.practicalId(p.id)
            ElevatedCard(onClick = { actions.onOpenReader(pack.subjectId, id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val done = state.progress[id]?.done == true
                    Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.Science, null, tint = MaterialTheme.colorScheme.primary)
                    Text(p.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

package app.studydesk.feature.exams

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.designsystem.component.Tag
import app.studydesk.core.model.ExamBody
import app.studydesk.core.model.ExamSchedule
import app.studydesk.core.model.ExamSession
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy")
private val india = ZoneId.of("Asia/Kolkata")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ExamsScreen(viewModel: ExamsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var tab by rememberSaveable { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val noCalendar = stringResource(R.string.exams_no_calendar)
    val noBrowser = stringResource(R.string.exams_no_browser)
    fun open(intent: Intent, failure: String) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(failure) }
        }
    }
    val openUrl: (String) -> Unit = { open(Intent(Intent.ACTION_VIEW, Uri.parse(it)), noBrowser) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { LargeTopAppBar(title = { Text(stringResource(R.string.exams_title)) }, scrollBehavior = scroll) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                listOf(R.string.exams_mine, R.string.exams_all).forEachIndexed { i, label ->
                    SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(stringResource(label)) }
                }
            }
            when (val s = state) {
                ExamsUiState.Loading -> SkeletonList()
                is ExamsUiState.Ready -> if (tab == 0) {
                    MyExams(s, viewModel, onSeeAll = { tab = 1 }, openUrl = openUrl, addToCalendar = { e -> open(calendarIntent(context, e), noCalendar) })
                } else {
                    AllSheets(s, openUrl)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyExams(
    state: ExamsUiState.Ready,
    viewModel: ExamsViewModel,
    onSeeAll: () -> Unit,
    openUrl: (String) -> Unit,
    addToCalendar: (PaperEntry) -> Unit,
) {
    val showPast by viewModel.showPast.collectAsStateWithLifecycle()
    if (state.course == null) {
        EmptyState(Icons.Outlined.EventBusy, stringResource(R.string.exams_none_title), stringResource(R.string.exams_no_course), actionLabel = stringResource(R.string.exams_see_all), onAction = onSeeAll)
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().toggleable(showPast, role = Role.Switch) { viewModel.showPast.value = it }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.exams_show_past), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = showPast, onCheckedChange = null)
            }
        }
        if (state.mine.isEmpty()) {
            item {
                EmptyState(
                    Icons.Outlined.EventBusy,
                    stringResource(R.string.exams_none_title),
                    stringResource(R.string.exams_none_body),
                    actionLabel = stringResource(R.string.exams_see_all),
                    onAction = onSeeAll,
                )
            }
        } else {
            val sessions = state.mine.map { it.session }.distinctBy { it.id }
            item {
                Text(
                    stringResource(R.string.exams_source, sessions.joinToString { it.lastChecked }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            state.mine.groupBy { it.date }.forEach { (date, entries) ->
                stickyHeader(key = "d-$date") {
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            date.format(dayFormat) + " · " + relativeDay(date, state.today),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (date.isBefore(state.today)) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).semantics { heading() },
                        )
                    }
                }
                items(entries, key = { "${it.session.id}-${it.paper.subjectCode}-${it.paper.date}-${it.paper.start}" }) { e ->
                    PaperCard(e, openUrl, addToCalendar)
                }
            }
        }
    }
}

@Composable
private fun PaperCard(e: PaperEntry, openUrl: (String) -> Unit, addToCalendar: (PaperEntry) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e.paper.subjectCode, style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                StatusTag(e.session.status)
                if (e.paper.paperType == "re-appear") Tag(stringResource(R.string.exams_reappear), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(e.paper.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.exams_paper_time, e.paper.start, e.paper.end) + " · " + (e.body?.shortName ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { addToCalendar(e) }) {
                    Icon(Icons.Outlined.EventAvailable, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.exams_calendar), modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = { openUrl(e.session.officialUrl) }) {
                    Text(stringResource(R.string.exams_official))
                    Icon(Icons.Outlined.OpenInNew, null, Modifier.padding(start = 4.dp).size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AllSheets(state: ExamsUiState.Ready, openUrl: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.schedules.forEach { (body, schedule) ->
            item(key = "b-${schedule.bodyId}") {
                Text(body?.name ?: schedule.bodyId.uppercase(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            }
            items(schedule.sessions, key = { "${schedule.bodyId}-${it.id}" }) { session -> SessionCard(body, schedule, session, openUrl) }
        }
    }
}

@Composable
private fun SessionCard(body: ExamBody?, schedule: ExamSchedule, session: ExamSession, openUrl: (String) -> Unit) {
    var open by rememberSaveable(schedule.bodyId + session.id) { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(session.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                StatusTag(session.status)
            }
            Text(
                listOfNotNull(
                    session.publishedOn?.let { stringResource(R.string.exams_published, LocalDate.parse(it).format(shortDate)) },
                    stringResource(R.string.exams_papers, session.papers.size),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            session.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(stringResource(R.string.exams_source, session.lastChecked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                TextButton(onClick = { openUrl(session.officialUrl) }) {
                    Text(stringResource(R.string.exams_official))
                    Icon(Icons.Outlined.OpenInNew, null, Modifier.padding(start = 4.dp).size(16.dp))
                }
                Box(Modifier.weight(1f))
                IconButton(onClick = { open = !open }) {
                    Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, stringResource(R.string.exams_session_papers))
                }
            }
            AnimatedVisibility(open) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    session.papers.sortedWith(compareBy({ it.date }, { it.start })).forEach { p ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(LocalDate.parse(p.date).format(DateTimeFormatter.ofPattern("d MMM")), style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(width = 52.dp, height = 20.dp))
                            Text("${p.subjectCode} · ${p.title}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(p.start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: String) {
    val (label, container, content) = when (status) {
        "final" -> Triple(R.string.exams_final, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "revised" -> Triple(R.string.exams_revised, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> Triple(R.string.exams_tentative, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    }
    Tag(stringResource(label), container, content)
}

@Composable
private fun relativeDay(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> stringResource(R.string.exams_done)
        days == 0L -> stringResource(R.string.exams_today)
        days == 1L -> stringResource(R.string.exams_tomorrow)
        else -> stringResource(R.string.exams_days_left, days.toInt())
    }
}

/** A calendar event the student confirms in their own calendar app; no calendar permission needed. */
private fun calendarIntent(context: Context, e: PaperEntry): Intent {
    val start = e.date.atTime(LocalTime.parse(e.paper.start)).atZone(india).toInstant().toEpochMilli()
    val end = e.date.atTime(LocalTime.parse(e.paper.end)).atZone(india).toInstant().toEpochMilli()
    val status = context.getString(
        when (e.session.status) {
            "final" -> R.string.exams_final
            "revised" -> R.string.exams_revised
            else -> R.string.exams_tentative
        },
    )
    return Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, "${e.paper.subjectCode} ${e.paper.title}")
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, india.id)
        .putExtra(CalendarContract.Events.DESCRIPTION, context.getString(R.string.exams_calendar_note, e.body?.shortName ?: "", status, e.session.officialUrl))
}

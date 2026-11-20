package app.studydesk.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ElevatedCard
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.studydesk.core.designsystem.component.DecorativeCircles
import app.studydesk.core.designsystem.component.StatusBarIconsOnDark
import app.studydesk.core.designsystem.theme.Accent
import app.studydesk.core.designsystem.theme.Brand
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.InfoBanner
import app.studydesk.core.designsystem.component.SkeletonCards
import app.studydesk.core.designsystem.component.SkeletonLine
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.designsystem.component.Tag
import app.studydesk.core.designsystem.component.UnreviewedTag
import app.studydesk.core.designsystem.component.relativeTime
import app.studydesk.core.designsystem.component.sharedBoundsIfAvailable
import app.studydesk.core.designsystem.component.subjectSharedKey
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.model.Subject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeActions(
    val onOpenSubject: (subjectId: String) -> Unit,
    val onOpenReader: (packId: String, docId: String) -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenExams: () -> Unit,
    val onChangeCourse: () -> Unit,
    val onRevise: () -> Unit,
    val onQuickQuiz: (packId: String) -> Unit,
    val onUpdateApp: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    StatusBarIconsOnDark()
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)) { padding ->
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = viewModel::refresh, modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                HomeUiState.Loading -> Column {
                    HomeHero(actions, courseLabel = null, ready = 0, total = 0, nextExam = null)
                    HomeSkeleton()
                }
                HomeUiState.NoCourse -> Column {
                    HomeHero(actions, courseLabel = null, ready = 0, total = 0, nextExam = null)
                    EmptyState(
                        icon = Icons.Outlined.School,
                        title = stringResource(R.string.home_no_course_title),
                        message = stringResource(R.string.home_no_course_body),
                        actionLabel = stringResource(R.string.home_pick_course),
                        onAction = actions.onChangeCourse,
                    )
                }
                is HomeUiState.Ready -> HomeContent(s, viewModel.visibility, actions)
            }
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Ready, visibility: ContentVisibility, actions: HomeActions) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            HomeHero(
                actions = actions,
                courseLabel = state.courseLabel,
                ready = state.subjects.count { visibility.canOpen(it) },
                total = state.subjects.size,
                nextExam = state.upcoming.firstOrNull(),
            )
        }
        item {
            InfoBanner(
                visible = state.sync.appUpdateRequired,
                message = stringResource(app.studydesk.core.designsystem.R.string.sync_update_app),
                icon = Icons.Outlined.SystemUpdate,
                actionLabel = stringResource(R.string.home_update_app),
                onAction = actions.onUpdateApp,
            )
            InfoBanner(
                visible = state.sync.offline && !state.sync.appUpdateRequired,
                message = (state.sync.lastUpdatedAt ?: state.sync.lastCheckedAt)
                    ?.let { stringResource(R.string.home_offline, relativeTime(it)) }
                    ?: stringResource(R.string.home_offline_bundled),
            )
        }
        item { SectionTitle(stringResource(R.string.home_your_subjects)) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.subjects, key = { it.id }) { subject ->
                    SubjectCard(subject, visibility.canOpen(subject)) { actions.onOpenSubject(subject.pack ?: subject.id) }
                }
            }
        }
        if (state.recent.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.home_continue)) }
            items(state.recent, key = { "${it.packId}/${it.docId}" }) { p ->
                val accent = Brand.subject(p.packId)
                ElevatedCard(
                    onClick = { actions.onOpenReader(p.packId, p.docId) },
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(46.dp).clip(MaterialTheme.shapes.medium).background(accent.gradient),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = Color.White) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(p.subjectTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            LinearProgressIndicator(
                                progress = { p.fraction },
                                color = accent.solid,
                                trackColor = accent.tint(),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(CircleShape),
                            )
                        }
                        Text(
                            stringResource(R.string.home_percent_done, (p.fraction * 100).toInt()),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionTile(
                    accent = Accent(Color(0xFFD97706), Color(0xFFF59E0B), Icons.Outlined.Style),
                    title = stringResource(R.string.home_revise_title),
                    body = if (state.dueCards > 0) pluralStringResource(R.plurals.home_revise_due, state.dueCards, state.dueCards)
                    else stringResource(R.string.home_revise_none),
                    action = stringResource(R.string.home_revise_start),
                    enabled = state.dueCards > 0,
                    onClick = actions.onRevise,
                    modifier = Modifier.weight(1f),
                )
                state.quizSubject?.let { q ->
                    ActionTile(
                        accent = Accent(Color(0xFF7C3AED), Color(0xFF6366F1), Icons.Outlined.Bolt),
                        title = stringResource(R.string.home_quiz_title),
                        body = stringResource(R.string.home_quiz_body, q.code),
                        action = stringResource(R.string.home_quiz_start),
                        enabled = true,
                        onClick = { actions.onQuickQuiz(q.pack ?: q.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_next_exams),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = actions.onOpenExams) { Text(stringResource(R.string.home_see_all)) }
            }
        }
        if (state.upcoming.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_no_exams),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        } else {
            items(state.upcoming, key = { "${it.session.id}-${it.paper.subjectCode}-${it.paper.date}" }) { ExamRow(it, actions.onOpenExams) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp).semantics { heading() },
    )
}

@Composable
private fun SubjectCard(subject: Subject, ready: Boolean, onClick: () -> Unit) {
    val accent = Brand.subject(subject.pack ?: subject.id)
    val top = if (ready) accent.gradient else Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceContainerHigh),
    )
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.width(176.dp).height(196.dp).sharedBoundsIfAvailable(subjectSharedKey(subject.pack ?: subject.id)),
    ) {
        Box(Modifier.fillMaxWidth().height(78.dp).background(top)) {
            DecorativeCircles(Modifier.matchParentSize(), alpha = if (ready) 0.16f else 0.4f)
            Icon(
                accent.icon,
                contentDescription = null,
                tint = if (ready) Color.White else MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp).size(30.dp),
            )
            Text(
                subject.code,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (ready) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
        }
        Column(Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                subject.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (ready) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            when {
                ready && subject.reviewed == false -> UnreviewedTag()
                ready -> Tag(stringResource(R.string.home_ready), accent.tint(0.16f), accent.solid)
                else -> Tag(
                    stringResource(R.string.home_coming_soon),
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    accent: Accent,
    title: String,
    body: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent.gradient), contentAlignment = Alignment.Center) {
                Icon(accent.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = accent.solid, contentColor = Color.White),
            ) { Text(action) }
        }
    }
}

@Composable
private fun ExamRow(item: UpcomingPaper, onClick: () -> Unit) {
    val dayLabel = pluralStringResource(R.plurals.home_days_label, item.daysLeft.toInt())
    val date = LocalDate.parse(item.paper.date).format(DateTimeFormatter.ofPattern("EEE d MMM"))
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CountdownRing(item.daysLeft, Modifier.semantics { contentDescription = "${item.daysLeft} $dayLabel" })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.paper.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.home_exam_time, date, item.paper.start, item.paper.end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.session.isTentative) {
                Tag(stringResource(R.string.home_tentative), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

/** Days left inside a ring that fills as the exam gets closer (full at 0, empty at 30+). */
@Composable
private fun CountdownRing(daysLeft: Long, modifier: Modifier = Modifier) {
    val target = (1f - daysLeft.coerceIn(0, 30) / 30f)
    val progress = remember { Animatable(0f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(target) { if (reduce) progress.snapTo(target) else progress.animateTo(target) }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val color = if (daysLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(56.dp)) {
            val stroke = 5.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress.value, false, topLeft, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (daysLeft == 0L) {
                Text(stringResource(R.string.home_today), style = MaterialTheme.typography.labelSmall, color = color)
            } else {
                Text(daysLeft.toString(), style = MaterialTheme.typography.titleMedium, color = color)
                Text(pluralStringResource(R.plurals.home_days_label, daysLeft.toInt()), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Gradient header: greeting, course, how many subjects are ready, days to the next exam. */
@Composable
private fun HomeHero(actions: HomeActions, courseLabel: String?, ready: Int, total: Int, nextExam: UpcomingPaper?) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brand.heroGradient),
    ) {
        DecorativeCircles(Modifier.matchParentSize(), alpha = 0.10f)
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = actions.onOpenSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.home_search), tint = Color.White) }
                IconButton(onClick = actions.onOpenSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.home_settings), tint = Color.White) }
            }
            if (courseLabel != null) {
                Text(
                    courseLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.padding(end = 12.dp).semantics { heading() },
                )
                Row(Modifier.padding(top = 16.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroStat(
                        value = stringResource(R.string.home_stat_ready_value, ready, total),
                        label = stringResource(R.string.home_stat_ready),
                        modifier = Modifier.weight(1f),
                    )
                    if (nextExam != null) {
                        HeroStat(
                            value = if (nextExam.daysLeft == 0L) stringResource(R.string.home_today) else nextExam.daysLeft.toString(),
                            label = if (nextExam.daysLeft == 0L) stringResource(R.string.home_stat_exam_today)
                            else pluralStringResource(R.plurals.home_stat_exam, nextExam.daysLeft.toInt()),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Surface(
                    onClick = actions.onChangeCourse,
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.home_change_course_long), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = 0.14f), contentColor = Color.White, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonLine(height = 18.dp, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(0.5f))
        SkeletonCards()
        SkeletonList(rows = 3)
    }
}

@Composable
private fun greeting(): String {
    val hour = LocalTime.now().hour
    return stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
}

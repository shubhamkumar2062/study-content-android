package app.studydesk.feature.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.designsystem.theme.Brand
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.model.Catalog
import app.studydesk.core.model.CourseSelection

/**
 * Stream → course → syllabus (only when there are two) → semester/year, one step per page,
 * sliding forward and back. System back goes one step back before leaving.
 */
@Composable
internal fun CoursePicker(
    catalog: Catalog,
    visibility: ContentVisibility,
    onPicked: (CourseSelection) -> Unit,
    onExit: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var streamId by rememberSaveable { mutableStateOf<String?>(null) }
    var programmeId by rememberSaveable { mutableStateOf<String?>(null) }
    var schemeId by rememberSaveable { mutableStateOf<String?>(null) }

    val stream = catalog.streams.firstOrNull { it.id == streamId }
    val programme = stream?.programmes?.firstOrNull { it.id == programmeId }
    val scheme = programme?.schemes?.firstOrNull { it.id == schemeId }
    val depth = when {
        scheme != null -> 3
        programme != null -> 2
        stream != null -> 1
        else -> 0
    }

    fun back() {
        when (depth) {
            3 -> {
                schemeId = null
                if (programme!!.pickableSchemes().size == 1) programmeId = null
            }
            2 -> programmeId = null
            1 -> streamId = null
            else -> onExit?.invoke()
        }
    }
    BackHandler(enabled = depth > 0 || onExit != null) { back() }

    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = depth,
        transitionSpec = {
            if (reduce) {
                fadeIn() togetherWith fadeOut()
            } else {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
            }
        },
        label = "course-picker",
        modifier = modifier.fillMaxSize(),
    ) { step ->
        when (step) {
            0 -> Step(stringResource(R.string.picker_title_stream), contentPadding) {
                items(catalog.streamsAvailableFirst(), key = { it.id }) { s ->
                    val n = s.pickableProgrammes().size
                    OptionCard(
                        title = s.name,
                        subtitle = if (n > 0) pluralStringResourceSafe(n) else null,
                        accent = Brand.stream(s.id),
                        enabled = n > 0,
                        onClick = { streamId = s.id },
                    )
                }
            }
            1 -> Step(stringResource(R.string.picker_title_programme), contentPadding) {
                items(stream?.programmes.orEmpty(), key = { it.id }) { p ->
                    val pickable = p.pickableSchemes()
                    OptionCard(
                        title = p.name,
                        subtitle = null,
                        accent = stream?.let { Brand.stream(it.id) },
                        enabled = pickable.isNotEmpty(),
                        onClick = {
                            programmeId = p.id
                            if (pickable.size == 1) schemeId = pickable.single().id
                        },
                    )
                }
            }
            2 -> Step(stringResource(R.string.picker_title_scheme), contentPadding) {
                items(programme?.pickableSchemes().orEmpty(), key = { it.id }) { sc ->
                    OptionCard(title = sc.name, subtitle = sc.note, accent = stream?.let { Brand.stream(it.id) }, enabled = true, onClick = { schemeId = sc.id })
                }
            }
            else -> {
                val kind = stringResource(if (scheme?.termKind == "year") R.string.term_kind_year else R.string.term_kind_semester)
                Step(stringResource(R.string.picker_title_term, kind), contentPadding) {
                    items(scheme?.terms.orEmpty(), key = { it.id }) { t ->
                        OptionCard(
                            title = t.name,
                            subtitle = stringResource(R.string.picker_subjects_ready, t.readyCount(visibility), t.theoryCount()),
                            accent = stream?.let { Brand.stream(it.id) },
                            label = t.number.toString(),
                            progress = if (t.theoryCount() > 0) t.readyCount(visibility).toFloat() / t.theoryCount() else null,
                            enabled = true,
                            onClick = { onPicked(CourseSelection(stream!!.id, programme!!.id, scheme!!.id, t.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pluralStringResourceSafe(count: Int): String = pluralStringResource(R.plurals.picker_programmes, count, count)

@Composable
private fun Step(
    title: String,
    contentPadding: PaddingValues,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
                )
            }
            content()
        }
    }
}

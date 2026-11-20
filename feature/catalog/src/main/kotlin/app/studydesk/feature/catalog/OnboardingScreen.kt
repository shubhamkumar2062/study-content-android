package app.studydesk.feature.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.ErrorState
import app.studydesk.core.designsystem.component.SkeletonList
import app.studydesk.core.designsystem.theme.Motion
import app.studydesk.core.designsystem.theme.Accent
import app.studydesk.core.designsystem.theme.Brand
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import app.studydesk.core.model.CourseSelection
import kotlinx.coroutines.launch

private enum class OnboardingStep { Intro, Course, Disclaimer }

@Composable
internal fun OnboardingScreen(onFinished: () -> Unit, viewModel: CoursePickerViewModel = hiltViewModel()) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Intro) }
    var picked by rememberSaveable(stateSaver = courseSaver) { mutableStateOf<CourseSelection?>(null) }

    Scaffold { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
            label = "onboarding",
        ) { current ->
            when (current) {
                OnboardingStep.Intro -> IntroPager(onDone = { step = OnboardingStep.Course }, modifier = Modifier.padding(padding))
                OnboardingStep.Course -> when (val state = catalog) {
                    CatalogUiState.Loading -> SkeletonList(modifier = Modifier.padding(padding))
                    CatalogUiState.Unreadable -> ErrorState(
                        title = stringResource(R.string.loading_error_title),
                        message = stringResource(R.string.loading_error_body),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = viewModel::retry,
                    )
                    is CatalogUiState.Ready -> CoursePicker(
                        catalog = state.catalog,
                        visibility = viewModel.visibility,
                        onPicked = {
                            picked = it
                            step = OnboardingStep.Disclaimer
                        },
                        onExit = { step = OnboardingStep.Intro },
                        contentPadding = padding,
                    )
                }
                OnboardingStep.Disclaimer -> {
                    BackHandler { step = OnboardingStep.Course }
                    Disclaimer(
                        onAccept = { picked?.let { viewModel.save(it, finishOnboarding = true, onSaved = onFinished) } },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

private data class IntroPage(val accent: Accent, val title: Int, val body: Int)

private val pages = listOf(
    IntroPage(Accent(Color(0xFF4338CA), Color(0xFF7C3AED), Icons.Outlined.AutoStories), R.string.intro_1_title, R.string.intro_1_body),
    IntroPage(Accent(Color(0xFF0F766E), Color(0xFF0EA5E9), Icons.Outlined.Quiz), R.string.intro_2_title, R.string.intro_2_body),
    IntroPage(Accent(Color(0xFFD97706), Color(0xFFE11D48), Icons.Outlined.CloudDone), R.string.intro_3_title, R.string.intro_3_body),
)

@Composable
private fun IntroPager(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(stringResource(R.string.intro_skip)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(240.dp).background(page.accent.tint(0.08f), CircleShape))
                    Box(Modifier.size(190.dp).background(page.accent.tint(0.14f), CircleShape))
                    Box(
                        Modifier.size(140.dp).shadow(16.dp, CircleShape, ambientColor = page.accent.start, spotColor = page.accent.start)
                            .background(page.accent.gradient, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(page.accent.icon, null, Modifier.size(64.dp), tint = Color.White)
                    }
                }
                Spacer(Modifier.height(40.dp))
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val accent = pages[pager.currentPage].accent
            val buttonColor by animateColorAsState(accent.solid, label = "intro-button-color")
            PageDots(count = pages.size, current = pager.currentPage, color = buttonColor)
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 52.dp),
            ) {
                AnimatedContent(targetState = last, label = "intro-button") { isLast ->
                    Text(stringResource(if (isLast) R.string.intro_get_started else R.string.intro_next))
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, Motion.tweenOrSnap(), label = "dot")
            val color by animateColorAsState(
                if (selected) color else MaterialTheme.colorScheme.outlineVariant,
                label = "dot-color",
            )
            Box(Modifier.height(8.dp).width(width).background(color, CircleShape))
        }
    }
}

@Composable
private fun Disclaimer(onAccept: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(88.dp).background(Brand.heroGradient, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(44.dp), tint = Color.White)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.disclaimer_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).widthIn(max = 480.dp)) {
            Text(stringResource(R.string.disclaimer_accept))
        }
    }
}

internal val courseSaver = androidx.compose.runtime.saveable.Saver<CourseSelection?, List<String>>(
    save = { c -> c?.let { listOf(it.streamId, it.programmeId, it.schemeId, it.termId) } ?: emptyList() },
    restore = { list -> if (list.size == 4) CourseSelection(list[0], list[1], list[2], list[3]) else null },
)

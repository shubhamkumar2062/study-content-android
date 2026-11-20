package app.studydesk.feature.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.EmptyState
import app.studydesk.core.designsystem.component.ProbabilityChip
import app.studydesk.core.designsystem.component.SkeletonDocument
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.designsystem.theme.StudyTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlashcardsScreen(onBack: () -> Unit, viewModel: FlashcardsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(if (viewModel.route.packId == null) R.string.cards_title_due else R.string.cards_title))
                        if (state.queue.isNotEmpty()) Text(stringResource(R.string.cards_progress, state.queue.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.practice_back)) } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> SkeletonDocument()
                !state.started -> EmptyState(
                    icon = Icons.Outlined.Style,
                    title = stringResource(R.string.cards_empty_title),
                    message = stringResource(R.string.cards_empty_body),
                    actionLabel = stringResource(R.string.result_done),
                    onAction = onBack,
                )
                state.finished -> EmptyState(
                    icon = Icons.Outlined.EmojiEvents,
                    title = stringResource(R.string.cards_done_title),
                    message = stringResource(R.string.cards_done_body, state.known, state.again),
                    actionLabel = stringResource(R.string.result_done),
                    onAction = onBack,
                )
                else -> Deck(state, viewModel)
            }
        }
    }
}

@Composable
private fun Deck(state: FlashcardsUiState, viewModel: FlashcardsViewModel) {
    val card = state.current ?: return
    val view = LocalView.current
    val reduce = LocalReduceMotion.current
    fun decide(knew: Boolean) {
        view.answerHaptic(knew)
        viewModel.answer(knew)
    }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.cards_swipe_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        AnimatedContent(
            targetState = card.packId + card.question.id + state.known + state.again,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
            label = "card",
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
        ) { _ ->
            FlipCard(card, reduce, onDecide = ::decide)
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { decide(false) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Icon(Icons.Rounded.Replay, null)
                Text(stringResource(R.string.cards_again), modifier = Modifier.padding(start = 8.dp))
            }
            Button(onClick = { decide(true) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Icon(Icons.Rounded.Check, null)
                Text(stringResource(R.string.cards_know), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun FlipCard(card: Flashcard, reduce: Boolean, onDecide: (Boolean) -> Unit) {
    var flipped by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, if (reduce) tween(0) else tween(450), label = "flip")
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val know = stringResource(R.string.cards_know)
    val again = stringResource(R.string.cards_again)
    val c = StudyTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = drag.value
                rotationZ = drag.value / 40f
            }
            .pointerInput(card) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                drag.value > 260f -> onDecide(true)
                                drag.value < -260f -> onDecide(false)
                                else -> drag.animateTo(0f)
                            }
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { drag.snapTo(drag.value + amount) }
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(know) { onDecide(true); true },
                    CustomAccessibilityAction(again) { onDecide(false); true },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .clickable { flipped = !flipped },
        ) {
            val showBack = rotation > 90f
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(card.subjectTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (!showBack) {
                        ProbabilityChip(card.question.rating)
                        Text(card.question.question, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.cards_tap_flip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(card.question.question, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text(card.question.answer, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        // Swipe hints fade in as the card moves.
        if (abs(drag.value) > 20f) {
            Text(
                if (drag.value > 0) know else again,
                color = if (drag.value > 0) c.correct else c.wrong,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).graphicsLayer { alpha = (abs(drag.value) / 260f).coerceIn(0f, 1f) },
            )
        }
    }
}


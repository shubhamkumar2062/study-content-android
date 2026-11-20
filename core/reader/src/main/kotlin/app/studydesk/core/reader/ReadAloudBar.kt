package app.studydesk.core.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studydesk.core.reader.readaloud.ReadAloudState

private val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f, 0.5f)

@Composable
internal fun ReadAloudBar(
    state: ReadAloudState,
    onPlayPause: () -> Unit,
    onSkip: (Int) -> Unit,
    onRate: (Float) -> Unit,
    onStop: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = {
                val i = speeds.indexOfFirst { kotlin.math.abs(it - state.rate) < 0.01f }
                onRate(speeds[(i + 1) % speeds.size])
            }) { Text(stringResource(R.string.reader_tts_speed, state.rate.toString().removeSuffix(".0"))) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSkip(-1) }) { Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.reader_tts_back)) }
                FilledIconButton(onClick = onPlayPause) {
                    AnimatedContent(state.status == ReadAloudState.Status.Speaking, label = "tts") { speaking ->
                        if (speaking) Icon(Icons.Rounded.Pause, stringResource(R.string.reader_tts_pause))
                        else Icon(Icons.Rounded.PlayArrow, stringResource(R.string.reader_tts_play))
                    }
                }
                IconButton(onClick = { onSkip(1) }) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.reader_tts_forward)) }
            }
            IconButton(onClick = onStop) { Icon(Icons.Rounded.Close, stringResource(R.string.reader_tts_stop)) }
        }
    }
}

package app.studydesk.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studydesk.core.designsystem.R
import app.studydesk.core.designsystem.theme.StudyTheme
import app.studydesk.core.model.Probability

/** Small coloured label: "High chance", "Medium chance", "Low chance". */
@Composable
fun ProbabilityChip(probability: Probability, modifier: Modifier = Modifier) {
    val c = StudyTheme.colors
    val (bg, fg, label) = when (probability) {
        Probability.High -> Triple(c.high, c.onHigh, R.string.probability_high)
        Probability.Medium -> Triple(c.medium, c.onMedium, R.string.probability_medium)
        Probability.Low -> Triple(c.low, c.onLow, R.string.probability_low)
    }
    Tag(stringResource(label), bg, fg, modifier)
}

@Composable
fun Tag(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

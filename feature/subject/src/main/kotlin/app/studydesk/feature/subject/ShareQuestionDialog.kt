package app.studydesk.feature.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.studydesk.core.designsystem.component.ProbabilityChip
import app.studydesk.core.designsystem.component.Sharing
import app.studydesk.core.model.Question
import app.studydesk.core.model.SubjectPack
import kotlinx.coroutines.launch

/** Shows the question as a card and shares exactly that card as a picture. */
@Composable
internal fun ShareQuestionDialog(question: Question, pack: SubjectPack, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.question_share_title), style = MaterialTheme.typography.titleLarge)
                // The card that becomes the image.
                Column(
                    Modifier
                        .widthIn(max = 420.dp)
                        .drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                        }
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(pack.code, style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.filter_unit, question.unit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.question_marks, question.marks), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ProbabilityChip(question.rating)
                    Text(question.question, style = MaterialTheme.typography.titleMedium)
                    Text(question.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.question_card_footer, pack.title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = {
                        scope.launch {
                            val bitmap = layer.toImageBitmap().asAndroidBitmap()
                            Sharing.shareImage(context, bitmap, "${pack.code}-${question.id}.png", question.question)
                            onDismiss()
                        }
                    }) { Text(stringResource(R.string.question_share_button)) }
                }
            }
        }
    }
}

package app.studydesk.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.designsystem.component.Tag
import app.studydesk.core.designsystem.component.UnreviewedTag
import app.studydesk.core.designsystem.component.sharedBoundsIfAvailable
import app.studydesk.core.designsystem.component.subjectSharedKey
import app.studydesk.core.model.Subject

/** Where tapping a subject goes: practicals open their theory subject on the Practicals tab. */
fun Subject.destination(): Pair<String, String?> =
    if (isPractical && pack != null) pack!! to "practicals" else (pack ?: id) to null

@Composable
internal fun SubjectRow(
    subject: Subject,
    visibility: ContentVisibility,
    onClick: () -> Unit,
    supporting: String? = null,
) {
    val ready = visibility.canOpen(subject)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .then(if (subject.isPractical) Modifier else Modifier.sharedBoundsIfAvailable(subjectSharedKey(subject.pack ?: subject.id)))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subject.code,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
                kindLabel(subject.kind)?.let {
                    Tag(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                // Beside the code rather than at the row's end, so the title keeps the full width.
                if (ready && subject.reviewed == false) UnreviewedTag()
            }
            Text(subject.title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            ready -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.subject_ready),
                tint = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.subject_coming_soon),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun kindLabel(kind: String): String? = when (kind) {
    "practical" -> stringResource(R.string.subject_kind_practical)
    "elective" -> stringResource(R.string.subject_kind_elective)
    "project" -> stringResource(R.string.subject_kind_project)
    "training" -> stringResource(R.string.subject_kind_training)
    else -> null
}

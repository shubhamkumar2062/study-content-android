package app.studydesk.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import app.studydesk.core.designsystem.theme.Accent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.designsystem.component.Tag
import app.studydesk.core.model.Programme
import app.studydesk.core.model.Scheme
import app.studydesk.core.model.Stream
import app.studydesk.core.model.Term

internal fun Scheme.isPickable() = isListed && terms.isNotEmpty()
internal fun Programme.pickableSchemes() = schemes.filter { it.isPickable() }
internal fun Stream.pickableProgrammes() = programmes.filter { it.pickableSchemes().isNotEmpty() }

/** Streams the student can pick come first; the catalog's own order is kept otherwise. */
internal fun app.studydesk.core.model.Catalog.streamsAvailableFirst() =
    streams.sortedByDescending { it.pickableProgrammes().isNotEmpty() }

internal fun Term.readyCount(visibility: ContentVisibility) =
    subjects.count { !it.isPractical && visibility.canOpen(it) }

internal fun Term.theoryCount() = subjects.count { !it.isPractical }

/**
 * A big tappable choice: a coloured tile (the stream's icon, or [label] such as a semester
 * number), title, subtitle, an optional progress bar, and "Coming soon" when it can't be picked yet.
 */
@Composable
internal fun OptionCard(
    title: String,
    subtitle: String?,
    accent: Accent?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    progress: Float? = null,
) {
    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .semantics { role = Role.Button },
    ) {
        Row(
            Modifier.padding(14.dp).alpha(if (enabled) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (accent != null) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (enabled) accent.gradient else SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest)),
                    contentAlignment = Alignment.Center,
                ) {
                    val tint = if (enabled) Color.White else MaterialTheme.colorScheme.outline
                    if (label != null) Text(label, style = MaterialTheme.typography.titleLarge, color = tint)
                    else Icon(accent.icon, contentDescription = null, tint = tint)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (progress != null && enabled) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = accent?.solid ?: MaterialTheme.colorScheme.primary,
                        trackColor = accent?.tint() ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                        drawStopIndicator = {},
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(5.dp).clip(CircleShape),
                    )
                }
            }
            if (enabled) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Tag(
                    stringResource(R.string.picker_coming_soon),
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

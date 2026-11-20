package app.studydesk.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.relativeTime
import app.studydesk.core.designsystem.component.syncResultMessage
import app.studydesk.core.model.AppTheme
import app.studydesk.core.model.ReaderSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onChangeCourse: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var pending by remember { mutableStateOf<app.studydesk.core.content.SyncResult?>(null) }
    var tooSoon by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Synced -> pending = event.result
                SettingsEvent.TooSoon -> tooSoon = true
            }
        }
    }
    pending?.let { result ->
        val message = syncResultMessage(result)
        LaunchedEffect(result) {
            message?.let { snackbar.showSnackbar(it) }
            pending = null
        }
    }
    if (tooSoon) {
        val message = stringResource(R.string.settings_check_cooldown)
        LaunchedEffect(Unit) {
            snackbar.showSnackbar(message)
            tooSoon = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.settings_back)) } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Section(R.string.settings_section_course)
            NavRow(Icons.Outlined.School, stringResource(R.string.settings_change_course), state.courseLabel ?: stringResource(R.string.settings_no_course), onChangeCourse)

            Section(R.string.settings_section_look)
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            val themes = listOf(AppTheme.System to R.string.settings_theme_system, AppTheme.Light to R.string.settings_theme_light, AppTheme.Dark to R.string.settings_theme_dark)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                themes.forEachIndexed { i, (theme, label) ->
                    SegmentedButton(
                        selected = state.settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                        shape = SegmentedButtonDefaults.itemShape(i, themes.size),
                    ) { Text(stringResource(label)) }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(stringResource(R.string.settings_dynamic_color), null, state.settings.dynamicColor, viewModel::setDynamicColor)
            }

            Section(R.string.settings_section_reading)
            TextScaleRow(state.settings.reader.textScale, viewModel::setTextScale)
            SwitchRow(stringResource(R.string.settings_serif), null, state.settings.reader.serif, viewModel::setSerif)
            SwitchRow(stringResource(R.string.settings_keep_screen_on), null, state.settings.reader.keepScreenOn, viewModel::setKeepScreenOn)

            Section(R.string.settings_section_data)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = !state.sync.running, onClick = viewModel::checkNow).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AnimatedContent(state.sync.running, label = "sync-icon") { running ->
                    if (running) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.CloudSync, null)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (state.sync.running) R.string.settings_checking else R.string.settings_check_now), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_last_checked, relativeTime(state.sync.lastCheckedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.sync.lastUpdatedAt != null) {
                        Text(
                            stringResource(R.string.settings_last_updated, relativeTime(state.sync.lastUpdatedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.sync.contentGeneratedAt?.let {
                        Text(
                            stringResource(R.string.settings_content_version, it.take(16).replace('T', ' ')),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SwitchRow(
                stringResource(R.string.settings_wifi_images),
                stringResource(R.string.settings_wifi_images_hint),
                state.settings.imagesWifiOnly,
                viewModel::setImagesWifiOnly,
            )

            Section(R.string.settings_section_about)
            NavRow(Icons.Outlined.Info, stringResource(R.string.settings_about), null, onOpenAbout)
        }
    }
}

@Composable
private fun Section(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TextScaleRow(scale: Float, onChange: (Float) -> Unit) {
    var value by remember(scale) { mutableFloatStateOf(scale) }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.settings_text_size), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.settings_text_size_value, (value * 100).roundToInt()), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onChange(value) },
            valueRange = ReaderSettings.MIN_SCALE..ReaderSettings.MAX_SCALE,
            steps = 11,
        )
    }
}


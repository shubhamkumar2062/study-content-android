package app.studydesk.feature.catalog

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.component.ErrorState
import app.studydesk.core.designsystem.component.SkeletonList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChangeCourseScreen(onDone: () -> Unit, viewModel: CoursePickerViewModel = hiltViewModel()) {
    val state by viewModel.catalog.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_course_title)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.picker_back)) } },
            )
        },
    ) { padding ->
        when (val s = state) {
            CatalogUiState.Loading -> SkeletonList(modifier = Modifier.padding(padding))
            CatalogUiState.Unreadable -> ErrorState(
                title = stringResource(R.string.loading_error_title),
                message = stringResource(R.string.loading_error_body),
                actionLabel = stringResource(R.string.action_retry),
                onAction = viewModel::retry,
                modifier = Modifier.padding(padding),
            )
            is CatalogUiState.Ready -> CoursePicker(
                catalog = s.catalog,
                visibility = viewModel.visibility,
                onPicked = { viewModel.save(it, finishOnboarding = false, onSaved = onDone) },
                onExit = onDone,
                contentPadding = padding,
            )
        }
    }
}

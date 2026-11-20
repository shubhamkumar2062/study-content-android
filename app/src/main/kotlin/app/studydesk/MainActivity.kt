package app.studydesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studydesk.core.designsystem.theme.StudyTheme
import app.studydesk.core.designsystem.theme.ThemeMode
import app.studydesk.core.model.AppTheme
import app.studydesk.navigation.StudyDeskApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep the splash only until the saved settings are read (a few milliseconds).
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            if (state is MainUiState.Ready) {
                val mode = when (state.settings.theme) {
                    AppTheme.System -> ThemeMode.System
                    AppTheme.Light -> ThemeMode.Light
                    AppTheme.Dark -> ThemeMode.Dark
                }
                StudyTheme(mode = mode, dynamicColor = state.settings.dynamicColor) {
                    StudyDeskApp(onboarded = state.settings.onboarded)
                }
            }
        }
    }
}

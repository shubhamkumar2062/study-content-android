package app.studydesk.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.settings.AboutScreen
import app.studydesk.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable data object SettingsRoute
@Serializable data object AboutRoute

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit, onChangeCourse: () -> Unit, onOpenAbout: () -> Unit) {
    screen<SettingsRoute> { SettingsScreen(onBack, onChangeCourse, onOpenAbout) }
}

fun NavGraphBuilder.aboutScreen(onBack: () -> Unit) {
    screen<AboutRoute> { AboutScreen(onBack) }
}

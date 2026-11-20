package app.studydesk.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.home.HomeActions
import app.studydesk.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

fun NavGraphBuilder.homeScreen(actions: HomeActions) {
    screen<HomeRoute> { HomeScreen(actions) }
}

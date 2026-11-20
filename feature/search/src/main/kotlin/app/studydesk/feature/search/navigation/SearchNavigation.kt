package app.studydesk.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.search.SearchActions
import app.studydesk.feature.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable data object SearchRoute

fun NavGraphBuilder.searchScreen(actions: SearchActions) {
    screen<SearchRoute> { SearchScreen(actions) }
}

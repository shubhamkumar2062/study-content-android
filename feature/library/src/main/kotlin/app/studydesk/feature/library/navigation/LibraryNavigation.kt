package app.studydesk.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.library.LibraryActions
import app.studydesk.feature.library.LibraryScreen
import kotlinx.serialization.Serializable

@Serializable data object LibraryRoute

fun NavGraphBuilder.libraryScreen(actions: LibraryActions) {
    screen<LibraryRoute> { LibraryScreen(actions) }
}

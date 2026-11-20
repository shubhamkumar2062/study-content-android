package app.studydesk.feature.catalog.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.catalog.BrowseScreen
import app.studydesk.feature.catalog.ChangeCourseScreen
import app.studydesk.feature.catalog.OnboardingScreen
import app.studydesk.feature.catalog.ProgrammeScreen
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object BrowseRoute
@Serializable data class ProgrammeRoute(val streamId: String, val programmeId: String)
@Serializable data object ChangeCourseRoute

fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    screen<OnboardingRoute> { OnboardingScreen(onFinished) }
}

fun NavGraphBuilder.browseScreen(
    onOpenCourse: (streamId: String, programmeId: String) -> Unit,
    onOpenSubject: (subjectId: String, tab: String?) -> Unit,
) {
    screen<BrowseRoute> { BrowseScreen(onOpenCourse, onOpenSubject) }
}

fun NavGraphBuilder.programmeScreen(onBack: () -> Unit, onOpenSubject: (subjectId: String, tab: String?) -> Unit) {
    screen<ProgrammeRoute> { entry ->
        val route = entry.toRoute<ProgrammeRoute>()
        ProgrammeScreen(route.streamId, route.programmeId, onBack, onOpenSubject)
    }
}

fun NavGraphBuilder.changeCourseScreen(onDone: () -> Unit) {
    screen<ChangeCourseRoute> { ChangeCourseScreen(onDone) }
}

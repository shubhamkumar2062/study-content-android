package app.studydesk.feature.exams.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.exams.ExamsScreen
import kotlinx.serialization.Serializable

@Serializable data object ExamsRoute

fun NavGraphBuilder.examsScreen() {
    screen<ExamsRoute> { ExamsScreen() }
}

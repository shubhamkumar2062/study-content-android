package app.studydesk.feature.subject.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.subject.SubjectActions
import app.studydesk.feature.subject.SubjectScreen
import kotlinx.serialization.Serializable

/** @param tab "notes", "questions", "mcqs", "tests" or "practicals"; null opens Notes. */
@Serializable
data class SubjectRoute(val subjectId: String, val tab: String? = null)

fun NavGraphBuilder.subjectScreen(actions: SubjectActions) {
    screen<SubjectRoute> { SubjectScreen(actions) }
}

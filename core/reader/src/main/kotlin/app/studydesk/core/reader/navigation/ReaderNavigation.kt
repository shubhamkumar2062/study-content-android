package app.studydesk.core.reader.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.core.reader.ReaderScreen
import kotlinx.serialization.Serializable

/** @param blockIndex block to jump to (from search or a bookmark); -1 opens at the saved position. */
@Serializable
data class ReaderRoute(val packId: String, val docId: String, val blockIndex: Int = -1)

fun NavGraphBuilder.readerScreen(onBack: () -> Unit, onOpenQuestions: (packId: String) -> Unit) {
    screen<ReaderRoute> { ReaderScreen(onBack = onBack, onOpenQuestions = onOpenQuestions) }
}

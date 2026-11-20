package app.studydesk.feature.practice.navigation

import androidx.navigation.NavGraphBuilder
import app.studydesk.core.designsystem.component.screen
import app.studydesk.feature.practice.FlashcardsScreen
import app.studydesk.feature.practice.McqScreen
import app.studydesk.feature.practice.MockResultScreen
import app.studydesk.feature.practice.MockTestScreen
import kotlinx.serialization.Serializable

/** @param unit 0 for all units. */
@Serializable data class McqRoute(val packId: String, val unit: Int = 0, val wrongOnly: Boolean = false, val quick: Boolean = false)

/** @param packId null: every due card across subjects ("Revise today"). */
@Serializable data class FlashcardsRoute(val packId: String? = null)

@Serializable data class MockTestRoute(val packId: String, val testId: String)

@Serializable data class MockResultRoute(val resultId: Long)

fun NavGraphBuilder.practiceScreens(
    onBack: () -> Unit,
    onRetryWrong: (packId: String) -> Unit,
    onMockFinished: (resultId: Long) -> Unit,
) {
    screen<McqRoute> { McqScreen(onBack, onRetryWrong) }
    screen<FlashcardsRoute> { FlashcardsScreen(onBack) }
    screen<MockTestRoute> { MockTestScreen(onBack, onMockFinished) }
    screen<MockResultRoute> { MockResultScreen(onBack) }
}

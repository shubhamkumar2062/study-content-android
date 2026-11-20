package app.studydesk.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import app.studydesk.core.reader.navigation.ReaderRoute
import app.studydesk.core.reader.navigation.readerScreen
import app.studydesk.feature.exams.navigation.examsScreen
import app.studydesk.feature.library.LibraryActions
import app.studydesk.feature.library.navigation.libraryScreen
import app.studydesk.feature.practice.navigation.FlashcardsRoute
import app.studydesk.feature.practice.navigation.McqRoute
import app.studydesk.feature.practice.navigation.MockResultRoute
import app.studydesk.feature.practice.navigation.MockTestRoute
import app.studydesk.feature.practice.navigation.practiceScreens
import app.studydesk.feature.search.SearchActions
import app.studydesk.feature.search.navigation.SearchRoute
import app.studydesk.feature.search.navigation.searchScreen
import app.studydesk.feature.subject.SubjectActions
import app.studydesk.feature.subject.navigation.SubjectRoute
import app.studydesk.feature.subject.navigation.subjectScreen

internal fun NavGraphBuilder.featureDestinations(nav: NavHostController) {
    subjectScreen(
        SubjectActions(
            onBack = nav::navigateUp,
            onOpenReader = { packId, docId -> nav.openReader(packId, docId) },
            onPractice = { packId, unit, wrongOnly, quick -> nav.navigate(McqRoute(packId, unit, wrongOnly, quick)) },
            onFlashcards = { nav.openFlashcards(it) },
            onMockTest = { packId, testId -> nav.navigate(MockTestRoute(packId, testId)) },
            onMockResult = { nav.navigate(MockResultRoute(it)) },
        ),
    )
    readerScreen(onBack = nav::navigateUp, onOpenQuestions = { nav.openSubject(it, "questions") })
    practiceScreens(
        onBack = nav::navigateUp,
        onRetryWrong = { packId ->
            nav.navigate(McqRoute(packId, wrongOnly = true)) { popUpTo<McqRoute> { inclusive = true } }
        },
        onMockFinished = { id ->
            nav.navigate(MockResultRoute(id)) { popUpTo<MockTestRoute> { inclusive = true } }
        },
    )
    examsScreen()
    searchScreen(
        SearchActions(
            onBack = nav::navigateUp,
            onOpenReader = { packId, docId, block -> nav.openReader(packId, docId, block) },
            onOpenSubject = { packId, tab -> nav.openSubject(packId, tab) },
        ),
    )
    libraryScreen(
        LibraryActions(
            onOpenReader = { packId, docId, block -> nav.openReader(packId, docId, block) },
            onOpenSubject = { packId, tab -> nav.openSubject(packId, tab) },
        ),
    )
}

internal fun NavHostController.openSubject(subjectId: String, tab: String?) = navigate(SubjectRoute(subjectId, tab))
internal fun NavHostController.openReader(packId: String, docId: String, blockIndex: Int = -1) = navigate(ReaderRoute(packId, docId, blockIndex))
internal fun NavHostController.openSearch() = navigate(SearchRoute)
internal fun NavHostController.openFlashcards(packId: String?) = navigate(FlashcardsRoute(packId))
internal fun NavHostController.openQuickQuiz(packId: String) = navigate(McqRoute(packId, quick = true))

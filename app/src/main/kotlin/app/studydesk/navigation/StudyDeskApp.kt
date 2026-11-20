package app.studydesk.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.studydesk.core.designsystem.component.LocalSharedTransitionScope
import app.studydesk.core.designsystem.theme.LocalReduceMotion
import app.studydesk.core.designsystem.theme.Motion
import app.studydesk.feature.catalog.navigation.BrowseRoute
import app.studydesk.feature.catalog.navigation.ChangeCourseRoute
import app.studydesk.feature.catalog.navigation.OnboardingRoute
import app.studydesk.feature.catalog.navigation.ProgrammeRoute
import app.studydesk.feature.catalog.navigation.browseScreen
import app.studydesk.feature.catalog.navigation.changeCourseScreen
import app.studydesk.feature.catalog.navigation.onboardingScreen
import app.studydesk.feature.catalog.navigation.programmeScreen
import app.studydesk.feature.home.HomeActions
import app.studydesk.feature.home.navigation.HomeRoute
import app.studydesk.feature.home.navigation.homeScreen
import app.studydesk.feature.settings.navigation.AboutRoute
import app.studydesk.feature.settings.navigation.SettingsRoute
import app.studydesk.feature.settings.navigation.aboutScreen
import app.studydesk.feature.settings.navigation.settingsScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StudyDeskApp(onboarded: Boolean) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val reduce = LocalReduceMotion.current
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val topLevel = TopLevel.entries.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // No browser on the phone; nothing sensible to do.
        }
    }

    Column(Modifier.fillMaxSize()) {
        SharedTransitionLayout(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = nav,
                    startDestination = if (onboarded) HomeRoute else OnboardingRoute,
                    enterTransition = { if (reduce) fadeIn() else slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 4 } + fadeIn() },
                    exitTransition = { if (reduce) fadeOut() else slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { -it / 4 } + fadeOut() },
                    popEnterTransition = { if (reduce) fadeIn() else slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { -it / 4 } + fadeIn() },
                    popExitTransition = { if (reduce) fadeOut() else slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 4 } + fadeOut() },
                ) {
                    onboardingScreen(onFinished = {
                        nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                    })
                    homeScreen(
                        HomeActions(
                            onOpenSubject = { nav.openSubject(it, null) },
                            onOpenReader = { packId, docId -> nav.openReader(packId, docId) },
                            onOpenSearch = { nav.openSearch() },
                            onOpenSettings = { nav.navigate(SettingsRoute) },
                            onOpenExams = { nav.switchTab(TopLevel.Exams) },
                            onChangeCourse = { nav.navigate(ChangeCourseRoute) },
                            onRevise = { nav.openFlashcards(null) },
                            onQuickQuiz = { nav.openQuickQuiz(it) },
                            onUpdateApp = { openUrl(PLAY_URL) },
                        ),
                    )
                    browseScreen(
                        onOpenCourse = { s, p -> nav.navigate(ProgrammeRoute(s, p)) },
                        onOpenSubject = { id, tab -> nav.openSubject(id, tab) },
                    )
                    programmeScreen(onBack = nav::navigateUp, onOpenSubject = { id, tab -> nav.openSubject(id, tab) })
                    changeCourseScreen(onDone = nav::navigateUp)
                    settingsScreen(
                        onBack = nav::navigateUp,
                        onChangeCourse = { nav.navigate(ChangeCourseRoute) },
                        onOpenAbout = { nav.navigate(AboutRoute) },
                    )
                    aboutScreen(onBack = nav::navigateUp)
                    featureDestinations(nav)
                }
            }
        }
        AnimatedVisibility(
            visible = topLevel != null,
            enter = if (reduce) fadeIn() else slideInVertically { it } + fadeIn(),
            exit = if (reduce) fadeOut() else slideOutVertically { it } + fadeOut(),
        ) {
            NavigationBar {
                TopLevel.entries.forEach { tab ->
                    val selected = tab == topLevel
                    NavigationBarItem(
                        selected = selected,
                        onClick = { nav.switchTab(tab) },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

/** Switches bottom tabs the standard way: one copy of each tab, state kept, back goes Home. */
internal fun NavHostController.switchTab(tab: TopLevel) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private const val PLAY_URL = "https://play.google.com/store/apps/details?id=app.studydesk"

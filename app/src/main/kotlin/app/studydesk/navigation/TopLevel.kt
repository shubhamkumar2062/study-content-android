package app.studydesk.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import app.studydesk.R
import app.studydesk.feature.catalog.navigation.BrowseRoute
import app.studydesk.feature.exams.navigation.ExamsRoute
import app.studydesk.feature.home.navigation.HomeRoute
import app.studydesk.feature.library.navigation.LibraryRoute

internal enum class TopLevel(
    val route: Any,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: Int,
) {
    Home(HomeRoute, Icons.Outlined.Home, Icons.Filled.Home, R.string.nav_home),
    Browse(BrowseRoute, Icons.Outlined.Explore, Icons.Filled.Explore, R.string.nav_browse),
    Exams(ExamsRoute, Icons.Outlined.Event, Icons.Filled.Event, R.string.nav_exams),
    Library(LibraryRoute, Icons.Outlined.CollectionsBookmark, Icons.Filled.CollectionsBookmark, R.string.nav_library),
}

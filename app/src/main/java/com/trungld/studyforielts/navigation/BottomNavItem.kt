package com.trungld.studyforielts.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.trungld.studyforielts.R

/**
 * One bottom-nav tab. Each tab owns its own [androidx.navigation.NavHostController]
 * so the back stack is preserved when the user switches tabs.
 *
 * `route` is a logical key for [MainScreen] selection, not a real destination
 * path; each tab's NavHost has its own `startDestination`.
 */
enum class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(
        route = "tab/home",
        labelRes = R.string.bottom_nav_home,
        icon = Icons.Default.Home,
    ),
    Listening(
        route = "tab/listening",
        labelRes = R.string.bottom_nav_listening,
        icon = Icons.Default.Headphones,
    ),
    Writing(
        route = "tab/writing",
        labelRes = R.string.bottom_nav_writing,
        icon = Icons.Default.Edit,
    ),
}

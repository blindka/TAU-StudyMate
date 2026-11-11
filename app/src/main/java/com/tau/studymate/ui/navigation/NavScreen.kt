package com.tau.studymate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavScreen(
    val route: String,
    @androidx.annotation.StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : NavScreen("home", com.tau.studymate.R.string.nav_home, Icons.Default.Home)
    data object Tasks : NavScreen("tasks", com.tau.studymate.R.string.nav_tasks, Icons.Default.Checklist)
    data object Courses : NavScreen("courses", com.tau.studymate.R.string.nav_courses, Icons.Default.School)
    data object Readings : NavScreen("readings", com.tau.studymate.R.string.nav_readings, Icons.AutoMirrored.Filled.MenuBook)
    data object Habits : NavScreen("habits", com.tau.studymate.R.string.nav_focus, Icons.Default.Timer)

    companion object {
        val bottomNavItems: List<NavScreen>
            get() = listOf(Home, Tasks, Courses, Readings, Habits)
    }
}

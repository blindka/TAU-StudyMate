package com.tau.studymate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : NavScreen("home", "היום שלי", Icons.Default.Home)
    data object Tasks : NavScreen("tasks", "מטלות", Icons.Default.Checklist)
    data object Courses : NavScreen("courses", "קורסים", Icons.Default.School)
    data object Habits : NavScreen("habits", "הרגלים וטיימר", Icons.Default.Timer)

    companion object {
        val bottomNavItems: List<NavScreen>
            get() = listOf(Home, Tasks, Courses, Habits)
    }
}

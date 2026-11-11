package com.tau.studymate.data.model

import androidx.compose.ui.graphics.Color

enum class Department(@androidx.annotation.StringRes val labelRes: Int, val color: Color, val darkColor: Color) {
    COMMUNICATION(com.tau.studymate.R.string.dept_communication, Color(0xFF1E88E5), Color(0xFF90CAF9)),
    LABOR_STUDIES(com.tau.studymate.R.string.dept_labor, Color(0xFF2E7D32), Color(0xFFA5D6A7)),
    GENERAL(com.tau.studymate.R.string.dept_general, Color(0xFF7B1FA2), Color(0xFFCE93D8));

    companion object {
        fun fromString(value: String): Department {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: GENERAL
        }
    }
}

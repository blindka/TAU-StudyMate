package com.tau.studymate.data.model

import androidx.compose.ui.graphics.Color

enum class Priority(@androidx.annotation.StringRes val labelRes: Int, val color: Color) {
    LOW(com.tau.studymate.R.string.priority_low, Color(0xFF4CAF50)),
    MEDIUM(com.tau.studymate.R.string.priority_medium, Color(0xFFFFA000)),
    HIGH(com.tau.studymate.R.string.priority_high, Color(0xFFE53935))
}

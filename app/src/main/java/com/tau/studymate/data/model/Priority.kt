package com.tau.studymate.data.model

import androidx.compose.ui.graphics.Color

enum class Priority(val displayName: String, val color: Color) {
    LOW("נמוכה", Color(0xFF4CAF50)),
    MEDIUM("בינונית", Color(0xFFFFA000)),
    HIGH("גבוהה / דחוף", Color(0xFFE53935))
}

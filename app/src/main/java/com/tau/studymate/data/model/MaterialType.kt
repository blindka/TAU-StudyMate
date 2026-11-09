package com.tau.studymate.data.model

enum class MaterialType(val displayName: String) {
    SUMMARY("סיכום שיעור"),
    RECORDING_LINK("קישור להקלטה (Panopto/Zoom)"),
    ARTICLE("מאמר קריאה"),
    SLIDES("מצגת"),
    NOTE("הערה / תזכורת חומר")
}

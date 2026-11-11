package com.tau.studymate.data.model

enum class MaterialType(@androidx.annotation.StringRes val labelRes: Int) {
    SUMMARY(com.tau.studymate.R.string.material_summary),
    RECORDING_LINK(com.tau.studymate.R.string.material_recording),
    ARTICLE(com.tau.studymate.R.string.material_article),
    SLIDES(com.tau.studymate.R.string.material_slides),
    NOTE(com.tau.studymate.R.string.material_note)
}

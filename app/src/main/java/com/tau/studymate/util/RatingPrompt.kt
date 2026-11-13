package com.tau.studymate.util

import android.app.Activity
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview

/**
 * Asks for a Play rating once, at a happy moment (a study streak of [MIN_STREAK]+ days).
 * Google decides whether the dialog actually shows, and it never shows outside a Play install.
 */
object RatingPrompt {
    private const val MIN_STREAK = 5

    suspend fun maybeAsk(activity: Activity, streakDays: Int) {
        val prefs = activity.appPrefs()
        if (streakDays < MIN_STREAK || prefs.getBoolean("rating_asked", false)) return
        prefs.edit { putBoolean("rating_asked", true) } // once, whatever the user does
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            manager.launchReview(activity, manager.requestReview())
        }
    }
}

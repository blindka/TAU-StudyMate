package com.tau.studymate.util

import android.content.Context
import androidx.core.content.edit
import androidx.work.*
import com.tau.studymate.StudyMateApp
import com.tau.studymate.notification.NotificationHelper
import com.tau.studymate.notification.TaskNotificationScheduler
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Fetch + merge + store a Moodle feed. Shared by the sync button and the daily background job. */
object MoodleSync {

    /** Returns the fetch result and how many new tasks were added. */
    suspend fun syncAndStore(app: StudyMateApp, url: String): Pair<MoodleSyncResult, Int> {
        val repo = app.repository
        val courses = repo.allCourses.first()
        val result = MoodleSyncService.fetchAndSyncMoodle(url, courses)
        if (!result.success) return result to 0
        val names = courses.associate { it.id to it.name }
        val (inserts, updates) = MoodleSyncService.merge(repo.allTasks.first(), result.tasks)
        updates.forEach { t ->
            repo.updateTask(t)
            TaskNotificationScheduler.scheduleTask(app, t, t.courseId?.let(names::get))
        }
        inserts.forEach { t ->
            val id = repo.insertTask(t)
            TaskNotificationScheduler.scheduleTask(app, t.copy(id = id), t.courseId?.let(names::get))
        }
        app.secretPrefs().edit { putString(KEY_URL, url) }
        app.appPrefs().edit { remove(KEY_URL) } // older versions kept it in the backed-up prefs
        schedule(app)
        return result to inserts.size
    }

    fun savedUrl(context: Context): String =
        context.secretPrefs().getString(KEY_URL, null) ?: context.appPrefs().getString(KEY_URL, "") ?: ""

    /** Daily sync on any network; a no-op until the user has synced once (no saved link). */
    fun schedule(context: Context) {
        if (savedUrl(context).isBlank()) return
        val request = PeriodicWorkRequestBuilder<Worker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("moodle_daily_sync", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as StudyMateApp
            val url = savedUrl(app).ifBlank { return Result.success() }
            val (result, added) = syncAndStore(app, url)
            if (added > 0) {
                NotificationHelper.showTimerNotification(
                    app, "נוספו $added מטלות מ-Moodle 📚", "הסנכרון היומי מצא מועדי הגשה חדשים"
                )
            }
            return if (result.success) Result.success() else Result.retry()
        }
    }

    private const val KEY_URL = "moodle_feed_url"
}

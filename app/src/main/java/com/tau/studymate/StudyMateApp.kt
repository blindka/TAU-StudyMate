package com.tau.studymate

import android.app.Application
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.tau.studymate.data.local.AppDatabase
import com.tau.studymate.data.repository.StudyRepository
import com.tau.studymate.notification.NotificationHelper

class StudyMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // androidx.pdf runs its PDF service in an isolated process that also creates this Application;
        // system services (notifications, alarms, WorkManager) don't exist there, so skip app setup.
        if (android.os.Build.VERSION.SDK_INT >= 28 && android.os.Process.isIsolated()) return
        NotificationHelper.createNotificationChannels(this)
        com.tau.studymate.util.MoodleSync.schedule(this) // keeps the daily sync registered
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.tau.studymate.util.TauAcademicCalendar.refresh(this@StudyMateApp) }
        // Keep the home-screen widget in step with tasks/readings (one place instead of every edit)
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default).launch {
            kotlinx.coroutines.flow.combine(repository.allTasks, database.readingDao().getForWeek(
                com.tau.studymate.ui.viewmodel.weekStartOf(java.time.LocalDate.now()).toEpochDay())) { _, _ -> }
                .drop(1).debounce(1000)
                .collect { com.tau.studymate.widget.UpcomingWidget.refresh(this@StudyMateApp) }
        }
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        StudyRepository(
            db = database,
            courseDao = database.courseDao(),
            taskDao = database.taskDao(),
            studyMaterialDao = database.studyMaterialDao(),
            studySessionDao = database.studySessionDao()
        )
    }
}

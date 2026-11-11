package com.tau.studymate

import android.app.Application
import com.tau.studymate.data.local.AppDatabase
import com.tau.studymate.data.repository.StudyRepository
import com.tau.studymate.notification.NotificationHelper

class StudyMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
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

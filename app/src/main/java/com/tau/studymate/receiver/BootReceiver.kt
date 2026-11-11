package com.tau.studymate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tau.studymate.StudyMateApp
import com.tau.studymate.notification.TaskNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Android clears all alarms on reboot, so re-schedule reminders for future pending tasks. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as StudyMateApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val courses = app.repository.allCourses.first().associateBy { it.id }
                app.repository.pendingTasks.first()
                    .filter { it.reminderEnabled && it.dueDate > now }
                    .forEach { TaskNotificationScheduler.scheduleTask(app, it, it.courseId?.let { id -> courses[id]?.name }) }
            } finally {
                pending.finish()
            }
        }
    }
}

package com.tau.studymate.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tau.studymate.data.model.Task
import com.tau.studymate.receiver.TaskAlarmReceiver

object TaskNotificationScheduler {

    fun scheduleTask(context: Context, task: Task, courseName: String? = null) {
        if (task.isCompleted || !task.reminderEnabled) return

        val now = System.currentTimeMillis()
        // If due date is in the future, schedule for due date (or 9:00 AM on the due date)
        // If due date is already passed or less than 1 hour away, we can remind in 10 seconds for testing/quick alert
        val triggerTime = if (task.dueDate > now) {
            task.dueDate
        } else {
            // If already overdue or set for today, notify shortly (e.g. in 5 seconds or now)
            now + 5000L
        }

        scheduleReminder(
            context = context,
            taskId = task.id,
            title = task.title,
            description = task.description,
            courseName = courseName,
            triggerTimeMs = triggerTime
        )
    }

    fun scheduleReminder(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        courseName: String?,
        triggerTimeMs: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_DESC, description)
            putExtra(TaskAlarmReceiver.EXTRA_COURSE_NAME, courseName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

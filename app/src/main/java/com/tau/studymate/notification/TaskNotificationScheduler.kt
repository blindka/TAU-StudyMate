package com.tau.studymate.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tau.studymate.data.model.Task
import com.tau.studymate.receiver.TaskAlarmReceiver

object TaskNotificationScheduler {

    private const val HOUR = 60 * 60 * 1000L
    /** Reminder offsets before the deadline, with the text shown in the notification. */
    private val LEADS = listOf(24 * HOUR to "ההגשה מחר", 3 * HOUR to "ההגשה בעוד 3 שעות")

    /** Schedules "a day before" and "3 hours before" reminders. Past ones are skipped, never fired late. */
    fun scheduleTask(context: Context, task: Task, courseName: String? = null) {
        cancelReminder(context, task.id)
        if (task.isCompleted || !task.reminderEnabled) return
        val now = System.currentTimeMillis()
        LEADS.forEachIndexed { i, (lead, label) ->
            val at = task.dueDate - lead
            if (at > now) scheduleReminder(context, task.id, requestCode(task.id, i), task.title, label, courseName, at)
        }
    }

    private fun requestCode(taskId: Long, slot: Int) = (taskId * 2 + slot).toInt() + 1_000_000

    private fun scheduleReminder(
        context: Context,
        taskId: Long,
        code: Int,
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
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
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
        // slots 0/1, plus the single due-time alarm older versions used (request code = task id)
        (listOf(taskId.toInt()) + LEADS.indices.map { requestCode(taskId, it) }).forEach { code ->
            PendingIntent.getBroadcast(
                context, code, Intent(context, TaskAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it); it.cancel() }
        }
    }
}

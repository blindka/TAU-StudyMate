package com.tau.studymate.notification

import com.tau.studymate.util.appPrefs
import com.tau.studymate.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tau.studymate.data.model.Task
import com.tau.studymate.receiver.TaskAlarmReceiver

object TaskNotificationScheduler {

    private const val HOUR = 60 * 60 * 1000L

    /** Every lead time the user can pick (hours before the deadline) and its notification text. Index = alarm slot. */
    val LEAD_OPTIONS = listOf(72 to R.string.reminder_due_3d, 24 to R.string.reminder_due_tomorrow, 3 to R.string.reminder_due_3h, 1 to R.string.reminder_due_1h)
    const val DEFAULT_LEADS = "24,3"

    fun selectedLeads(context: Context): Set<Int> =
        context.appPrefs().getString("reminder_leads", DEFAULT_LEADS)!!.split(',').mapNotNull { it.toIntOrNull() }.toSet()

    /** Schedules the reminders chosen in Settings. Past ones are skipped, never fired late. */
    fun scheduleTask(context: Context, task: Task, courseName: String? = null) {
        cancelReminder(context, task.id)
        if (task.isCompleted || !task.reminderEnabled) return
        val now = System.currentTimeMillis()
        val leads = selectedLeads(context)
        LEAD_OPTIONS.forEachIndexed { slot, (hours, label) ->
            val at = task.dueDate - hours * HOUR
            if (hours in leads && at > now) scheduleReminder(context, task.id, requestCode(task.id, slot), task.title, context.getString(label), courseName, at)
        }
    }

    private fun requestCode(taskId: Long, slot: Int) = (taskId * 4 + slot).toInt() + 2_000_000

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
        // current slots, plus request codes used by older versions (single due-time alarm; 2-slot scheme)
        (listOf(taskId.toInt()) + (0..1).map { (taskId * 2 + it).toInt() + 1_000_000 } + LEAD_OPTIONS.indices.map { requestCode(taskId, it) })
            .forEach { code ->
            PendingIntent.getBroadcast(
                context, code, Intent(context, TaskAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it); it.cancel() }
        }
    }
}

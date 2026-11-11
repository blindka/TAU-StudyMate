package com.tau.studymate.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tau.studymate.MainActivity
import com.tau.studymate.R

object NotificationHelper {

    private const val TIMER_NOTIFICATION_ID = Int.MAX_VALUE // task notifications use task ids
    const val CHANNEL_TASKS_ID = "tau_studymate_tasks_channel"

    const val CHANNEL_TIMER_ID = "tau_studymate_timer_channel"

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tasks Reminder Channel
        val tasksChannel = NotificationChannel(
            CHANNEL_TASKS_ID,
            context.getString(R.string.channel_tasks_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_tasks_desc)
            enableVibration(true)
            enableLights(true)
        }

        // Timer Channel
        val timerChannel = NotificationChannel(
            CHANNEL_TIMER_ID,
            context.getString(R.string.channel_timer_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_timer_desc)
            enableVibration(true)
            enableLights(true)
        }

        notificationManager.createNotificationChannel(tasksChannel)
        notificationManager.createNotificationChannel(timerChannel)
    
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showTaskNotification(
        context: Context,
        taskId: Long,
        title: String,
        description: String = "",
        courseName: String? = null
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TASK_ID, taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            if (!courseName.isNullOrBlank()) {
                append("$courseName • ")
            }
            if (description.isNotBlank()) {
                append(description)
            } else {
                append(context.getString(R.string.reminder_default_body))
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title_format, title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        } catch (_: SecurityException) {}
    }

    fun showTimerNotification(
        context: Context,
        title: String = context.getString(R.string.timer_done_title),
        message: String = context.getString(R.string.timer_done_default_body)
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            TIMER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun showTestNotification(context: Context) {
        showTaskNotification(
            context = context,
            taskId = -1, // never collides with a real task id
            title = context.getString(R.string.test_notification_title),
            description = context.getString(R.string.test_notification_body),
            courseName = context.getString(R.string.test_notification_source)
        )
    }

    fun cancelNotification(context: Context, id: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(id)
        } catch (_: Exception) {}
    }
}

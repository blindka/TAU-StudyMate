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
    private const val CHANNEL_TASKS_NAME = "תזכורות למטלות"
    private const val CHANNEL_TASKS_DESC = "התראות על מועדי הגשה ותזכורות למטלות אקדמיות"

    const val CHANNEL_TIMER_ID = "tau_studymate_timer_channel"
    private const val CHANNEL_TIMER_NAME = "טיימר פוקוס למידה"
    private const val CHANNEL_TIMER_DESC = "התראות על סיום זמני למידה ופומודורו"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Tasks Reminder Channel
            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                CHANNEL_TASKS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_TASKS_DESC
                enableVibration(true)
                enableLights(true)
            }

            // Timer Channel
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                CHANNEL_TIMER_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_TIMER_DESC
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(tasksChannel)
            notificationManager.createNotificationChannel(timerChannel)
        }
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
                append("מועד ההגשה מתקרב! זה הזמן לסיים את המטלה 📚")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("תזכורת למטלה: $title")
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
        title: String = "זמן הלמידה הסתיים! ⏰",
        message: String = "כל הכבוד על הפוקוס! קח/י הפסקה קצרה וממשיכים 💪"
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
            title = "בדיקת התראות - StudyMate",
            description = "מעולה! מערכת ההתראות פעילה ותקפיץ לך תזכורות למטלות בזמן 🎯",
            courseName = "מערכת תזכורות"
        )
    }

    fun cancelNotification(context: Context, id: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(id)
        } catch (_: Exception) {}
    }
}

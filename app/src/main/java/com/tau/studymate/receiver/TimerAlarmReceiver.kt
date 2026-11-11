package com.tau.studymate.receiver

import com.tau.studymate.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tau.studymate.notification.NotificationHelper
import com.tau.studymate.util.SoundManager

/** Fires when a focus countdown ends, even if the app was closed or the phone locked. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        SoundManager.playTimerFinishedSound(context)
        NotificationHelper.showTimerNotification(
            context, context.getString(R.string.timer_done_title), context.resources.getQuantityString(R.plurals.timer_done_body, minutes, minutes)
        )
    }

    companion object {
        private const val EXTRA_MINUTES = "minutes"
        private const val REQUEST_CODE = -1001 // task reminders use positive task ids

        private fun pending(context: Context, minutes: Int) = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, TimerAlarmReceiver::class.java).putExtra(EXTRA_MINUTES, minutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun schedule(context: Context, atMillis: Long, minutes: Int) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val pi = pending(context, minutes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi) // may arrive a little late
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        }

        fun cancel(context: Context) {
            context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, 0))
        }
    }
}

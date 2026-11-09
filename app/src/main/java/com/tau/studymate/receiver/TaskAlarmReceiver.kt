package com.tau.studymate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tau.studymate.notification.NotificationHelper

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESC = "extra_task_desc"
        const val EXTRA_COURSE_NAME = "extra_course_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "תזכורת למטלה"
        val desc = intent.getStringExtra(EXTRA_TASK_DESC) ?: ""
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME)

        if (taskId != -1L) {
            NotificationHelper.showTaskNotification(
                context = context,
                taskId = taskId,
                title = title,
                description = desc,
                courseName = courseName
            )
        }
    }
}

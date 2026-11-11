package com.tau.studymate.widget

import com.tau.studymate.R
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tau.studymate.MainActivity
import com.tau.studymate.StudyMateApp
import com.tau.studymate.ui.viewmodel.weekStartOf
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Home-screen widget: next 3 deadlines + this week's reading progress. Tapping opens the app. */
class UpcomingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as StudyMateApp
        val zone = ZoneId.systemDefault()
        val startOfToday = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val tasks = app.repository.allTasks.first()
            .filter { !it.isCompleted && it.dueDate >= startOfToday }
            .sortedBy { it.dueDate }
            .take(3)
        val readings = app.database.readingDao().getForWeek(weekStartOf(LocalDate.now()).toEpochDay()).first()
        val fmt = DateTimeFormatter.ofPattern("dd/MM")

        provideContent {
            val white = ColorProvider(Color.White)
            Column(
                GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(Color(0xFF6D28D9))
                    .padding(14.dp).clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.End
            ) {
                Text("StudyMate", style = TextStyle(color = white, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                Spacer(GlanceModifier.height(6.dp))
                if (tasks.isEmpty()) {
                    Text(context.getString(R.string.widget_no_deadlines), style = TextStyle(color = white, fontSize = 13.sp))
                } else tasks.forEach { t ->
                    val day = Instant.ofEpochMilli(t.dueDate).atZone(zone).toLocalDate()
                    Text(
                        "${day.format(fmt)} · ${t.title}", maxLines = 1,
                        style = TextStyle(color = white, fontSize = 13.sp, textAlign = TextAlign.End),
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
                if (readings.isNotEmpty()) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        context.getString(R.string.widget_readings, readings.count { it.isRead }, readings.size),
                        style = TextStyle(color = ColorProvider(Color(0xFFEDE9FE)), fontSize = 12.sp)
                    )
                }
            }
        }
    }

    companion object {
        /** Call after task/reading changes so the widget isn't stale until the next system refresh. */
        suspend fun refresh(context: Context) = runCatching { UpcomingWidget().updateAll(context) }
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()
}

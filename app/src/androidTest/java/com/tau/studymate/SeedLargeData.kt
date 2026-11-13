package com.tau.studymate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.Priority
import com.tau.studymate.data.model.StudySession
import com.tau.studymate.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Manual load-test helper, skipped in normal runs. Fills the real app DB with 600 tasks and 400 sessions:
 *   adb shell am instrument -w -e seed true -e class com.tau.studymate.SeedLargeData com.tau.studymate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class SeedLargeData {
    /** A few realistic tasks for store screenshots: -e demo true */
    @Test
    fun demo() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("demo") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as StudyMateApp
        val courses = app.repository.allCourses.first()
        val day = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        listOf(
            Triple("Response paper: McLuhan, The Medium is the Message", 1L, Priority.HIGH),
            Triple("Midterm essay: the global labor market", 3L, Priority.HIGH),
            Triple("Problem set 2: communication models", 5L, Priority.MEDIUM),
            Triple("Group presentation: agenda-setting theory", 9L, Priority.MEDIUM)
        ).forEachIndexed { i, (title, d, p) ->
            val c = courses.getOrNull(i % courses.size)
            app.repository.insertTask(Task(title = title, dueDate = now + d * day, courseId = c?.id, department = c?.department ?: Department.GENERAL, priority = p, reminderEnabled = false))
        }
        repeat(5) { app.repository.logStudySession(null, null, 25 + it * 10, "") }
    }

    @Test
    fun seed() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seed") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as StudyMateApp
        val repo = app.repository
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        repeat(600) { i ->
            repo.insertTask(
                Task(
                    title = "מטלת עומס מספר $i: קריאה וסיכום פרק ${i % 12}",
                    description = "תיאור ארוך יחסית לבדיקת ביצועים של הרשימה ושל החיפוש ".repeat(2),
                    dueDate = now + (i - 300) * day / 3,
                    isCompleted = i % 3 == 0,
                    completedAt = if (i % 3 == 0) now - (i % 40) * day else null,
                    department = Department.entries[i % 3],
                    priority = Priority.entries[i % 3],
                    actualMinutes = i % 90,
                    reminderEnabled = false
                )
            )
        }
        repeat(400) { i -> repo.logStudySession(null, null, 25 + i % 30, "") }
        Unit
    }
}

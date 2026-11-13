package com.tau.studymate

import com.tau.studymate.ui.viewmodel.computeStreak
import com.tau.studymate.ui.viewmodel.weekStartOf
import java.time.LocalDate
import com.tau.studymate.util.MoodleSyncService
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreLogicTest {
    private val day = 24 * 60 * 60 * 1000L

    @Test fun icsParserUnfoldsUnescapesAndSkipsPast() {
        val ics = "BEGIN:VCALENDAR\r\n" +
            "BEGIN:VEVENT\r\nSUMMARY:Essay\\, part 1 is due\r\nDESCRIPTION:long \r\n text\r\nDTEND:29991231T120000Z\r\nEND:VEVENT\r\n" +
            "BEGIN:VEVENT\r\nSUMMARY:Old quiz\r\nDTEND:20000101T120000Z\r\nEND:VEVENT\r\n" +
            "END:VCALENDAR"
        val tasks = MoodleSyncService.parseIcsContent(ics, emptyList())
        assertEquals(1, tasks.size)
        assertEquals("Essay, part 1", tasks[0].title)
        assertEquals("long text", tasks[0].description)
    }

    @Test fun streakCountsConsecutiveDays() {
        val now = System.currentTimeMillis()
        assertEquals(3, computeStreak(listOf(now, now - day, now - 2 * day, now - 4 * day), now))
        assertEquals(1, computeStreak(listOf(now - day), now)) // yesterday still counts
        assertEquals(0, computeStreak(emptyList(), now))
    }

    @Test fun studyWeekStartsOnSunday() {
        val sunday = LocalDate.of(2026, 11, 8)
        assertEquals(sunday, weekStartOf(sunday))
        assertEquals(sunday, weekStartOf(LocalDate.of(2026, 11, 14))) // Saturday
        assertEquals(sunday.plusWeeks(1), weekStartOf(LocalDate.of(2026, 11, 15)))
    }

    @Test fun timerUsesWallClock() {
        val now = 1_000_000L
        assertEquals(60, com.tau.studymate.ui.viewmodel.timerSecondsAt(true, now + 60_000, 0, now))
        assertEquals(1, com.tau.studymate.ui.viewmodel.timerSecondsAt(true, now + 200, 0, now)) // rounds up, never shows 0 early
        assertEquals(0, com.tau.studymate.ui.viewmodel.timerSecondsAt(true, now - 5_000, 0, now)) // app reopened after the end
        assertEquals(130, com.tau.studymate.ui.viewmodel.timerSecondsAt(false, now - 100_000, 30, now)) // stopwatch keeps paused time
    }

    @Test fun moodleMergeUpdatesByUidAndSkipsDuplicates() {
        val base = com.tau.studymate.data.model.Task(id = 5, title = "Essay", dueDate = 100, externalId = "uid-1", isCompleted = true)
        val manual = com.tau.studymate.data.model.Task(id = 6, title = "Quiz", dueDate = 200)
        val incoming = listOf(
            base.copy(id = 0, dueDate = 150, isCompleted = false), // extension: same UID, new date
            manual.copy(id = 0),                                  // already exists by title+date
            com.tau.studymate.data.model.Task(title = "New", dueDate = 300, externalId = "uid-2")
        )
        val (inserts, updates) = MoodleSyncService.merge(listOf(base, manual), incoming)
        assertEquals(listOf("New"), inserts.map { it.title })
        assertEquals(1, updates.size)
        assertEquals(150L, updates[0].dueDate)
        assertEquals(true, updates[0].isCompleted) // user's completion is kept
    }

    @Test fun semesterNameFollowsCalendarTable() {
        val cal = com.tau.studymate.util.TauAcademicCalendar
        assertEquals("סמסטר א'", cal.getCurrentSemesterName(LocalDate.of(2026, 11, 11)))
        assertEquals("סמסטר ב'", cal.getCurrentSemesterName(LocalDate.of(2027, 4, 1)))
        assertEquals("סמסטר ב'", cal.getCurrentSemesterName(LocalDate.of(2027, 7, 20))) // B exams, not summer
    }

    @Test fun weightedAverageUsesCredits() {
        val c = { g: Float?, cr: Float -> com.tau.studymate.data.model.Course(courseNumber = "1", name = "x", department = com.tau.studymate.data.model.Department.GENERAL, credits = cr, grade = g) }
        val (avg, credits) = com.tau.studymate.ui.screens.courses.weightedAverage(listOf(c(90f, 4f), c(70f, 2f), c(null, 3f)))!!
        assertEquals(83.33f, avg, 0.01f) // (90*4 + 70*2) / 6
        assertEquals(6f, credits, 0f)
        assertEquals(null, com.tau.studymate.ui.screens.courses.weightedAverage(listOf(c(null, 3f))))
    }

    @Test fun calendarJsonParses() {
        val json = java.io.File("../docs/calendar.json").readText()
        val periods = com.tau.studymate.util.TauAcademicCalendar.parse(json)
        assertEquals(10, periods.size)
        assertEquals(true, periods.first { it.yearEn == "2026/27" }.isTeachingSemester)
    }

    @Test fun courseShareRoundTripDropsPrivateData() {
        val m = com.tau.studymate.data.model.Department.COMMUNICATION
        val course = com.tau.studymate.data.model.Course(id = 9, courseNumber = "1082.1100", name = "Mass Comm", department = m, credits = 4f, grade = 95f, examA = 123L)
        val open = com.tau.studymate.data.model.Task(id = 1, title = "Essay", dueDate = 500, courseId = 9, repeatWeekly = true)
        val done = open.copy(id = 2, title = "Old", isCompleted = true)
        val slot = com.tau.studymate.data.model.ClassSlot(courseId = 9, dayOfWeek = 7, startMinute = 600, endMinute = 720, room = "144")
        val json = com.tau.studymate.util.CourseShare.toJson(com.tau.studymate.util.CourseShare.Bundle(course, listOf(open, done), emptyList(), listOf(slot)))
        val back = com.tau.studymate.util.CourseShare.fromJson(json)
        assertEquals("1082.1100", back.course.courseNumber)
        assertEquals(null, back.course.grade)          // grades stay private
        assertEquals(123L, back.course.examA)
        assertEquals(listOf("Essay"), back.tasks.map { it.title }) // completed tasks aren't shared
        assertEquals(true, back.tasks[0].repeatWeekly)
        assertEquals(600, back.slots[0].startMinute)
        assertEquals(true, runCatching { com.tau.studymate.util.CourseShare.fromJson("{}") }.isFailure)
    }
}

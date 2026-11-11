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
}

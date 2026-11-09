package com.tau.studymate.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TauAcademicStatus(
    val displayText: String,
    val isExamPeriod: Boolean
)

object TauAcademicCalendar {

    data class Period(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val title: String,
        val datesDisplay: String,
        val isExam: Boolean,
        val isTeachingSemester: Boolean = false
    )

    private val periods = listOf(
        // תשפ"ז (2026-2027) - אוניברסיטת תל אביב
        Period(
            startDate = LocalDate.of(2026, 10, 18),
            endDate = LocalDate.of(2027, 1, 18),
            title = "סמסטר א' תשפ\"ז",
            datesDisplay = "18/10/2026 – 18/01/2027",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2027, 1, 19),
            endDate = LocalDate.of(2027, 1, 19),
            title = "יום היערכות לבחינות סמסטר א' תשפ\"ז",
            datesDisplay = "19/01/2027",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2027, 1, 20),
            endDate = LocalDate.of(2027, 3, 8),
            title = "תקופת בחינות סמסטר א' תשפ\"ז",
            datesDisplay = "20/01/2027 – 08/03/2027",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2027, 3, 9),
            endDate = LocalDate.of(2027, 6, 27),
            title = "סמסטר ב' תשפ\"ז",
            datesDisplay = "09/03/2027 – 27/06/2027",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2027, 6, 28),
            endDate = LocalDate.of(2027, 6, 28),
            title = "יום היערכות לבחינות סמסטר ב' תשפ\"ז",
            datesDisplay = "28/06/2027",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2027, 6, 29),
            endDate = LocalDate.of(2027, 9, 17),
            title = "תקופת בחינות סמסטר ב' תשפ\"ז",
            datesDisplay = "29/06/2027 – 17/09/2027",
            isExam = true
        ),
        // תשפ"ו (2025-2026)
        Period(
            startDate = LocalDate.of(2025, 10, 26),
            endDate = LocalDate.of(2026, 1, 25),
            title = "סמסטר א' תשפ\"ו",
            datesDisplay = "26/10/2025 – 25/01/2026",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2026, 1, 26),
            endDate = LocalDate.of(2026, 3, 13),
            title = "תקופת בחינות סמסטר א' תשפ\"ו",
            datesDisplay = "28/01/2026 – 13/03/2026",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2026, 3, 15),
            endDate = LocalDate.of(2026, 6, 26),
            title = "סמסטר ב' תשפ\"ו",
            datesDisplay = "15/03/2026 – 26/06/2026",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2026, 6, 27),
            endDate = LocalDate.of(2026, 9, 18),
            title = "תקופת בחינות סמסטר ב' תשפ\"ו",
            datesDisplay = "27/06/2026 – 18/09/2026",
            isExam = true
        )
    )

    fun getCurrentStatus(date: LocalDate = LocalDate.now()): TauAcademicStatus {
        val currentPeriod = periods.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }
        if (currentPeriod != null) {
            val text = if (currentPeriod.isTeachingSemester) {
                val daysPassed = ChronoUnit.DAYS.between(currentPeriod.startDate, date)
                val week = (daysPassed / 7).toInt() + 1
                "${currentPeriod.title} • ${currentPeriod.datesDisplay} (שבוע $week)"
            } else {
                "${currentPeriod.title} • ${currentPeriod.datesDisplay}"
            }
            return TauAcademicStatus(displayText = text, isExamPeriod = currentPeriod.isExam)
        }

        return TauAcademicStatus(
            displayText = "שנת הלימודים תשפ\"ז • אוניברסיטת תל אביב",
            isExamPeriod = false
        )
    }
}

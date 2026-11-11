package com.tau.studymate.util

import android.content.Context
import com.tau.studymate.R
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
        @androidx.annotation.StringRes val kind: Int,
        @androidx.annotation.StringRes val year: Int,
        val datesDisplay: String,
        val isExam: Boolean,
        val isTeachingSemester: Boolean = false
    )

    private val periods = listOf(
        // shortcut: dates are hardcoded through Sept 2027 (5787); add תשפ"ח before Aug 2027 or the banner falls back to a generic status.
        // תשפ"ז (2026-2027) - אוניברסיטת תל אביב
        Period(
            startDate = LocalDate.of(2026, 10, 18),
            endDate = LocalDate.of(2027, 1, 18),
            kind = R.string.cal_sem_a, year = R.string.cal_year_5787,
            datesDisplay = "18/10/2026 – 18/01/2027",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2027, 1, 19),
            endDate = LocalDate.of(2027, 1, 19),
            kind = R.string.cal_prep_a, year = R.string.cal_year_5787,
            datesDisplay = "19/01/2027",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2027, 1, 20),
            endDate = LocalDate.of(2027, 3, 8),
            kind = R.string.cal_exams_a, year = R.string.cal_year_5787,
            datesDisplay = "20/01/2027 – 08/03/2027",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2027, 3, 9),
            endDate = LocalDate.of(2027, 6, 27),
            kind = R.string.cal_sem_b, year = R.string.cal_year_5787,
            datesDisplay = "09/03/2027 – 27/06/2027",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2027, 6, 28),
            endDate = LocalDate.of(2027, 6, 28),
            kind = R.string.cal_prep_b, year = R.string.cal_year_5787,
            datesDisplay = "28/06/2027",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2027, 6, 29),
            endDate = LocalDate.of(2027, 9, 17),
            kind = R.string.cal_exams_b, year = R.string.cal_year_5787,
            datesDisplay = "29/06/2027 – 17/09/2027",
            isExam = true
        ),
        // תשפ"ו (2025-2026)
        Period(
            startDate = LocalDate.of(2025, 10, 26),
            endDate = LocalDate.of(2026, 1, 25),
            kind = R.string.cal_sem_a, year = R.string.cal_year_5786,
            datesDisplay = "26/10/2025 – 25/01/2026",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2026, 1, 26),
            endDate = LocalDate.of(2026, 3, 13),
            kind = R.string.cal_exams_a, year = R.string.cal_year_5786,
            datesDisplay = "28/01/2026 – 13/03/2026",
            isExam = true
        ),
        Period(
            startDate = LocalDate.of(2026, 3, 15),
            endDate = LocalDate.of(2026, 6, 26),
            kind = R.string.cal_sem_b, year = R.string.cal_year_5786,
            datesDisplay = "15/03/2026 – 26/06/2026",
            isExam = false,
            isTeachingSemester = true
        ),
        Period(
            startDate = LocalDate.of(2026, 6, 27),
            endDate = LocalDate.of(2026, 9, 18),
            kind = R.string.cal_exams_b, year = R.string.cal_year_5786,
            datesDisplay = "27/06/2026 – 18/09/2026",
            isExam = true
        )
    )

    fun getCurrentStatus(context: Context, date: LocalDate = LocalDate.now()): TauAcademicStatus {
        val p = periods.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }
            ?: return TauAcademicStatus(context.getString(R.string.cal_default), isExamPeriod = false)
        val kind = context.getString(p.kind); val year = context.getString(p.year)
        val text = if (p.isTeachingSemester) {
            val week = (ChronoUnit.DAYS.between(p.startDate, date) / 7).toInt() + 1
            context.getString(R.string.cal_teaching, kind, year, p.datesDisplay, week)
        } else context.getString(R.string.cal_period, kind, year, p.datesDisplay)
        return TauAcademicStatus(displayText = text, isExamPeriod = p.isExam)
    }

    /**
     * מחזיר את שם הסמסטר הנוכחי הרשמי לפי לוח השנה האקדמי של אוניברסיטת תל אביב
     */
    fun getCurrentSemesterName(date: LocalDate = LocalDate.now()): String {
        // Same table as the banner, so both always agree. Between periods, the next one counts.
        val period = periods.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }
            ?: periods.filter { it.startDate.isAfter(date) }.minByOrNull { it.startDate }
        return when {
            period == null -> "סמסטר א'"
            period.kind in setOf(R.string.cal_sem_b, R.string.cal_prep_b, R.string.cal_exams_b) -> "סמסטר ב'"
            else -> "סמסטר א'"
        }
    }
}

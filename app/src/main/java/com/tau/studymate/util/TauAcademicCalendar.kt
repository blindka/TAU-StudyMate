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
        /** Year label from the downloaded calendar (e.g. "תשפ״ח" / "2027/28"); built-in periods use [year]. */
        val yearHe: String? = null,
        val yearEn: String? = null,
        val datesDisplay: String,
        val isExam: Boolean,
        val isTeachingSemester: Boolean = false
    )

    private val builtIn = listOf(
        // Fallback when docs/calendar.json hasn't been downloaded yet. New years go in docs/calendar.json, not here.
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

    /**
     * Periods downloaded from docs/calendar.json (GitHub Pages) win over the built-in table, so new academic
     * years can be published without an app update. Cached in filesDir; refreshed at most weekly.
     */
    @Volatile private var remote: List<Period>? = null
    private val periods get() = remote ?: builtIn
    private const val URL = "https://blindka.github.io/TAU-StudyMate/calendar.json"
    private val KINDS = mapOf("sem_a" to R.string.cal_sem_a, "prep_a" to R.string.cal_prep_a, "exams_a" to R.string.cal_exams_a,
        "sem_b" to R.string.cal_sem_b, "prep_b" to R.string.cal_prep_b, "exams_b" to R.string.cal_exams_b)

    internal fun parse(json: String): List<Period> {
        val arr = org.json.JSONObject(json).getJSONArray("periods")
        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val start = LocalDate.parse(o.getString("start")); val end = LocalDate.parse(o.getString("end"))
            Period(start, end, KINDS.getValue(o.getString("kind")), R.string.cal_year_5787,
                yearHe = o.getString("yearHe"), yearEn = o.getString("yearEn"),
                datesDisplay = if (start == end) start.format(fmt) else "${start.format(fmt)} – ${end.format(fmt)}",
                isExam = o.getString("kind").startsWith("exams") || o.getString("kind").startsWith("prep"),
                isTeachingSemester = o.getString("kind").startsWith("sem"))
        }
    }

    /** Loads the cached calendar, then re-downloads it if the cache is a week old. Safe to call on every start. */
    suspend fun refresh(context: Context) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cache = java.io.File(context.filesDir, "calendar.json")
        runCatching { if (cache.exists()) remote = parse(cache.readText()) }
        if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < 7L * 24 * 60 * 60 * 1000) return@withContext
        runCatching {
            val conn = (java.net.URL(URL).openConnection() as java.net.HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 10_000 }
            try {
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    remote = parse(text) // validate before caching
                    cache.writeText(text)
                }
            } finally { conn.disconnect() }
        }
    }

    fun getCurrentStatus(context: Context, date: LocalDate = LocalDate.now()): TauAcademicStatus {
        val p = periods.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }
            ?: return TauAcademicStatus(context.getString(R.string.cal_default), isExamPeriod = false)
        val kind = context.getString(p.kind)
        val hebrew = context.resources.configuration.locales[0].language.let { it == "iw" || it == "he" }
        val year = (if (hebrew) p.yearHe else p.yearEn) ?: context.getString(p.year)
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

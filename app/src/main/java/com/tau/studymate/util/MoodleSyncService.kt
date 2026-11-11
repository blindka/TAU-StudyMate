package com.tau.studymate.util

import com.tau.studymate.R
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.Priority
import com.tau.studymate.data.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class MoodleSyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val errorMessage: String? = null,
    val tasks: List<Task> = emptyList()
)

object MoodleSyncService {

    private val icsDateFormats = listOf(
        SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US),
        SimpleDateFormat("yyyyMMdd", Locale.US)
    )

    /**
     * Downloads and parses Moodle iCal feed from URL into Task entities
     */
    suspend fun fetchAndSyncMoodle(
        context: android.content.Context,
        rawUrl: String,
        existingCourses: List<Course>
    ): MoodleSyncResult = withContext(Dispatchers.IO) {
        val sanitizedUrl = when {
            rawUrl.startsWith("webcal://", ignoreCase = true) -> "https://" + rawUrl.substring(9)
            !rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true) -> "https://$rawUrl"
            else -> rawUrl
        }.trim()

        try {
            val url = URL(sanitizedUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "TAU-StudyMate/1.0 (Android)")
                setRequestProperty("Accept", "text/calendar, text/plain, */*")
            }

            if (connection.responseCode !in 200..299) {
                return@withContext MoodleSyncResult(
                    success = false,
                    syncedCount = 0,
                    errorMessage = context.getString(R.string.moodle_http_error, connection.responseCode)
                )
            }

            val content = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val parsedTasks = parseIcsContent(content, existingCourses, context.getString(R.string.moodle_task_default_desc))

            if (parsedTasks.isEmpty()) {
                return@withContext MoodleSyncResult(
                    success = true,
                    syncedCount = 0,
                    errorMessage = context.getString(R.string.moodle_no_future)
                )
            }

            MoodleSyncResult(
                success = true,
                syncedCount = parsedTasks.size,
                tasks = parsedTasks
            )
        } catch (e: Exception) {
            MoodleSyncResult(
                success = false,
                syncedCount = 0,
                errorMessage = context.getString(R.string.moodle_fetch_error, e.localizedMessage ?: context.getString(R.string.moodle_unreachable))
            )
        }
    }

    /**
     * Parses iCalendar (ICS) text into Task models
     */
    fun parseIcsContent(content: String, existingCourses: List<Course>, defaultDescription: String = ""): List<Task> {
        val tasks = mutableListOf<Task>()
        // RFC 5545: long lines are folded onto the next line starting with a space or tab
        val unfolded = content.replace(Regex("\r?\n[ \t]"), "")
        val events = unfolded.split("BEGIN:VEVENT")
        if (events.size <= 1) return emptyList()

        val courseNumberRegex = Regex("\\b\\d{4}[.-]\\d{4}\\b")
        val now = System.currentTimeMillis()

        for (i in 1 until events.size) {
            val block = events[i].substringBefore("END:VEVENT")

            var summary = ""
            var description = ""
            var dtEndMillis = 0L
            var category = ""
            var uid: String? = null

            block.lines().forEach { rawLine ->
                val line = rawLine.trim()
                when {
                    line.startsWith("SUMMARY", ignoreCase = true) -> {
                        summary = unescapeIcs(line.substringAfter(':'))
                    }
                    line.startsWith("DESCRIPTION", ignoreCase = true) -> {
                        description = unescapeIcs(line.substringAfter(':'))
                    }
                    line.startsWith("UID", ignoreCase = true) -> {
                        uid = line.substringAfter(':').trim().ifBlank { null }
                    }
                    line.startsWith("CATEGORIES", ignoreCase = true) -> {
                        category = unescapeIcs(line.substringAfter(':'))
                    }
                    line.startsWith("DTEND", ignoreCase = true) || (dtEndMillis == 0L && line.startsWith("DTSTART", ignoreCase = true)) -> {
                        val dateStr = line.substringAfter(':').trim()
                        for (fmt in icsDateFormats) {
                            try {
                                val parsed = fmt.parse(dateStr)
                                if (parsed != null) {
                                    dtEndMillis = parsed.time
                                    break
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            // Cleanup Moodle summary text (e.g. "מועד סופי למטלה", "is due")
            val cleanedTitle = summary
                .replace(" is due", "", ignoreCase = true)
                .replace("מועד הגשה:", "", ignoreCase = true)
                .trim()

            // Skip blank titles and deadlines that already passed
            if (cleanedTitle.isBlank() || (dtEndMillis in 1 until now)) continue

            // Determine dueDate
            val dueDate = if (dtEndMillis > 0L) dtEndMillis else now + (7L * 24 * 60 * 60 * 1000)

            // Try to match to an existing course in DB
            val combinedText = "$cleanedTitle $description $category"
            val matchedCourseNumber = courseNumberRegex.find(combinedText)?.value
            val matchedCourse = existingCourses.find { course ->
                (matchedCourseNumber != null && course.courseNumber.contains(matchedCourseNumber)) ||
                        combinedText.contains(course.courseNumber, ignoreCase = true) ||
                        combinedText.contains(course.name, ignoreCase = true)
            }

            val department = matchedCourse?.department ?: Department.GENERAL
            val courseId = matchedCourse?.id

            val isUrgent = (dueDate - now) in 0..(4L * 24 * 60 * 60 * 1000)
            val priority = if (isUrgent) Priority.HIGH else Priority.MEDIUM

            tasks.add(
                Task(
                    title = cleanedTitle,
                    description = description.ifBlank { defaultDescription },
                    dueDate = dueDate,
                    courseId = courseId,
                    department = department,
                    priority = priority,
                    estimatedMinutes = 120,
                    externalId = uid
                )
            )
        }

        return tasks
    }

    private fun unescapeIcs(value: String): String = value.trim()
        .replace("\\n", "\n").replace("\\N", "\n")
        .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    /**
     * Decides what a sync changes. Same Moodle UID → update title/date (keeps the user's completion);
     * no UID match but same title+date → skip; otherwise insert.
     */
    fun merge(existing: List<Task>, incoming: List<Task>): Pair<List<Task>, List<Task>> {
        val byUid = existing.filter { it.externalId != null }.associateBy { it.externalId }
        val seen = existing.map { it.title to it.dueDate }.toSet()
        val inserts = mutableListOf<Task>(); val updates = mutableListOf<Task>()
        for (t in incoming) {
            val old = t.externalId?.let { byUid[it] }
            when {
                old != null -> if (old.title != t.title || old.dueDate != t.dueDate) updates += old.copy(title = t.title, dueDate = t.dueDate)
                (t.title to t.dueDate) !in seen -> inserts += t
            }
        }
        return inserts to updates
    }
}

package com.tau.studymate.util

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
                    errorMessage = "שגיאת התחברות לשרת המודל (${connection.responseCode}). יש לוודא שהקישור תקין."
                )
            }

            val content = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val parsedTasks = parseIcsContent(content, existingCourses)

            if (parsedTasks.isEmpty()) {
                return@withContext MoodleSyncResult(
                    success = true,
                    syncedCount = 0,
                    errorMessage = "הקישור נקלט בהצלחה, אך לא אותרו מועדי הגשה עתידיים בלוח השנה."
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
                errorMessage = "שגיאה במשיכת הנתונים: ${e.localizedMessage ?: "לא ניתן לגשת לכתובת המודל"}"
            )
        }
    }

    /**
     * Parses iCalendar (ICS) text into Task models
     */
    fun parseIcsContent(content: String, existingCourses: List<Course>): List<Task> {
        val tasks = mutableListOf<Task>()
        val events = content.split("BEGIN:VEVENT")
        if (events.size <= 1) return emptyList()

        val courseNumberRegex = Regex("\\b\\d{4}[.-]\\d{4}\\b")
        val now = System.currentTimeMillis()

        for (i in 1 until events.size) {
            val block = events[i].substringBefore("END:VEVENT")

            var summary = ""
            var description = ""
            var dtEndMillis = 0L
            var category = ""

            block.lines().forEach { rawLine ->
                val line = rawLine.trim()
                when {
                    line.startsWith("SUMMARY:", ignoreCase = true) -> {
                        summary = line.substring(8).trim()
                    }
                    line.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                        description = line.substring(12).trim().replace("\\n", "\n")
                    }
                    line.startsWith("CATEGORIES:", ignoreCase = true) -> {
                        category = line.substring(11).trim()
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
            var cleanedTitle = summary
                .replace(" is due", "", ignoreCase = true)
                .replace("מועד הגשה:", "", ignoreCase = true)
                .trim()

            if (cleanedTitle.isBlank()) continue

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
                    description = if (description.isNotBlank()) description else "מטלה שסונכרנה ישירות מ-Moodle אתו\"א",
                    dueDate = dueDate,
                    courseId = courseId,
                    department = department,
                    priority = priority,
                    estimatedMinutes = 120
                )
            )
        }

        return tasks
    }
}

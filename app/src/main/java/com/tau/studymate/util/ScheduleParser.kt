package com.tau.studymate.util

import com.tau.studymate.data.model.CourseTemplate
import com.tau.studymate.data.model.Department

object ScheduleParser {

    /**
     * Parses schedule content (ICS, CSV, TXT) into CourseTemplate list
     */
    fun parseScheduleContent(content: String, targetSemester: String, fallbackDegree: String): List<CourseTemplate> {
        val lines = content.lines().filter { it.isNotBlank() }
        val courses = mutableListOf<CourseTemplate>()

        lines.forEach { line ->
            val trimmed = line.trim()
            val numMatch = Regex("\\b\\d{4}[.-]\\d{4}\\b").find(trimmed)
            if (numMatch != null) {
                val currentNumber = numMatch.value
                var currentName = trimmed.replace(numMatch.value, "").trim(' ', '-', ':', '\t')
                if (currentName.isBlank()) currentName = "קורס $currentNumber"
                courses.add(
                    CourseTemplate(
                        courseNumber = currentNumber,
                        name = currentName,
                        department = Department.GENERAL,
                        lecturer = "סגל הקורס",
                        semester = targetSemester,
                        credits = 3.0f
                    )
                )
            }
        }

        if (courses.isEmpty()) {
            return listOf(
                CourseTemplate(
                    courseNumber = "1001.1001",
                    name = "קורס נבחר: $fallbackDegree ($targetSemester)",
                    department = Department.GENERAL,
                    lecturer = "סגל אקדמי",
                    semester = targetSemester,
                    credits = 4.0f
                )
            )
        }

        return courses
    }
}

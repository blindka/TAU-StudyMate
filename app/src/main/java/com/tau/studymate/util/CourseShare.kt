package com.tau.studymate.util

import com.tau.studymate.data.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * A course bundle a student can send to classmates: the course, its open tasks, materials and class times.
 * Plain JSON (no server); personal data such as grades, completion and study sessions is left out.
 */
object CourseShare {
    const val FORMAT = "studymate-course-v1"

    data class Bundle(val course: Course, val tasks: List<Task>, val materials: List<StudyMaterial>, val slots: List<ClassSlot>)

    fun toJson(b: Bundle): String = JSONObject().apply {
        put("format", FORMAT)
        put("course", JSONObject().apply {
            put("number", b.course.courseNumber); put("name", b.course.name); put("department", b.course.department.name)
            put("lecturer", b.course.lecturer); put("semester", b.course.semester); put("credits", b.course.credits.toDouble())
            b.course.examA?.let { put("examA", it) }; b.course.examB?.let { put("examB", it) }
        })
        put("tasks", JSONArray(b.tasks.filter { !it.isCompleted }.map { t ->
            JSONObject().put("title", t.title).put("description", t.description).put("due", t.dueDate)
                .put("priority", t.priority.name).put("minutes", t.estimatedMinutes).put("weekly", t.repeatWeekly)
        }))
        put("materials", JSONArray(b.materials.filter { !it.isMissingOrNeeded }.map { m ->
            JSONObject().put("title", m.title).put("type", m.type.name).put("content", m.contentOrUri)
        }))
        put("slots", JSONArray(b.slots.map { s ->
            JSONObject().put("day", s.dayOfWeek).put("start", s.startMinute).put("end", s.endMinute).put("room", s.room)
        }))
    }.toString(2)

    /** Throws IllegalArgumentException for anything that isn't a StudyMate course file. Ids are 0 (assigned on insert). */
    fun fromJson(text: String): Bundle {
        val o = runCatching { JSONObject(text) }.getOrNull()
        require(o != null && o.optString("format") == FORMAT) { "not a course file" }
        val c = o.getJSONObject("course")
        val course = Course(
            courseNumber = c.getString("number"), name = c.getString("name"), department = Department.fromString(c.optString("department")),
            lecturer = c.optString("lecturer"), semester = c.optString("semester", "סמסטר א'"), credits = c.optDouble("credits", 2.0).toFloat(),
            examA = if (c.has("examA")) c.getLong("examA") else null, examB = if (c.has("examB")) c.getLong("examB") else null
        )
        fun arr(n: String) = o.optJSONArray(n) ?: JSONArray()
        val tasks = (0 until arr("tasks").length()).map { i -> arr("tasks").getJSONObject(i) }.map { t ->
            Task(title = t.getString("title"), description = t.optString("description"), dueDate = t.getLong("due"), department = course.department,
                priority = runCatching { Priority.valueOf(t.optString("priority")) }.getOrDefault(Priority.MEDIUM),
                estimatedMinutes = t.optInt("minutes", 60), repeatWeekly = t.optBoolean("weekly"))
        }
        val materials = (0 until arr("materials").length()).map { i -> arr("materials").getJSONObject(i) }.map { m ->
            StudyMaterial(courseId = 0, title = m.getString("title"), contentOrUri = m.optString("content"),
                type = runCatching { MaterialType.valueOf(m.optString("type")) }.getOrDefault(MaterialType.NOTE))
        }
        val slots = (0 until arr("slots").length()).map { i -> arr("slots").getJSONObject(i) }.map { s ->
            ClassSlot(courseId = 0, dayOfWeek = s.getInt("day").coerceIn(1, 7), startMinute = s.getInt("start"), endMinute = s.getInt("end"), room = s.optString("room"))
        }
        return Bundle(course, tasks, materials, slots)
    }
}

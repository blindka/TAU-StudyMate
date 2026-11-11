package com.tau.studymate.data.repository

import com.tau.studymate.data.local.CourseDao
import com.tau.studymate.data.local.StudyMaterialDao
import com.tau.studymate.data.local.StudySessionDao
import com.tau.studymate.data.local.TaskDao
import com.tau.studymate.data.model.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class StudyRepository(
    private val db: androidx.room.RoomDatabase,
    private val courseDao: CourseDao,
    private val taskDao: TaskDao,
    private val studyMaterialDao: StudyMaterialDao,
    private val studySessionDao: StudySessionDao
) {
    // Courses
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()

    suspend fun getCourseById(id: Long): Course? = courseDao.getCourseById(id)

    suspend fun insertCourse(course: Course): Long = courseDao.insertCourse(course)


    suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course)

    /**
     * Syncs the course list to [courses] in one transaction, matching by course number:
     * courses that stay keep their id, so their materials, tasks and readings stay attached.
     * Only courses that are no longer in the list are removed.
     */
    suspend fun replaceCourses(courses: List<Course>) = db.withTransaction {
        val existing = courseDao.getAllOnce().associateBy { it.courseNumber }
        val keep = courses.map { it.courseNumber }.toSet()
        courseDao.deleteByIds(existing.values.filter { it.courseNumber !in keep }.map { it.id })
        courses.forEach { c ->
            val old = existing[c.courseNumber]
            if (old != null) courseDao.updateCourse(c.copy(id = old.id)) else courseDao.insertCourse(c.copy(id = 0))
        }
    }

    // Tasks
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<Task>> = taskDao.getPendingTasks()

    fun getTasksForCourse(courseId: Long): Flow<List<Task>> =
        taskDao.getTasksForCourse(courseId)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun toggleTaskCompletion(task: Task): Boolean {
        val newStatus = !task.isCompleted
        val updated = task.copy(
            isCompleted = newStatus,
            completedAt = if (newStatus) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
        return newStatus
    }

    // Study Materials & Gaps ("מה חסר לי")
    fun getMaterialsForCourse(courseId: Long): Flow<List<StudyMaterial>> =
        studyMaterialDao.getMaterialsForCourse(courseId)

    val allMissingMaterials: Flow<List<StudyMaterial>> =
        studyMaterialDao.getAllMissingMaterials()

    suspend fun insertMaterial(material: StudyMaterial): Long =
        studyMaterialDao.insertMaterial(material)

    suspend fun deleteMaterial(material: StudyMaterial) =
        studyMaterialDao.deleteMaterial(material)

    // Study Sessions & Habits
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()

    suspend fun logStudySession(
        courseId: Long?,
        taskId: Long?,
        durationMinutes: Int,
        notes: String = ""
    ): Long {
        val session = StudySession(
            courseId = courseId,
            taskId = taskId,
            durationMinutes = durationMinutes,
            notes = notes
        )
        // If tied to a task, update its actual minutes
        if (taskId != null) {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                taskDao.updateTask(task.copy(actualMinutes = task.actualMinutes + durationMinutes))
            }
        }
        return studySessionDao.insertSession(session)
    }

    // Export Functionality (Formatted text for WhatsApp / Email / Notes / Files)
    fun generateExportSummary(tasks: List<Task>, courses: List<Course>): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val courseMap = courses.associateBy { it.id }

        val sb = StringBuilder()
        sb.append("📋 דוח משימות ולימודים - אוניברסיטת תל אביב\n")
        sb.append("נוצר בתאריך: ${dateFormat.format(Date())}\n")
        sb.append("═══════════════════════════════\n\n")

        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }

        sb.append("⏳ מטלות פתוחות (${pending.size}):\n")
        if (pending.isEmpty()) {
            sb.append("אין מטלות פתוחות! כל הכבוד 🎉\n")
        } else {
            pending.forEachIndexed { index, task ->
                val courseName = task.courseId?.let { courseMap[it]?.name } ?: "כללי"
                val dueDateStr = dateFormat.format(Date(task.dueDate))
                sb.append("${index + 1}. [${task.department.displayName}] $courseName: ${task.title}\n")
                sb.append("   תאריך יעד: $dueDateStr | עדיפות: ${task.priority.displayName}\n")
                if (task.description.isNotBlank()) {
                    sb.append("   פירוט: ${task.description}\n")
                }
                sb.append("\n")
            }
        }

        sb.append("═══════════════════════════════\n")
        sb.append("✅ מטלות שהושלמו (${completed.size}):\n")
        completed.forEachIndexed { index, task ->
            val courseName = task.courseId?.let { courseMap[it]?.name } ?: "כללי"
            val completedDateStr = task.completedAt?.let { dateFormat.format(Date(it)) } ?: "-"
            sb.append("${index + 1}. [${task.department.displayName}] $courseName: ${task.title} (הושלם ב-$completedDateStr)\n")
        }

        return sb.toString()
    }
}

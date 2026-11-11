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

    fun getCoursesByDepartment(department: Department): Flow<List<Course>> =
        courseDao.getCoursesByDepartment(department)

    fun searchCourses(query: String): Flow<List<Course>> =
        courseDao.searchCourses(query)

    suspend fun getCourseById(id: Long): Course? = courseDao.getCourseById(id)

    suspend fun insertCourse(course: Course): Long = courseDao.insertCourse(course)

    suspend fun insertCourses(courses: List<Course>) = courseDao.insertCourses(courses)

    suspend fun updateCourse(course: Course) = courseDao.updateCourse(course)

    suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course)

    /** Replaces all courses and tasks atomically. Returns the ids of the removed tasks so their alarms can be cancelled. */
    suspend fun replaceDegreeCurriculum(courses: List<Course>, tasks: List<Task>): List<Long> = db.withTransaction {
        val oldIds = taskDao.getAllTaskIds()
        courseDao.deleteAllCourses()
        taskDao.deleteAllTasks()
        courseDao.insertCourses(courses)
        taskDao.insertTasks(tasks)
        oldIds
    }

    // Tasks
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<Task>> = taskDao.getPendingTasks()

    fun getTasksForCourse(courseId: Long): Flow<List<Task>> =
        taskDao.getTasksForCourse(courseId)

    fun getTasksByDepartment(department: Department): Flow<List<Task>> =
        taskDao.getTasksByDepartment(department)

    fun getUrgentTasks(timestamp: Long): Flow<List<Task>> =
        taskDao.getUrgentTasks(timestamp)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

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

    fun getMaterialsByType(courseId: Long, type: MaterialType): Flow<List<StudyMaterial>> =
        studyMaterialDao.getMaterialsByType(courseId, type)

    val allMissingMaterials: Flow<List<StudyMaterial>> =
        studyMaterialDao.getAllMissingMaterials()

    val missingMaterialsCount: Flow<Int> =
        studyMaterialDao.getMissingMaterialsCount()

    suspend fun insertMaterial(material: StudyMaterial): Long =
        studyMaterialDao.insertMaterial(material)

    suspend fun updateMaterial(material: StudyMaterial) =
        studyMaterialDao.updateMaterial(material)

    suspend fun deleteMaterial(material: StudyMaterial) =
        studyMaterialDao.deleteMaterial(material)

    // Study Sessions & Habits
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()

    fun getSessionsSince(timestamp: Long): Flow<List<StudySession>> =
        studySessionDao.getSessionsSince(timestamp)

    fun getTotalMinutesSince(timestamp: Long): Flow<Int?> =
        studySessionDao.getTotalMinutesSince(timestamp)

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

package com.tau.studymate.ui.viewmodel

import com.tau.studymate.R
import androidx.core.content.edit
import com.tau.studymate.util.appPrefs
import com.tau.studymate.util.decodeScaled
import com.tau.studymate.util.secretPrefs
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tau.studymate.data.model.*
import com.tau.studymate.data.repository.StudyRepository
import com.tau.studymate.notification.NotificationHelper
import com.tau.studymate.notification.TaskNotificationScheduler
import com.tau.studymate.util.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class CelebrationReward(
    val title: String,
    val message: String,
    val completedTaskTitle: String,
    val streakDays: Int
)

enum class TaskFilter {
    ALL, PENDING, COMPLETED
}

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository
) : AndroidViewModel(application) {

    // One Room query for the task table, shared by every list and statistic below
    // (each separate collector used to re-read the whole table on every change).
    private val tasksFlow: Flow<List<Task>> = repository.allTasks
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)


    // User Academic Profile (Degree, Track, Year, Semester)
    private val _academicProfile = MutableStateFlow(UserAcademicProfile())
    val academicProfile: StateFlow<UserAcademicProfile> = _academicProfile.asStateFlow()

    // Saved Schedule Photo & File
    private val _scheduleBitmap = MutableStateFlow<Bitmap?>(null)
    val scheduleBitmap: StateFlow<Bitmap?> = _scheduleBitmap.asStateFlow()

    private val _scheduleFileName = MutableStateFlow<String?>(null)
    val scheduleFileName: StateFlow<String?> = _scheduleFileName.asStateFlow()

    init {
        loadSavedProfile()
    }

    private fun loadSavedProfile() {
        val prefs = getApplication<Application>().appPrefs()
        val savedDegree = prefs.getString("profile_degree_name", null)
        if (savedDegree != null) {
            val isDouble = prefs.getBoolean("profile_is_double_major", true)
            val year = prefs.getString("profile_year", "שנה א'") ?: "שנה א'"
            val semester = prefs.getString("profile_semester", "סמסטר א'") ?: "סמסטר א'"
            val faculty = prefs.getString("profile_faculty", "הפקולטה למדעי החברה") ?: "הפקולטה למדעי החברה"
            _academicProfile.value = UserAcademicProfile(
                degreeName = savedDegree,
                isDoubleMajor = isDouble,
                year = year,
                semester = semester,
                faculty = faculty
            )
        }

        val scheduleName = prefs.getString("saved_schedule_file_name", null)
        _scheduleFileName.value = scheduleName
        val scheduleFile = File(getApplication<Application>().filesDir, "saved_schedule_photo.jpg")
        if (scheduleFile.exists()) viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _scheduleBitmap.value = decodeScaled(scheduleFile, maxSide = 2048)
        }
    }

    fun saveSchedulePhoto(bitmap: Bitmap, fileName: String = "schedule.jpg") {
        viewModelScope.launch {
            try {
                val file = File(getApplication<Application>().filesDir, "saved_schedule_photo.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                _scheduleBitmap.value = bitmap
                _scheduleFileName.value = fileName
                val prefs = getApplication<Application>().appPrefs()
                prefs.edit { putString("saved_schedule_file_name", fileName) }
            } catch (_: Exception) {}
        }
    }

    fun saveScheduleFile(fileName: String, bitmap: Bitmap? = null) {
        viewModelScope.launch {
            _scheduleFileName.value = fileName
            if (bitmap != null) {
                saveSchedulePhoto(bitmap, fileName)
            } else {
                val prefs = getApplication<Application>().appPrefs()
                prefs.edit { putString("saved_schedule_file_name", fileName) }
            }
        }
    }

    fun clearSchedule() {
        viewModelScope.launch {
            _scheduleBitmap.value = null
            _scheduleFileName.value = null
            val file = File(getApplication<Application>().filesDir, "saved_schedule_photo.jpg")
            if (file.exists()) file.delete()
            val prefs = getApplication<Application>().appPrefs()
            prefs.edit { remove("saved_schedule_file_name") }
        }
    }

    // Department Filter
    private val _selectedDepartment = MutableStateFlow<Department?>(null)
    val selectedDepartment: StateFlow<Department?> = _selectedDepartment.asStateFlow()

    // Task Filter (All, Pending, Completed)
    private val _taskFilter = MutableStateFlow(TaskFilter.ALL)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // All courses from DB
    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Tasks Flow
    val filteredTasks: StateFlow<List<Task>> = combine(
        tasksFlow,
        _selectedDepartment,
        _taskFilter,
        _searchQuery
    ) { tasks, dept, filter, query ->
        tasks.filter { task ->
            val matchesDept = dept == null || task.department == dept
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            matchesDept && matchesFilter && matchesQuery
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openTaskCountByCourse: StateFlow<Map<Long, Int>> = tasksFlow
        .map { tasks -> tasks.filter { !it.isCompleted && it.courseId != null }.groupingBy { it.courseId!! }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // All missing materials ("מה חסר לי")
    val missingMaterials: StateFlow<List<StudyMaterial>> = repository.allMissingMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All study sessions
    val studySessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Consecutive days (ending today or yesterday) with a completed task or a study session
    val streakDays: StateFlow<Int> = combine(tasksFlow, repository.allSessions) { tasks, sessions ->
        computeStreak(tasks.mapNotNull { it.completedAt } + sessions.map { it.timestamp })
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Reward / Celebration Event
    private val _celebrationReward = MutableStateFlow<CelebrationReward?>(null)
    val celebrationReward: StateFlow<CelebrationReward?> = _celebrationReward.asStateFlow()

    // Motivational Hebrew messages for reward
    private val rewardQuotes = listOf(
        R.string.celebration_quote_1, R.string.celebration_quote_2, R.string.celebration_quote_3,
        R.string.celebration_quote_4, R.string.celebration_quote_5
    )

    fun setDepartmentFilter(dept: Department?) {
        _selectedDepartment.value = dept
    }

    fun setTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val newlyCompleted = repository.toggleTaskCompletion(task)
            if (newlyCompleted) {
                // נגן צליל הצלחה
                SoundManager.playTaskCompletedSound(getApplication())

                // בטל התראות עתידיות למטלה שהושלמה
                TaskNotificationScheduler.cancelReminder(getApplication(), task.id)

                // Celebrate only the first completion of the day; a popup on every ✓ gets tiresome
                val today = java.time.LocalDate.now().toString()
                val prefs = getApplication<Application>().appPrefs()
                if (prefs.getString("last_celebration_day", null) == today) return@launch
                prefs.edit { putString("last_celebration_day", today) }
                val randomQuote = getApplication<Application>().getString(rewardQuotes.random())
                _celebrationReward.value = CelebrationReward(
                    title = getApplication<Application>().getString(R.string.celebration_title),
                    message = randomQuote,
                    completedTaskTitle = task.title,
                    streakDays = calculateStreakDays()
                )
            } else {
                // Un-checking a task brings its reminders back
                val courseName = task.courseId?.let { repository.getCourseById(it)?.name }
                TaskNotificationScheduler.scheduleTask(getApplication(), task.copy(isCompleted = false), courseName)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            val courseName = task.courseId?.let { repository.getCourseById(it)?.name }
            TaskNotificationScheduler.scheduleTask(getApplication(), task, courseName)
        }
    }

    /** All tasks, ignoring the Tasks screen's search/filters (for Home and statistics). */
    val allTasks: StateFlow<List<Task>> = tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissCelebration() {
        _celebrationReward.value = null
    }

    /** Undo for a delete: same id, so reminders and course links come back as they were. */
    fun restoreTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
            val courseName = task.courseId?.let { repository.getCourseById(it)?.name }
            TaskNotificationScheduler.scheduleTask(getApplication(), task, courseName)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            TaskNotificationScheduler.cancelReminder(getApplication(), task.id)
            repository.deleteTask(task)
        }
    }

    fun addTask(
        title: String,
        description: String,
        dueDate: Long,
        courseId: Long?,
        department: Department,
        priority: Priority,
        estimatedMinutes: Int,
        enableReminder: Boolean = true
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                courseId = courseId,
                department = department,
                priority = priority,
                estimatedMinutes = estimatedMinutes,
                reminderEnabled = enableReminder
            )
            val insertedId = repository.insertTask(task)
            val course = courseId?.let { repository.getCourseById(it) }
            TaskNotificationScheduler.scheduleTask(getApplication(), task.copy(id = insertedId), course?.name)
        }
    }

    fun sendTestNotification() {
        NotificationHelper.showTestNotification(getApplication())
        SoundManager.playTaskCompletedSound(getApplication())
    }

    /**
     * Switch or update the degree curriculum.
     * Looks up matching courses and tasks from the catalog or creates a customized track.
     */
    fun updateDegreeProgram(
        degreeName: String,
        isDoubleMajor: Boolean,
        year: String,
        semester: String,
        importedCourses: List<CourseTemplate>? = null
    ): Boolean {
        val trimmed = degreeName.trim()
        if (trimmed.isEmpty()) return false
        // Any degree is accepted. Catalog degrees come with their course list; others start with the
        // user's current courses, which they add by hand or through Moodle.
        val foundTrack = AcademicCatalog.findProgram(trimmed, getApplication())
        val trackCourses = importedCourses?.takeIf { it.isNotEmpty() } ?: foundTrack?.defaultCourses

        val oldProfile = _academicProfile.value
        val newProfile = UserAcademicProfile(
            degreeName = foundTrack?.name ?: trimmed,
            isDoubleMajor = isDoubleMajor,
            year = year,
            semester = semester,
            faculty = foundTrack?.faculty ?: "הפקולטה למדעי החברה"
        )
        _academicProfile.value = newProfile
        saveProfile(newProfile)

        viewModelScope.launch {
            // Only rebuild the course list when the degree really changed, a schedule was imported,
            // or there are no courses yet. Editing year/semester must never wipe the user's tasks.
            val degreeChanged = newProfile.degreeName != oldProfile.degreeName || newProfile.isDoubleMajor != oldProfile.isDoubleMajor
            if (trackCourses != null && (!importedCourses.isNullOrEmpty() || degreeChanged || repository.allCourses.first().isEmpty())) {
                replaceCurriculum(trackCourses, semester)
            }
        }

        return true
    }

    /**
     * Updates the academic profile for a semester transition without resetting the chosen degree.
     * Optionally imports new courses from a schedule file.
     */
    fun transitionSemester(
        newSemester: String,
        importedCourses: List<CourseTemplate>? = null
    ) {
        val updatedProfile = _academicProfile.value.copy(semester = newSemester)
        _academicProfile.value = updatedProfile
        saveProfile(updatedProfile)
        if (!importedCourses.isNullOrEmpty()) viewModelScope.launch { replaceCurriculum(importedCourses, newSemester) }
    }

    private fun saveProfile(p: UserAcademicProfile) {
        getApplication<Application>().appPrefs().edit { putString("profile_degree_name", p.degreeName); putBoolean("profile_is_double_major", p.isDoubleMajor); putString("profile_year", p.year); putString("profile_semester", p.semester); putString("profile_faculty", p.faculty); putString("last_seen_semester", p.semester) }
    }

    private suspend fun replaceCurriculum(templates: List<CourseTemplate>, semester: String) {
        val newCourses = templates.map { ct ->
            Course(
                courseNumber = ct.courseNumber,
                name = ct.name,
                department = ct.department,
                lecturer = ct.lecturer,
                semester = semester,
                credits = ct.credits
            )
        }
        repository.replaceCourses(newCourses)
    }

    fun addCourse(
        courseNumber: String,
        name: String,
        department: Department,
        lecturer: String,
        semester: String,
        credits: Float
    ) {
        viewModelScope.launch {
            val course = Course(
                courseNumber = courseNumber,
                name = name,
                department = department,
                lecturer = lecturer,
                semester = semester,
                credits = credits
            )
            repository.insertCourse(course)
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch { repository.updateCourse(course) }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    fun addStudyMaterial(
        courseId: Long,
        title: String,
        type: MaterialType,
        contentOrUri: String,
        isMissing: Boolean
    ) {
        viewModelScope.launch {
            val material = StudyMaterial(
                courseId = courseId,
                title = title,
                type = type,
                contentOrUri = contentOrUri,
                isMissingOrNeeded = isMissing
            )
            repository.insertMaterial(material)
        }
    }

    /** "Got it": the material stays in the course, it just leaves the "what's missing" list. */
    fun markMaterialFound(material: StudyMaterial) {
        viewModelScope.launch { repository.updateMaterial(material.copy(isMissingOrNeeded = false)) }
    }

    fun deleteStudyMaterial(material: StudyMaterial) {
        viewModelScope.launch {
            repository.deleteMaterial(material)
        }
    }

    fun getMaterialsForCourse(courseId: Long): Flow<List<StudyMaterial>> {
        return repository.getMaterialsForCourse(courseId)
    }

    fun getTasksForCourse(courseId: Long): Flow<List<Task>> {
        return repository.getTasksForCourse(courseId)
    }

    fun getExportText(): String {
        return repository.generateExportSummary(
            getApplication(),
            tasks = filteredTasks.value,
            courses = courses.value
        )
    }

    fun syncMoodleFeed(feedUrl: String, onComplete: (com.tau.studymate.util.MoodleSyncResult) -> Unit) {
        viewModelScope.launch {
            val (result, _) = com.tau.studymate.util.MoodleSync.syncAndStore(getApplication<Application>() as com.tau.studymate.StudyMateApp, feedUrl)
            onComplete(result)
        }
    }

    fun getSavedMoodleUrl(): String = com.tau.studymate.util.MoodleSync.savedUrl(getApplication())

    private suspend fun calculateStreakDays(): Int =
        computeStreak(repository.allTasks.first().mapNotNull { it.completedAt } + repository.allSessions.first().map { it.timestamp })
}

/** Days in a row with activity, counting back from today (or yesterday, so the streak survives until tonight). */
internal fun computeStreak(timestamps: List<Long>, now: Long = System.currentTimeMillis()): Int {
    val zone = java.time.ZoneId.systemDefault()
    val days = timestamps.map { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
    var day = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    if (day !in days) day = day.minusDays(1)
    var count = 0
    while (day in days) { count++; day = day.minusDays(1) }
    return count
}

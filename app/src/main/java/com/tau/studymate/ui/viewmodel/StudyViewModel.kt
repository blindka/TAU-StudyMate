package com.tau.studymate.ui.viewmodel

import android.app.Application
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
        repository.allTasks,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All missing materials ("מה חסר לי")
    val missingMaterials: StateFlow<List<StudyMaterial>> = repository.allMissingMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All study sessions
    val studySessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reward / Celebration Event
    private val _celebrationReward = MutableStateFlow<CelebrationReward?>(null)
    val celebrationReward: StateFlow<CelebrationReward?> = _celebrationReward.asStateFlow()

    // Study Habit Timer State
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // Timer Mode: Countdown (Pomodoro) or Stopwatch
    private val _isCountdownMode = MutableStateFlow(true)
    val isCountdownMode: StateFlow<Boolean> = _isCountdownMode.asStateFlow()

    private val _targetMinutes = MutableStateFlow(25)
    val targetMinutes: StateFlow<Int> = _targetMinutes.asStateFlow()

    private val _timerSeconds = MutableStateFlow(25 * 60)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _timerCourseId = MutableStateFlow<Long?>(null)
    val timerCourseId: StateFlow<Long?> = _timerCourseId.asStateFlow()

    private val _timerTaskId = MutableStateFlow<Long?>(null)
    val timerTaskId: StateFlow<Long?> = _timerTaskId.asStateFlow()

    private val _timerFinishedDialog = MutableStateFlow<String?>(null)
    val timerFinishedDialog: StateFlow<String?> = _timerFinishedDialog.asStateFlow()

    private var timerJob: Job? = null

    // Motivational Hebrew messages for reward
    private val rewardQuotes = listOf(
        "אלוף/ה! עוד צעד משמעותי לעבר התואר! 🎓",
        "מטלה נוספת נמחקה! מגיע לך הפסקת קפה בבניין נפתלי ☕",
        "התמדה מנצחת! ההרגלים שלך משתפרים מיום ליום ✨",
        "וואו, איזה קצב! הראש פנוי לעוד הישג 🚀",
        "מעולה! תקשורת ולימודי עבודה קטנים עליך! 💪"
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

                // Trigger gamified celebration reward
                val randomQuote = rewardQuotes.random()
                _celebrationReward.value = CelebrationReward(
                    title = "מטלה הושלמה בהצלחה!",
                    message = randomQuote,
                    completedTaskTitle = task.title,
                    streakDays = calculateStreakDays()
                )
            }
        }
    }

    fun dismissCelebration() {
        _celebrationReward.value = null
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
                estimatedMinutes = estimatedMinutes
            )
            val insertedId = repository.insertTask(task)
            if (enableReminder) {
                val course = courseId?.let { repository.getCourseById(it) }
                val fullTask = task.copy(id = insertedId)
                TaskNotificationScheduler.scheduleTask(getApplication(), fullTask, course?.name)
            }
        }
    }

    fun sendTestNotification() {
        NotificationHelper.showTestNotification(getApplication())
        SoundManager.playTaskCompletedSound(getApplication())
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

    // Timer Controls
    fun setTimerPreset(isCountdown: Boolean, minutes: Int) {
        if (_isTimerRunning.value) return
        _isCountdownMode.value = isCountdown
        _targetMinutes.value = minutes
        _timerSeconds.value = if (isCountdown) minutes * 60 else 0
    }

    fun startTimer(courseId: Long? = null, taskId: Long? = null) {
        if (_isTimerRunning.value) return
        _timerCourseId.value = courseId ?: _timerCourseId.value
        _timerTaskId.value = taskId ?: _timerTaskId.value
        _isTimerRunning.value = true

        if (_isCountdownMode.value && _timerSeconds.value <= 0) {
            _timerSeconds.value = _targetMinutes.value * 60
        }

        timerJob = viewModelScope.launch {
            if (_isCountdownMode.value) {
                while (_isTimerRunning.value && _timerSeconds.value > 0) {
                    delay(1000)
                    _timerSeconds.value -= 1
                }
                if (_isTimerRunning.value && _timerSeconds.value <= 0) {
                    _isTimerRunning.value = false
                    // השמעת סאונד סיום טיימר
                    SoundManager.playTimerFinishedSound(getApplication())
                    // שליחת התראה
                    NotificationHelper.showTimerNotification(
                        getApplication(),
                        "זמן הלמידה הסתיים! ⏰",
                        "כל הכבוד על פוקוס של ${_targetMinutes.value} דקות! הגיע הזמן להפסקה קלה."
                    )
                    // תיעוד סשן למידה אוטומטי
                    repository.logStudySession(
                        courseId = _timerCourseId.value,
                        taskId = _timerTaskId.value,
                        durationMinutes = _targetMinutes.value,
                        notes = "סשן פוקוס הושלם בהצלחה"
                    )
                    _timerFinishedDialog.value = "השלמת בהצלחה סשן למידה של ${_targetMinutes.value} דקות! 🎯"
                }
            } else {
                while (_isTimerRunning.value) {
                    delay(1000)
                    _timerSeconds.value += 1
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _timerSeconds.value = if (_isCountdownMode.value) _targetMinutes.value * 60 else 0
    }

    fun dismissTimerFinishedDialog() {
        _timerFinishedDialog.value = null
        resetTimer()
    }

    fun finishTimerSession(notes: String = "") {
        val totalMinutes = if (_isCountdownMode.value) {
            val elapsed = (_targetMinutes.value * 60) - _timerSeconds.value
            (elapsed / 60).coerceAtLeast(1)
        } else {
            (_timerSeconds.value / 60).coerceAtLeast(1)
        }
        val courseId = _timerCourseId.value
        val taskId = _timerTaskId.value

        viewModelScope.launch {
            // השמעת צליל סיום
            SoundManager.playTimerFinishedSound(getApplication())

            repository.logStudySession(
                courseId = courseId,
                taskId = taskId,
                durationMinutes = totalMinutes,
                notes = notes
            )
            resetTimer()
        }
    }

    fun getExportText(): String {
        return repository.generateExportSummary(
            tasks = filteredTasks.value.ifEmpty { courses.value.let { emptyList() } },
            courses = courses.value
        )
    }

    private fun calculateStreakDays(): Int {
        return 4
    }
}

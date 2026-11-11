package com.tau.studymate.ui.viewmodel

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tau.studymate.data.repository.StudyRepository
import com.tau.studymate.receiver.TimerAlarmReceiver
import com.tau.studymate.util.SoundManager
import com.tau.studymate.util.appPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Focus timer driven by the wall clock, not by counting ticks: the remaining time is always
 * end − now, so it stays right when the screen is locked or the app is killed. State lives in prefs;
 * the end-of-session alert is an AlarmManager alarm (TimerAlarmReceiver).
 */
class TimerViewModel(application: Application, private val repository: StudyRepository) : AndroidViewModel(application) {

    private val prefs = application.appPrefs()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    private val _isCountdownMode = MutableStateFlow(prefs.getBoolean(K_MODE, true))
    val isCountdownMode: StateFlow<Boolean> = _isCountdownMode.asStateFlow()
    private val _targetMinutes = MutableStateFlow(prefs.getInt(K_TARGET, 25))
    val targetMinutes: StateFlow<Int> = _targetMinutes.asStateFlow()
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
    private val _timerFinishedDialog = MutableStateFlow<String?>(null)
    val timerFinishedDialog: StateFlow<String?> = _timerFinishedDialog.asStateFlow()

    /** Countdown: end time. Stopwatch: start time. 0 when paused. */
    private var anchor = prefs.getLong(K_ANCHOR, 0L)
    /** Seconds shown while paused (remaining for countdown, elapsed for stopwatch). */
    private var frozen = prefs.getInt(K_FROZEN, if (_isCountdownMode.value) _targetMinutes.value * 60 else 0)
    private var courseId: Long? = prefs.getLong(K_COURSE, -1).takeIf { it >= 0 }
    private var taskId: Long? = prefs.getLong(K_TASK, -1).takeIf { it >= 0 }
    private var completion: Job? = null

    init {
        _timerSeconds.value = frozen
        if (anchor > 0) { _isTimerRunning.value = true; scheduleCompletion() } // resume after process death
        // Refresh the display only while someone shows it (Focus screen, foreground); no wake-ups in the background
        viewModelScope.launch {
            _timerSeconds.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { visible ->
                while (visible) {
                    if (_isTimerRunning.value) _timerSeconds.value = currentSeconds()
                    delay(250)
                }
            }
        }
    }

    private fun now() = System.currentTimeMillis()

    private fun currentSeconds(): Int = timerSecondsAt(_isCountdownMode.value, anchor, frozen, now())

    /** One suspended wait until the countdown's end (no polling), then log the session. */
    private fun scheduleCompletion() {
        completion?.cancel()
        if (!_isCountdownMode.value) return
        completion = viewModelScope.launch {
            delay((anchor - now()).coerceAtLeast(0))
            if (_isTimerRunning.value) complete()
        }
    }

    private suspend fun complete() {
        val minutes = _targetMinutes.value
        _isTimerRunning.value = false
        anchor = 0; frozen = minutes * 60
        _timerSeconds.value = 0
        save()
        repository.logStudySession(courseId = courseId, taskId = taskId, durationMinutes = minutes, notes = "סשן פוקוס הושלם בהצלחה")
        _timerFinishedDialog.value = "השלמת בהצלחה סשן למידה של $minutes דקות! 🎯"
    }

    private fun save() = prefs.edit {
        putBoolean(K_MODE, _isCountdownMode.value); putInt(K_TARGET, _targetMinutes.value)
        putLong(K_ANCHOR, anchor); putInt(K_FROZEN, frozen)
        putLong(K_COURSE, courseId ?: -1); putLong(K_TASK, taskId ?: -1)
    }

    fun setTimerPreset(isCountdown: Boolean, minutes: Int) {
        if (_isTimerRunning.value) return
        _isCountdownMode.value = isCountdown
        _targetMinutes.value = minutes
        frozen = if (isCountdown) minutes * 60 else 0
        _timerSeconds.value = frozen
        save()
    }

    fun startTimer(courseId: Long? = null, taskId: Long? = null) {
        if (_isTimerRunning.value) return
        courseId?.let { this.courseId = it }
        taskId?.let { this.taskId = it }
        if (_isCountdownMode.value) {
            if (frozen <= 0) frozen = _targetMinutes.value * 60
            anchor = now() + frozen * 1000L
            TimerAlarmReceiver.schedule(getApplication(), anchor, _targetMinutes.value)
        } else {
            anchor = now()
        }
        _isTimerRunning.value = true
        save()
        scheduleCompletion()
    }

    fun pauseTimer() {
        if (!_isTimerRunning.value) return
        frozen = currentSeconds()
        anchor = 0
        _isTimerRunning.value = false
        completion?.cancel()
        TimerAlarmReceiver.cancel(getApplication())
        _timerSeconds.value = frozen
        save()
    }

    fun resetTimer() {
        pauseTimer()
        frozen = if (_isCountdownMode.value) _targetMinutes.value * 60 else 0
        _timerSeconds.value = frozen
        save()
    }

    fun dismissTimerFinishedDialog() {
        _timerFinishedDialog.value = null
        resetTimer()
    }

    fun finishTimerSession(notes: String = "") {
        pauseTimer()
        val elapsed = if (_isCountdownMode.value) _targetMinutes.value * 60 - frozen else frozen
        val minutes = (elapsed / 60).coerceAtLeast(1)
        val c = courseId; val t = taskId
        viewModelScope.launch {
            SoundManager.playTimerFinishedSound(getApplication())
            repository.logStudySession(courseId = c, taskId = t, durationMinutes = minutes, notes = notes)
            resetTimer()
        }
    }

    private companion object {
        const val K_MODE = "timer_countdown"; const val K_TARGET = "timer_target"; const val K_ANCHOR = "timer_anchor"
        const val K_FROZEN = "timer_frozen"; const val K_COURSE = "timer_course"; const val K_TASK = "timer_task"
    }
}

/** Seconds to show: countdown = time left until [anchor] (rounded up), stopwatch = [frozen] + time since [anchor]. */
internal fun timerSecondsAt(countdown: Boolean, anchor: Long, frozen: Int, now: Long): Int =
    if (countdown) ((anchor - now + 999) / 1000).toInt().coerceAtLeast(0)
    else frozen + ((now - anchor) / 1000).toInt()

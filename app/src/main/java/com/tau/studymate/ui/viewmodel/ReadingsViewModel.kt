package com.tau.studymate.ui.viewmodel

import com.tau.studymate.R
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tau.studymate.data.local.ReadingDao
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Reading
import com.tau.studymate.util.PdfStore
import com.tau.studymate.util.Summarizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** Israeli study weeks start on Sunday. */
fun weekStartOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingsViewModel(
    application: Application,
    private val dao: ReadingDao,
    coursesFlow: Flow<List<Course>>
) : AndroidViewModel(application) {

    private fun str(id: Int, vararg a: Any) = getApplication<Application>().getString(id, *a)

    val courses: StateFlow<List<Course>> = coursesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _week = MutableStateFlow(weekStartOf(LocalDate.now()))
    val week: StateFlow<LocalDate> = _week.asStateFlow()

    private val _courseFilter = MutableStateFlow<Long?>(null)
    val courseFilter: StateFlow<Long?> = _courseFilter.asStateFlow()

    /** Always the current calendar week, for the Home progress bar. */
    val currentWeek: StateFlow<List<Reading>> = dao.getForWeek(weekStartOf(LocalDate.now()).toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val weekReadings = _week.flatMapLatest { dao.getForWeek(it.toEpochDay()) }

    /** All readings of the selected week; used for the progress line regardless of the filter. */
    val allThisWeek: StateFlow<List<Reading>> = weekReadings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readings: StateFlow<List<Reading>> = combine(weekReadings, _courseFilter) { list, cid ->
        if (cid == null) list else list.filter { it.courseId == cid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    fun shiftWeek(delta: Long) { _week.value = _week.value.plusWeeks(delta) }
    fun setCourseFilter(id: Long?) { _courseFilter.value = id }

    fun import(uris: List<Uri>, courseId: Long?, week: LocalDate) {
        viewModelScope.launch {
            var ok = 0
            for (uri in uris) {
                try {
                    val f = PdfStore.import(getApplication(), uri)
                    dao.insert(Reading(courseId = courseId, title = f.title, filePath = f.path, pageCount = f.pageCount, weekStartEpochDay = week.toEpochDay()))
                    ok++
                } catch (e: Exception) {
                    _message.value = e.message ?: str(R.string.import_failed)
                }
            }
            if (ok > 0) _message.value = getApplication<Application>().resources.getQuantityString(R.plurals.import_added, ok, ok)
            _week.value = week
        }
    }

    fun rename(r: Reading, title: String) = viewModelScope.launch { if (title.isNotBlank()) dao.update(r.copy(title = title.trim())) }

    fun toggleRead(r: Reading) = viewModelScope.launch { dao.update(r.copy(isRead = !r.isRead)) }
    fun markRead(r: Reading) = viewModelScope.launch { dao.update(r.copy(isRead = true)) }

    fun delete(r: Reading) = viewModelScope.launch {
        dao.delete(r)
        File(r.filePath).delete()
        translationFile(r).delete()
    }

    fun observe(id: Long): Flow<Reading?> = dao.observe(id)

    // --- Summary (Gemini Nano, on device) ---
    private val _summaryStatus = MutableStateFlow<String?>(null)
    val summaryStatus: StateFlow<String?> = _summaryStatus.asStateFlow()

    private var summaryJob: kotlinx.coroutines.Job? = null
    private var translationJob: kotlinx.coroutines.Job? = null

    /** Stops a running summary or translation; partial results are dropped. */
    fun cancelWork() {
        summaryJob?.cancel(); translationJob?.cancel()
    }

    fun summarize(r: Reading) {
        if (_summaryStatus.value != null) return
        summaryJob = viewModelScope.launch {
            _summaryStatus.value = str(R.string.reading_document)
            try {
                val text = PdfStore.extractText(getApplication(), r.filePath, Summarizer.MAX_INPUT_CHARS)
                when {
                    text == null -> _message.value = str(R.string.no_text_in_pdf)
                    else -> when (val res = Summarizer.summarize(getApplication(), text) { _summaryStatus.value = it }) {
                        is Summarizer.Result.Ok -> dao.update(r.copy(summary = res.text))
                        is Summarizer.Result.Error -> _message.value = res.message
                    }
                }
            } finally {
                _summaryStatus.value = null
            }
        }
    }

    // --- Translation EN → HE (ML Kit, on device). Cached next to the PDF, one page per form feed. ---
    private fun translationFile(r: Reading) = File(r.filePath + ".he.txt")

    private val _translationStatus = MutableStateFlow<String?>(null)
    val translationStatus: StateFlow<String?> = _translationStatus.asStateFlow()

    private val _translation = MutableStateFlow<Pair<Long, List<String>>?>(null)
    val translation: StateFlow<Pair<Long, List<String>>?> = _translation.asStateFlow()

    /** Loads a cached translation, if this reading was translated before. */
    fun loadTranslation(r: Reading) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val f = translationFile(r)
        _translation.value = if (f.exists()) r.id to f.readText().split('\u000C') else null
    }

    fun translate(r: Reading) {
        if (_translationStatus.value != null) return
        translationJob = viewModelScope.launch {
            _translationStatus.value = str(R.string.reading_document)
            try {
                val pages = PdfStore.extractPages(getApplication(), r.filePath)
                if (pages == null) {
                    _message.value = str(R.string.no_text_in_pdf)
                    return@launch
                }
                val he = com.tau.studymate.util.Translator.translatePages(getApplication(), pages) { _translationStatus.value = it }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { translationFile(r).writeText(he.joinToString("\u000C")) }
                _translation.value = r.id to he
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = str(R.string.translation_failed, e.localizedMessage ?: str(R.string.check_internet_for_model))
            } finally {
                _translationStatus.value = null
            }
        }
    }
}

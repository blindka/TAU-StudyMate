package com.tau.studymate.util

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

/** On-device summaries with Gemini Nano (ML Kit GenAI Prompt API). Nothing leaves the phone. */
object Summarizer {

    sealed interface Result {
        data class Ok(val text: String) : Result
        data class Error(val message: String) : Result
    }

    // shortcut: Gemini Nano's input window is small (~4k tokens); we summarize only the start of long files.
    const val MAX_INPUT_CHARS = 6000

    suspend fun summarize(documentText: String, onStatus: (String) -> Unit): Result {
        val model = Generation.getClient()
        return try {
            when (model.checkStatus()) {
                FeatureStatus.UNAVAILABLE ->
                    return Result.Error("סיכום אוטומטי לא נתמך בטלפון הזה (דורש טלפון עם Gemini Nano, כמו Pixel 9 ומעלה או Galaxy S24 ומעלה).")
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                    onStatus("מוריד את מודל הסיכום (פעם אחת בלבד)…")
                    var failed: Throwable? = null
                    model.download().collect { if (it is DownloadStatus.DownloadFailed) failed = it.e }
                    failed?.let { return Result.Error("הורדת המודל נכשלה: ${it.localizedMessage}") }
                }
            }
            onStatus("מסכם…")
            val prompt = """
                סכם בעברית את המסמך הבא עבור סטודנט שמתכונן למבחן.
                מבנה התשובה:
                רעיון מרכזי: משפט אחד או שניים.
                נקודות חשובות: 3 עד 6 נקודות קצרות, כל אחת בשורה שמתחילה ב"•".
                מושגים למבחן: רשימה קצרה מופרדת בנקודות.

                המסמך:
                ${documentText.take(MAX_INPUT_CHARS)}
            """.trimIndent()
            val text = model.generateContent(prompt).candidates.firstOrNull()?.text
            if (text.isNullOrBlank()) Result.Error("לא התקבל סיכום. נסה שוב.") else Result.Ok(text.trim())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // user pressed "עצור"
        } catch (e: Exception) {
            Result.Error("שגיאה ביצירת הסיכום: ${e.localizedMessage ?: e.javaClass.simpleName}")
        } finally {
            model.close()
        }
    }
}

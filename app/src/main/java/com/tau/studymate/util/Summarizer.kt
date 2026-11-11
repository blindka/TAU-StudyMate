package com.tau.studymate.util

import com.tau.studymate.R
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

    suspend fun summarize(context: android.content.Context, documentText: String, onStatus: (String) -> Unit): Result {
        fun s(id: Int, vararg a: Any) = context.getString(id, *a)
        val model = Generation.getClient()
        return try {
            when (model.checkStatus()) {
                FeatureStatus.UNAVAILABLE ->
                    return Result.Error(s(R.string.summary_unsupported))
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                    onStatus(s(R.string.summary_downloading_model))
                    var failed: Throwable? = null
                    model.download().collect { if (it is DownloadStatus.DownloadFailed) failed = it.e }
                    failed?.let { return Result.Error(s(R.string.summary_download_failed, it.localizedMessage ?: "")) }
                }
            }
            onStatus(s(R.string.summarizing))
            // The prompt (and so the summary language) follows the app language
            val prompt = s(R.string.summary_prompt, documentText.take(MAX_INPUT_CHARS))
            val text = model.generateContent(prompt).candidates.firstOrNull()?.text
            if (text.isNullOrBlank()) Result.Error(s(R.string.summary_empty)) else Result.Ok(text.trim())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // user pressed Stop
        } catch (e: Exception) {
            Result.Error(s(R.string.summary_error, e.localizedMessage ?: e.javaClass.simpleName))
        } finally {
            model.close()
        }
    }
}

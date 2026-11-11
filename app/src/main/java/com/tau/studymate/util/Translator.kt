package com.tau.studymate.util

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** English → Hebrew with ML Kit's offline model. The text never leaves the phone; only the ~30MB model is downloaded once. */
object Translator {

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resumeWithException(it) }
    }

    /** Translates page by page, paragraph by paragraph (the model works best on short inputs). */
    suspend fun translatePages(pages: List<String>, onStatus: (String) -> Unit): List<String> {
        val client = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.HEBREW)
                .build()
        )
        try {
            onStatus("מוריד את מודל התרגום (פעם אחת בלבד, כ-30MB)…")
            client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            return pages.mapIndexed { i, page ->
                onStatus("מתרגם עמוד ${i + 1} מתוך ${pages.size}…")
                page.split(Regex("\\n\\s*\\n")).map { para ->
                    val line = para.replace(Regex("\\s*\\n\\s*"), " ").trim() // undo PDF line breaks inside a paragraph
                    if (line.isBlank()) "" else client.translate(line).await()
                }.joinToString("\n\n")
            }
        } finally {
            client.close()
        }
    }
}

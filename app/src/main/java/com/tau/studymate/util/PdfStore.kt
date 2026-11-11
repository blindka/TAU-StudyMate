package com.tau.studymate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.UUID

/** Imported PDFs live in the app's private storage, so they never clutter the user's Downloads folder. */
object PdfStore {

    data class Imported(val title: String, val path: String, val pageCount: Int)

    suspend fun import(context: Context, uri: Uri): Imported = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "readings").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.pdf")
        val input = context.contentResolver.openInputStream(uri) ?: error("לא ניתן לפתוח את הקובץ")
        input.use { inp -> file.outputStream().use { inp.copyTo(it) } }
        val pages = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { PdfRenderer(it).use { r -> r.pageCount } }
        } catch (e: Exception) {
            file.delete() // not a valid PDF: don't leave junk behind
            throw IllegalArgumentException("הקובץ אינו PDF תקין", e)
        }
        Imported(displayName(context, uri), file.absolutePath, pages)
    }

    private fun displayName(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.substringBeforeLast('.')?.replace('_', ' ')?.trim()?.ifBlank { null } ?: "מסמך"

    /**
     * Text of every page, or null when it can't be read (Android 8, or a scanned PDF with no text layer).
     * Android 15+ uses the built-in PdfRenderer text API; Android 9–14 use androidx.pdf.
     */
    suspend fun extractPages(context: Context, path: String): List<String>? = withContext(Dispatchers.IO) {
        val pages = when {
            Build.VERSION.SDK_INT >= 35 ->
                ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { r ->
                        (0 until r.pageCount).map { i -> r.openPage(i).use { p -> p.textContents.joinToString("\n") { it.text } } }
                    }
                }
            Build.VERSION.SDK_INT >= 28 -> extractPagesAndroidxPdf(context, path)
            else -> null
        }
        pages?.takeIf { list -> list.any { it.isNotBlank() } }
    }

    /** androidx.pdf path (Android 9+). Separate so the device test can exercise it on any OS version. */
    internal suspend fun extractPagesAndroidxPdf(context: Context, path: String): List<String>? = runCatching {
        // The library runs in a separate sandboxed process; never let a stuck bind freeze the feature
        kotlinx.coroutines.withTimeoutOrNull(30_000) {
            androidx.pdf.SandboxedPdfLoader(context.applicationContext, Dispatchers.IO)
                .openDocument(Uri.fromFile(File(path)), null).use { doc ->
                    (0 until doc.pageCount).map { i -> doc.getPageContent(i)?.textContents?.joinToString("\n") { it.text }.orEmpty() }
                }
        }
    }.getOrNull()

    /** First [maxChars] characters of the document's text, for the summary prompt. */
    suspend fun extractText(context: Context, path: String, maxChars: Int): String? =
        extractPages(context, path)?.joinToString("\n")?.take(maxChars)?.ifBlank { null }

    /** Renders pages on demand. PdfRenderer allows one open page at a time, hence the mutex. */
    class Document(path: String) : Closeable {
        private val fd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        private val renderer = PdfRenderer(fd)
        private val lock = Mutex()
        val pageCount get() = renderer.pageCount

        suspend fun render(index: Int, widthPx: Int): Bitmap = lock.withLock {
            withContext(Dispatchers.IO) {
                renderer.openPage(index).use { page ->
                    val h = (widthPx.toFloat() * page.height / page.width).toInt()
                    Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(Color.WHITE)
                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }

        // Close in the background after any in-flight render, so leaving the reader never blocks the UI
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        override fun close() {
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { lock.withLock { renderer.close(); fd.close() } }
        }
    }
}

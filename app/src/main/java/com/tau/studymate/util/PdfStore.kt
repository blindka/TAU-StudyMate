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
        }?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "מסמך"

    /** Page text needs Android 15+ (PdfRenderer text API). Returns null when the device can't extract text. */
    suspend fun extractText(path: String, maxChars: Int): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 35) return@withContext null
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                val sb = StringBuilder()
                for (i in 0 until r.pageCount) {
                    if (sb.length >= maxChars) break
                    r.openPage(i).use { p -> p.textContents.forEach { sb.append(it.text).append('\n') } }
                }
                sb.toString().take(maxChars).ifBlank { null }
            }
        }
    }

    /** Text of every page (Android 15+). Null when the device can't extract text or the PDF is scanned images. */
    suspend fun extractPages(path: String): List<String>? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 35) return@withContext null
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                (0 until r.pageCount).map { i -> r.openPage(i).use { p -> p.textContents.joinToString("\n") { it.text } } }
            }
        }.takeIf { pages -> pages.any { it.isNotBlank() } }
    }

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

        // Wait for an in-flight render so the renderer isn't closed under it
        override fun close() = kotlinx.coroutines.runBlocking { lock.withLock { renderer.close(); fd.close() } }
    }
}

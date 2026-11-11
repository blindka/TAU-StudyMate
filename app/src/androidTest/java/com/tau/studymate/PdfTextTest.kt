package com.tau.studymate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tau.studymate.util.PdfStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The androidx.pdf fallback (used on Android 9–14) must read the same text as the framework API. */
@RunWith(AndroidJUnit4::class)
class PdfTextTest {
    @Test
    fun androidxPdfReadsText() = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        val file = File(inst.targetContext.cacheDir, "sample.pdf")
        inst.context.assets.open("sample.pdf").use { i -> file.outputStream().use { i.copyTo(it) } }
        val pages = PdfStore.extractPagesAndroidxPdf(inst.targetContext, file.absolutePath)
        assertTrue("got: $pages", pages?.joinToString()?.contains("opinion leaders") == true)
    }
}

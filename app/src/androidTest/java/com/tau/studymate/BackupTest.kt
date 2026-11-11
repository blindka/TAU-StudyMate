package com.tau.studymate

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tau.studymate.util.BackupManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipFile

/** Export must produce a zip with the database in it (restore is checked by hand: it restarts the app). */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    @Test
    fun exportContainsDatabase() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val out = File(ctx.cacheDir, "backup-test.zip")
        BackupManager.export(ctx, Uri.fromFile(out))
        val names = ZipFile(out).use { z -> z.entries().toList().map { it.name } }
        assertTrue(names.toString(), "databases/tau_study_mate_db" in names)
    }
}

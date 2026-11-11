package com.tau.studymate.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tau.studymate.StudyMateApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Full backup as one .zip: the database, the imported PDFs (with translations) and app settings.
 * The Moodle link (secret prefs) is left out on purpose. Restoring replaces everything and restarts the app;
 * Room migrations upgrade a backup made by an older version.
 */
object BackupManager {
    private const val DB = "tau_study_mate_db"
    private const val PREFS = "shared_prefs/tau_studymate_prefs.xml"

    suspend fun export(context: Context, target: Uri) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as StudyMateApp
        // Flush the write-ahead log so the single .db file holds everything
        app.database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dataDir = File(app.applicationInfo.dataDir)
        val files = buildList {
            add("databases/$DB" to app.getDatabasePath(DB))
            File(app.filesDir, "readings").listFiles()?.forEach { add("files/readings/${it.name}" to it) }
            File(dataDir, PREFS).takeIf { it.exists() }?.let { add(PREFS to it) }
        }
        app.contentResolver.openOutputStream(target)?.use { out ->
            ZipOutputStream(out).use { zip ->
                for ((name, file) in files) {
                    zip.putNextEntry(ZipEntry(name)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
        } ?: error("לא ניתן לכתוב את הקובץ")
    }

    /** Throws if the file isn't a StudyMate backup. On success the app restarts. */
    suspend fun restore(context: Context, source: Uri) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as StudyMateApp
        val dataDir = File(app.applicationInfo.dataDir).canonicalFile
        val staging = File(app.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var hasDb = false
        app.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { e ->
                    val allowed = e.name == "databases/$DB" || e.name == PREFS || e.name.startsWith("files/readings/")
                    val out = File(staging, e.name).canonicalFile
                    // Only our own paths, and no "../" escapes (zip slip)
                    if (!allowed || e.isDirectory || !out.path.startsWith(staging.canonicalPath)) return@forEach
                    out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) }
                    if (e.name == "databases/$DB") hasDb = true
                }
            }
        } ?: error("לא ניתן לקרוא את הקובץ")
        require(hasDb) { "זה לא קובץ גיבוי של StudyMate" }

        app.database.close()
        File(app.getDatabasePath(DB).path + "-wal").delete(); File(app.getDatabasePath(DB).path + "-shm").delete()
        File(app.filesDir, "readings").deleteRecursively()
        staging.walkTopDown().filter { it.isFile }.forEach { f ->
            val dest = File(dataDir, f.relativeTo(staging).path)
            dest.parentFile?.mkdirs(); f.copyTo(dest, overwrite = true)
        }
        staging.deleteRecursively()
        // Restart so every screen and the prefs cache read the restored data
        app.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
            app.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        Runtime.getRuntime().exit(0)
    }
}

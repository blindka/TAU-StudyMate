package com.tau.studymate

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tau.studymate.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Upgrading must keep user data. Fails if a migration's SQL doesn't match the entities. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To5KeepsTasks() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO courses (id, courseNumber, name, department, lecturer, semester, credits, colorHex) VALUES (1, '1082.1100', 'מבוא', 'COMMUNICATION', '', 'סמסטר א''', 4.0, '#000000')")
            execSQL("INSERT INTO tasks (id, courseId, title, description, dueDate, isCompleted, estimatedMinutes, actualMinutes, department, priority) VALUES (7, 1, 'מטלה', '', 0, 0, 60, 0, 'COMMUNICATION', 'MEDIUM')")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 5, true, AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
        db.query("SELECT title, reminderEnabled, externalId FROM tasks WHERE id = 7").use {
            it.moveToFirst()
            assertEquals("מטלה", it.getString(0))
            assertEquals(1, it.getInt(1)) // existing tasks keep reminders on
            assertEquals(true, it.isNull(2)) // no Moodle UID for old tasks
        }
        db.query("SELECT COUNT(*) FROM readings").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
    }
}

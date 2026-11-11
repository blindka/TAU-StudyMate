package com.tau.studymate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Reading
import com.tau.studymate.data.model.StudyMaterial
import com.tau.studymate.data.model.StudySession
import com.tau.studymate.data.model.Task

@Database(
    entities = [Course::class, Task::class, StudyMaterial::class, StudySession::class, Reading::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun taskDao(): TaskDao
    abstract fun studyMaterialDao(): StudyMaterialDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun readingDao(): ReadingDao

    companion object {
        internal val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        internal val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseId` INTEGER, `title` TEXT NOT NULL, `filePath` TEXT NOT NULL, `pageCount` INTEGER NOT NULL, `weekStartEpochDay` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `summary` TEXT, `addedAt` INTEGER NOT NULL, FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_readings_courseId` ON `readings` (`courseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_readings_weekStartEpochDay` ON `readings` (`weekStartEpochDay`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tau_study_mate_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.tau.studymate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.MaterialType
import com.tau.studymate.data.model.Priority
import com.tau.studymate.data.model.StudyMaterial
import com.tau.studymate.data.model.StudySession
import com.tau.studymate.data.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Course::class, Task::class, StudyMaterial::class, StudySession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun taskDao(): TaskDao
    abstract fun studyMaterialDao(): StudyMaterialDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tau_study_mate_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialTAUData(database)
                    }
                }
            }

            private suspend fun populateInitialTAUData(db: AppDatabase) {
                val courseDao = db.courseDao()
                val taskDao = db.taskDao()
                val materialDao = db.studyMaterialDao()

                // Default TAU Courses: Communication & Labor Studies
                val c1 = Course(
                    id = 1,
                    courseNumber = "1082.1100",
                    name = "מבוא לתקשורת המונים",
                    department = Department.COMMUNICATION,
                    lecturer = "פרופ' שוורץ",
                    semester = "סמסטר א'",
                    credits = 4.0f
                )
                val c2 = Course(
                    id = 2,
                    courseNumber = "1082.2001",
                    name = "תקשורת דיגיטלית ורשתות חברתיות",
                    department = Department.COMMUNICATION,
                    lecturer = "ד\"ר לוי",
                    semester = "סמסטר א'",
                    credits = 3.0f
                )
                val c3 = Course(
                    id = 3,
                    courseNumber = "1051.1100",
                    name = "מבוא ללימודי עבודה",
                    department = Department.LABOR_STUDIES,
                    lecturer = "ד\"ר כהן",
                    semester = "סמסטר א'",
                    credits = 4.0f
                )
                val c4 = Course(
                    id = 4,
                    courseNumber = "1051.2100",
                    name = "יחסי עבודה ודיני עבודה בישראל",
                    department = Department.LABOR_STUDIES,
                    lecturer = "פרופ' פרידמן",
                    semester = "סמסטר א'",
                    credits = 3.0f
                )
                val c5 = Course(
                    id = 5,
                    courseNumber = "1082.2105",
                    name = "שיטות מחקר איכותניות",
                    department = Department.COMMUNICATION,
                    lecturer = "ד\"ר ברק",
                    semester = "סמסטר א'",
                    credits = 2.0f
                )

                courseDao.insertCourses(listOf(c1, c2, c3, c4, c5))

                val now = System.currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L

                // Initial Tasks
                taskDao.insertTasks(
                    listOf(
                        Task(
                            id = 1,
                            courseId = 1,
                            title = "קריאת מאמר: מקלוהן והמדיום הוא המסר",
                            description = "עמודים 12-45 לקראת השיעור הבא",
                            dueDate = now + (2 * oneDay),
                            isCompleted = false,
                            estimatedMinutes = 90,
                            department = Department.COMMUNICATION,
                            priority = Priority.HIGH
                        ),
                        Task(
                            id = 2,
                            courseId = 3,
                            title = "מטלת אמצע: ניתוח שוק העבודה הגלובלי",
                            description = "הגשת נייר עמדה של 3 עמודים דרך ה-Moodle",
                            dueDate = now + (5 * oneDay),
                            isCompleted = false,
                            estimatedMinutes = 180,
                            department = Department.LABOR_STUDIES,
                            priority = Priority.HIGH
                        ),
                        Task(
                            id = 3,
                            courseId = 4,
                            title = "סיכום פסק דין בעניין חופש ההתאגדות",
                            description = "הכנת שאלות לדיון בכיתה",
                            dueDate = now + (7 * oneDay),
                            isCompleted = false,
                            estimatedMinutes = 60,
                            department = Department.LABOR_STUDIES,
                            priority = Priority.MEDIUM
                        )
                    )
                )

                // Initial Study Materials & "What's missing"
                materialDao.insertMaterial(
                    StudyMaterial(
                        courseId = 1,
                        title = "קישור להקלטות שיעורים (Panopto)",
                        type = MaterialType.RECORDING_LINK,
                        contentOrUri = "https://panopto.tau.ac.il",
                        isMissingOrNeeded = false
                    )
                )
                materialDao.insertMaterial(
                    StudyMaterial(
                        courseId = 3,
                        title = "חסר סיכום שיעור 3 (הסכמים קיבוציים)",
                        type = MaterialType.SUMMARY,
                        contentOrUri = "להשיג מחבר מהכיתה עד יום חמישי",
                        isMissingOrNeeded = true // Shows up in "מה חסר לי"
                    )
                )
            }
        }
    }
}

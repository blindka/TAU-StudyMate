package com.tau.studymate.data.model

data class AcademicTrack(
    val id: String,
    val name: String,
    val faculty: String,
    val isDoubleMajor: Boolean = false,
    val defaultCourses: List<CourseTemplate> = emptyList()
)

data class CourseTemplate(
    val courseNumber: String,
    val name: String,
    val department: Department,
    val lecturer: String,
    val semester: String,
    val credits: Float,
    val sampleTasks: List<TaskTemplate> = emptyList()
)

data class TaskTemplate(
    val title: String,
    val description: String,
    val daysFromNow: Int,
    val priority: Priority,
    val estimatedMinutes: Int
)

data class UserAcademicProfile(
    val degreeName: String = "תקשורת ולימודי עבודה",
    val isDoubleMajor: Boolean = true,
    val year: String = "שנה א'",
    val semester: String = "סמסטר א'",
    val faculty: String = "מדעי החברה"
)

object AcademicCatalog {

    val AVAILABLE_PROGRAMS: List<AcademicTrack> = listOf(
        AcademicTrack(
            id = "comm_labor",
            name = "תקשורת ולימודי עבודה",
            faculty = "הפקולטה למדעי החברה",
            isDoubleMajor = true,
            defaultCourses = listOf(
                CourseTemplate(
                    "1082.1100", "מבוא לתקשורת ההמונים", Department.COMMUNICATION,
                    "פרופ' אברהם", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("קריאת מאמר: מקלוהן והמדיום הוא המסר", "עמודים 12-45 לקראת השיעור הבא", 2, Priority.HIGH, 90),
                        TaskTemplate("תרגיל בית 1: מודלים בתקשורת", "הגשת ניתוח אירוע עיתונאי", 7, Priority.MEDIUM, 120)
                    )
                ),
                CourseTemplate(
                    "1051.1100", "מבוא ללימודי עבודה", Department.LABOR_STUDIES,
                    "ד\"ר כהן", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("מטלת אמצע: ניתוח שוק העבודה הגלובלי", "הגשת נייר עמדה של 3 עמודים דרך ה-Moodle", 5, Priority.HIGH, 180)
                    )
                ),
                CourseTemplate(
                    "1082.2100", "תיאוריות מתקדמות בתקשורת", Department.COMMUNICATION,
                    "ד\"ר לוי", "סמסטר א'", 3.0f,
                    listOf(
                        TaskTemplate("רפרט קבוצתי: תיאוריית סדר היום", "הכנת מצגת לשיעור", 10, Priority.MEDIUM, 120)
                    )
                ),
                CourseTemplate(
                    "1051.2100", "יחסי עבודה ודיני עבודה בישראל", Department.LABOR_STUDIES,
                    "פרופ' פרידמן", "סמסטר א'", 3.0f,
                    listOf(
                        TaskTemplate("סיכום פסק דין בעניין חופש ההתאגדות", "הכנת שאלות לדיון בכיתה", 7, Priority.LOW, 60)
                    )
                ),
                CourseTemplate(
                    "1082.2105", "שיטות מחקר איכותניות", Department.COMMUNICATION,
                    "ד\"ר ברק", "סמסטר א'", 2.0f,
                    listOf(
                        TaskTemplate("תרגיל תצפית וראיון עומק", "איסוף נתונים ראשוני", 12, Priority.MEDIUM, 150)
                    )
                )
            )
        ),
        AcademicTrack(
            id = "cs",
            name = "מדעי המחשב",
            faculty = "הפקולטה למדעים מדויקים",
            isDoubleMajor = false,
            defaultCourses = listOf(
                CourseTemplate(
                    "0368.1111", "מבני נתונים", Department.GENERAL,
                    "פרופ' רודריך", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("תרגיל רטוב 1: עצי חיפוש בינאריים", "מימוש ב-Java/C++ ובדיקות יחידה", 5, Priority.HIGH, 240),
                        TaskTemplate("תרגיל יבש 2: ניתוח סיבוכיות אלגוריתמים", "הגשה בכתב יד/LaTeX", 9, Priority.MEDIUM, 180)
                    )
                ),
                CourseTemplate(
                    "0368.2158", "אלגוריתמים", Department.GENERAL,
                    "פרופ' חזן", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("תרגיל בית 3: תכנות דינמי וגרפים", "אלגוריתם דייקסטרה ובלמן-פורד", 6, Priority.HIGH, 300)
                    )
                ),
                CourseTemplate(
                    "0368.2161", "מערכות הפעלה", Department.GENERAL,
                    "ד\"ר עמית", "סמסטר א'", 3.5f,
                    listOf(
                        TaskTemplate("פרויקט Threads ו-Locks בקרנל", "מימוש סנכרון תהליכים", 14, Priority.HIGH, 360)
                    )
                )
            )
        ),
        AcademicTrack(
            id = "law",
            name = "משפטים",
            faculty = "הפקולטה למשפטים ע\"ש בוכמן",
            isDoubleMajor = false,
            defaultCourses = listOf(
                CourseTemplate(
                    "1411.1110", "דיני חוזים", Department.GENERAL,
                    "פרופ' גרוסקופף", "סמסטר א'", 5.0f,
                    listOf(
                        TaskTemplate("עבודה 1: תום לב במשא ומתן לקראת חוזה", "ניתוח קייס עובדתי וציטוט פסיקה", 7, Priority.HIGH, 240)
                    )
                ),
                CourseTemplate(
                    "1411.1120", "משפט חוקתי", Department.GENERAL,
                    "פרופ' מדינה", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("קריאת פסק דין בנק המזרחי", "פסקת ההגבלה וביקורת שיפוטית", 4, Priority.HIGH, 150)
                    )
                ),
                CourseTemplate(
                    "1411.1130", "דיני עונשין", Department.GENERAL,
                    "ד\"ר קרמניצר", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("ניתוח יסוד נפשי בעבירות הריגה", "תרגיל הכנה לבחינת ביניים", 10, Priority.MEDIUM, 180)
                    )
                )
            )
        ),
        AcademicTrack(
            id = "psychology_comm",
            name = "פסיכולוגיה ותקשורת",
            faculty = "הפקולטה למדעי החברה",
            isDoubleMajor = true,
            defaultCourses = listOf(
                CourseTemplate(
                    "1071.1102", "מבוא לפסיכולוגיה", Department.GENERAL,
                    "פרופ' שנקר", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("קריאת פרק 4: תפיסה וזיכרון", "הכנה למבחן אמצע", 4, Priority.MEDIUM, 90)
                    )
                ),
                CourseTemplate(
                    "1082.1100", "מבוא לתקשורת ההמונים", Department.COMMUNICATION,
                    "פרופ' אברהם", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("קריאת מאמר: מקלוהן והמדיום הוא המסר", "עמודים 12-45", 3, Priority.HIGH, 90)
                    )
                ),
                CourseTemplate(
                    "1071.2103", "פסיכולוגיה חברתית", Department.GENERAL,
                    "ד\"ר לוין", "סמסטר א'", 3.5f,
                    listOf(
                        TaskTemplate("דוח ניסוי קבוצתי: קונפורמיות והשפעה חברתית", "הגשת ניתוח SPSS", 8, Priority.HIGH, 200)
                    )
                )
            )
        ),
        AcademicTrack(
            id = "economics_management",
            name = "כלכלה וניהול",
            faculty = "מדעי החברה וניהול ע\"ש קולר",
            isDoubleMajor = true,
            defaultCourses = listOf(
                CourseTemplate(
                    "1011.1108", "מבוא למיקרו-כלכלה", Department.GENERAL,
                    "פרופ' שפיגל", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("תרגיל בית 4: שיווי משקל תחרותי ורווחה", "פתרון גרפי וחישובי", 5, Priority.HIGH, 180)
                    )
                ),
                CourseTemplate(
                    "1221.3111", "יסודות השיווק", Department.GENERAL,
                    "ד\"ר רוזן", "סמסטר א'", 3.0f,
                    listOf(
                        TaskTemplate("ניתוח אסטרטגיית מותג דיגיטלי", "הגשת מצגת ניתוח SWOT", 9, Priority.MEDIUM, 140)
                    )
                ),
                CourseTemplate(
                    "1011.2109", "מאקרו כלכלה א'", Department.GENERAL,
                    "פרופ' בנטל", "סמסטר א'", 4.0f,
                    listOf(
                        TaskTemplate("מודל IS-LM ומדיניות מוניטרית", "פתרון שאלות מבחן", 7, Priority.HIGH, 150)
                    )
                )
            )
        )
    )

    fun findProgram(query: String): AcademicTrack? {
        val trimmed = query.trim().lowercase()
        return AVAILABLE_PROGRAMS.find { track ->
            track.name.lowercase().contains(trimmed) ||
                    trimmed.contains(track.name.lowercase()) ||
                    track.name.split(" ").any { word -> word.length > 2 && trimmed.contains(word.lowercase()) }
        }
    }
}

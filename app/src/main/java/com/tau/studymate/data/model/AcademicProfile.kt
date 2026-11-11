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
    val credits: Float
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
                    "פרופ' אברהם", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "1051.1100", "מבוא ללימודי עבודה", Department.LABOR_STUDIES,
                    "ד\"ר כהן", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "1082.2100", "תיאוריות מתקדמות בתקשורת", Department.COMMUNICATION,
                    "ד\"ר לוי", "סמסטר א'", 3.0f
                ),
                CourseTemplate(
                    "1051.2100", "יחסי עבודה ודיני עבודה בישראל", Department.LABOR_STUDIES,
                    "פרופ' פרידמן", "סמסטר א'", 3.0f
                ),
                CourseTemplate(
                    "1082.2105", "שיטות מחקר איכותניות", Department.COMMUNICATION,
                    "ד\"ר ברק", "סמסטר א'", 2.0f
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
                    "פרופ' רודריך", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "0368.2158", "אלגוריתמים", Department.GENERAL,
                    "פרופ' חזן", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "0368.2161", "מערכות הפעלה", Department.GENERAL,
                    "ד\"ר עמית", "סמסטר א'", 3.5f
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
                    "פרופ' גרוסקופף", "סמסטר א'", 5.0f
                ),
                CourseTemplate(
                    "1411.1120", "משפט חוקתי", Department.GENERAL,
                    "פרופ' מדינה", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "1411.1130", "דיני עונשין", Department.GENERAL,
                    "ד\"ר קרמניצר", "סמסטר א'", 4.0f
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
                    "פרופ' שנקר", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "1082.1100", "מבוא לתקשורת ההמונים", Department.COMMUNICATION,
                    "פרופ' אברהם", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "1071.2103", "פסיכולוגיה חברתית", Department.GENERAL,
                    "ד\"ר לוין", "סמסטר א'", 3.5f
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
                    "פרופ' שפיגל", "סמסטר א'", 4.0f
                ),
                CourseTemplate(
                    "1221.3111", "יסודות השיווק", Department.GENERAL,
                    "ד\"ר רוזן", "סמסטר א'", 3.0f
                ),
                CourseTemplate(
                    "1011.2109", "מאקרו כלכלה א'", Department.GENERAL,
                    "פרופ' בנטל", "סמסטר א'", 4.0f
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

package com.tau.studymate.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tau.studymate.R

/**
 * Year/semester are stored as their Hebrew values (existing data, prefs and backups use them).
 * This shows a known value in the UI language and leaves anything else as typed.
 */
private val labels = mapOf(
    "שנה א'" to R.string.year_1, "שנה ב'" to R.string.year_2, "שנה ג'" to R.string.year_3, "שנה ד'" to R.string.year_4,
    "תואר שני" to R.string.year_masters,
    "סמסטר א'" to R.string.semester_a, "סמסטר ב'" to R.string.semester_b, "סמסטר קיץ" to R.string.semester_summer
)

@Composable
fun profileLabel(value: String): String = labels[value]?.let { stringResource(it) } ?: value

/** A catalog degree in the UI language; a degree the user typed is shown as typed. */
@Composable
fun degreeLabel(name: String): String =
    com.tau.studymate.data.model.AcademicCatalog.AVAILABLE_PROGRAMS.find { it.name == name && it.labelRes != 0 }
        ?.let { stringResource(it.labelRes) } ?: name

/** Catalog course names (stored in Hebrew) in the UI language; courses the user added keep their name. */
private val courseNames = mapOf(
    "מבוא לתקשורת ההמונים" to R.string.course_cat_1,
    "מבוא ללימודי עבודה" to R.string.course_cat_2,
    "תיאוריות מתקדמות בתקשורת" to R.string.course_cat_3,
    "יחסי עבודה ודיני עבודה בישראל" to R.string.course_cat_4,
    "שיטות מחקר איכותניות" to R.string.course_cat_5,
    "מבני נתונים" to R.string.course_cat_6,
    "אלגוריתמים" to R.string.course_cat_7,
    "מערכות הפעלה" to R.string.course_cat_8,
    "דיני חוזים" to R.string.course_cat_9,
    "משפט חוקתי" to R.string.course_cat_10,
    "דיני עונשין" to R.string.course_cat_11,
    "מבוא לפסיכולוגיה" to R.string.course_cat_12,
    "פסיכולוגיה חברתית" to R.string.course_cat_13,
    "מבוא למיקרו-כלכלה" to R.string.course_cat_14,
    "יסודות השיווק" to R.string.course_cat_15,
    "מאקרו כלכלה א'" to R.string.course_cat_16,
)

@Composable
fun courseLabel(name: String): String = courseNames[name]?.let { stringResource(it) } ?: name

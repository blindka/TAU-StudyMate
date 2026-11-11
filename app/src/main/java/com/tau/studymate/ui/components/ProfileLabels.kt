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

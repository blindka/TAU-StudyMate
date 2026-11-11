package com.tau.studymate.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseNumber: String, // e.g. "1082.1100" (TAU format)
    val name: String,
    val department: Department,
    val lecturer: String = "",
    val semester: String = "סמסטר א'",
    val credits: Float = 2.0f,
    val colorHex: Long = 0xFF1E88E5,
    /** Final grade (0–100), null until entered; used for the credit-weighted average. */
    val grade: Float? = null
)

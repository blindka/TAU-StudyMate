package com.tau.studymate.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A PDF (article or presentation) stored privately inside the app and assigned to a course and a study week. */
@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL // courses are replaced on semester switch; keep the files
        )
    ],
    indices = [Index(value = ["courseId"]), Index(value = ["weekStartEpochDay"])]
)
data class Reading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long?,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    /** LocalDate.toEpochDay() of the Sunday that starts the study week. */
    val weekStartEpochDay: Long,
    val isRead: Boolean = false,
    val summary: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

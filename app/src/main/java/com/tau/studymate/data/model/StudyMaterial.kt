package com.tau.studymate.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_materials",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class StudyMaterial(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val title: String,
    val type: MaterialType,
    val contentOrUri: String, // Web link (Panopto, Zoom, Drive), local file path, or note
    val isMissingOrNeeded: Boolean = false, // Indicates "מה חסר לי" (e.g. missing summary)
    val dateAdded: Long = System.currentTimeMillis()
)

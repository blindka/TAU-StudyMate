package com.tau.studymate.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long? = null,
    val title: String,
    val description: String = "",
    val dueDate: Long, // timestamp in ms
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val estimatedMinutes: Int = 60, // Estimated time to complete
    val actualMinutes: Int = 0,     // Actual time tracked
    val department: Department = Department.GENERAL,
    val priority: Priority = Priority.MEDIUM,
    @androidx.room.ColumnInfo(defaultValue = "1")
    val reminderEnabled: Boolean = true
)

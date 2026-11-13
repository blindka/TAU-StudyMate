package com.tau.studymate.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A weekly class meeting of a course (e.g. Sunday 10:00–12:00, Gilman 144). */
@Entity(
    tableName = "class_slots",
    foreignKeys = [ForeignKey(entity = Course::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["courseId"])]
)
data class ClassSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    /** java.time.DayOfWeek value: 1 = Monday … 7 = Sunday. */
    val dayOfWeek: Int,
    /** Minutes from midnight. */
    val startMinute: Int,
    val endMinute: Int,
    val room: String = ""
)

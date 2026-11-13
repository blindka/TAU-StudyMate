package com.tau.studymate.data.local

import androidx.room.*
import com.tau.studymate.data.model.ClassSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSlotDao {
    @Query("SELECT * FROM class_slots ORDER BY dayOfWeek, startMinute")
    fun getAll(): Flow<List<ClassSlot>>

    @Query("SELECT * FROM class_slots WHERE courseId = :courseId ORDER BY dayOfWeek, startMinute")
    fun forCourse(courseId: Long): Flow<List<ClassSlot>>

    @Insert
    suspend fun insert(slot: ClassSlot): Long

    @Delete
    suspend fun delete(slot: ClassSlot)
}

package com.tau.studymate.data.local

import androidx.room.*
import com.tau.studymate.data.model.Reading
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings WHERE weekStartEpochDay = :week ORDER BY isRead ASC, addedAt ASC")
    fun getForWeek(week: Long): Flow<List<Reading>>

    @Query("SELECT * FROM readings WHERE id = :id")
    fun observe(id: Long): Flow<Reading?>

    @Insert
    suspend fun insert(reading: Reading): Long

    @Update
    suspend fun update(reading: Reading)

    @Delete
    suspend fun delete(reading: Reading)
}

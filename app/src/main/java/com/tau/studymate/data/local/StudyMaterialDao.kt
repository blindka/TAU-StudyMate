package com.tau.studymate.data.local

import androidx.room.*
import com.tau.studymate.data.model.MaterialType
import com.tau.studymate.data.model.StudyMaterial
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyMaterialDao {
    @Query("SELECT * FROM study_materials WHERE courseId = :courseId ORDER BY dateAdded DESC")
    fun getMaterialsForCourse(courseId: Long): Flow<List<StudyMaterial>>

    @Query("SELECT * FROM study_materials WHERE courseId = :courseId AND type = :type ORDER BY dateAdded DESC")
    fun getMaterialsByType(courseId: Long, type: MaterialType): Flow<List<StudyMaterial>>

    @Query("SELECT * FROM study_materials WHERE isMissingOrNeeded = 1 ORDER BY dateAdded DESC")
    fun getAllMissingMaterials(): Flow<List<StudyMaterial>>

    @Query("SELECT COUNT(*) FROM study_materials WHERE isMissingOrNeeded = 1")
    fun getMissingMaterialsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: StudyMaterial): Long

    @Update
    suspend fun updateMaterial(material: StudyMaterial)

    @Delete
    suspend fun deleteMaterial(material: StudyMaterial)
}

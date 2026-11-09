package com.tau.studymate.data.local

import androidx.room.*
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY courseNumber ASC")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE department = :dept ORDER BY courseNumber ASC")
    fun getCoursesByDepartment(dept: Department): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: Long): Course?

    @Query("SELECT * FROM courses WHERE courseNumber LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    fun searchCourses(query: String): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)
}

package com.tau.studymate.data.local

import androidx.room.TypeConverter
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.MaterialType
import com.tau.studymate.data.model.Priority

class Converters {
    @TypeConverter
    fun fromDepartment(department: Department): String = department.name

    @TypeConverter
    fun toDepartment(value: String): Department = Department.fromString(value)

    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = try {
        Priority.valueOf(value)
    } catch (_: Exception) {
        Priority.MEDIUM
    }

    @TypeConverter
    fun fromMaterialType(type: MaterialType): String = type.name

    @TypeConverter
    fun toMaterialType(value: String): MaterialType = try {
        MaterialType.valueOf(value)
    } catch (_: Exception) {
        MaterialType.NOTE
    }
}

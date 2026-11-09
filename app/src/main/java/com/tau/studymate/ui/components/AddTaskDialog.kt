package com.tau.studymate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        dueDate: Long,
        courseId: Long?,
        department: Department,
        priority: Priority,
        estimatedMinutes: Int,
        enableReminder: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Long?>(courses.firstOrNull()?.id) }
    var selectedDepartment by remember {
        mutableStateOf(courses.firstOrNull()?.department ?: Department.COMMUNICATION)
    }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var daysAhead by remember { mutableIntStateOf(3) }
    var estimatedMinutes by remember { mutableStateOf("60") }
    var enableReminder by remember { mutableStateOf(true) }

    var expandedCourseDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "הוספת מטלה חדשה",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("כותרת המטלה *") },
                    placeholder = { Text("לדוגמה: הגשת תרגיל בית 2") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Course selector dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCourseDropdown,
                    onExpandedChange = { expandedCourseDropdown = !expandedCourseDropdown }
                ) {
                    val currentCourse = courses.find { it.id == selectedCourseId }
                    OutlinedTextField(
                        value = currentCourse?.let { "${it.courseNumber} - ${it.name}" } ?: "ללא קורס",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("שיוך לקורס") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourseDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCourseDropdown,
                        onDismissRequest = { expandedCourseDropdown = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.courseNumber} - ${course.name}") },
                                onClick = {
                                    selectedCourseId = course.id
                                    selectedDepartment = course.department
                                    expandedCourseDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("הערות או קישור לחומרים") },
                    placeholder = { Text("הנחיות, עמודים בספר או דגשים...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due date selector
                Text(
                    text = "מועד הגשה:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "מחר", 3 to "בעוד 3 ימים", 7 to "שבוע", 14 to "שבועיים").forEach { (days, label) ->
                        FilterChip(
                            selected = daysAhead == days,
                            onClick = { daysAhead = days },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority
                Text(
                    text = "עדיפות:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Priority.entries.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.displayName, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = estimatedMinutes,
                    onValueChange = { estimatedMinutes = it },
                    label = { Text("הערכת זמן ביצוע בדקות (להרגלי למידה)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Reminder notification switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "הפעל התראת תזכורת למטלה 🔔",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Switch(
                        checked = enableReminder,
                        onCheckedChange = { enableReminder = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ביטול")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val oneDayMs = 24 * 60 * 60 * 1000L
                                val dueDate = System.currentTimeMillis() + (daysAhead * oneDayMs)
                                val minutes = estimatedMinutes.toIntOrNull() ?: 60

                                onConfirm(
                                    title.trim(),
                                    description.trim(),
                                    dueDate,
                                    selectedCourseId,
                                    selectedDepartment,
                                    selectedPriority,
                                    minutes,
                                    enableReminder
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("הוסף מטלה")
                    }
                }
            }
        }
    }
}

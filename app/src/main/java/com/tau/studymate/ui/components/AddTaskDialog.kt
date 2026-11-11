package com.tau.studymate.ui.components

import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
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
    initial: com.tau.studymate.data.model.Task? = null, // non-null = edit mode
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
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var selectedCourseId by remember { mutableStateOf(if (initial != null) initial.courseId else courses.firstOrNull()?.id) }
    var selectedDepartment by remember {
        mutableStateOf(initial?.department ?: courses.firstOrNull()?.department ?: Department.COMMUNICATION)
    }
    var selectedPriority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    // Default deadline: 3 days from today at 23:59 (TAU submissions usually close at midnight)
    var dueAt by remember {
        mutableStateOf(
            initial?.let { java.time.Instant.ofEpochMilli(it.dueDate).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() }
                ?: java.time.LocalDate.now().plusDays(3).atTime(23, 59)
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var estimatedMinutes by remember { mutableStateOf((initial?.estimatedMinutes ?: 60).toString()) }
    var enableReminder by remember { mutableStateOf(initial?.reminderEnabled ?: true) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    text = stringResource(if (initial == null) R.string.task_add_title else R.string.task_edit_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    placeholder = { Text(stringResource(R.string.task_title_hint)) },
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
                        value = currentCourse?.let { "${it.courseNumber} - ${it.name}" } ?: stringResource(R.string.no_course),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.task_course_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourseDropdown) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                    label = { Text(stringResource(R.string.task_notes_label)) },
                    placeholder = { Text(stringResource(R.string.task_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due date selector
                Text(
                    text = stringResource(R.string.due_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(dueAt.format(java.time.format.DateTimeFormatter.ofPattern("EEE d.M")))
                    }
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(dueAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority
                Text(
                    text = stringResource(R.string.priority_label),
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
                            label = { Text(stringResource(priority.labelRes), fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = estimatedMinutes,
                    onValueChange = { estimatedMinutes = it },
                    label = { Text(stringResource(R.string.estimate_label)) },
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
                            text = stringResource(R.string.reminder_toggle),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Switch(
                        checked = enableReminder,
                        onCheckedChange = { enableReminder = it }
                    )
                }
                // Android 14+ turns exact alarms off by default; without it reminders can arrive minutes late
                if (enableReminder && android.os.Build.VERSION.SDK_INT >= 31 &&
                    context.getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() == false
                ) {
                    TextButton(onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}")))
                    }) { Text(stringResource(R.string.exact_alarm_link), fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val dueDate = dueAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
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
                        Icon(if (initial == null) Icons.Default.Add else Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(if (initial == null) R.string.add_task_button else R.string.save))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueAt.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() // picker returns UTC midnight
                        dueAt = d.atTime(dueAt.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state) }
    }
    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = dueAt.hour, initialMinute = dueAt.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { dueAt = dueAt.withHour(state.hour).withMinute(state.minute); showTimePicker = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state) }
        )
    }
}

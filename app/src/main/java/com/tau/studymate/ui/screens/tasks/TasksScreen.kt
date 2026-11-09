package com.tau.studymate.ui.screens.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.Priority
import com.tau.studymate.data.model.Task
import com.tau.studymate.ui.components.AddTaskDialog
import com.tau.studymate.ui.components.DepartmentBadge
import com.tau.studymate.ui.components.ExportDialog
import com.tau.studymate.ui.components.MoodleSyncDialog
import com.tau.studymate.ui.theme.*
import com.tau.studymate.ui.viewmodel.StudyViewModel
import com.tau.studymate.ui.viewmodel.TaskFilter
import java.text.SimpleDateFormat
import java.util.*

private data class TaskFilterTab(
    val filter: TaskFilter,
    val label: String,
    val bg: Color,
    val text: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val selectedDept by viewModel.selectedDepartment.collectAsState()
    val taskFilter by viewModel.taskFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showMoodleSyncDialog by remember { mutableStateOf(false) }

    val courseMap = remember(courses) { courses.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val filterTabs = remember {
        listOf(
            TaskFilterTab(TaskFilter.ALL, "הכל", PastelLavender, PastelLavenderDark),
            TaskFilterTab(TaskFilter.PENDING, "ממתין לביצוע", PastelSky, PastelSkyDark),
            TaskFilterTab(TaskFilter.COMPLETED, "הושלמו", PastelMint, PastelMintDark)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = PastelLavenderDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף מטלה")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search & Pastel Export / Alert Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("חיפוש מטלה או נושא...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PastelLavenderDark)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "נקה")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = { showMoodleSyncDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            PastelLavender,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "סנכרן ממודל",
                        tint = PastelLavenderDark
                    )
                }

                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            PastelSky,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "ייצוא",
                        tint = PastelSkyDark
                    )
                }

                IconButton(
                    onClick = { viewModel.sendTestNotification() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            PastelButter,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "בדיקת התראה",
                        tint = PastelButterDark
                    )
                }
            }

            // Department Filters (All / Communication / Labor Studies / General)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDept == null,
                        onClick = { viewModel.setDepartmentFilter(null) },
                        label = { Text("כל החוגים") }
                    )
                }
                items(Department.entries) { dept ->
                    FilterChip(
                        selected = selectedDept == dept,
                        onClick = { viewModel.setDepartmentFilter(dept) },
                        label = { Text(dept.displayName) }
                    )
                }
            }

            // Completion Filter Tabs (All / Pending / Completed) in Pastel Styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEach { tab ->
                    val isSelected = taskFilter == tab.filter
                    val bg = if (isSelected) tab.bg else MaterialTheme.colorScheme.surface
                    val textColor = if (isSelected) tab.text else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) tab.text.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setTaskFilter(tab.filter) }
                    ) {
                        Text(
                            text = tab.label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = PastelLavenderDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "לא נמצאו מטלות ברשימה",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "לחץ על + למטה כדי להוסיף מטלה חדשה",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val course = courseMap[task.courseId]
                        TaskItemCard(
                            task = task,
                            course = course,
                            dueDateFormatted = dateFormat.format(Date(task.dueDate)),
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            courses = courses,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, dueDate, courseId, dept, priority, estimatedMins, enableReminder ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    dueDate = dueDate,
                    courseId = courseId,
                    department = dept,
                    priority = priority,
                    estimatedMinutes = estimatedMins,
                    enableReminder = enableReminder
                )
                showAddTaskDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            exportText = viewModel.getExportText(),
            onDismiss = { showExportDialog = false }
        )
    }

    if (showMoodleSyncDialog) {
        MoodleSyncDialog(
            initialUrl = viewModel.getSavedMoodleUrl(),
            onDismiss = { showMoodleSyncDialog = false },
            onSyncUrl = { url, onFinished ->
                viewModel.syncMoodleFeed(url) { result ->
                    onFinished(result.success, result.syncedCount, result.errorMessage)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItemCard(
    task: Task,
    course: Course?,
    dueDateFormatted: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    PastelCoralDark else Color.Transparent,
                label = "color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "מחק מטלה",
                    tint = Color.White
                )
            }
        }
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PastelMintDark,
                            uncheckedColor = PastelLavenderDark.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "מחק",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer with Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DepartmentBadge(department = task.department)

                        if (course != null) {
                            Text(
                                text = "${course.courseNumber} - ${course.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    // Pastel Due date badge
                    val isUrgent = task.priority == Priority.HIGH
                    val badgeBg = if (isUrgent) PastelCoral else PastelLavender
                    val badgeBorder = if (isUrgent) PastelCoralBorder else PastelLavenderBorder
                    val badgeText = if (isUrgent) PastelCoralDark else PastelLavenderDark

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = badgeText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dueDateFormatted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }
                    }
                }
            }
        }
    }
}

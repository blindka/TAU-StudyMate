package com.tau.studymate.ui.screens.tasks

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.saveable.rememberSaveable
import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
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
    @androidx.annotation.StringRes val label: Int,
    val bg: Color,
    val text: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: StudyViewModel,
    openTaskId: Long? = null,
    onOpenTaskHandled: () -> Unit = {},
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
            TaskFilterTab(TaskFilter.ALL, R.string.filter_all, PastelLavender, PastelLavenderDark),
            TaskFilterTab(TaskFilter.PENDING, R.string.filter_pending, PastelSky, PastelSkyDark),
            TaskFilterTab(TaskFilter.COMPLETED, R.string.filter_done, PastelMint, PastelMintDark)
        )
    }

    val snackbar = remember { SnackbarHostState() }
    val deletedMsg = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.undo)
    val requestNotifications = com.tau.studymate.ui.components.rememberNotificationPermissionRequest()
    var editing by remember { mutableStateOf<Task?>(null) }
    var calendarMode by rememberSaveable { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(java.time.LocalDate.now()) }
    val allTasks by viewModel.allTasks.collectAsState()
    LaunchedEffect(openTaskId, allTasks) {
        if (openTaskId != null) allTasks.find { it.id == openTaskId }?.let { editing = it; onOpenTaskHandled() }
    }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = PastelLavenderDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.a11y_add_task))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tasks_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { calendarMode = !calendarMode }) {
                    Icon(
                        if (calendarMode) Icons.AutoMirrored.Filled.List else Icons.Default.CalendarMonth,
                        stringResource(if (calendarMode) R.string.a11y_list_view else R.string.a11y_calendar_view)
                    )
                }
            }
            if (calendarMode) {
                // Month view: dots show busy days; the list below shows the selected day
                val zone = java.time.ZoneId.systemDefault()
                val dayTasks = allTasks.filter { java.time.Instant.ofEpochMilli(it.dueDate).atZone(zone).toLocalDate() == selectedDay }
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { com.tau.studymate.ui.components.TaskCalendar(allTasks, selectedDay, onSelect = { selectedDay = it }) }
                    if (dayTasks.isEmpty()) item { Text(stringResource(R.string.calendar_day_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
                    items(dayTasks, key = { it.id }) { task ->
                        com.tau.studymate.ui.components.TaskRowCard(task = task, course = courseMap[task.courseId], onToggle = { viewModel.toggleTaskCompletion(task) }, onClick = { editing = task })
                    }
                }
                return@Column
            }
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
                    placeholder = { Text(stringResource(R.string.tasks_search), maxLines = 1) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PastelLavenderDark)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.a11y_clear))
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
                        contentDescription = stringResource(R.string.a11y_sync_moodle),
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
                        contentDescription = stringResource(R.string.a11y_export),
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
                        contentDescription = stringResource(R.string.a11y_test_notification),
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
                        label = { Text(stringResource(R.string.filter_all_departments)) }
                    )
                }
                items(Department.entries) { dept ->
                    FilterChip(
                        selected = selectedDept == dept,
                        onClick = { viewModel.setDepartmentFilter(dept) },
                        label = { Text(stringResource(dept.labelRes)) }
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
                            text = stringResource(tab.label),
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
                            text = stringResource(R.string.tasks_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.tasks_empty_hint),
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
                            onClick = { editing = task },
                            onDelete = {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    val res = snackbar.showSnackbar(deletedMsg, actionLabel = undoLabel, duration = SnackbarDuration.Short)
                                    if (res == SnackbarResult.ActionPerformed) viewModel.restoreTask(task)
                                }
                            }
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
            onConfirm = { title, desc, dueDate, courseId, dept, priority, estimatedMins, enableReminder, repeatWeekly ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    dueDate = dueDate,
                    courseId = courseId,
                    department = dept,
                    priority = priority,
                    estimatedMinutes = estimatedMins,
                    enableReminder = enableReminder,
                    repeatWeekly = repeatWeekly
                )
                if (enableReminder) requestNotifications()
                showAddTaskDialog = false
            }
        )
    }

    editing?.let { task ->
        AddTaskDialog(
            courses = courses,
            initial = task,
            onDismiss = { editing = null },
            onConfirm = { title, desc, dueDate, courseId, dept, priority, estimatedMins, enableReminder, repeatWeekly ->
                viewModel.updateTask(task.copy(repeatWeekly = repeatWeekly, title = title, description = desc, dueDate = dueDate, courseId = courseId,
                    department = dept, priority = priority, estimatedMinutes = estimatedMins, reminderEnabled = enableReminder))
                editing = null
            }
        )
    }

    if (showExportDialog) {
        // Built once, off the main thread (it used to rebuild on every recomposition)
        val exportText by produceState<String?>(null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { viewModel.getExportText() }
        }
        exportText?.let { text ->
            ExportDialog(
                exportText = text,
                onDismiss = { showExportDialog = false }
            )
        }
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
    onClick: () -> Unit,
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
                    contentDescription = stringResource(R.string.a11y_delete_task),
                    tint = Color.White
                )
            }
        }
    ) {
        com.tau.studymate.ui.components.TaskRowCard(task = task, course = course, onToggle = onToggle, showDescription = true, onClick = onClick)
    }
}

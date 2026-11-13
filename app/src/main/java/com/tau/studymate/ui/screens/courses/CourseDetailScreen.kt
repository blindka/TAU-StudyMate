package com.tau.studymate.ui.screens.courses

import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.MaterialType
import com.tau.studymate.data.model.StudyMaterial
import com.tau.studymate.data.model.Task
import com.tau.studymate.ui.components.AddMaterialDialog
import com.tau.studymate.ui.components.DepartmentBadge
import com.tau.studymate.ui.theme.LaborGreen
import com.tau.studymate.ui.theme.TauBluePrimary
import com.tau.studymate.ui.theme.UrgentRed
import com.tau.studymate.ui.viewmodel.StudyViewModel

enum class CourseSubTab(@androidx.annotation.StringRes val title: Int) {
    MATERIALS(com.tau.studymate.R.string.tab_materials),
    RECORDINGS(com.tau.studymate.R.string.tab_recordings),
    TASKS(com.tau.studymate.R.string.tab_course_tasks),
    SCHEDULE(com.tau.studymate.R.string.tab_schedule)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: Course,
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CourseSubTab.MATERIALS) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

    // remember: a new Flow on every recomposition re-opened both Room queries each frame
    val materials by remember(course.id) { viewModel.getMaterialsForCourse(course.id) }.collectAsState(initial = emptyList())
    val courseTasks by remember(course.id) { viewModel.getTasksForCourse(course.id) }.collectAsState(initial = emptyList())

    val context = LocalContext.current

    // Configuration-aware resources (follow a language change)

    val res = androidx.compose.ui.platform.LocalResources.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${course.courseNumber} - ${com.tau.studymate.ui.components.courseLabel(course.name)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.course_lecturer_credits, course.lecturer, course.credits.toString()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    val shareScope = rememberCoroutineScope()
                    val shareTitle = stringResource(R.string.share_course_chooser)
                    IconButton(onClick = {
                        shareScope.launch {
                            val uri = viewModel.exportCourse(course)
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json")
                                .putExtra(android.content.Intent.EXTRA_STREAM, uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(android.content.Intent.createChooser(send, shareTitle))
                        }
                    }) { Icon(Icons.Default.Share, stringResource(R.string.a11y_share_course)) }
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    DepartmentBadge(department = course.department, modifier = Modifier.padding(end = 12.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMaterialDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.a11y_add_material))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Course Tabs - Scrollable to prevent overflow on large display scales
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                CourseSubTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = stringResource(tab.title),
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                CourseSubTab.MATERIALS -> {
                    val summaryList = materials.filter { it.type != MaterialType.RECORDING_LINK }
                    if (summaryList.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Description,
                            title = stringResource(R.string.materials_empty),
                            subtitle = stringResource(R.string.materials_empty_hint)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(summaryList, key = { it.id }) { item ->
                                MaterialCardItem(
                                    material = item,
                                    onDelete = { viewModel.deleteStudyMaterial(item) }
                                )
                            }
                        }
                    }
                }
                CourseSubTab.RECORDINGS -> {
                    val recordingList = materials.filter { it.type == MaterialType.RECORDING_LINK }
                    if (recordingList.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.VideoLibrary,
                            title = stringResource(R.string.recordings_empty),
                            subtitle = stringResource(R.string.recordings_empty_hint)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recordingList, key = { it.id }) { item ->
                                RecordingCardItem(
                                    material = item,
                                    onOpenLink = {
                                        val raw = item.contentOrUri.trim()
                                        val url = if (raw.contains("://")) raw else "https://$raw" // users often paste "panopto.tau.ac.il/..."
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, res.getString(R.string.err_open_link), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { viewModel.deleteStudyMaterial(item) }
                                )
                            }
                        }
                    }
                }
                CourseSubTab.SCHEDULE -> CourseScheduleTab(course.id, viewModel)
                CourseSubTab.TASKS -> {
                    if (courseTasks.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Checklist,
                            title = stringResource(R.string.course_tasks_empty),
                            subtitle = stringResource(R.string.course_tasks_empty_hint)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(courseTasks, key = { it.id }) { task ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                            colors = CheckboxDefaults.colors(checkedColor = LaborGreen)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            if (task.description.isNotBlank()) {
                                                Text(
                                                    text = task.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMaterialDialog) {
        AddMaterialDialog(
            onDismiss = { showAddMaterialDialog = false },
            onConfirm = { title, type, content, isMissing ->
                viewModel.addStudyMaterial(
                    courseId = course.id,
                    title = title,
                    type = type,
                    contentOrUri = content,
                    isMissing = isMissing
                )
                showAddMaterialDialog = false
            }
        )
    }
}

@Composable
private fun MaterialCardItem(
    material: StudyMaterial,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (material.isMissingOrNeeded)
                UrgentRed.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (material.isMissingOrNeeded) UrgentRed.copy(alpha = 0.15f)
                        else TauBluePrimary.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (material.isMissingOrNeeded) Icons.Default.PriorityHigh else Icons.Default.Article,
                    contentDescription = null,
                    tint = if (material.isMissingOrNeeded) UrgentRed else TauBluePrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(material.type.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (material.contentOrUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = material.contentOrUri,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.a11y_delete),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RecordingCardItem(
    material: StudyMaterial,
    onOpenLink: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LaborGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = LaborGreen
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (material.contentOrUri.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.recording_open),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenLink() }
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.a11y_delete),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Exam dates and weekly class times of one course. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CourseScheduleTab(courseId: Long, viewModel: StudyViewModel) {
    val courses by viewModel.courses.collectAsState()
    val course = courses.find { it.id == courseId } ?: return
    val slots by remember(courseId) { viewModel.classSlotsFor(courseId) }.collectAsState(initial = emptyList())
    var pickExam by remember { mutableStateOf<Int?>(null) } // 0 = A, 1 = B
    var adding by remember { mutableStateOf(false) }
    val zone = java.time.ZoneId.systemDefault()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(stringResource(R.string.exams_title), fontWeight = FontWeight.Bold) }
        listOf(R.string.exam_a to course.examA, R.string.exam_b to course.examB).forEachIndexed { i, (label, date) ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), Modifier.weight(1f))
                    OutlinedButton(onClick = { pickExam = i }) {
                        Text(date?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate().format(fmt) } ?: stringResource(R.string.exam_not_set))
                    }
                    if (date != null) IconButton(onClick = { viewModel.updateCourse(if (i == 0) course.copy(examA = null) else course.copy(examB = null)) }) {
                        Icon(Icons.Default.Clear, stringResource(R.string.a11y_clear_date))
                    }
                }
            }
        }
        item { Text(stringResource(R.string.weekly_classes), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
        if (slots.isEmpty()) item { Text(stringResource(R.string.no_classes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(slots, key = { it.id }) { slot ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${java.time.DayOfWeek.of(slot.dayOfWeek).getDisplayName(java.time.format.TextStyle.FULL, locale)} · ${minutesText(slot.startMinute)}–${minutesText(slot.endMinute)}" +
                        if (slot.room.isNotBlank()) " · ${slot.room}" else "",
                    Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.deleteClassSlot(slot) }) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.a11y_delete)) }
            }
        }
        item { TextButton(onClick = { adding = true }) { Text(stringResource(R.string.add_class)) } }
    }

    pickExam?.let { which ->
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickExam = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        // Picker returns UTC midnight; store 09:00 local so the date never shifts across time zones
                        val local = java.time.Instant.ofEpochMilli(utc).atZone(java.time.ZoneOffset.UTC).toLocalDate().atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
                        viewModel.updateCourse(if (which == 0) course.copy(examA = local) else course.copy(examB = local))
                    }
                    pickExam = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickExam = null }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state) }
    }

    if (adding) {
        var day by remember { mutableStateOf(java.time.DayOfWeek.SUNDAY) }
        var start by remember { mutableStateOf(10 * 60) }
        var end by remember { mutableStateOf(12 * 60) }
        var room by remember { mutableStateOf("") }
        var editing by remember { mutableStateOf<Int?>(null) } // 0 = start, 1 = end
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.add_class).removePrefix("+ ")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf(java.time.DayOfWeek.SUNDAY) + java.time.DayOfWeek.entries.filter { it != java.time.DayOfWeek.SUNDAY && it != java.time.DayOfWeek.SATURDAY }).forEach { d ->
                            FilterChip(selected = day == d, onClick = { day = d }, label = { Text(d.getDisplayName(java.time.format.TextStyle.SHORT, locale)) })
                        }
                    }
                    OutlinedButton(onClick = { editing = 0 }) { Text(stringResource(R.string.class_start, minutesText(start))) }
                    OutlinedButton(onClick = { editing = 1 }) { Text(stringResource(R.string.class_end, minutesText(end))) }
                    OutlinedTextField(room, { room = it }, label = { Text(stringResource(R.string.class_room)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addClassSlot(com.tau.studymate.data.model.ClassSlot(courseId = courseId, dayOfWeek = day.value, startMinute = start, endMinute = maxOf(end, start + 15), room = room.trim()))
                    adding = false
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) } }
        )
        editing?.let { which ->
            val init = if (which == 0) start else end
            val t = rememberTimePickerState(initialHour = init / 60, initialMinute = init % 60, is24Hour = true)
            AlertDialog(
                onDismissRequest = { editing = null },
                confirmButton = { TextButton(onClick = { val m = t.hour * 60 + t.minute; if (which == 0) start = m else end = m; editing = null }) { Text(stringResource(R.string.ok)) } },
                dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
                text = { TimePicker(t) }
            )
        }
    }
}

internal fun minutesText(m: Int) = "%02d:%02d".format(m / 60, m % 60)

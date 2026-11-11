package com.tau.studymate.ui.screens.courses

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.R
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import com.tau.studymate.ui.components.AcademicProfileDialog
import com.tau.studymate.ui.components.AddCourseDialog
import com.tau.studymate.ui.components.DepartmentBadge
import com.tau.studymate.ui.components.ScheduleViewerDialog
import com.tau.studymate.ui.theme.*
import com.tau.studymate.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: StudyViewModel,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val openTaskCounts by viewModel.openTaskCountByCourse.collectAsState()
    var courseToDelete by remember { mutableStateOf<Course?>(null) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }
    courseToEdit?.let { course ->
        AddCourseDialog(
            existingNumbers = courses.map { it.courseNumber }.toSet(),
            initial = course,
            onDismiss = { courseToEdit = null },
            onConfirm = { number, name, dept, lecturer, semester, credits ->
                viewModel.updateCourse(course.copy(courseNumber = number, name = name, department = dept, lecturer = lecturer, semester = semester, credits = credits))
                courseToEdit = null
            }
        )
    }
    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text(stringResource(R.string.course_delete_title)) },
            text = { Text(stringResource(R.string.course_delete_body, course.name)) },
            confirmButton = { TextButton(onClick = { viewModel.deleteCourse(course); courseToDelete = null }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    var selectedDept by remember { mutableStateOf<Department?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    val filteredCourses = remember(courses, selectedDept, searchQuery) {
        courses.filter { course ->
            val matchesDept = selectedDept == null || course.department == selectedDept
            val matchesQuery = searchQuery.isBlank() ||
                    course.name.contains(searchQuery, ignoreCase = true) ||
                    course.courseNumber.contains(searchQuery, ignoreCase = true)
            matchesDept && matchesQuery
        }
    }

    val academicProfile by viewModel.academicProfile.collectAsState()
    val scheduleBitmap by viewModel.scheduleBitmap.collectAsState()
    val scheduleFileName by viewModel.scheduleFileName.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showScheduleViewer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCourseDialog = true },
                containerColor = TauPurplePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.a11y_add_course))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Actions (Schedule button + Degree cap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.courses_search), maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TauPurplePrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.a11y_clear))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                // Quick Schedule Button
                IconButton(
                    onClick = {
                        if (scheduleBitmap != null || scheduleFileName != null) {
                            showScheduleViewer = true
                        } else {
                            showProfileDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(TauPurpleVibrant.copy(alpha = 0.2f), TauMagentaAccent.copy(alpha = 0.2f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (scheduleBitmap != null || scheduleFileName != null) Icons.Default.CalendarMonth else Icons.Default.AddAPhoto,
                        contentDescription = stringResource(R.string.a11y_timetable),
                        tint = TauPurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Degree Cap Quick Switcher Icon
                IconButton(
                    onClick = { showProfileDialog = true },
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            TauPurpleLight.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_academic_cap),
                        contentDescription = stringResource(R.string.a11y_switch_degree),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Degree Header Banner pill with Purple accent
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TauPurpleLight.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showProfileDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.courses_degree_line, academicProfile.degreeName, com.tau.studymate.ui.components.profileLabel(academicProfile.year), com.tau.studymate.ui.components.profileLabel(academicProfile.semester)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TauPurpleDark
                    )
                    Text(
                        text = stringResource(R.string.courses_switch_degree),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TauPurplePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Department Filters
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDept == null,
                        onClick = { selectedDept = null },
                        label = { Text(stringResource(R.string.courses_all, courses.size)) }
                    )
                }
                items(Department.entries) { dept ->
                    val count = courses.count { it.department == dept }
                    FilterChip(
                        selected = selectedDept == dept,
                        onClick = { selectedDept = dept },
                        label = { Text("${stringResource(dept.labelRes)} ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = TauPurplePrimary.copy(alpha = 0.4f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.courses_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.courses_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCourses, key = { it.id }) { course ->
                        CourseCard(
                            course = course,
                            openTasks = openTaskCounts[course.id] ?: 0,
                            onClick = { onCourseClick(course) },
                            onDelete = { courseToDelete = course },
                            onEdit = { courseToEdit = course }
                        )
                    }
                }
            }
        }
    }

    if (showAddCourseDialog) {
        AddCourseDialog(
            existingNumbers = courses.map { it.courseNumber }.toSet(),
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { courseNumber, name, dept, lecturer, semester, credits ->
                viewModel.addCourse(
                    courseNumber = courseNumber,
                    name = name,
                    department = dept,
                    lecturer = lecturer,
                    semester = semester,
                    credits = credits
                )
                showAddCourseDialog = false
            }
        )
    }

    if (showProfileDialog) {
        AcademicProfileDialog(
            currentProfile = academicProfile,
            initialScheduleBitmap = scheduleBitmap,
            initialScheduleFileName = scheduleFileName,
            onDismiss = { showProfileDialog = false },
            onSave = { degree, isDouble, year, semester, imported, sBitmap, sFileName ->
                val ok = viewModel.updateDegreeProgram(
                    degreeName = degree,
                    isDoubleMajor = isDouble,
                    year = year,
                    semester = semester,
                    importedCourses = imported
                )
                if (ok) {
                    if (sBitmap != null) {
                        viewModel.saveSchedulePhoto(sBitmap)
                    } else if (sFileName != null) {
                        viewModel.saveScheduleFile(sFileName)
                    } else {
                        viewModel.clearSchedule()
                    }
                }
                ok
            }
        )
    }

    if (showScheduleViewer) {
        ScheduleViewerDialog(
            bitmap = scheduleBitmap,
            fileName = scheduleFileName,
            onDismiss = { showScheduleViewer = false },
            onOpenEdit = {
                showScheduleViewer = false
                showProfileDialog = true
            }
        )
    }
}

@Composable
private fun CourseCard(
    course: Course,
    openTasks: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val (chipBg, chipFg) = com.tau.studymate.ui.screens.readings.departmentColors(course.department)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, PastelLavenderBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(course.department.labelRes), color = chipFg, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.background(chipBg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(course.courseNumber, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(R.string.a11y_edit_course), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) { // default 48dp touch target
                    Icon(Icons.Default.DeleteOutline, stringResource(R.string.a11y_delete_course), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                }
            }
            Text(course.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(course.lecturer.takeIf { it.isNotBlank() }, com.tau.studymate.ui.components.profileLabel(course.semester), stringResource(R.string.credits_short, course.credits.toString())).joinToString(" · "),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (openTasks == 0) stringResource(R.string.course_no_open_tasks) else pluralStringResource(R.plurals.course_open_tasks, openTasks, openTasks),
                    color = PastelLavenderDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.background(PastelLavender, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp)
                )
                Text(
                    stringResource(R.string.course_materials_pill), color = chipFg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.background(chipBg, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

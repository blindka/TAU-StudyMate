package com.tau.studymate.ui.screens.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.R
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Task
import com.tau.studymate.data.model.UserAcademicProfile
import com.tau.studymate.ui.components.AcademicProfileDialog
import com.tau.studymate.ui.components.DepartmentBadge
import com.tau.studymate.ui.components.SemesterTransitionDialog
import com.tau.studymate.ui.theme.LaborGreen
import com.tau.studymate.ui.theme.TauBluePrimary
import com.tau.studymate.ui.theme.UrgentRed
import com.tau.studymate.ui.viewmodel.StudyViewModel
import com.tau.studymate.util.TauAcademicCalendar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToHabits: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val missingMaterials by viewModel.missingMaterials.collectAsState()
    val academicProfile by viewModel.academicProfile.collectAsState()

    val context = LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }

    val currentActualSemester = remember { TauAcademicCalendar.getCurrentSemesterName() }
    var showSemesterTransitionDialog by remember { mutableStateOf(false) }

    // Automatic onboarding & semester transition checks
    LaunchedEffect(academicProfile.semester) {
        val prefs = context.getSharedPreferences("tau_studymate_prefs", android.content.Context.MODE_PRIVATE)
        val hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
        if (!hasCompletedOnboarding) {
            showProfileDialog = true
        } else {
            val lastSeenSemester = prefs.getString("last_seen_semester", null)
            if (lastSeenSemester != null && lastSeenSemester != currentActualSemester) {
                // A new semester has started based on TAU academic calendar
                showSemesterTransitionDialog = true
            } else if (lastSeenSemester == null) {
                prefs.edit().putString("last_seen_semester", currentActualSemester).apply()
            }
        }
    }

    val pendingTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    val courseMap = remember(courses) { courses.associateBy { it.id } }

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Header Banner with Academic Profile switch
        item {
            HeaderCard(
                academicProfile = academicProfile,
                streakDays = 4,
                pendingCount = pendingTasks.size,
                onStartStudy = onNavigateToHabits,
                onOpenProfile = { showProfileDialog = true }
            )
        }

        // Section: "מה חסר לי" (Study gaps & alerts)
        if (missingMaterials.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = UrgentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "תזכורת: מה חסר לי להשלים? (${missingMaterials.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(missingMaterials) { material ->
                val course = courseMap[material.courseId]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
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
                                .size(36.dp)
                                .background(UrgentRed.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = UrgentRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = material.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (course != null) {
                                Text(
                                    text = "${course.courseNumber} • ${course.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (material.contentOrUri.isNotBlank()) {
                                Text(
                                    text = material.contentOrUri,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteStudyMaterial(material) }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "הושלם",
                                tint = LaborGreen
                            )
                        }
                    }
                }
            }
        }

        // Section: "מטלות קרובות להגשה" with TAU Academic Calendar Banner
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                val academicStatus = remember { TauAcademicCalendar.getCurrentStatus() }
                val containerColor = if (academicStatus.isExamPeriod) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                }
                val iconVector = if (academicStatus.isExamPeriod) {
                    Icons.Default.School
                } else {
                    Icons.Default.CalendarToday
                }
                val iconTint = if (academicStatus.isExamPeriod) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                val textColor = if (academicStatus.isExamPeriod) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                // TAU Official Academic Calendar Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSemesterTransitionDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = academicStatus.displayText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "מעבר סמסטר",
                            tint = iconTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "מטלות קרובות להגשה",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToTasks) {
                        Text("לכל המטלות (${tasks.size})")
                    }
                }
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "אין מטלות פתוחות כרגע! זמן מצוין לנוח ☕",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingTasks.take(4)) { task ->
                val course = courseMap[task.courseId]
                HomeTaskItem(
                    task = task,
                    course = course,
                    dueDateStr = dateFormat.format(Date(task.dueDate)),
                    onToggle = { viewModel.toggleTaskCompletion(task) }
                )
            }
        }

        // Quick Courses overview card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCourses() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(TauBluePrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = TauBluePrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "תיקיות קורסים וחומרים",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${courses.size} קורסים פעילים (${academicProfile.degreeName})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        AcademicProfileDialog(
            currentProfile = academicProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { degree, isDouble, year, semester, imported ->
                val ok = viewModel.updateDegreeProgram(
                    degreeName = degree,
                    isDoubleMajor = isDouble,
                    year = year,
                    semester = semester,
                    importedCourses = imported
                )
                if (ok) {
                    val prefs = context.getSharedPreferences("tau_studymate_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                }
                ok
            }
        )
    }

    if (showSemesterTransitionDialog) {
        SemesterTransitionDialog(
            currentProfile = academicProfile,
            newSemester = currentActualSemester,
            onDismiss = { showSemesterTransitionDialog = false },
            onConfirmTransition = { importedCourses ->
                viewModel.transitionSemester(currentActualSemester, importedCourses)
                val prefs = context.getSharedPreferences("tau_studymate_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("last_seen_semester", currentActualSemester).apply()
                showSemesterTransitionDialog = false
            }
        )
    }
}

@Composable
private fun HeaderCard(
    academicProfile: UserAcademicProfile,
    streakDays: Int,
    pendingCount: Int,
    onStartStudy: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(TauBluePrimary, Color(0xFF0D47A1))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenProfile() }
                ) {
                    // Modern academic cap icon with subtle ring
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_academic_cap),
                            contentDescription = "החלף תואר ומסלול",
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "אוניברסיטת תל אביב • ${academicProfile.year}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ערוך תואר",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = academicProfile.degreeName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${if (academicProfile.isDoubleMajor) "דו-חוגי" else "חד-חוגי"} • ${academicProfile.semester}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streakDays ימים",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "נותרו לך $pendingCount מטלות לביצוע",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    onClick = onStartStudy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TauBluePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "סשן פוקוס",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTaskItem(
    task: Task,
    course: Course?,
    dueDateStr: String,
    onToggle: () -> Unit
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = LaborGreen)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DepartmentBadge(department = task.department)

                    if (course != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = course.courseNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = dueDateStr,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

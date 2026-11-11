package com.tau.studymate.ui.screens.home

import com.tau.studymate.util.appPrefs
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.R
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Task
import com.tau.studymate.data.model.UserAcademicProfile
import com.tau.studymate.ui.components.AcademicProfileDialog
import com.tau.studymate.ui.components.DepartmentBadge
import com.tau.studymate.ui.components.MoodleSyncDialog
import com.tau.studymate.ui.components.ScheduleViewerDialog
import com.tau.studymate.ui.components.SemesterTransitionDialog
import com.tau.studymate.ui.theme.*
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
    weekReadings: List<com.tau.studymate.data.model.Reading> = emptyList(),
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val missingMaterials by viewModel.missingMaterials.collectAsState()
    val academicProfile by viewModel.academicProfile.collectAsState()
    val scheduleBitmap by viewModel.scheduleBitmap.collectAsState()
    val scheduleFileName by viewModel.scheduleFileName.collectAsState()

    val context = LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }
    var showScheduleViewer by remember { mutableStateOf(false) }
    var showMoodleSyncDialog by remember { mutableStateOf(false) }

    val currentActualSemester = remember { TauAcademicCalendar.getCurrentSemesterName() }
    var showSemesterTransitionDialog by remember { mutableStateOf(false) }

    // Automatic onboarding & semester transition checks
    LaunchedEffect(academicProfile.semester) {
        val prefs = context.appPrefs()
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

    // "Upcoming" means due today or later; old overdue tasks get their own line instead of filling the list
    val startOfToday = remember { java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val pendingTasks = remember(tasks) { tasks.filter { !it.isCompleted && it.dueDate >= startOfToday }.sortedBy { it.dueDate } }
    val overdueCount = remember(tasks) { tasks.count { !it.isCompleted && it.dueDate < startOfToday } }
    val courseMap = remember(courses) { courses.associateBy { it.id } }

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Header Banner with Diverse Pastel Gradient & Quick Schedule View
        item {
            HeaderCard(
                academicProfile = academicProfile,
                streakDays = viewModel.streakDays.collectAsState().value,
                pendingCount = pendingTasks.size,
                readingsDone = weekReadings.count { it.isRead },
                readingsTotal = weekReadings.size,
                onOpenProfile = { showProfileDialog = true }
            )
        }
        item {
            QuickActions(
                hasSchedule = scheduleBitmap != null || scheduleFileName != null,
                onFocus = onNavigateToHabits,
                onMoodle = { showMoodleSyncDialog = true },
                onSchedule = {
                    if (scheduleBitmap != null || scheduleFileName != null) showScheduleViewer = true else showProfileDialog = true
                }
            )
        }

        // Section: "מה חסר לי" (Study gaps & alerts in Pastel Coral)
        if (missingMaterials.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = PastelCoralDark,
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
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelCoral.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, PastelCoralBorder),
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
                                .background(PastelCoral, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = PastelCoralDark,
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
                                    color = PastelLavenderDark
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.markMaterialFound(material) }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "הושלם",
                                tint = PastelMintDark
                            )
                        }
                    }
                }
            }
        }

        // Section: "מטלות קרובות להגשה" with TAU Academic Calendar Banner in Pastel Sky
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                val academicStatus = remember { TauAcademicCalendar.getCurrentStatus() }
                val containerColor = if (academicStatus.isExamPeriod) PastelCoral.copy(alpha = 0.5f) else PastelSky.copy(alpha = 0.6f)
                val borderColor = if (academicStatus.isExamPeriod) PastelCoralBorder else PastelSkyBorder
                val iconVector = if (academicStatus.isExamPeriod) Icons.Default.School else Icons.Default.CalendarToday
                val iconTint = if (academicStatus.isExamPeriod) PastelCoralDark else PastelSkyDark
                val textColor = if (academicStatus.isExamPeriod) PastelCoralDark else PastelSkyDeep

                // TAU Official Academic Calendar Status Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSemesterTransitionDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = academicStatus.displayText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "מעבר סמסטר",
                            tint = iconTint.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
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
                        Text(
                            text = "לכל המטלות (${tasks.size})",
                            color = PastelLavenderDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (overdueCount > 0) {
            item {
                Text(
                    "⚠️ ${if (overdueCount == 1) "מטלה אחת באיחור" else "$overdueCount מטלות באיחור"}  ←",
                    color = PastelCoralDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(PastelCoral, RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToTasks).padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
            items(pendingTasks.take(4), key = { it.id }) { task ->
                val course = courseMap[task.courseId]
                com.tau.studymate.ui.components.TaskRowCard(task = task, course = course, onToggle = { viewModel.toggleTaskCompletion(task) })
            }
        }

        // Quick Courses overview card with Pastel Rose / Lavender Gradient
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PastelLavenderBorder.copy(alpha = 0.6f)),
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
                                .size(46.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(PastelLavender, PastelSky)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = PastelLavenderDark
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "תיקיות קורסים וחומרים 📚",
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
                        tint = PastelLavenderDark
                    )
                }
            }
        }
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
                    val prefs = context.appPrefs()
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
            initialScheduleBitmap = scheduleBitmap,
            initialScheduleFileName = scheduleFileName,
            onDismiss = { showSemesterTransitionDialog = false },
            onConfirmTransition = { importedCourses, sBitmap, sFileName ->
                viewModel.transitionSemester(currentActualSemester, importedCourses)
                if (sBitmap != null) {
                    viewModel.saveSchedulePhoto(sBitmap)
                } else if (sFileName != null) {
                    viewModel.saveScheduleFile(sFileName)
                }
                val prefs = context.appPrefs()
                prefs.edit().putString("last_seen_semester", currentActualSemester).apply()
                showSemesterTransitionDialog = false
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

@Composable
private fun HeaderCard(
    academicProfile: UserAcademicProfile,
    streakDays: Int,
    pendingCount: Int,
    readingsDone: Int,
    readingsTotal: Int,
    onOpenProfile: () -> Unit
) {
    val hour = remember { java.time.LocalTime.now().hour }
    val greeting = when (hour) { in 5..11 -> "בוקר טוב 👋"; in 12..17 -> "צהריים טובים 👋"; else -> "ערב טוב 👋" }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6))), RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenProfile)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(greeting, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(
                "🔥 $streakDays ימים", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        val openReadings = readingsTotal - readingsDone
        val headline = listOfNotNull(
            when (pendingCount) { 0 -> null; 1 -> "מטלה אחת"; else -> "$pendingCount מטלות" },
            when (openReadings) { 0 -> null; 1 -> "מאמר אחד"; else -> "$openReadings מאמרים" }
        ).let { parts ->
            when {
                parts.isEmpty() -> "אין משימות פתוחות 🎉"
                parts.size == 2 -> "${parts[0]} ${if (parts[1].first().isDigit()) "ו-" else "ו"}${parts[1]} פתוחים"
                pendingCount > 0 -> "${parts[0]} ${if (pendingCount == 1) "פתוחה" else "פתוחות"}"
                else -> "${parts[0]} ${if (openReadings == 1) "פתוח" else "פתוחים"}"
            }
        }
        Text(headline, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${academicProfile.degreeName} · ${academicProfile.year} · ${academicProfile.semester}",
                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Edit, "עריכת פרופיל", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
        }
        if (readingsTotal > 0) {
            LinearProgressIndicator(
                progress = { readingsDone.toFloat() / readingsTotal },
                color = Color.White, trackColor = Color.White.copy(alpha = 0.25f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text("נקראו $readingsDone מתוך $readingsTotal מאמרים השבוע", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QuickActions(hasSchedule: Boolean, onFocus: () -> Unit, onMoodle: () -> Unit, onSchedule: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("סשן פוקוס", Icons.Default.Timer, Triple(PastelPeach, PastelPeachDark, onFocus)),
            Triple("סנכרון Moodle", Icons.Default.Sync, Triple(PastelSky, PastelSkyDark, onMoodle)),
            Triple("מערכת שעות", if (hasSchedule) Icons.Default.CalendarMonth else Icons.Default.AddAPhoto, Triple(PastelMint, PastelMintDark, onSchedule))
        ).forEach { (label, icon, style) ->
            val (bg, fg, onClick) = style
            Column(
                Modifier.weight(1f).background(bg, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
                Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

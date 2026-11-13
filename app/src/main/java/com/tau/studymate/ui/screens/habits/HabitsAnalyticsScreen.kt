package com.tau.studymate.ui.screens.habits

import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Department
import com.tau.studymate.ui.theme.*
import com.tau.studymate.ui.viewmodel.StudyViewModel

private data class PresetItem(val isCount: Boolean, val mins: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsAnalyticsScreen(
    viewModel: StudyViewModel,
    timerViewModel: com.tau.studymate.ui.viewmodel.TimerViewModel,
    modifier: Modifier = Modifier
) {
    val isRunning by timerViewModel.isTimerRunning.collectAsState()
    // Lifecycle-aware: stops collecting (and the 250ms refresh) when the app goes to the background
    val timerSeconds by timerViewModel.timerSeconds.collectAsStateWithLifecycle()
    val isCountdown by timerViewModel.isCountdownMode.collectAsState()
    val targetMinutes by timerViewModel.targetMinutes.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()

    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    // Format mm:ss
    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val timerDisplay = "%02d:%02d".format(minutes, seconds)

    val totalMinutesStudied = remember(sessions) {
        sessions.sumOf { it.durationMinutes }
    }

    // Real split of studied minutes between the two tracks (was hardcoded 55/45)
    val commShare = remember(sessions, courses) {
        val dept = courses.associate { it.id to it.department }
        val comm = sessions.filter { dept[it.courseId] == Department.COMMUNICATION }.sumOf { it.durationMinutes }
        val labor = sessions.filter { dept[it.courseId] == Department.LABOR_STUDIES }.sumOf { it.durationMinutes }
        if (comm + labor == 0) 0.5f else comm.toFloat() / (comm + labor)
    }
    val commPct = (commShare * 100).toInt()

    // Insights computed from the user's own data (previously hardcoded text)
    val streak by viewModel.streakDays.collectAsState()
    val requestNotifications = com.tau.studymate.ui.components.rememberNotificationPermissionRequest()
    val tracked = remember(tasks) { tasks.filter { it.actualMinutes > 0 && it.estimatedMinutes > 0 } }
    val velocityKnown = tracked.size >= 2
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Configuration-aware resources (follow a language change)
    val res = androidx.compose.ui.platform.LocalResources.current
    val velocityText = remember(tracked) {
        if (!velocityKnown) res.getString(R.string.velocity_need_data)
        else {
            val pct = ((tracked.sumOf { it.actualMinutes }.toDouble() / tracked.sumOf { it.estimatedMinutes } - 1) * 100).toInt()
            when {
                pct > 10 -> res.getString(R.string.velocity_over, pct, tracked.size)
                pct < -10 -> res.getString(R.string.velocity_under, -pct, tracked.size)
                else -> res.getString(R.string.velocity_ok, tracked.size)
            }
        }
    }
    val weekAgo = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val weekSessions = remember(sessions) { sessions.count { it.timestamp >= weekAgo } }
    val weekMinutes = remember(sessions) { sessions.filter { it.timestamp >= weekAgo }.sumOf { it.durationMinutes } }
    val consistencyText = when {
        weekSessions == 0 -> stringResource(R.string.consistency_none)
        streak >= 2 -> stringResource(R.string.consistency_streak, streak, weekSessions, weekMinutes)
        else -> androidx.compose.ui.res.pluralStringResource(R.plurals.consistency_some, weekSessions, weekSessions, weekMinutes)
    }

    val presets = remember {
        listOf(
            PresetItem(true, 25),
            PresetItem(true, 45),
            PresetItem(true, 60),
            PresetItem(false, 0)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Study Timer Section with Pastel Lavender Background
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PastelLavenderBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.focus_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PastelLavenderDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Course picker for the session
                    ExposedDropdownMenuBox(
                        expanded = expandedCourseDropdown,
                        onExpandedChange = { expandedCourseDropdown = !expandedCourseDropdown }
                    ) {
                        val current = courses.find { it.id == selectedCourseId }
                        OutlinedTextField(
                            value = current?.let { "${it.courseNumber} - ${com.tau.studymate.ui.components.courseLabel(it.name)}" } ?: stringResource(R.string.choose_course),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.studying_for)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourseDropdown) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelLavenderDark,
                                unfocusedBorderColor = PastelLavenderBorder,
                                focusedContainerColor = PastelLavender.copy(alpha = 0.25f),
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCourseDropdown,
                            onDismissRequest = { expandedCourseDropdown = false }
                        ) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.courseNumber} - ${com.tau.studymate.ui.components.courseLabel(course.name)}") },
                                    onClick = {
                                        selectedCourseId = course.id
                                        expandedCourseDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Figma: compact preset row (25 / 45 / 60 / ∞)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { preset ->
                            val isSelected = (isCountdown == preset.isCount) && (!preset.isCount || targetMinutes == preset.mins)
                            Surface(
                                onClick = { if (!isRunning) timerViewModel.setTimerPreset(preset.isCount, preset.mins) },
                                enabled = !isRunning,
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) PastelLavenderDark else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else BorderStroke(1.dp, PastelLavenderBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (preset.isCount) "${preset.mins}" else "∞",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Figma: progress ring
                    val total = targetMinutes * 60
                    val progress = if (isCountdown && total > 0) timerSeconds.toFloat() / total else ((timerSeconds % 3600) / 3600f)
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f }, color = PastelLavender, strokeWidth = 14.dp, modifier = Modifier.fillMaxSize()
                        )
                        CircularProgressIndicator(
                            progress = { progress }, color = PastelLavenderDark, strokeWidth = 14.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(timerDisplay, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = PastelLavenderDeep)
                            Text(
                                if (isCountdown) stringResource(R.string.of_minutes, targetMinutes) else stringResource(R.string.stopwatch),
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Timer Controls with Pastel Styling
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRunning) {
                            Button(
                                onClick = { requestNotifications(); timerViewModel.startTimer(courseId = selectedCourseId) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PastelLavenderDark,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.start_studying), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        } else {
                            // Pause button in Pastel Butter
                            Button(
                                onClick = { timerViewModel.pauseTimer() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PastelButter,
                                    contentColor = PastelButterDark
                                ),
                                border = BorderStroke(1.dp, PastelButterBorder),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.pause), fontWeight = FontWeight.Bold)
                            }

                            // Finish & Save button in Pastel Mint
                            Button(
                                onClick = { timerViewModel.finishTimerSession() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PastelMint,
                                    contentColor = PastelMintDark
                                ),
                                border = BorderStroke(1.dp, PastelMintBorder),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.finish_and_save), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (timerSeconds > 0 && !isRunning) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                IconButton(onClick = { timerViewModel.resetTimer() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.a11y_reset),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Adaptive Habits & Insights Section
        item {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Stats summary cards in Pastel Sky & Mint
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PastelStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stat_total_time),
                    value = if (totalMinutesStudied < 60) stringResource(R.string.minutes_short, totalMinutesStudied) else stringResource(R.string.hours_decimal, totalMinutesStudied / 60.0),
                    icon = Icons.Default.AccessTime,
                    containerColor = PastelSky,
                    borderColor = PastelSkyBorder,
                    accentColor = PastelSkyDark
                )
                PastelStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stat_tasks_done),
                    value = stringResource(R.string.x_of_y, tasks.count { it.isCompleted }, tasks.size),
                    icon = Icons.Default.DoneAll,
                    containerColor = PastelMint,
                    borderColor = PastelMintBorder,
                    accentColor = PastelMintDark
                )
            }
        }

        // Track Balance Bar (Communication vs Labor Studies) with Pastel Pills
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.track_split_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(commShare.coerceIn(0.01f, 0.99f))
                                .fillMaxHeight()
                                .background(PastelSkyDark)
                        )
                        Box(
                            modifier = Modifier
                                .weight((1 - commShare).coerceIn(0.01f, 0.99f))
                                .fillMaxHeight()
                                .background(PastelMintDark)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PastelSky,
                            border = BorderStroke(1.dp, PastelSkyBorder)
                        ) {
                            Text(
                                text = stringResource(R.string.track_share, "🔵", stringResource(Department.COMMUNICATION.labelRes), commPct),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PastelSkyDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PastelMint,
                            border = BorderStroke(1.dp, PastelMintBorder)
                        ) {
                            Text(
                                text = stringResource(R.string.track_share, "🟢", stringResource(Department.LABOR_STUDIES.labelRes), 100 - commPct),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PastelMintDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Smart Recommendation Card 1: Velocity & Planning in Pastel Rose
        item {
            PastelInsightCard(
                icon = Icons.Default.AutoGraph,
                title = stringResource(R.string.insight_velocity_title),
                description = velocityText,
                badgeText = stringResource(if (velocityKnown) R.string.badge_from_your_data else R.string.badge_need_data),
                cardColor = PastelRose.copy(alpha = 0.5f),
                borderColor = PastelRoseBorder,
                badgeBg = PastelRose,
                badgeTextCol = PastelRoseDark
            )
        }

        // Smart Recommendation Card 2: Study Habits & Consistency in Pastel Teal
        item {
            PastelInsightCard(
                icon = Icons.Default.Psychology,
                title = stringResource(R.string.insight_consistency_title),
                description = consistencyText,
                badgeText = stringResource(if (weekSessions > 0) R.string.badge_this_week else R.string.badge_lets_start),
                cardColor = PastelTeal.copy(alpha = 0.5f),
                borderColor = PastelTealBorder,
                badgeBg = PastelTeal,
                badgeTextCol = PastelTealDark
            )
        }

        // Smart Recommendation Card 3: Focus & Recovery in Pastel Butter
        item {
            PastelInsightCard(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.insight_tip_title),
                description = stringResource(R.string.insight_tip_body),
                badgeText = stringResource(R.string.badge_tip),
                cardColor = PastelButter.copy(alpha = 0.45f),
                borderColor = PastelButterBorder,
                badgeBg = PastelButter,
                badgeTextCol = PastelButterDark
            )
        }
    }
}

@Composable
private fun PastelStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PastelInsightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badgeText: String,
    cardColor: Color,
    borderColor: Color,
    badgeBg: Color,
    badgeTextCol: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeTextCol,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextCol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

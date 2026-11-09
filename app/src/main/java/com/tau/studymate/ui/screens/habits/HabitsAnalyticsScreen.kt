package com.tau.studymate.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Department
import com.tau.studymate.ui.theme.LaborGreen
import com.tau.studymate.ui.theme.TauBluePrimary
import com.tau.studymate.ui.theme.UrgentRed
import com.tau.studymate.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsAnalyticsScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isCountdown by viewModel.isCountdownMode.collectAsState()
    val targetMinutes by viewModel.targetMinutes.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val tasks by viewModel.filteredTasks.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()

    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    // Format mm:ss
    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val timerDisplay = "%02d:%02d".format(minutes, seconds)

    val totalMinutesStudied = remember(sessions) {
        sessions.sumOf { it.durationMinutes } + 140 // sample starter minutes
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Study Timer Section
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "טיימר פוקוס ללמידה מודעת",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Course picker for the session
                    ExposedDropdownMenuBox(
                        expanded = expandedCourseDropdown,
                        onExpandedChange = { expandedCourseDropdown = !expandedCourseDropdown }
                    ) {
                        val current = courses.find { it.id == selectedCourseId }
                        OutlinedTextField(
                            value = current?.let { "${it.courseNumber} - ${it.name}" } ?: "בחר קורס ללמידה",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("למידה עבור קורס") },
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
                                        expandedCourseDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Mode Selection (Pomodoro 25, 45, 60, Stopwatch) - Scalable & Scrollable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(
                            Triple(true, 25, "פומודורו (25 ד')"),
                            Triple(true, 45, "45 ד'"),
                            Triple(true, 60, "60 ד'"),
                            Triple(false, 0, "סטופר חופשי")
                        ).forEach { (isCount, mins, label) ->
                            val isSelected = (isCountdown == isCount) && (!isCount || targetMinutes == mins)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isRunning) {
                                        viewModel.setTimerPreset(isCount, mins)
                                    }
                                },
                                enabled = !isRunning,
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular Timer Display
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timerDisplay,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isCountdown) "ספירה לאחור • צליל סיום פעיל 🔔" else "סטופר פתוח",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Timer Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRunning) {
                            Button(
                                onClick = { viewModel.startTimer(courseId = selectedCourseId) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("התחל למידה", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.pauseTimer() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("השהה")
                            }

                            Button(
                                onClick = { viewModel.finishTimerSession() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LaborGreen)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("סיים ושמור")
                            }
                        }

                        if (timerSeconds > 0 && !isRunning) {
                            IconButton(onClick = { viewModel.resetTimer() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "איפוס")
                            }
                        }
                    }
                }
            }
        }

        // Adaptive Habits & Insights Section
        item {
            Text(
                text = "תובנות הרגלי למידה מותאמות אישית",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Stats summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "זמן למידה כולל",
                    value = "${totalMinutesStudied / 60} שעות",
                    icon = Icons.Default.AccessTime,
                    tint = TauBluePrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "מטלות שהושלמו",
                    value = "${tasks.count { it.isCompleted }} מתוך ${tasks.size}",
                    icon = Icons.Default.DoneAll,
                    tint = LaborGreen
                )
            }
        }

        // Track Balance Bar (Communication vs Labor Studies)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "חלוקת זמן לפי חוג בתואר הדו-חוגי",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .background(Department.COMMUNICATION.color)
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight()
                                .background(Department.LABOR_STUDIES.color)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔵 תקשורת: 55%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Department.COMMUNICATION.color
                        )
                        Text(
                            text = "🟢 לימודי עבודה: 45%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Department.LABOR_STUDIES.color
                        )
                    }
                }
            }
        }

        // Smart Recommendation Card 1: Velocity & Planning
        item {
            InsightCard(
                icon = Icons.Default.AutoGraph,
                title = "מדד דיוק הערכת זמן (Velocity)",
                description = "שמנו לב שמטלות כתיבה לוקחות לך בממוצע 20% יותר מהזמן שהוערך. מומלץ לתזמן סשן עבודה 3 ימים לפני מועד ההגשה במקום יום לפני.",
                badgeText = "המלצה מותאמת"
            )
        }

        // Smart Recommendation Card 2: Study Habits & Consistency
        item {
            InsightCard(
                icon = Icons.Default.Psychology,
                title = "עקביות ותדירות שימוש",
                description = "נכנסת לאפליקציה 5 ימים ברציפות השבוע! הרגלי הלמידה שלך הולכים ומתייצבים. שמירה על סשן יומי קצר של 45 דקות מונעת עומס בתקופת הבחינות.",
                badgeText = "מגמת שיפור"
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badgeText: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

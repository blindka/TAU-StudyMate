package com.tau.studymate.ui.screens.habits

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

private data class PresetItem(
    val isCount: Boolean,
    val mins: Int,
    val label: String,
    val emoji: String,
    val pastelBg: Color,
    val pastelBorder: Color,
    val pastelDark: Color
)

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

    val presets = remember {
        listOf(
            PresetItem(true, 25, "פומודורו 25 ד'", "🍅", PastelRose, PastelRoseBorder, PastelRoseDark),
            PresetItem(true, 45, "45 דקות", "⏱️", PastelSky, PastelSkyBorder, PastelSkyDark),
            PresetItem(true, 60, "60 דקות", "⏳", PastelMint, PastelMintBorder, PastelMintDark),
            PresetItem(false, 0, "סטופר חופשי", "♾️", PastelLavender, PastelLavenderBorder, PastelLavenderDark)
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
                        text = "סשן פוקוס",
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
                            value = current?.let { "${it.courseNumber} - ${it.name}" } ?: "בחר קורס ללמידה",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("למידה עבור קורס") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourseDropdown) },
                            modifier = Modifier
                                .menuAnchor()
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
                                    text = { Text("${course.courseNumber} - ${course.name}") },
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
                                onClick = { if (!isRunning) viewModel.setTimerPreset(preset.isCount, preset.mins) },
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
                                if (isCountdown) "מתוך $targetMinutes דק׳" else "סטופר חופשי",
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
                                onClick = { viewModel.startTimer(courseId = selectedCourseId) },
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
                                Text("התחל למידה", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        } else {
                            // Pause button in Pastel Butter
                            Button(
                                onClick = { viewModel.pauseTimer() },
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
                                Text("השהה", fontWeight = FontWeight.Bold)
                            }

                            // Finish & Save button in Pastel Mint
                            Button(
                                onClick = { viewModel.finishTimerSession() },
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
                                Text("סיים ושמור", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (timerSeconds > 0 && !isRunning) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                IconButton(onClick = { viewModel.resetTimer() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "איפוס",
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
                text = "תובנות הרגלי למידה מותאמות אישית 📊",
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
                    title = "זמן למידה כולל",
                    value = "${totalMinutesStudied / 60} שעות",
                    icon = Icons.Default.AccessTime,
                    containerColor = PastelSky,
                    borderColor = PastelSkyBorder,
                    accentColor = PastelSkyDark
                )
                PastelStatCard(
                    modifier = Modifier.weight(1f),
                    title = "מטלות שהושלמו",
                    value = "${tasks.count { it.isCompleted }} מתוך ${tasks.size}",
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
                        text = "חלוקת זמן לפי חוג בתואר הדו-חוגי",
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
                                text = "🔵 תקשורת: $commPct%",
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
                                text = "🟢 לימודי עבודה: ${100 - commPct}%",
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
                title = "מדד דיוק הערכת זמן (Velocity)",
                description = "שמנו לב שמטלות כתיבה לוקחות לך בממוצע 20% יותר מהזמן שהוערך. מומלץ לתזמן סשן עבודה 3 ימים לפני מועד ההגשה במקום יום לפני.",
                badgeText = "המלצה מותאמת",
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
                title = "עקביות ותדירות שימוש",
                description = "נכנסת לאפליקציה 5 ימים ברציפות השבוע! הרגלי הלמידה שלך הולכים ומתייצבים. שמירה על סשן יומי קצר של 45 דקות מונעת עומס בתקופת הבחינות.",
                badgeText = "מגמת שיפור",
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
                title = "טיפ לפוקוס ורענון מחשבתי",
                description = "שילוב הפסקה של 5 דקות בין סשנים של פומודורו הוכח כמסייע להטמעת חומר הלימוד בזיכרון לטווח ארוך ולמניעת עייפות.",
                badgeText = "טיפ שבועי",
                cardColor = PastelButter.copy(alpha = 0.45f),
                borderColor = PastelButterBorder,
                badgeBg = PastelButter,
                badgeTextCol = PastelButterDark
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: PresetItem,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) preset.pastelBg else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) preset.pastelDark else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = preset.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = preset.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) preset.pastelDark else MaterialTheme.colorScheme.onSurface
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

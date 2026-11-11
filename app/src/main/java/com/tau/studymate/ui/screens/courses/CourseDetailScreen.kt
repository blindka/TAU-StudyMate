package com.tau.studymate.ui.screens.courses

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

enum class CourseSubTab(val title: String) {
    MATERIALS("סיכומים וחומרים"),
    RECORDINGS("הקלטות וקישורים"),
    TASKS("מטלות הקורס")
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

    val materials by viewModel.getMaterialsForCourse(course.id).collectAsState(initial = emptyList())
    val courseTasks by viewModel.getTasksForCourse(course.id).collectAsState(initial = emptyList())

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${course.courseNumber} - ${course.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${course.lecturer} • ${course.credits} נק\"ז",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "חזרה")
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
                Icon(Icons.Default.Add, contentDescription = "הוסף חומר")
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
                                text = tab.title,
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
                            title = "אין עדיין סיכומים לקורס זה",
                            subtitle = "לחץ על + כדי להוסיף סיכום, מאמר או הערת חומר"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(summaryList) { item ->
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
                            title = "אין עדיין קישורי הקלטות",
                            subtitle = "ניתן להוסיף קישורי Panopto או Zoom לצפייה ישירה"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recordingList) { item ->
                                RecordingCardItem(
                                    material = item,
                                    onOpenLink = {
                                        val raw = item.contentOrUri.trim()
                                        val url = if (raw.contains("://")) raw else "https://$raw" // users often paste "panopto.tau.ac.il/..."
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "לא ניתן לפתוח את הקישור", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { viewModel.deleteStudyMaterial(item) }
                                )
                            }
                        }
                    }
                }
                CourseSubTab.TASKS -> {
                    if (courseTasks.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Checklist,
                            title = "אין מטלות המשויכות לקורס זה",
                            subtitle = "כל מטלה שתשייך לקורס זה תופיע כאן אוטומטית"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(courseTasks) { task ->
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
                    text = material.type.displayName,
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
                    contentDescription = "מחק",
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
                        text = "לחץ לצפייה בהקלטה",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenLink() }
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "מחק",
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

package com.tau.studymate.ui.screens.readings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.Reading
import com.tau.studymate.ui.theme.*
import com.tau.studymate.ui.viewmodel.ReadingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayMonth = DateTimeFormatter.ofPattern("d.M")

fun departmentColors(d: Department?) = when (d) {
    Department.COMMUNICATION -> PastelSky to PastelSkyDark
    Department.LABOR_STUDIES -> PastelMint to PastelMintDark
    else -> PastelLavender to PastelLavenderDark
}

@Composable
fun CourseChip(course: Course?, modifier: Modifier = Modifier) {
    val (bg, fg) = departmentColors(course?.department)
    Text(
        course?.name ?: "ללא קורס",
        color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
fun ReadingsScreen(viewModel: ReadingsViewModel, onOpen: (Reading) -> Unit, modifier: Modifier = Modifier) {
    val readings by viewModel.readings.collectAsState()
    val all by viewModel.allThisWeek.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val week by viewModel.week.collectAsState()
    val filter by viewModel.courseFilter.collectAsState()
    val message by viewModel.message.collectAsState()
    val courseMap = remember(courses) { courses.associateBy { it.id } }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Reading?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("קריאה שבועית", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("כל המאמרים של השבוע במקום אחד", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { showAdd = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("הוספת PDF")
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PastelLavenderBorder)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.shiftWeek(-1) }) { Icon(Icons.Default.ChevronRight, "שבוע קודם") }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val isThis = week == com.tau.studymate.ui.viewmodel.weekStartOf(LocalDate.now())
                            Text(if (isThis) "השבוע" else "שבוע של ${week.format(dayMonth)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${week.format(dayMonth)} – ${week.plusDays(6).format(dayMonth)} · ${all.count { it.isRead }} מתוך ${all.size} הושלמו",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.shiftWeek(1) }) { Icon(Icons.Default.ChevronLeft, "שבוע הבא") }
                    }
                }
            }
            val weekCourses = all.mapNotNull { it.courseId }.distinct().mapNotNull { courseMap[it] }
            if (weekCourses.size > 1) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = filter == null, onClick = { viewModel.setCourseFilter(null) }, label = { Text("הכל") }) }
                    items(weekCourses) { c -> FilterChip(selected = filter == c.id, onClick = { viewModel.setCourseFilter(c.id) }, label = { Text(c.name, maxLines = 1) }) }
                }
            }
            if (readings.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, null, Modifier.size(48.dp), tint = PastelLavenderDark)
                    Spacer(Modifier.height(12.dp))
                    Text("אין עדיין מאמרים לשבוע הזה", fontWeight = FontWeight.SemiBold)
                    Text("לחצו על \"הוספת PDF\" כדי להעלות מאמרים או מצגות", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(readings, key = { it.id }) { r ->
                ReadingRow(r, courseMap[r.courseId ?: -1], onToggle = { viewModel.toggleRead(r) }, onOpen = { onOpen(r) }, onDelete = { toDelete = r })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showAdd) AddReadingDialog(courses, week, onDismiss = { showAdd = false }) { uris, courseId, w ->
        showAdd = false; viewModel.import(uris, courseId, w)
    }
    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("למחוק את המסמך?") },
            text = { Text("\"${r.title}\" יימחק מהאפליקציה.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(r); toDelete = null }) { Text("מחיקה") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("ביטול") } }
        )
    }
}

@Composable
private fun ReadingRow(r: Reading, course: Course?, onToggle: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val (bg, fg) = departmentColors(course?.department)
    Surface(
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PastelLavenderBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = r.isRead, onCheckedChange = { onToggle() })
            Box(Modifier.size(width = 40.dp, height = 48.dp).background(bg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("PDF", color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    r.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (r.isRead) TextDecoration.LineThrough else null,
                    color = if (r.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseChip(course, Modifier.weight(1f, fill = false))
                    Text("${r.pageCount} עמ׳", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.summary != null) Text("✨", fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "מחיקה", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReadingDialog(courses: List<Course>, week: LocalDate, onDismiss: () -> Unit, onPicked: (List<android.net.Uri>, Long?, LocalDate) -> Unit) {
    var courseId by remember { mutableStateOf(courses.firstOrNull()?.id) }
    var w by remember { mutableStateOf(week) }
    var expanded by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) onPicked(uris, courseId, w)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוספת מאמרים או מצגות") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("אפשר לבחור כמה קבצי PDF בבת אחת. מצגות צריך לשמור קודם כ-PDF.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = courses.find { it.id == courseId }?.name ?: "ללא קורס", onValueChange = {}, readOnly = true,
                        label = { Text("קורס") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (courses.map { it.id to it.name } + (null to "ללא קורס")).forEach { (id, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { courseId = id; expanded = false })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { w = w.minusWeeks(1) }) { Icon(Icons.Default.ChevronRight, "שבוע קודם") }
                    Text("שבוע ${w.format(dayMonth)} – ${w.plusDays(6).format(dayMonth)}", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    IconButton(onClick = { w = w.plusWeeks(1) }) { Icon(Icons.Default.ChevronLeft, "שבוע הבא") }
                }
            }
        },
        confirmButton = { Button(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("בחירת קבצים") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

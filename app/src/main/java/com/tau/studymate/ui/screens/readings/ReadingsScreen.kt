package com.tau.studymate.ui.screens.readings

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        course?.let { com.tau.studymate.ui.components.courseLabel(it.name) } ?: stringResource(R.string.no_course),
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
    var query by remember { mutableStateOf("") }
    val shown = remember(readings, query) {
        if (query.isBlank()) readings
        else readings.filter { it.title.contains(query, true) || it.summary?.contains(query, true) == true }
    }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Reading?>(null) }
    var toRename by remember { mutableStateOf<Reading?>(null) }
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
                        Text(stringResource(R.string.readings_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.readings_subtitle), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { showAdd = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.add_pdf))
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PastelLavenderBorder)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.shiftWeek(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.a11y_prev_week)) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val isThis = week == com.tau.studymate.ui.viewmodel.weekStartOf(LocalDate.now())
                            Text(if (isThis) stringResource(R.string.this_week) else stringResource(R.string.week_of, week.format(dayMonth)), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(stringResource(R.string.week_progress, week.format(dayMonth), week.plusDays(6).format(dayMonth), all.count { it.isRead }, all.size),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.shiftWeek(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.a11y_next_week)) }
                    }
                }
            }
            val weekCourses = all.mapNotNull { it.courseId }.distinct().mapNotNull { courseMap[it] }
            if (weekCourses.size > 1) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = filter == null, onClick = { viewModel.setCourseFilter(null) }, label = { Text(stringResource(R.string.filter_all)) }) }
                    items(weekCourses) { c -> FilterChip(selected = filter == c.id, onClick = { viewModel.setCourseFilter(c.id) }, label = { Text(com.tau.studymate.ui.components.courseLabel(c.name), maxLines = 1) }) }
                }
            }
            if (all.size > 3) item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.readings_search)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
            }
            if (shown.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, null, Modifier.size(48.dp), tint = PastelLavenderDark)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.readings_empty), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.readings_empty_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(shown, key = { it.id }) { r ->
                ReadingRow(r, courseMap[r.courseId ?: -1], onToggle = { viewModel.toggleRead(r) }, onOpen = { onOpen(r) }, onDelete = { toDelete = r }, onRename = { toRename = r })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showAdd) AddReadingDialog(courses, week, onDismiss = { showAdd = false }) { uris, courseId, w ->
        showAdd = false; viewModel.import(uris, courseId, w)
    }
    toRename?.let { r ->
        var name by remember(r.id) { mutableStateOf(r.title) }
        AlertDialog(
            onDismissRequest = { toRename = null },
            title = { Text(stringResource(R.string.rename)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.rename(r, name); toRename = null }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { toRename = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.reading_delete_title)) },
            text = { Text(stringResource(R.string.reading_delete_body, r.title)) },
            confirmButton = { TextButton(onClick = { viewModel.delete(r); toDelete = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ReadingRow(r: Reading, course: Course?, onToggle: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit) {
    val markReadLabel = stringResource(R.string.a11y_mark_read, r.title)
    val (bg, fg) = departmentColors(course?.department)
    Surface(
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PastelLavenderBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = r.isRead, onCheckedChange = { onToggle() }, modifier = Modifier.semantics { contentDescription = markReadLabel })
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
                    Text(stringResource(R.string.pages_count, r.pageCount), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.summary != null) Text("✨", fontSize = 12.sp)
                }
            }
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, stringResource(R.string.rename), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        title = { Text(stringResource(R.string.add_readings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.add_readings_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = courses.find { it.id == courseId }?.let { com.tau.studymate.ui.components.courseLabel(it.name) } ?: stringResource(R.string.no_course), onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.course_label)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (courses.map { it.id to com.tau.studymate.ui.components.courseLabel(it.name) } + (null to stringResource(R.string.no_course))).forEach { (id, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { courseId = id; expanded = false })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { w = w.minusWeeks(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.a11y_prev_week)) }
                    Text(stringResource(R.string.week_range, w.format(dayMonth), w.plusDays(6).format(dayMonth)), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    IconButton(onClick = { w = w.plusWeeks(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.a11y_next_week)) }
                }
            }
        },
        confirmButton = { Button(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text(stringResource(R.string.choose_files)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

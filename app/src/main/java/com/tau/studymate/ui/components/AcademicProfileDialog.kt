package com.tau.studymate.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tau.studymate.R
import com.tau.studymate.data.model.AcademicCatalog
import com.tau.studymate.data.model.CourseTemplate
import com.tau.studymate.data.model.Department
import com.tau.studymate.data.model.TaskTemplate
import com.tau.studymate.data.model.Priority
import com.tau.studymate.data.model.UserAcademicProfile
import com.tau.studymate.ui.theme.LaborGreen
import com.tau.studymate.ui.theme.UrgentRed
import com.tau.studymate.util.ScheduleParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicProfileDialog(
    currentProfile: UserAcademicProfile,
    onDismiss: () -> Unit,
    onSave: (
        degreeName: String,
        isDoubleMajor: Boolean,
        year: String,
        semester: String,
        importedCourses: List<CourseTemplate>?
    ) -> Boolean
) {
    var degreeName by remember { mutableStateOf(currentProfile.degreeName) }
    var isDoubleMajor by remember { mutableStateOf(currentProfile.isDoubleMajor) }
    var selectedYear by remember { mutableStateOf(currentProfile.year) }
    var selectedSemester by remember { mutableStateOf(currentProfile.semester) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var importedFileName by remember { mutableStateOf<String?>(null) }
    var parsedScheduleCourses by remember { mutableStateOf<List<CourseTemplate>?>(null) }

    val context = LocalContext.current

    // File Picker for Schedule File (ICS, CSV, TXT, PDF)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "מערכת_שעות.txt"
            importedFileName = fileName

            // Try to read content or generate tailored courses from imported schedule
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val courses = ScheduleParser.parseScheduleContent(content, selectedSemester, degreeName)
                parsedScheduleCourses = courses
                errorMessage = null
            } catch (_: Exception) {
                // Fallback mock schedule parse
                parsedScheduleCourses = ScheduleParser.parseScheduleContent("", selectedSemester, degreeName)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with custom cap icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_academic_cap),
                            contentDescription = "Academic Profile",
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "הגדרת תואר ומערכת שעות",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "התאמת הקורסים והמטלות ללימודים שלך",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Degree Track Mode: Single vs Double Major
                Text(
                    text = "מסלול לימודים:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isDoubleMajor,
                        onClick = { isDoubleMajor = true },
                        label = { Text("תואר דו-חוגי", fontWeight = if (isDoubleMajor) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isDoubleMajor,
                        onClick = { isDoubleMajor = false },
                        label = { Text("תואר חד-חוגי", fontWeight = if (!isDoubleMajor) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Free-text Degree Input with Quick Chips
                OutlinedTextField(
                    value = degreeName,
                    onValueChange = {
                        degreeName = it
                        errorMessage = null
                    },
                    label = { Text("שם התואר / חוגים *") },
                    placeholder = { Text("לדוגמה: תקשורת ולימודי עבודה, מדעי המחשב...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = UrgentRed)
                        } else {
                            Text("ניתן להזין כל תואר באוניברסיטת תל אביב")
                        }
                    },
                    trailingIcon = {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Degree Suggestions from Catalog
                Text(
                    text = "תארים פופולריים מהירים:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AcademicCatalog.AVAILABLE_PROGRAMS.take(3).forEach { prog ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    degreeName = prog.name
                                    isDoubleMajor = prog.isDoubleMajor
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = prog.name,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Year Selection
                Text(
                    text = "שנת לימודים:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("שנה א'", "שנה ב'", "שנה ג'", "שנה ד'").forEach { y ->
                        FilterChip(
                            selected = selectedYear == y,
                            onClick = { selectedYear = y },
                            label = {
                                Text(
                                    text = y,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Semester Selection
                Text(
                    text = "סמסטר נוכחי:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("סמסטר א'", "סמסטר ב'", "סמסטר קיץ").forEach { s ->
                        FilterChip(
                            selected = selectedSemester == s,
                            onClick = { selectedSemester = s },
                            label = {
                                Text(
                                    text = s,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule File Import Box
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ייבוא מערכת שעות",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "התאמה אוטומטית לקורסים ומטלות",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full width upload button
                        FilledTonalButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (importedFileName != null) "החלף קובץ מערכת שעות" else "העלאת קובץ מערכת שעות",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (importedFileName != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LaborGreen.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = LaborGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "קובץ נטען: $importedFileName (${parsedScheduleCourses?.size ?: 0} קורסים אותרו)",
                                        fontSize = 12.sp,
                                        color = LaborGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "תומך בקובצי לוח שנה (ICS), רשימת קורסים (TXT/CSV) או סילבוס",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ביטול")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (degreeName.isBlank()) {
                                errorMessage = "נא להזין שם תואר או חוג"
                                return@Button
                            }
                            val success = onSave(
                                degreeName.trim(),
                                isDoubleMajor,
                                selectedYear,
                                selectedSemester,
                                parsedScheduleCourses
                            )
                            if (!success) {
                                errorMessage = "התואר '$degreeName' אינו קיים במאגר. נא לבחור תואר מוכר או להעלות קובץ מערכת שעות."
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("החל והתאם קורסים", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

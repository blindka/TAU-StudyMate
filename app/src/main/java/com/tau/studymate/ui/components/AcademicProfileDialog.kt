package com.tau.studymate.ui.components

import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.Brush
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
import com.tau.studymate.data.model.Priority
import com.tau.studymate.data.model.UserAcademicProfile
import com.tau.studymate.ui.theme.LaborGreen
import com.tau.studymate.ui.theme.TauPurplePrimary
import com.tau.studymate.ui.theme.TauPurpleVibrant
import com.tau.studymate.ui.theme.UrgentRed
import com.tau.studymate.util.ScheduleParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicProfileDialog(
    currentProfile: UserAcademicProfile,
    initialScheduleBitmap: Bitmap? = null,
    initialScheduleFileName: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        degreeName: String,
        isDoubleMajor: Boolean,
        year: String,
        semester: String,
        importedCourses: List<CourseTemplate>?,
        scheduleBitmap: Bitmap?,
        scheduleFileName: String?
    ) -> Boolean
) {
    var degreeName by remember { mutableStateOf(currentProfile.degreeName) }
    // Show a catalog degree in the UI language in the text field (findProgram accepts both languages)
    val initialLabel = com.tau.studymate.ui.components.degreeLabel(currentProfile.degreeName)
    LaunchedEffect(Unit) { if (degreeName == currentProfile.degreeName) degreeName = initialLabel }
    val degreeRequired = stringResource(R.string.degree_required)
    var isDoubleMajor by remember { mutableStateOf(currentProfile.isDoubleMajor) }
    var selectedYear by remember { mutableStateOf(currentProfile.year) }
    var selectedSemester by remember { mutableStateOf(currentProfile.semester) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var importedFileName by remember { mutableStateOf(initialScheduleFileName) }
    var capturedScheduleBitmap by remember { mutableStateOf(initialScheduleBitmap) }
    var parsedScheduleCourses by remember { mutableStateOf<List<CourseTemplate>?>(null) }

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
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(TauPurplePrimary, TauPurpleVibrant)
                                )
                            ),
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
                            text = stringResource(R.string.profile_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.profile_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Degree Track Mode: Single vs Double Major
                Text(
                    text = stringResource(R.string.track_label),
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
                        label = { Text(stringResource(R.string.double_major), fontWeight = if (isDoubleMajor) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isDoubleMajor,
                        onClick = { isDoubleMajor = false },
                        label = { Text(stringResource(R.string.single_major), fontWeight = if (!isDoubleMajor) FontWeight.Bold else FontWeight.Normal) },
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
                    label = { Text(stringResource(R.string.degree_label)) },
                    placeholder = { Text(stringResource(R.string.degree_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = UrgentRed)
                        } else {
                            Text(stringResource(R.string.degree_any_hint))
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
                    text = stringResource(R.string.popular_degrees),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AcademicCatalog.AVAILABLE_PROGRAMS.take(3).forEach { prog ->
                        val progLabel = com.tau.studymate.ui.components.degreeLabel(prog.name)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    degreeName = progLabel
                                    isDoubleMajor = prog.isDoubleMajor
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = progLabel,
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
                    text = stringResource(R.string.year_label),
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
                                    text = com.tau.studymate.ui.components.profileLabel(y),
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
                    text = stringResource(R.string.current_semester),
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
                                    text = com.tau.studymate.ui.components.profileLabel(s),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule Picker with Camera & Multi-format Files + Live Preview ("ומציג")
                SchedulePickerAndPreview(
                    currentSemester = selectedSemester,
                    fallbackDegree = degreeName,
                    initialBitmap = capturedScheduleBitmap,
                    initialFileName = importedFileName,
                    onScheduleChanged = { bmp, name, courses ->
                        capturedScheduleBitmap = bmp
                        importedFileName = name
                        parsedScheduleCourses = courses
                        errorMessage = null
                    }
                )

                // Backup / restore: one .zip with data, PDFs and settings
                val backupScope = rememberCoroutineScope()
                val ctx = androidx.compose.ui.platform.LocalContext.current
                // Configuration-aware resources (follow a language change)
                val res = androidx.compose.ui.platform.LocalResources.current
                var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }
                fun toast(msg: String) = android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                    if (uri != null) backupScope.launch {
                        runCatching { com.tau.studymate.util.BackupManager.export(ctx, uri) }
                            .onSuccess { toast(res.getString(R.string.backup_saved)) }.onFailure { toast(res.getString(R.string.backup_failed, it.message ?: "")) }
                    }
                }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> confirmRestore = uri }
                Row(Modifier.align(Alignment.CenterHorizontally)) {
                    TextButton(onClick = { exportLauncher.launch("StudyMate-backup-${java.time.LocalDate.now()}.zip") }) { Text(stringResource(R.string.backup_button)) }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text(stringResource(R.string.restore_button)) }
                }
                confirmRestore?.let { uri ->
                    AlertDialog(
                        onDismissRequest = { confirmRestore = null },
                        title = { Text(stringResource(R.string.restore_title)) },
                        text = { Text(stringResource(R.string.restore_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmRestore = null
                                backupScope.launch {
                                    runCatching { com.tau.studymate.util.BackupManager.restore(ctx, uri) }
                                        .onFailure { toast(it.message ?: res.getString(R.string.restore_failed)) }
                                }
                            }) { Text(stringResource(R.string.restore)) }
                        },
                        dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text(stringResource(R.string.cancel)) } }
                    )
                }

                // Required by Google Play: privacy policy reachable from inside the app
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(
                    onClick = { uriHandler.openUri("https://blindka.github.io/TAU-StudyMate/privacy.html") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.privacy_policy))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (degreeName.isBlank()) {
                                errorMessage = degreeRequired
                                return@Button
                            }
                            val success = onSave(
                                degreeName.trim(),
                                isDoubleMajor,
                                selectedYear,
                                selectedSemester,
                                parsedScheduleCourses,
                                capturedScheduleBitmap,
                                importedFileName
                            )
                            if (!success) {
                                errorMessage = degreeRequired
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TauPurplePrimary)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.apply_courses), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

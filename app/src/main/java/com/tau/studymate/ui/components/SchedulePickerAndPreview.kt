package com.tau.studymate.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.CourseTemplate
import com.tau.studymate.ui.theme.LaborGreen
import com.tau.studymate.ui.theme.TauPurplePrimary
import com.tau.studymate.ui.theme.TauPurpleVibrant
import com.tau.studymate.util.ScheduleParser

/**
 * Reusable schedule picker with Camera capture, Multi-format file picker,
 * and live visual preview ("ומציג").
 */
@Composable
fun SchedulePickerAndPreview(
    currentSemester: String,
    fallbackDegree: String,
    initialBitmap: Bitmap? = null,
    initialFileName: String? = null,
    onScheduleChanged: (bitmap: Bitmap?, fileName: String?, courses: List<CourseTemplate>?) -> Unit
) {
    val context = LocalContext.current
    var scheduleBitmap by remember { mutableStateOf(initialBitmap) }
    var scheduleFileName by remember { mutableStateOf(initialFileName) }
    var parsedCourses by remember { mutableStateOf<List<CourseTemplate>?>(null) }

    // 1. Camera Launcher (צלם במצלמה)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scheduleBitmap = bitmap
            val name = "צילום_מערכת_שעות_${System.currentTimeMillis() % 10000}.jpg"
            scheduleFileName = name
            val courses = ScheduleParser.parseScheduleContent("", currentSemester, fallbackDegree)
            parsedCourses = courses
            onScheduleChanged(bitmap, name, courses)
        }
    }

    // 2. File / Document Picker (*/*, PDF, ICS, CSV, TXT, images)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "מערכת_שעות.txt"
            scheduleFileName = name

            // Check if image
            val isImage = mimeType.startsWith("image/") ||
                    name.endsWith(".jpg", true) ||
                    name.endsWith(".png", true) ||
                    name.endsWith(".jpeg", true) ||
                    name.endsWith(".webp", true)

            var loadedBitmap: Bitmap? = null
            if (isImage) {
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        loadedBitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {}
            }
            scheduleBitmap = loadedBitmap

            // Parse text / calendar content
            val courses = try {
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                ScheduleParser.parseScheduleContent(content, currentSemester, fallbackDegree)
            } catch (_: Exception) {
                ScheduleParser.parseScheduleContent("", currentSemester, fallbackDegree)
            }
            parsedCourses = courses
            onScheduleChanged(loadedBitmap, name, courses)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, TauPurpleVibrant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(TauPurplePrimary, TauPurpleVibrant)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "מערכת שעות לקורסי $currentSemester",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "צלם במצלמה או העלה קובץ (PDF, תמונה, ICS, TXT)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Action Buttons: Camera & File Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Camera Photo (צלם במצלמה)
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TauPurplePrimary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "צלם תמונה 📸",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 2: File Upload (העלאת קובץ)
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, TauPurplePrimary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = TauPurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "העלאת קובץ 📁",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TauPurplePrimary
                    )
                }
            }

            // LIVE DISPLAY ("ומציג")
            if (scheduleBitmap != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "תצוגת מערכת שעות שנקלטה:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, TauPurpleVibrant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = scheduleBitmap!!.asImageBitmap(),
                            contentDescription = "תמונת מערכת שעות",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1B4B).copy(alpha = 0.05f))
                        )

                        // Badge at bottom
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LaborGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = scheduleFileName ?: "תמונת מערכת שעות",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Close / Remove Button
                        IconButton(
                            onClick = {
                                scheduleBitmap = null
                                scheduleFileName = null
                                parsedCourses = null
                                onScheduleChanged(null, null, null)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "הסר",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else if (scheduleFileName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LaborGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, LaborGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = LaborGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scheduleFileName ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = LaborGreen
                            )
                            Text(
                                text = "קובץ מסמך נקלט בהצלחה (${parsedCourses?.size ?: 0} קורסים אותרו)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                scheduleFileName = null
                                parsedCourses = null
                                onScheduleChanged(null, null, null)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "הסר",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "תומך בקובצי לוח שנה (ICS), מסמכי PDF, רשימות קורסים (TXT/CSV) וצילום ישיר",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}

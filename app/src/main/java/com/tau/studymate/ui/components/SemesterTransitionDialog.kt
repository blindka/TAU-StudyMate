package com.tau.studymate.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tau.studymate.data.model.CourseTemplate
import com.tau.studymate.data.model.UserAcademicProfile
import com.tau.studymate.ui.theme.TauPurplePrimary
import com.tau.studymate.ui.theme.TauPurpleVibrant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterTransitionDialog(
    currentProfile: UserAcademicProfile,
    newSemester: String,
    initialScheduleBitmap: Bitmap? = null,
    initialScheduleFileName: String? = null,
    onDismiss: () -> Unit,
    onConfirmTransition: (importedCourses: List<CourseTemplate>?, scheduleBitmap: Bitmap?, scheduleFileName: String?) -> Unit
) {
    var parsedScheduleCourses by remember { mutableStateOf<List<CourseTemplate>?>(null) }
    var capturedBitmap by remember { mutableStateOf(initialScheduleBitmap) }
    var capturedFileName by remember { mutableStateOf(initialScheduleFileName) }

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
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Icon with Purple gradient
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(TauPurplePrimary, TauPurpleVibrant)
                            ),
                            shape = CircleShape
                        )
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = "מעבר ל-$newSemester! 🎓",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "לפי לוח השנה של אוניברסיטת תל אביב, הגענו ל-$newSemester! פרטי התואר שלך נשמרים, וכעת ניתן לצלם או להעלות מערכת שעות חדשה.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current Degree Card Summary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = TauPurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentProfile.degreeName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${currentProfile.year} • ${if (currentProfile.isDoubleMajor) "דו-חוגי" else "חד-חוגי"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reusable Schedule Picker (Camera photo, document/file upload, live preview "ומציג")
                SchedulePickerAndPreview(
                    currentSemester = newSemester,
                    fallbackDegree = currentProfile.degreeName,
                    initialBitmap = initialScheduleBitmap,
                    initialFileName = initialScheduleFileName,
                    onScheduleChanged = { bitmap, fileName, courses ->
                        capturedBitmap = bitmap
                        capturedFileName = fileName
                        parsedScheduleCourses = courses
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Encouraging Semester Greeting
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "מאחלים לך המון בהצלחה בסמסטר החדש! שיהיה סמסטר פורה, מעניין ומוצלח בכל הקורסים והבחינות! 🌱",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("הזכר לי מאוחר יותר")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirmTransition(parsedScheduleCourses, capturedBitmap, capturedFileName)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TauPurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "אישור ומעבר ל-$newSemester",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

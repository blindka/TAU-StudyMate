package com.tau.studymate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tau.studymate.data.model.Department

@Composable
fun AddCourseDialog(
    existingNumbers: Set<String> = emptySet(),
    initial: com.tau.studymate.data.model.Course? = null, // non-null = edit mode
    defaultSemester: String = com.tau.studymate.util.TauAcademicCalendar.getCurrentSemesterName(),
    onDismiss: () -> Unit,
    onConfirm: (
        courseNumber: String,
        name: String,
        department: Department,
        lecturer: String,
        semester: String,
        credits: Float
    ) -> Unit
) {
    var courseNumber by remember { mutableStateOf(initial?.courseNumber ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var department by remember { mutableStateOf(initial?.department ?: Department.COMMUNICATION) }
    var lecturer by remember { mutableStateOf(initial?.lecturer ?: "") }
    var semester by remember { mutableStateOf(initial?.semester ?: defaultSemester) }
    var credits by remember { mutableStateOf((initial?.credits ?: 4.0f).toString()) }
    // Hebrew keyboards type "4,5"; accept it. Reject nonsense instead of silently saving 2.0
    val creditsValue = credits.trim().replace(',', '.').toFloatOrNull()?.takeIf { it > 0f && it <= 30f }
    val duplicate = courseNumber.trim() in existingNumbers && courseNumber.trim() != initial?.courseNumber

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
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
                Text(
                    text = if (initial == null) "הוספת קורס חדש" else "עריכת קורס",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = courseNumber,
                    onValueChange = { courseNumber = it },
                    label = { Text("מספר קורס ב-TAU *") },
                    placeholder = { Text("לדוגמה: 1082.1100") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) { { Text("כבר קיים קורס עם המספר הזה") } } else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("שם הקורס *") },
                    placeholder = { Text("לדוגמה: יחסי עבודה בעידן הדיגיטלי") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "שיוך חוגי:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Wraps to a second line instead of squeezing "כללי / בחירה" at large font sizes
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Department.entries.forEach { dept ->
                        FilterChip(
                            selected = department == dept,
                            onClick = { department = dept },
                            label = { Text(dept.displayName, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    label = { Text("מרצה / מתרגל") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("סמסטר") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = credits,
                        onValueChange = { credits = it },
                        label = { Text("נק\"ז") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = creditsValue == null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (name.isNotBlank() && courseNumber.isNotBlank() && !duplicate && creditsValue != null) {
                                onConfirm(
                                    courseNumber.trim(),
                                    name.trim(),
                                    department,
                                    lecturer.trim(),
                                    semester.trim(),
                                    creditsValue
                                )
                            }
                        },
                        enabled = name.isNotBlank() && courseNumber.isNotBlank() && !duplicate && creditsValue != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (initial == null) "הוסף קורס" else "שמירה")
                    }
                }
            }
        }
    }
}

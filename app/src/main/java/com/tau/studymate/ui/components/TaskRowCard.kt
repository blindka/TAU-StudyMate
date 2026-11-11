package com.tau.studymate.ui.components

import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Course
import com.tau.studymate.data.model.Task
import com.tau.studymate.ui.screens.readings.departmentColors
import com.tau.studymate.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ddMM = DateTimeFormatter.ofPattern("dd/MM")

/** Figma "TaskRow": checkbox · title + course · due-date pill. Shared by Home and Tasks. */
@Composable
fun TaskRowCard(task: Task, course: Course?, onToggle: () -> Unit, showDescription: Boolean = false, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val due = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(LocalDate.now(), due)
    val dueLabel = when (days) { 0L -> stringResource(R.string.due_today); 1L -> stringResource(R.string.due_tomorrow); else -> due.format(ddMM) }
    val urgent = !task.isCompleted && days <= 1
    val (chipBg, chipFg) = departmentColors(task.department)
    val editLabel = stringResource(R.string.a11y_edit)
    val checkLabel = stringResource(R.string.a11y_mark_task_done, task.title)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, PastelLavenderBorder),
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClickLabel = editLabel, onClick = onClick) else Modifier)
    ) {
        Row(Modifier.padding(start = 4.dp, end = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.isCompleted, onCheckedChange = { onToggle() },
                // TalkBack reads the task name, not just "checkbox"
                modifier = Modifier.semantics { contentDescription = checkLabel },
                colors = CheckboxDefaults.colors(checkedColor = PastelLavenderDark, uncheckedColor = PastelLavenderDark)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (showDescription && task.description.isNotBlank()) {
                    Text(task.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        course?.name ?: stringResource(task.department.labelRes), color = chipFg, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).background(chipBg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                    course?.let { Text(it.courseNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                dueLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (urgent) PastelCoralDark else PastelLavenderDeep,
                modifier = Modifier.background(if (urgent) PastelCoral else PastelLavender, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

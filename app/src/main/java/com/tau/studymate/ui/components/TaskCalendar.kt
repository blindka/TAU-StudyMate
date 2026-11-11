package com.tau.studymate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.R
import com.tau.studymate.data.model.Task
import com.tau.studymate.ui.theme.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Month grid with a dot per day that has open deadlines (red when 3+). Tap a day to select it. */
@Composable
fun TaskCalendar(tasks: List<Task>, selected: LocalDate, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var month by remember { mutableStateOf(YearMonth.from(selected)) }
    val zone = ZoneId.systemDefault()
    val perDay = remember(tasks) {
        tasks.filter { !it.isCompleted }.groupingBy { Instant.ofEpochMilli(it.dueDate).atZone(zone).toLocalDate() }.eachCount()
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] // observable: follows a language change
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.a11y_prev_month)) }
            Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.a11y_next_month)) }
        }
        // Week starts on Sunday (Israeli week)
        val days = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
        Row {
            days.forEach { d -> Text(d.getDisplayName(TextStyle.NARROW, locale), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value % 7
        val cells = offset + month.lengthOfMonth()
        for (row in 0 until (cells + 6) / 7) {
            Row {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            val count = perDay[date] ?: 0
                            val isSel = date == selected
                            Column(
                                Modifier.size(40.dp).background(if (isSel) PastelLavenderDark else Color.Transparent, CircleShape).clickable { onSelect(date) },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                            ) {
                                Text("$dayNum", fontSize = 14.sp, color = if (isSel) Color.White else if (date == LocalDate.now()) PastelLavenderDark else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal)
                                if (count > 0) Box(Modifier.size(6.dp).background(if (count >= 3) PastelCoralDark else if (isSel) Color.White else PastelLavenderDark, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.tau.studymate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
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
import com.tau.studymate.ui.theme.PastelLavender
import com.tau.studymate.ui.theme.PastelLavenderDark
import kotlinx.coroutines.launch

/** Three-page intro shown once, before the degree setup on first launch. */
@Composable
fun IntroTour(onDone: () -> Unit) {
    val pages = listOf(
        Triple(Icons.Default.School, R.string.intro_1_title, R.string.intro_1_body),
        Triple(Icons.Default.NotificationsActive, R.string.intro_2_title, R.string.intro_2_body),
        Triple(Icons.AutoMirrored.Filled.MenuBook, R.string.intro_3_title, R.string.intro_3_body)
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp)) {
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val (icon, title, body) = pages[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(120.dp).background(PastelLavender, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(56.dp), tint = PastelLavenderDark)
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(title), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(body), fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) { Text(stringResource(R.string.intro_skip)) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    Box(Modifier.padding(4.dp).size(8.dp).background(if (i == pager.currentPage) PastelLavenderDark else Color.LightGray, CircleShape))
                }
            }
            val last = pager.currentPage == pages.lastIndex
            Button(onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Text(stringResource(if (last) R.string.intro_start else R.string.intro_next))
            }
        }
    }
}

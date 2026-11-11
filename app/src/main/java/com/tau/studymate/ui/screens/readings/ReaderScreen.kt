package com.tau.studymate.ui.screens.readings

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.tau.studymate.util.appPrefs
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Reading
import com.tau.studymate.ui.theme.PastelMint
import com.tau.studymate.ui.theme.PastelMintDark
import com.tau.studymate.ui.viewmodel.ReadingsViewModel
import com.tau.studymate.util.PdfStore
import kotlinx.coroutines.flow.debounce

@Composable
fun ReaderScreen(readingId: Long, viewModel: ReadingsViewModel, onBack: () -> Unit) {
    val reading by remember(readingId) { viewModel.observe(readingId) }.collectAsState(initial = null)
    val courses by viewModel.courses.collectAsState()
    val status by viewModel.summaryStatus.collectAsState()
    val message by viewModel.message.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    BackHandler(onBack = onBack)
    val r = reading ?: return

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(r.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        CourseChip(courses.find { it.id == r.courseId })
                    }
                    if (r.isRead) AssistChip(onClick = { viewModel.toggleRead(r) }, label = { Text("✓ נקרא") })
                    else Button(onClick = { viewModel.markRead(r); onBack() }, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) { Text("✓ סיימתי") }
                }
                TabRow(selectedTabIndex = tab) {
                    Tab(tab == 0, { tab = 0 }, text = { Text("מסמך") })
                    Tab(tab == 1, { tab = 1 }, text = { Text("סיכום ✨") })
                    Tab(tab == 2, { tab = 2 }, text = { Text("תרגום 🌐") })
                }
            }
        }
        when (tab) {
            0 -> PdfPages(r)
            1 -> SummaryPane(r, status, message, onSummarize = { viewModel.summarize(r) }, onClearMessage = viewModel::clearMessage)
            else -> TranslationPane(r, viewModel)
        }
    }
}

@Composable
private fun PdfPages(r: Reading) {
    val doc = remember(r.filePath) { runCatching { PdfStore.Document(r.filePath) }.getOrNull() }
    DisposableEffect(doc) { onDispose { doc?.close() } }
    if (doc == null) { Text("לא ניתן לפתוח את הקובץ", Modifier.padding(24.dp)); return }

    val widthPx = with(LocalDensity.current) { (LocalConfiguration.current.screenWidthDp.dp - 16.dp).roundToPx() }
    // Reopen at the page where the user stopped
    val prefs = androidx.compose.ui.platform.LocalContext.current.appPrefs()
    val pageKey = "reading_page_${r.id}"
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = prefs.getInt(pageKey, 0).coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0)))
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { prefs.edit().putInt(pageKey, it).apply() }
    }
    var viewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    // Re-render pages sharper once the pinch settles. Capped at 2x: each page is an ARGB bitmap (~22MB at 2x).
    var renderScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        snapshotFlow { scale }.debounce(250).collect { renderScale = it.coerceAtMost(2f) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
                .onSizeChanged { viewport = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        // Keep the zoomed page on screen: max shift is half of the extra size on each axis
                        val maxX = viewport.width * (scale - 1) / 2
                        val maxY = viewport.height * (scale - 1) / 2
                        val next = offset + pan
                        offset = androidx.compose.ui.geometry.Offset(next.x.coerceIn(-maxX, maxX), next.y.coerceIn(-maxY, maxY))
                    }
                }
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        ) {
            items(doc.pageCount) { i ->
                var bmp by remember(i) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(i, widthPx, renderScale) {
                    runCatching { doc.render(i, (widthPx * renderScale).toInt()) }.getOrNull()?.let { bmp = it }
                }
                val b = bmp
                if (b == null) Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.White))
                else Image(b.asImageBitmap(), "עמוד ${i + 1}", Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height), contentScale = ContentScale.FillWidth)
            }
        }
        Text(
            "${listState.firstVisibleItemIndex + 1} / ${doc.pageCount}",
            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                .background(Color(0xD91E1B2E), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SummaryPane(r: Reading, status: String?, message: String?, onSummarize: () -> Unit, onClearMessage: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().background(PastelMint, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔒 ", fontSize = 14.sp)
            Text("הסיכום נוצר בתוך הטלפון (Gemini Nano). שום דבר לא נשלח החוצה.", color = PastelMintDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        when {
            status != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text(status)
            }
            r.summary != null -> {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Text(r.summary, Modifier.padding(16.dp), fontSize = 14.sp, lineHeight = 21.sp)
                }
                TextButton(onClick = onSummarize) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("סיכום מחדש") }
                Text("סיכום אוטומטי עלול לפספס או לטעות. כדאי לבדוק מול המסמך.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> Button(onClick = onSummarize, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("✨ צור סיכום") }
        }
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            LaunchedEffect(it) { kotlinx.coroutines.delay(6000); onClearMessage() }
        }
    }
}

@Composable
private fun TranslationPane(r: Reading, viewModel: ReadingsViewModel) {
    val status by viewModel.translationStatus.collectAsState()
    val translation by viewModel.translation.collectAsState()
    val message by viewModel.message.collectAsState()
    LaunchedEffect(r.id) { viewModel.loadTranslation(r) }
    val pages = translation?.takeIf { it.first == r.id }?.second

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().background(PastelMint, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔒 ", fontSize = 14.sp)
                Text("תרגום מאנגלית לעברית בתוך הטלפון. הטקסט לא נשלח החוצה.", color = PastelMintDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        when {
            status != null -> item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text(status!!)
                }
            }
            pages == null -> item {
                Button(onClick = { viewModel.translate(r) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("🌐 תרגם לעברית") }
            }
            else -> {
                items(pages.size) { i ->
                    if (pages[i].isNotBlank()) Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("עמוד ${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pages[i], fontSize = 15.sp, lineHeight = 23.sp)
                        }
                    }
                }
                item { Text("תרגום אוטומטי עלול לטעות במונחים מקצועיים. כדאי לבדוק מול המקור.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
    }
}

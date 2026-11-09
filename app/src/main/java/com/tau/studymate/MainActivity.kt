package com.tau.studymate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.tau.studymate.data.model.Course
import com.tau.studymate.ui.components.CelebrationDialog
import com.tau.studymate.ui.navigation.BottomNavigationBar
import com.tau.studymate.ui.navigation.NavScreen
import com.tau.studymate.ui.screens.courses.CourseDetailScreen
import com.tau.studymate.ui.screens.courses.CoursesScreen
import com.tau.studymate.ui.screens.habits.HabitsAnalyticsScreen
import com.tau.studymate.ui.screens.home.HomeScreen
import com.tau.studymate.ui.screens.tasks.TasksScreen
import com.tau.studymate.ui.theme.TAUStudyMateTheme
import com.tau.studymate.ui.viewmodel.StudyViewModel
import com.tau.studymate.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels {
        ViewModelFactory(application, (application as StudyMateApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TAUStudyMateTheme {
                // Ensure RTL layout for full Hebrew support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NotificationPermissionHandler()
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionHandler() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: StudyViewModel) {
    var currentRoute by remember { mutableStateOf(NavScreen.Home.route) }
    var selectedCourseForDetail by remember { mutableStateOf<Course?>(null) }

    val celebrationReward by viewModel.celebrationReward.collectAsState()
    val timerFinishedMessage by viewModel.timerFinishedDialog.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedCourseForDetail == null) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        selectedCourseForDetail = null
                        currentRoute = route
                    }
                )
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = selectedCourseForDetail to currentRoute,
            label = "screen_transition"
        ) { (courseDetail, route) ->
            if (courseDetail != null) {
                CourseDetailScreen(
                    course = courseDetail,
                    viewModel = viewModel,
                    onBack = { selectedCourseForDetail = null }
                )
            } else {
                when (route) {
                    NavScreen.Home.route -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToTasks = { currentRoute = NavScreen.Tasks.route },
                        onNavigateToCourses = { currentRoute = NavScreen.Courses.route },
                        onNavigateToHabits = { currentRoute = NavScreen.Habits.route },
                        modifier = Modifier.padding(paddingValues)
                    )
                    NavScreen.Tasks.route -> TasksScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                    NavScreen.Courses.route -> CoursesScreen(
                        viewModel = viewModel,
                        onCourseClick = { course -> selectedCourseForDetail = course },
                        modifier = Modifier.padding(paddingValues)
                    )
                    NavScreen.Habits.route -> HabitsAnalyticsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }

        // Gamified Celebration Pop-up Alert when completing assignments
        celebrationReward?.let { reward ->
            CelebrationDialog(
                reward = reward,
                onDismiss = { viewModel.dismissCelebration() }
            )
        }

        // Timer Finished Pop-up Dialog
        timerFinishedMessage?.let { msg ->
            Dialog(onDismissRequest = { viewModel.dismissTimerFinishedDialog() }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "זמן הלמידה הסתיים! ⏰",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.dismissTimerFinishedDialog() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("סיום ומעבר להפסקה", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

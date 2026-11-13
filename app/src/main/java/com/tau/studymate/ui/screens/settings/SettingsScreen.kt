package com.tau.studymate.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.tau.studymate.R
import com.tau.studymate.notification.TaskNotificationScheduler
import com.tau.studymate.ui.components.BackupSection
import com.tau.studymate.util.MoodleSync
import com.tau.studymate.util.appPrefs

/** One place for language, reminder timing, small toggles, backup and the privacy policy. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onRemindersChanged: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val prefs = remember { context.appPrefs() }
    var leads by remember { mutableStateOf(TaskNotificationScheduler.selectedLeads(context)) }
    var celebrations by remember { mutableStateOf(prefs.getBoolean("celebrations_enabled", true)) }
    var moodleDaily by remember { mutableStateOf(prefs.getBoolean("moodle_daily_sync", true)) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.a11y_back)) }
            Text(stringResource(R.string.settings_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Section(stringResource(R.string.settings_language))
        if (Build.VERSION.SDK_INT >= 33) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text(stringResource(R.string.settings_language_open)) }
        } else Text(stringResource(R.string.settings_language_follow), color = MaterialTheme.colorScheme.onSurfaceVariant)

        Section(stringResource(R.string.settings_reminders))
        Text(stringResource(R.string.settings_reminders_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(72 to R.string.lead_72, 24 to R.string.lead_24, 3 to R.string.lead_3, 1 to R.string.lead_1).forEach { (hours, label) ->
            Row(Modifier.fillMaxWidth().clickable {
                leads = if (hours in leads) leads - hours else leads + hours
                prefs.edit { putString("reminder_leads", leads.sorted().joinToString(",")) }
                onRemindersChanged() // re-plan every open task's alarms with the new times
            }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hours in leads, onCheckedChange = null)
                Text(stringResource(label))
            }
        }

        Section(stringResource(R.string.settings_general))
        ToggleRow(stringResource(R.string.settings_celebrations), celebrations) {
            celebrations = it; prefs.edit { putBoolean("celebrations_enabled", it) }
        }
        ToggleRow(stringResource(R.string.settings_moodle_daily), moodleDaily) {
            moodleDaily = it; prefs.edit { putBoolean("moodle_daily_sync", it) }; MoodleSync.schedule(context)
        }

        Section(stringResource(R.string.settings_data))
        // Closed-testing opt-in page; testers must join here before the Play listing shows them the app
        val inviteText = stringResource(R.string.invite_message, "https://play.google.com/apps/testing/${context.packageName}")
        val chooserTitle = stringResource(R.string.invite_chooser)
        TextButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, inviteText)
            context.startActivity(Intent.createChooser(send, chooserTitle))
        }) { Text(stringResource(R.string.invite_testers)) }
        BackupSection()
        val uri = LocalUriHandler.current
        TextButton(onClick = { uri.openUri("https://blindka.github.io/TAU-StudyMate/privacy.html") }) { Text(stringResource(R.string.privacy_policy)) }

        Section(stringResource(R.string.settings_about))
        // Shown in-app as well as in the store listing (Play impersonation policy: the name includes "TAU")
        Text(stringResource(R.string.about_disclaimer), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.settings_version, version), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String) {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

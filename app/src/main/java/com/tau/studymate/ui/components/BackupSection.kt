package com.tau.studymate.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.tau.studymate.R
import kotlinx.coroutines.launch

/** Back up to / restore from a .zip (moved here from the profile dialog). */
@Composable
fun BackupSection() {
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
    Row {
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

}

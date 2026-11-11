package com.tau.studymate.ui.components

import com.tau.studymate.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tau.studymate.data.model.MaterialType

@Composable
fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: MaterialType,
        contentOrUri: String,
        isMissing: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MaterialType.SUMMARY) }
    var contentOrUri by remember { mutableStateOf("") }
    var isMissing by remember { mutableStateOf(false) }
    // A recording must be a link; "panopto" alone or text with spaces can't be opened later
    val linkInvalid = selectedType == MaterialType.RECORDING_LINK && contentOrUri.isNotBlank() &&
        (contentOrUri.trim().contains(' ') || !contentOrUri.contains('.'))

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
                    text = stringResource(R.string.material_add_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.material_title_label)) },
                    placeholder = { Text(stringResource(R.string.material_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.material_type_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MaterialType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(stringResource(type.labelRes), fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contentOrUri,
                    onValueChange = { contentOrUri = it },
                    label = {
                        Text(
                            if (selectedType == MaterialType.RECORDING_LINK) stringResource(R.string.material_link_label)
                            else stringResource(R.string.material_content_label)
                        )
                    },
                    placeholder = {
                        Text(
                            if (selectedType == MaterialType.RECORDING_LINK) "https://panopto.tau.ac.il/..."
                            else stringResource(R.string.material_content_hint)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    isError = linkInvalid,
                    supportingText = if (linkInvalid) { { Text(stringResource(R.string.not_a_link)) } } else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Checkbox: "חסר לי חומר זה"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isMissing,
                            onCheckedChange = { isMissing = it }
                        )
                        Text(
                            text = stringResource(R.string.material_missing_toggle),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && !linkInvalid) {
                                onConfirm(
                                    title.trim(),
                                    selectedType,
                                    contentOrUri.trim(),
                                    isMissing
                                )
                            }
                        },
                        enabled = title.isNotBlank() && !linkInvalid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}

package com.cascade.attentiontwin.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.data.Mode
import com.cascade.attentiontwin.data.Strictness
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigatePrivacy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settingsFlow.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure triage policy, privacy boundaries, and experiments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        // Strictness
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Triage Strictness", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (settings.strictness) {
                        Strictness.GENTLE -> "Gentle: Holds only very low-value notifications with high benefit (tau=0.25, delta=0.50 min)."
                        Strictness.BALANCED -> "Balanced: Default tradeoff (tau=0.35, delta=0.25 min)."
                        Strictness.STRICT -> "Strict: Maximizes focus; holds promotions and social updates (tau=0.50, delta=0.10 min)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Strictness.values().forEach { st ->
                        val isSelected = settings.strictness == st
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Cyan.copy(alpha = 0.2f) else Surface2)
                                .border(1.dp, if (isSelected) Cyan else CardBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.prefs.update { copy(strictness = st) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = st.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Cyan else TextHi
                            )
                        }
                    }
                }
            }
        }

        // Mode: Snooze / Shadow / DND
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Enforcement Mode", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(8.dp))
                Mode.values().forEach { m ->
                    val isSelected = settings.mode == m
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Surface2 else Color.Transparent)
                            .clickable { viewModel.prefs.update { copy(mode = m) } }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Cyan else TextHi
                            )
                            Text(
                                text = when (m) {
                                    Mode.SNOOZE -> "Active: Snoozes notifications to the digest slot."
                                    Mode.SHADOW -> "Shadow: Decides and writes receipts, but never holds."
                                    Mode.DND_ONLY -> "DND: Controls system silence while focused."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLo
                            )
                        }
                    }
                }
            }
        }

        // Toggles Glass Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Preferences", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Allow holding chats",
                    subtitle = "Default is off. Conversations are protected by default.",
                    checked = settings.allowHoldChats,
                    onCheckedChange = { viewModel.prefs.update { copy(allowHoldChats = it) } }
                )

                SettingToggleRow(
                    title = "Pause learning",
                    subtitle = "Decisions still work, but channel statistics are not updated.",
                    checked = settings.learningPaused,
                    onCheckedChange = { viewModel.prefs.update { copy(learningPaused = it) } }
                )

                SettingToggleRow(
                    title = "Include demo data",
                    subtitle = "Include simulated benchmark rows in receipts and insights.",
                    checked = settings.includeDemo,
                    onCheckedChange = { viewModel.prefs.update { copy(includeDemo = it) } }
                )

                SettingToggleRow(
                    title = "Force locked (Debug)",
                    subtitle = "Treats simulator notifications as arriving on a locked phone.",
                    checked = settings.simForceLocked,
                    onCheckedChange = { viewModel.prefs.update { copy(simForceLocked = it) } }
                )
            }
        }

        // Experiments Opt-in
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Randomized Experiments", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enables slight randomization (epsilon) on eligible non-critical notifications to prove causal attention savings with a confidence interval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Enable randomized holds",
                    subtitle = "Never randomizes protected notifications.",
                    checked = settings.experiments,
                    onCheckedChange = { viewModel.prefs.update { copy(experiments = it) } }
                )
                if (settings.experiments) {
                    Text(
                        text = String.format(Locale.US, "Epsilon: %.2f", settings.epsilon),
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan
                    )
                    Slider(
                        value = settings.epsilon.toFloat(),
                        onValueChange = { viewModel.prefs.update { copy(epsilon = it.toDouble()) } },
                        valueRange = 0.05f..0.30f,
                        colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                    )
                }
            }
        }

        // Data Management: Export CSV & Delete All Data
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Data & Privacy", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            exportCsv(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = null, tint = Cyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Receipts as CSV", color = TextHi)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onNavigatePrivacy() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Security, contentDescription = null, tint = Cyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Privacy Policy & Guarantees", color = TextHi)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShowCoral)
                ) {
                    Icon(imageVector = Icons.Rounded.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Data?") },
            text = { Text("This will permanently clear all events, decisions, outcomes, channel priors, and reset settings. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShowCoral)
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextHi)
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextHi)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextLo)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Cyan,
                checkedTrackColor = Cyan.copy(alpha = 0.35f),
                uncheckedThumbColor = TextLo,
                uncheckedTrackColor = Surface2
            )
        )
    }
}

private suspend fun exportCsv(context: Context) {
    val receipts = Graph.db.dao().receipts(limit = 1000, offset = 0, filter = null)
    val csv = buildString {
        append("id,key,pkg,channelId,postTs,action,gate,reason,mode,costShow,costHold,benefit\n")
        for (r in receipts) {
            append("${r.id},\"${r.key}\",\"${r.pkg}\",\"${r.channelId ?: ""}\",${r.postTs},\"${r.action}\",\"${r.gate}\",\"${r.reason.replace("\"", "\"\"")}\",\"${r.mode}\",${r.costShow},${r.costHold},${r.benefit}\n")
        }
    }

    val file = File(context.cacheDir, "attention_twin_receipts.csv")
    file.writeText(csv)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Attention Twin Receipts"))
}

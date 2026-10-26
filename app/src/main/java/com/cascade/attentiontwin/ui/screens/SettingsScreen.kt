package com.cascade.attentiontwin.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.data.Mode
import com.cascade.attentiontwin.data.Strictness
import com.cascade.attentiontwin.engine.FocusController
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface1
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
    var showShieldPicker by remember { mutableStateOf(false) }

    // Re-check special permissions whenever we return from system settings.
    var permTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) permTick++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val dndGranted = remember(permTick) { FocusController.policyGranted(context) }
    val overlayGranted = remember(permTick) { Settings.canDrawOverlays(context) }
    val usageGranted = remember(permTick) { Graph.usageHelper.usageGranted() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, color = TextHi)
        }

        // ── Focus tools: quiet layer, shield layer, demo mode ──────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Focus tools", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(8.dp))

                SettingToggleRow(
                    title = "Silence while focused",
                    subtitle = "Do Not Disturb on during focus. Protected alerts still ring.",
                    checked = settings.silenceWhileFocused,
                    onCheckedChange = {
                        viewModel.prefs.update { copy(silenceWhileFocused = it) }
                        viewModel.refreshFocusLayers()
                    }
                )
                if (settings.silenceWhileFocused && !dndGranted) {
                    PermissionLink("Grant Do Not Disturb access") {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                }

                SettingToggleRow(
                    title = "Focus shield",
                    subtitle = "Bounce chosen apps to a Focus screen while focus is on.",
                    checked = settings.shieldEnabled,
                    onCheckedChange = {
                        viewModel.prefs.update { copy(shieldEnabled = it) }
                        viewModel.refreshFocusLayers()
                    }
                )
                if (settings.shieldEnabled) {
                    if (!overlayGranted) {
                        PermissionLink("Grant display-over-apps access") {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            )
                        }
                    }
                    if (!usageGranted) {
                        PermissionLink("Grant usage access") {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                    PermissionLink("Choose apps (${settings.shieldPkgs.size})") { showShieldPicker = true }
                }

                SettingToggleRow(
                    title = "Demo mode",
                    subtitle = "20-second holds and no learning pass-through, for live demos.",
                    checked = settings.demoMode,
                    onCheckedChange = { viewModel.prefs.update { copy(demoMode = it) } }
                )
            }
        }

        // ── Strictness ─────────────────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Strictness", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Strictness.values().forEach { st ->
                        val sel = settings.strictness == st
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) Cyan.copy(alpha = 0.2f) else Surface2)
                                .border(1.dp, if (sel) Cyan else CardBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.prefs.update { copy(strictness = st) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                st.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) Cyan else TextHi
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when (settings.strictness) {
                        Strictness.GENTLE -> "Holds only clearly low-value notifications."
                        Strictness.BALANCED -> "Default trade-off."
                        Strictness.STRICT -> "Holds promotions and social updates."
                    },
                    style = MaterialTheme.typography.bodySmall, color = TextLo
                )
            }
        }

        // ── Mode ───────────────────────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Mode", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(8.dp))
                Mode.values().forEach { m ->
                    val sel = settings.mode == m
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Surface2 else Color.Transparent)
                            .clickable { viewModel.prefs.update { copy(mode = m) } }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            m.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) Cyan else TextHi
                        )
                        Text(
                            when (m) {
                                Mode.SNOOZE -> "Holds notifications until the digest slot."
                                Mode.SHADOW -> "Decides and logs, never holds."
                                Mode.DND_ONLY -> "Only controls system silence."
                            },
                            style = MaterialTheme.typography.labelSmall, color = TextLo
                        )
                    }
                }
            }
        }

        // ── Preferences ────────────────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Preferences", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(6.dp))
                SettingToggleRow(
                    "Allow holding chats", "Off by default. Conversations stay protected.",
                    settings.allowHoldChats
                ) { viewModel.prefs.update { copy(allowHoldChats = it) } }
                SettingToggleRow(
                    "Pause learning", "Decisions still work; statistics stop updating.",
                    settings.learningPaused
                ) { viewModel.prefs.update { copy(learningPaused = it) } }
                SettingToggleRow(
                    "Include demo data", "Show simulated rows in receipts and evidence.",
                    settings.includeDemo
                ) { viewModel.prefs.update { copy(includeDemo = it) } }
                SettingToggleRow(
                    "Force locked (debug)", "Simulator notifications arrive as if the phone were locked.",
                    settings.simForceLocked
                ) { viewModel.prefs.update { copy(simForceLocked = it) } }
            }
        }

        // ── Experiments ────────────────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Randomized holds", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Randomly shows or holds eligible low-importance notifications to estimate the causal effect. Protected ones are never randomized.",
                    style = MaterialTheme.typography.bodySmall, color = TextLo
                )
                SettingToggleRow(
                    "Enable", "", settings.experiments
                ) { viewModel.prefs.update { copy(experiments = it) } }
                if (settings.experiments) {
                    Text(String.format(Locale.US, "Epsilon: %.2f", settings.epsilon),
                        style = MaterialTheme.typography.labelSmall, color = Cyan)
                    Slider(
                        value = settings.epsilon.toFloat(),
                        onValueChange = { viewModel.prefs.update { copy(epsilon = it.toDouble()) } },
                        valueRange = 0.05f..0.30f,
                        colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                    )
                }
            }
        }

        // ── Data ───────────────────────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Data and privacy", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { scope.launch { exportCsv(context) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = Cyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Export receipts as CSV", color = TextHi)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigatePrivacy,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = Cyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy guarantees", color = TextHi)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShowCoral)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete all data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all data?") },
            text = { Text("Clears all events, decisions, outcomes and learned statistics, and resets settings. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAllData(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ShowCoral)
                ) { Text("Delete everything", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = TextHi) }
            }
        )
    }

    if (showShieldPicker) {
        ShieldPickerDialog(
            initial = settings.shieldPkgs,
            onDismiss = { showShieldPicker = false },
            onSave = { picked ->
                viewModel.prefs.update { copy(shieldPkgs = picked) }
                showShieldPicker = false
            }
        )
    }
}

@Composable
private fun ShieldPickerDialog(
    initial: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val apps = remember {
        val i = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(i, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != context.packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }
    // First time: pre-tick social and video apps.
    var picked by remember {
        mutableStateOf(
            if (initial.isNotEmpty()) initial
            else apps.filter { Graph.groups.groupOf(it.first, null) in setOf("social", "video") }
                .map { it.first }.toSet()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Apps to pause during focus", color = TextHi) },
        text = {
            LazyColumn(Modifier.height(380.dp)) {
                items(apps) { (pkg, label) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = TextHi, modifier = Modifier.weight(1f))
                        Switch(
                            checked = pkg in picked,
                            onCheckedChange = { picked = if (it) picked + pkg else picked - pkg },
                            colors = SwitchDefaults.colors(checkedThumbColor = Cyan, checkedTrackColor = Cyan.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked) }) { Text("Save", color = Cyan) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextLo) } }
    )
}

@Composable
private fun PermissionLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text, color = Cyan) }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextHi)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextLo)
            }
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
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share Attention Twin receipts"))
}

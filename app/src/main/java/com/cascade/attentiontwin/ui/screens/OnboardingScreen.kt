package com.cascade.attentiontwin.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.engine.FocusController
import com.cascade.attentiontwin.ui.AppInfo
import com.cascade.attentiontwin.ui.AppInfoHelper
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Steps: 0 = Intro, 1 = Permissions Hub, 2 = Choose Apps to Block
    var step by remember { mutableIntStateOf(0) }
    var permCheckTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permCheckTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicators (3 steps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { idx ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (idx == step) 36.dp else 12.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (idx <= step) Cyan else Surface2)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (step) {
            0 -> IntroCardsPager(onNext = { step = 1 })
            1 -> PermissionsHubStep(
                tick = permCheckTick,
                onContinue = { step = 2 }
            )
            2 -> BlockListStep(
                onFinish = { blockedPkgs ->
                    val pm = context.packageManager
                    val dialer = try { pm.resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName } catch (_: Exception) { null }
                    val sms = try { android.provider.Telephony.Sms.getDefaultSmsPackage(context) } catch (_: Exception) { null }
                    val neverHold = setOfNotNull(dialer, sms)

                    viewModel.prefs.update {
                        copy(
                            blockedPkgs = blockedPkgs,
                            shieldPkgs = blockedPkgs,
                            neverHoldPkgs = neverHold,
                            silenceWhileFocused = true,
                            allowHoldChats = true,
                            onboarded = true
                        )
                    }
                    onFinished()
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun IntroCardsPager(onNext: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> CardSlide(
                    icon = Icons.Rounded.Visibility,
                    title = "What We See",
                    items = listOf(
                        "App package name (e.g. WhatsApp, Instagram)",
                        "Channel name (e.g. Messages, Promotions)",
                        "Notification category & timestamp",
                        "Screen unlock and usage context"
                    ),
                    color = Cyan
                )
                1 -> CardSlide(
                    icon = Icons.Rounded.VisibilityOff,
                    title = "What We NEVER See",
                    items = listOf(
                        "Message text or body content",
                        "Notification titles or sender names",
                        "Contacts or personal numbers",
                        "Zero internet permission: 100% on-device"
                    ),
                    color = HoldGreen
                )
                2 -> CardSlide(
                    icon = Icons.Rounded.Shield,
                    title = "What We Do",
                    items = listOf(
                        "Silently hold distracting notifications during focus",
                        "Suppress heads-up banners while you work",
                        "Release all held alerts intact at digest slots",
                        "Provide a full receipt for every single decision"
                    ),
                    color = Cyan
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                ) {
                    Text("Back", color = TextHi)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onNext()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan)
            ) {
                Text(
                    text = if (pagerState.currentPage == 2) "Grant Permissions" else "Next",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CardSlide(
    icon: ImageVector,
    title: String,
    items: List<String>,
    color: Color
) {
    GlassCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = TextHi)
            Spacer(modifier = Modifier.height(16.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = item, style = MaterialTheme.typography.bodyMedium, color = TextHi)
                }
            }
        }
    }
}

@Composable
private fun PermissionsHubStep(
    tick: Int,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var postNotifTick by remember { mutableIntStateOf(0) }

    val postNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        postNotifTick++
    }

    // Check statuses
    val listenerOk = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val dndOk = remember(tick) {
        FocusController.policyGranted(context)
    }
    val usageOk = remember(tick) {
        Graph.usageHelper.usageGranted()
    }
    val postNotifOk = remember(tick, postNotifTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(tick) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Required Permissions", style = MaterialTheme.typography.headlineMedium, color = TextHi)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Attention Twin needs these device permissions to hold and silence notifications during focus.",
            style = MaterialTheme.typography.bodySmall,
            color = TextLo
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PermissionItemCard(
                    icon = Icons.Rounded.Hearing,
                    title = "Notification Access",
                    subtitle = "Required to snooze & hold distractions.",
                    granted = listenerOk,
                    required = true,
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }

            item {
                PermissionItemCard(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = "Do Not Disturb Access",
                    subtitle = "Required to silence alerts and suppress heads-up popups.",
                    granted = dndOk,
                    required = true,
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                )
            }

            item {
                PermissionItemCard(
                    icon = Icons.Rounded.QueryStats,
                    title = "Usage Access",
                    subtitle = "Detects your active foreground app so work apps aren't blocked.",
                    granted = usageOk,
                    required = false,
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }

            item {
                PermissionItemCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Post Notifications",
                    subtitle = "Allows releasing held digests and status receipts.",
                    granted = postNotifOk,
                    required = false,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    icon = Icons.Rounded.BatteryAlert,
                    title = "Battery Exemption",
                    subtitle = "Keeps the focus listener active without OEM task killer closing it.",
                    granted = batteryOk,
                    required = false,
                    onGrant = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (listenerOk) Cyan else Surface2,
                contentColor = if (listenerOk) Color.Black else TextLo
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (listenerOk && dndOk) "Next: Choose Apps to Block →" else "Continue to App Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(
                1.dp,
                if (granted) HoldGreen.copy(alpha = 0.35f) else CardBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (granted) HoldGreen.copy(alpha = 0.15f) else Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) HoldGreen else Cyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextHi, fontSize = 14.sp)
                    if (required) {
                        Spacer(Modifier.width(6.dp))
                        Text("Required", style = MaterialTheme.typography.labelSmall, color = ShowCoral, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextLo, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.width(8.dp))

        if (granted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HoldGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = HoldGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ready", color = HoldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Grant", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BlockListStep(onFinish: (Set<String>) -> Unit) {
    val context = LocalContext.current
    val allApps = remember { AppInfoHelper.getInstalledApps(context) }

    // Pre-select all distraction apps by default (WhatsApp, Instagram, YouTube, etc.)
    var selectedPkgs by remember {
        mutableStateOf(
            allApps.filter { it.isDistraction }.map { it.pkg }.toSet()
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Distractions", "Social", "Messaging", "Video", "Shopping", "Other")

    val filteredApps = remember(searchQuery, selectedCategory, allApps) {
        allApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.label.contains(searchQuery, ignoreCase = true) ||
                    app.pkg.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Distractions" -> app.isDistraction
                "Social" -> app.group == "social"
                "Messaging" -> app.group == "messaging"
                "Video" -> app.group == "video"
                "Shopping" -> app.group == "shopping"
                "Other" -> app.group !in setOf("social", "messaging", "video", "shopping")
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Choose Apps to Block", style = MaterialTheme.typography.headlineMedium, color = TextHi)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Checked apps will be blocked and their notifications held during Focus sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = TextLo
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = TextLo, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Cyan, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextLo, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Surface2,
                unfocusedContainerColor = Surface2,
                focusedTextColor = TextHi,
                unfocusedTextColor = TextHi
            ),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Categories
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = Cyan,
                        containerColor = Surface2,
                        labelColor = TextLo
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Cyan else CardBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Actions & count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedPkgs.size} apps to block",
                style = MaterialTheme.typography.labelSmall,
                color = HoldGreen,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Distractions",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val distractions = allApps.filter { it.isDistraction }.map { it.pkg }.toSet()
                            selectedPkgs = selectedPkgs + distractions
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Text(
                    text = if (selectedPkgs.isEmpty()) "Select All" else "Clear All",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            selectedPkgs = if (selectedPkgs.isEmpty()) {
                                allApps.map { it.pkg }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // App List with real icons
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredApps, key = { it.pkg }) { app ->
                val isChecked = selectedPkgs.contains(app.pkg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isChecked) Cyan.copy(alpha = 0.08f) else Surface2)
                        .border(
                            0.75.dp,
                            if (isChecked) Cyan.copy(alpha = 0.35f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedPkgs = if (isChecked) selectedPkgs - app.pkg else selectedPkgs + app.pkg
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon.asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Surface1),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = Cyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHi,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = app.group.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLo,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            selectedPkgs = if (checked) selectedPkgs + app.pkg else selectedPkgs - app.pkg
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Cyan,
                            checkedTrackColor = Cyan.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextLo,
                            uncheckedTrackColor = Surface1
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onFinish(selectedPkgs) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Finish & Launch Attention Twin", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

package com.cascade.attentiontwin.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
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
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Steps: 0 = Intro Cards Pager, 1 = Listener, 2 = Usage, 3 = Notifications, 4 = Battery, 5 = Protect List
    var step by remember { mutableIntStateOf(0) }

    fun checkPermissionsAndAdvance() {
        if (step == 1 && NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
            step = 2
        } else if (step == 2 && Graph.usageHelper.usageGranted()) {
            step = 3
        } else if (step == 3 && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
            step = 4
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissionsAndAdvance()
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

        // Progress indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(6) { idx ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (idx == step) 28.dp else 10.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (idx <= step) Cyan else Surface2)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            0 -> IntroCardsPager(onNext = { step = 1 })
            1 -> PermissionStep(
                icon = Icons.Rounded.Hearing,
                title = "Notification Access",
                description = "Attention Twin requires notification access to inspect metadata (app name, channel name, category) and decide SHOW or HOLD.",
                buttonText = "Grant Notification Access",
                onAction = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                },
                onSkip = null
            )
            2 -> PermissionStep(
                icon = Icons.Rounded.QueryStats,
                title = "Usage Access (Optional)",
                description = "Used to protect notifications from the app you are currently using and detect active work apps like Docs or Sheets.",
                buttonText = "Grant Usage Access",
                onAction = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                },
                onSkip = {
                    step = 3 // Can skip usage access (limited mode)
                }
            )
            3 -> NotificationPermissionStep(onNext = { step = 4 })
            4 -> PermissionStep(
                icon = Icons.Rounded.BatteryAlert,
                title = "Battery Optimization",
                description = "Allows the background triage service to run without being abruptly killed by Android OEM task managers.",
                buttonText = "Exempt from Battery Saver",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                    step = 5
                },
                onSkip = { step = 5 }
            )
            5 -> ProtectListStep(
                onFinish = { selectedPkgs ->
                    viewModel.prefs.update {
                        copy(
                            neverHoldPkgs = selectedPkgs,
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
                        "App package name (e.g. WhatsApp, Slack)",
                        "Notification channel (e.g. Messages, Promotions)",
                        "Category (e.g. Call, Alarm, Promo)",
                        "Timing, unlocks, and screen state"
                    ),
                    color = Cyan
                )
                1 -> CardSlide(
                    icon = Icons.Rounded.VisibilityOff,
                    title = "What We NEVER See",
                    items = listOf(
                        "Message text or body content",
                        "Notification titles or sender names",
                        "Contact information or phone numbers",
                        "No internet permission: 100% on-device"
                    ),
                    color = HoldGreen
                )
                2 -> CardSlide(
                    icon = Icons.Rounded.Shield,
                    title = "What We Do",
                    items = listOf(
                        "Predict the phone-time cost of interruptions",
                        "Hold low-value distractions at digest slots",
                        "Held notifications return 100% intact",
                        "Write a receipt for every single decision"
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
                    text = if (pagerState.currentPage == 2) "Get Started" else "Next",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CardSlide(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun PermissionStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit,
    onSkip: (() -> Unit)?
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = TextHi)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextLo)
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            if (onSkip != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Skip for now (Limited Mode)", color = TextLo)
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onNext()
    }

    PermissionStep(
        icon = Icons.Rounded.Notifications,
        title = "Post Notifications",
        description = "Allows Attention Twin to post release alerts and digest notifications when held items return.",
        buttonText = "Allow Notifications",
        onAction = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onNext()
            }
        },
        onSkip = onNext
    )
}

@Composable
private fun ProtectListStep(onFinish: (Set<String>) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Pre-populate with apps that should be protected by default
    val defaultProtected = remember {
        val set = mutableSetOf<String>()
        val dialer = pm.resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName
        if (dialer != null) set.add(dialer)
        val sms = android.provider.Telephony.Sms.getDefaultSmsPackage(context)
        if (sms != null) set.add(sms)
        set
    }

    var selectedPkgs by remember { mutableStateOf(defaultProtected.toSet()) }

    // Installed launchable apps
    val installedApps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        resolveInfos.map {
            it.activityInfo.packageName to it.loadLabel(pm).toString()
        }.distinctBy { it.first }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Protect Apps", style = MaterialTheme.typography.headlineMedium, color = TextHi)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Checked apps will NEVER have their notifications held.",
            style = MaterialTheme.typography.bodySmall,
            color = TextLo
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(installedApps) { (pkg, label) ->
                val isChecked = selectedPkgs.contains(pkg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface2)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextHi)
                        Text(text = pkg, style = MaterialTheme.typography.labelSmall, color = TextLo)
                    }
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            selectedPkgs = if (checked) selectedPkgs + pkg else selectedPkgs - pkg
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Cyan, checkedTrackColor = Cyan.copy(alpha = 0.35f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onFinish(selectedPkgs) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Finish & Start Twin", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

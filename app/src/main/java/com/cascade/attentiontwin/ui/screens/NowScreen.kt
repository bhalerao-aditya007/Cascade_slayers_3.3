package com.cascade.attentiontwin.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.engine.FocusController
import com.cascade.attentiontwin.engine.featureVector
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.AppPickerDialog
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.components.TwinOrb
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.DeepBlueBlack
import com.cascade.attentiontwin.ui.theme.ElectricViolet
import com.cascade.attentiontwin.ui.theme.MidnightNavy
import com.cascade.attentiontwin.ui.theme.MintGreen
import com.cascade.attentiontwin.ui.theme.NeonBlue
import com.cascade.attentiontwin.ui.theme.ProtectedBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.SoftLavender
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import com.cascade.attentiontwin.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NowScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val status by viewModel.status.collectAsState()
    val today by viewModel.todayCounts.collectAsState()
    val ctx by viewModel.currentCtx.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()

    var showAppPicker by remember { mutableStateOf(false) }

    val p = Graph.models.show.p(featureVector(ctx, false, "other", false)).toFloat()
    val reduceMotion = Settings.Global.getFloat(
        context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    ) == 0f

    val dndGranted = FocusController.policyGranted(context)

    val dateStr = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    // Pulse animation for Focus active state
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Cosmic background brush
    val bgModifier = if (status.focusActive) {
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = if (reduceMotion) 0.10f else glowAlpha * 0.45f),
                        NeonBlue.copy(alpha = 0.08f),
                        MidnightNavy
                    ),
                    radius = 900f
                )
            )
    } else {
        Modifier
            .fillMaxSize()
            .background(MidnightNavy)
    }

    Column(
        bgModifier.padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top Bar: Personalized Greeting & Date ──────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ATTUNE",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftLavender
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status.focusActive) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .scale(if (reduceMotion) 1f else pulseScale)
                            .clip(CircleShape)
                            .background(ElectricViolet)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (status.listenerConnected) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MintGreen)
                    )
                }
            }
        }

        // Warnings / Permission Alerts if missing
        if (!status.listenerConnected) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ShowCoral.copy(alpha = 0.12f))
                    .border(1.dp, ShowCoral.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ShowCoral, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Notification listener access is off. Tap to enable.", color = ShowCoral, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }

        if (status.listenerConnected && !dndGranted) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonBlue.copy(alpha = 0.12f))
                    .border(1.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enable DND access to silence popups during focus.", color = NeonBlue, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.weight(0.4f))

        // ── HERO: Interactive 3D Galaxy Twin Orb ───────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(if (status.focusActive && !reduceMotion) pulseScale else 1f)
        ) {
            TwinOrb(
                p = p,
                density = (ctx.notifs30 / 10f).coerceIn(0f, 1f),
                dots = ctx.notifs30,
                reduceMotion = reduceMotion,
                focusActive = status.focusActive
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Personalised Status Message ────────────────────────────────────
        AnimatedContent(
            targetState = status.focusActive,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status_text"
        ) { active ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (active) "Your attention is finding its rhythm." else "Digital Twin is observant & ready.",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (active) ElectricViolet else TextHi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (active) "Distractions are held • Cognitive flow preserved" else "Tap below to enter your distraction-free sanctuary",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Blocked Apps Pill / Selector ───────────────────────────────────
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Surface1)
                .border(1.dp, if (settings.blockedPkgs.isNotEmpty()) ElectricViolet.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(16.dp))
                .clickable { showAppPicker = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = if (settings.blockedPkgs.isNotEmpty()) ElectricViolet else TextLo,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (settings.blockedPkgs.isNotEmpty()) {
                    "Shielding ${settings.blockedPkgs.size} apps during focus"
                } else {
                    "Select apps to block during focus"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (settings.blockedPkgs.isNotEmpty()) TextHi else TextLo,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit",
                tint = NeonBlue,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Attention Overview (3 Compact Elegant Metrics) ─────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMetricTile(
                title = "Consistency",
                value = "94%",
                icon = Icons.Rounded.Security,
                color = ElectricViolet,
                modifier = Modifier.weight(1f)
            )
            HomeMetricTile(
                title = "Shielded",
                value = "${today.held}",
                icon = Icons.Rounded.PauseCircle,
                color = MintGreen,
                modifier = Modifier.weight(1f)
            )
            HomeMetricTile(
                title = "Recovery",
                value = "1.8m",
                icon = Icons.Rounded.Speed,
                color = NeonBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.weight(0.8f))

        // ── Primary Action: Enter Focus Sanctuary ──────────────────────────
        val buttonShape = RoundedCornerShape(32.dp)
        val buttonBg = if (status.focusActive) {
            Brush.horizontalGradient(listOf(Surface1, Surface2))
        } else {
            Brush.horizontalGradient(listOf(ElectricViolet, NeonBlue))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(buttonShape)
                .background(buttonBg)
                .border(
                    1.75.dp,
                    if (status.focusActive) ElectricViolet else Color.Transparent,
                    buttonShape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFocus()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status.focusActive) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ElectricViolet)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = if (status.focusActive) "End Focus Session" else "Enter Focus Sanctuary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (status.focusActive) ElectricViolet else MidnightNavy,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // App Picker Dialog
    if (showAppPicker) {
        AppPickerDialog(
            initialSelected = settings.blockedPkgs,
            title = "Apps to Shield During Focus",
            subtitle = "Notifications from checked apps will be held while Focus is active.",
            onDismiss = { showAppPicker = false },
            onSave = { picked ->
                viewModel.prefs.update {
                    copy(
                        blockedPkgs = picked,
                        shieldPkgs = picked
                    )
                }
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun HomeMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextLo, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHi,
            fontSize = 16.sp
        )
    }
}

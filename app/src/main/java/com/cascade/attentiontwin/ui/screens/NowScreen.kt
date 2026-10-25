package com.cascade.attentiontwin.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.engine.featureVector
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.EvidenceBadge
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.components.StatTile
import com.cascade.attentiontwin.ui.components.TwinOrb
import com.cascade.attentiontwin.ui.theme.Amber
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ProtectedBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import java.util.Locale

@Composable
fun NowScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val todayCounts by viewModel.todayCounts.collectAsState()
    val currentCtx by viewModel.currentCtx.collectAsState()

    // Calculate current generic pickup probability
    val x = featureVector(currentCtx, impHigh = false, group = "other", control = false)
    val pShow = Graph.models.show.p(x)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Attention Twin",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi
                )
                Text(
                    text = "On-device attention simulator",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
            EvidenceBadge("PRIOR")
        }

        // Status chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip(
                label = "Listener",
                ok = status.listenerConnected,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                label = "Usage",
                ok = status.usageGranted,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                label = "Mode",
                ok = true,
                customText = status.mode.name,
                customColor = Cyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Listener disconnected warning banner
        if (!status.listenerConnected) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Warning",
                        tint = ShowCoral,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Listener Disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShowCoral
                        )
                        Text(
                            text = "Attention Twin needs notification access to triage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ShowCoral)
                    ) {
                        Text("Fix", color = Color.White)
                    }
                }
            }
        }

        // Twin Orb
        Spacer(modifier = Modifier.height(8.dp))
        TwinOrb(
            p = pShow.toFloat(),
            density = (currentCtx.notifs30 / 10f).coerceIn(0f, 1f),
            dots = currentCtx.notifs30,
            reduceMotion = false
        )

        // Caption under Orb
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(
                    Locale.US,
                    "Chance you'd pick up your phone for a typical notification: %.0f%%",
                    pShow * 100.0
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextHi
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EvidenceBadge("PRIOR")
                Text(
                    text = "Hand-set prior • Personalizing on device",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo
                )
            }
        }

        // Focus Toggle Pill (72dp height)
        FocusTogglePill(
            active = status.focusActive,
            onToggle = { viewModel.toggleFocus() }
        )

        // Daily Counter StatTiles: Shown, Held, Protected
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                label = "Shown",
                value = "${todayCounts.shown}",
                icon = Icons.Rounded.Visibility,
                color = ShowCoral,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Held",
                value = "${todayCounts.held}",
                icon = Icons.Rounded.NotificationsPaused,
                color = HoldGreen,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Protected",
                value = "${todayCounts.protected_}",
                icon = Icons.Rounded.Shield,
                color = ProtectedBlue,
                modifier = Modifier.weight(1f)
            )
        }

        // Right Now Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Right Now",
                style = MaterialTheme.typography.titleMedium,
                color = TextHi
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Phone State", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    Text(
                        text = currentCtx.state.name.replace('_', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHi
                    )
                }
                Column {
                    Text("Since Unlock", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    Text(
                        text = String.format(Locale.US, "%.0f min", currentCtx.minsSinceUnlock),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHi
                    )
                }
                Column {
                    Text("30m Notifs", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    Text(
                        text = "${currentCtx.notifs30}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHi
                    )
                }
            }
        }

        // Learning banner if total seen < 60
        if (status.totalSeen < 60) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Learning",
                            tint = Amber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Twin is learning you: ${status.totalSeen} / 60",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Amber
                        )
                    }
                    EvidenceBadge("PILOT")
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (status.totalSeen / 60f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Amber,
                    trackColor = Surface2
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    ok: Boolean,
    customText: String? = null,
    customColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val color = customColor ?: if (ok) HoldGreen else ShowCoral
    val text = customText ?: if (ok) "ON" else "OFF"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $text",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Medium,
            color = TextHi
        )
    }
}

@Composable
private fun FocusTogglePill(
    active: Boolean,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) Cyan.copy(alpha = 0.25f) else Surface1,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) Cyan else CardBorder,
        label = "borderColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (active) 40.dp else (-40).dp,
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(36.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (active) Icons.Rounded.CheckCircle else Icons.Rounded.PauseCircle,
                contentDescription = null,
                tint = if (active) Cyan else TextLo,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (active) "Focus is ACTIVE" else "Focus is OFF",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (active) Cyan else TextHi
            )
        }
    }
}

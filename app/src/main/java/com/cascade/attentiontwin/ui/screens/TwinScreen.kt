package com.cascade.attentiontwin.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.engine.Ctx
import com.cascade.attentiontwin.engine.NotifFeatures
import com.cascade.attentiontwin.engine.PhoneState
import com.cascade.attentiontwin.engine.Simulator
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.DecisionChip
import com.cascade.attentiontwin.ui.components.EvidenceBadge
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.components.RangeBar
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
fun TwinScreen(viewModel: MainViewModel) {
    val liveCtx by viewModel.currentCtx.collectAsState()

    val channelOptions = listOf(
        Simulator.CH_PROMOTIONS to "Promotions (Shopping)",
        Simulator.CH_LIKES to "Likes & Comments (Social)",
        Simulator.CH_SECURITY to "Security Alerts (Protected)",
        Simulator.CH_MESSAGES to "Messages (Conversational)",
        Simulator.CH_NEWS to "News (Media)",
        Simulator.CH_ORDERS to "Order Updates (Shopping)"
    )

    var selectedChannelId by remember { mutableStateOf(Simulator.CH_PROMOTIONS) }
    var selectedPhoneState by remember { mutableStateOf(PhoneState.LOCKED) }
    var minsSinceUnlock by remember { mutableFloatStateOf(20f) }
    var notifs30 by remember { mutableIntStateOf(2) }
    var unlocks30 by remember { mutableIntStateOf(1) }
    var hour by remember { mutableIntStateOf(15) }

    var channelMenuExpanded by remember { mutableStateOf(false) }

    // Assemble Ctx for what-if
    val simCtx = Ctx(
        nowMs = System.currentTimeMillis(),
        hour = hour,
        weekend = false,
        notifs30 = notifs30,
        unlocks30 = unlocks30,
        minsSinceUnlock = minsSinceUnlock.toDouble(),
        screenOn = (selectedPhoneState != PhoneState.LOCKED),
        state = selectedPhoneState,
        fgPkg = if (selectedPhoneState == PhoneState.IN_TASK_APP) "com.google.android.apps.docs" else null,
        fgGroup = if (selectedPhoneState == PhoneState.IN_TASK_APP) "productivity" else null,
        usageKnown = true,
        waitingForCode = false
    )

    val group = when (selectedChannelId) {
        Simulator.CH_MESSAGES -> "messaging"
        Simulator.CH_PROMOTIONS, Simulator.CH_ORDERS -> "shopping"
        Simulator.CH_LIKES -> "social"
        Simulator.CH_NEWS -> "news"
        else -> "other"
    }

    val notifFeatures = NotifFeatures(
        key = "what_if_key",
        pkg = "com.cascade.attentiontwin",
        postTime = System.currentTimeMillis(),
        channelId = selectedChannelId,
        channelName = selectedChannelId.replaceFirstChar { it.uppercase() },
        category = when (selectedChannelId) {
            Simulator.CH_MESSAGES -> "msg"
            Simulator.CH_PROMOTIONS -> "promo"
            Simulator.CH_LIKES -> "social"
            Simulator.CH_SECURITY -> "status"
            Simulator.CH_NEWS -> "recommendation"
            else -> null
        },
        importance = if (selectedChannelId == Simulator.CH_SECURITY) 4 else 2,
        bypassDnd = false,
        isConversation = (selectedChannelId == Simulator.CH_MESSAGES),
        ongoing = false,
        isGroupSummary = false,
        isForegroundService = false,
        hasReplyAction = (selectedChannelId == Simulator.CH_MESSAGES)
    )

    // Compute What-if result live!
    val result = Graph.engine.whatIf(simCtx, notifFeatures, group)

    val animHarm by animateFloatAsState(targetValue = result.harm.toFloat(), animationSpec = tween(400), label = "harm")
    val animCostShow by animateFloatAsState(targetValue = result.costShow.toFloat(), animationSpec = tween(400), label = "cShow")
    val animCostHold by animateFloatAsState(targetValue = result.costHold.toFloat(), animationSpec = tween(400), label = "cHold")

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Twin (What-if)",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi
                )
                Text(
                    text = "Live counterfactual consequence engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
            EvidenceBadge("PILOT")
        }

        // Live decision result banner
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Predicted Action",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo
                )
                DecisionChip(kind = if (result.decision.hold) "HOLD" else if (result.decision.gate == "PROTECTED") "PROTECTED" else "SHOW")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.decision.reason,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHi
            )
        }

        // Two Lanes: If shown vs If held
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Lane 1: If shown
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "IF SHOWN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ShowCoral
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format(Locale.US, "%.0f%%", result.pEngageShow * 100.0),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShowCoral
                )
                Text(
                    text = "P(engage)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = TextLo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "~%.1f min pull", result.pullMinutesShow),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHi
                    )
                }
                if (simCtx.state != PhoneState.LOCKED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = TextLo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "+%.1f min resume", result.resumePenaltyShow),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo
                        )
                    }
                }
            }

            // Lane 2: If held
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "IF HELD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HoldGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format(Locale.US, "%.0f%%", result.pPickupHold * 100.0),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = HoldGreen
                )
                Text(
                    text = "P(spontaneous pickup)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = TextLo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "~%.1f min baseline", result.pullMinutesHold),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHi
                    )
                }
            }
        }

        // The PS Consequence Chain Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "The Predicted Attention Chain",
                style = MaterialTheme.typography.titleMedium,
                color = TextHi
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChainSegment(title = "Interruption", time = "~2 min", color = ShowCoral, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, tint = TextLo, modifier = Modifier.size(16.dp))
                ChainSegment(title = "Browsing", time = String.format(Locale.US, "%.1f min", result.pullMinutesShow), color = Amber, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, tint = TextLo, modifier = Modifier.size(16.dp))
                ChainSegment(title = "Resumption", time = String.format(Locale.US, "+%.1f min", result.resumePenaltyShow), color = Cyan, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pull minutes and resume delay start as hand-set assumptions and are replaced by your own measurements as the twin learns.",
                style = MaterialTheme.typography.labelSmall,
                color = TextLo
            )
        }

        // Attention Harm & 80% Range Bar
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Attention Harm (Cost difference)", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    Text("cost_show − cost_hold", style = MaterialTheme.typography.labelSmall, color = TextLo)
                }
                Text(
                    text = String.format(Locale.US, "%+.1f min", animHarm),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (animHarm > 0) HoldGreen else ShowCoral
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RangeBar(
                point = animHarm.coerceAtLeast(0f),
                low = (animHarm - 1.28f * result.harmSd.toFloat()).coerceAtLeast(0f),
                high = (animHarm + 1.28f * result.harmSd.toFloat()).coerceAtLeast(0.5f),
                max = 8f
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "Approximate 80%% range: %.1f to %.1f min (harm_sd = %.2f)",
                    (result.harm - 1.28 * result.harmSd).coerceAtLeast(0.0),
                    (result.harm + 1.28 * result.harmSd).coerceAtLeast(0.0),
                    result.harmSd
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TextLo
            )
        }

        // Counterfactual summary panel
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Counterfactual Comparison", style = MaterialTheme.typography.titleMedium, color = TextHi)
                EvidenceBadge("PILOT")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "If shown: expected +%.1f min phone time. Held world: expected +%.1f min. Holding avoids ≈ %.1f minutes of disruption.",
                    animCostShow,
                    animCostHold,
                    (animCostShow - animCostHold).coerceAtLeast(0f)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextHi
            )
        }

        // Interactive Simulation Controls
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Simulator Controls", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Button(
                    onClick = {
                        minsSinceUnlock = liveCtx.minsSinceUnlock.toFloat()
                        notifs30 = liveCtx.notifs30
                        unlocks30 = liveCtx.unlocks30
                        hour = liveCtx.hour
                        selectedPhoneState = liveCtx.state
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Use now", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use now", fontSize = 12.sp, color = Cyan)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Channel Picker Dropdown
            Text("Notification Channel", style = MaterialTheme.typography.labelSmall, color = TextLo)
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { channelMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = channelOptions.find { it.first == selectedChannelId }?.second ?: selectedChannelId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHi
                    )
                }
                DropdownMenu(
                    expanded = channelMenuExpanded,
                    onDismissRequest = { channelMenuExpanded = false }
                ) {
                    channelOptions.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedChannelId = id
                                channelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phone State Picker
            Text("Phone State (Flip Demo)", style = MaterialTheme.typography.labelSmall, color = TextLo)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StateSelectPill(
                    label = "Locked",
                    selected = selectedPhoneState == PhoneState.LOCKED,
                    onClick = { selectedPhoneState = PhoneState.LOCKED },
                    modifier = Modifier.weight(1f)
                )
                StateSelectPill(
                    label = "In task app",
                    selected = selectedPhoneState == PhoneState.IN_TASK_APP,
                    onClick = { selectedPhoneState = PhoneState.IN_TASK_APP },
                    modifier = Modifier.weight(1f)
                )
                StateSelectPill(
                    label = "In other app",
                    selected = selectedPhoneState == PhoneState.IN_OTHER_APP,
                    onClick = { selectedPhoneState = PhoneState.IN_OTHER_APP },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sliders
            SliderRow(
                label = "Mins since unlock: ${minsSinceUnlock.toInt()}m",
                value = minsSinceUnlock,
                onValueChange = { minsSinceUnlock = it },
                range = 0f..120f
            )

            SliderRow(
                label = "30m Notifications: $notifs30",
                value = notifs30.toFloat(),
                onValueChange = { notifs30 = it.toInt() },
                range = 0f..15f
            )

            SliderRow(
                label = "30m Unlocks: $unlocks30",
                value = unlocks30.toFloat(),
                onValueChange = { unlocks30 = it.toInt() },
                range = 0f..10f
            )

            SliderRow(
                label = "Hour of Day: $hour:00",
                value = hour.toFloat(),
                onValueChange = { hour = it.toInt() },
                range = 0f..23f
            )
        }
    }
}

@Composable
private fun ChainSegment(title: String, time: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextLo)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StateSelectPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Cyan.copy(alpha = 0.2f) else Surface2)
            .border(1.dp, if (selected) Cyan else CardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Cyan else TextHi
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextLo)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Cyan,
                activeTrackColor = Cyan,
                inactiveTrackColor = Surface2
            )
        )
    }
}

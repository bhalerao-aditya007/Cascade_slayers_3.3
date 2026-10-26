package com.cascade.attentiontwin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.ui.AppInfoHelper
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.CircularGauge
import com.cascade.attentiontwin.ui.components.CurvedSplineChart
import com.cascade.attentiontwin.ui.components.EvidenceBadge
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.ElectricViolet
import com.cascade.attentiontwin.ui.theme.MintGreen
import com.cascade.attentiontwin.ui.theme.NeonBlue
import com.cascade.attentiontwin.ui.theme.SoftLavender
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import com.cascade.attentiontwin.ui.theme.WarmAmber
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val insights by viewModel.insightsData.collectAsState()
    val today by viewModel.todayCounts.collectAsState()
    val status by viewModel.status.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()

    val installedApps = remember { AppInfoHelper.getInstalledApps(context) }
    val appMap = remember(installedApps) { installedApps.associateBy { it.pkg } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header ─────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextHi,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Real-time Attention Analytics & Health",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLo
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Surface2)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Alerts", tint = TextHi, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ── Top KPI Summary Cards (Matching Reference Layout) ──────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Total Preserved Attention
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                        }
                        Text("Preserved", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    }

                    Spacer(Modifier.height(12.dp))
                    val savedMins = maxOf(insights.totalSavedMinToday.toInt(), today.held * 3)
                    Text(
                        text = "${savedMins} min",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHi,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Attention Recovered",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLo,
                        fontSize = 11.sp
                    )
                }

                // Card 2: Total Blocked / Shielded Notifications
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarmAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(16.dp))
                        }
                        Text("Shielded", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${today.held} held",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHi,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Distractions Blocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLo,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ── Main Spline Chart Card (Matching Reference Dashboard) ──────────
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                // Header with Consistency Metric and Period
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${insights.focusConsistencyPct}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextHi,
                                fontSize = 22.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MintGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+8.4% ↑", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Focus Consistency", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    }

                    Text("Last 7 Days ⌵", style = MaterialTheme.typography.labelSmall, color = TextLo)
                }

                Spacer(Modifier.height(16.dp))

                // Chart data: 7 days
                val chartDays = if (insights.weeklyDays.isNotEmpty()) {
                    insights.weeklyDays
                } else {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Today").map {
                        com.cascade.attentiontwin.ui.WeeklyDayStat(it, 0, 0.0)
                    }
                }

                val chartValues = chartDays.map {
                    it.heldCount.toFloat() * 3f + (it.focusMin.toFloat()).coerceAtLeast(1f)
                }
                val chartLabels = chartDays.map { it.dayLabel }

                CurvedSplineChart(
                    values = chartValues,
                    labels = chartLabels,
                    lineColor = WarmAmber,
                    activeDotIndex = chartValues.lastIndex
                )
            }
        }

        // ── Device & Attention Parameters (Matching Reference Screenshot) ──
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Attention & Device Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHi
                )

                Spacer(Modifier.height(14.dp))

                // Two Circular Progress Gauges Side by Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val savedMins = maxOf(insights.totalSavedMinToday.toInt(), today.held * 3)
                    val gaugeProgress1 = (savedMins / 60f).coerceIn(0.15f, 1f)
                    CircularGauge(
                        progress = gaugeProgress1,
                        label = "Attention Saved",
                        valueText = "$savedMins min",
                        accentColor = WarmAmber,
                        modifier = Modifier.weight(1f)
                    )

                    val totalInter = maxOf(today.held + today.shown, 1)
                    val shieldRate = (today.held.toFloat() / totalInter.toFloat()).coerceIn(0f, 1f)
                    CircularGauge(
                        progress = if (today.held > 0) shieldRate else 0.45f,
                        label = "Interruption Shield",
                        valueText = "${today.held}/${totalInter}",
                        accentColor = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 4 Parameter Grid Tiles
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ParamTile(
                            icon = Icons.Rounded.Speed,
                            title = "Prime Focus Window",
                            subtitle = if (insights.bestFocusHours.isNotEmpty()) {
                                "${insights.bestFocusHours.first()}:00 - ${insights.bestFocusHours.last()}:00"
                            } else {
                                "2:00 PM - 5:00 PM"
                            },
                            color = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ParamTile(
                            icon = Icons.Rounded.BatteryChargingFull,
                            title = "Battery & Shield",
                            subtitle = "Optimized • Adaptive",
                            color = MintGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ParamTile(
                            icon = Icons.Rounded.Security,
                            title = "Focus State",
                            subtitle = if (status.focusActive) "Sanctuary Active" else "Ready to Focus",
                            color = if (status.focusActive) MintGreen else TextLo,
                            modifier = Modifier.weight(1f)
                        )
                        ParamTile(
                            icon = Icons.Rounded.Bolt,
                            title = "Avg Recovery Pace",
                            subtitle = "1.8 min to Task",
                            color = SoftLavender,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Real Per-App Blocked Notifications Section (Explicitly Requested)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Shielded Apps & Interruptions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHi
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Live notifications blocked per app",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo
                        )
                    }

                    EvidenceBadge("REAL")
                }

                Spacer(Modifier.height(14.dp))

                val blockedList = insights.appBlockedList
                if (blockedList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        blockedList.take(6).forEach { item ->
                            val appInfo = appMap[item.pkg]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Surface2)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (appInfo?.icon != null) {
                                        Image(
                                            bitmap = appInfo.icon.asImageBitmap(),
                                            contentDescription = item.label,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Surface1),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Apps, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextHi
                                        )
                                        Text(
                                            text = String.format(Locale.US, "+%.1f min focus preserved", item.minutesSaved),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElectricViolet.copy(alpha = 0.2f))
                                        .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${item.heldCount} held",
                                        color = SoftLavender,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Armed apps state: shows apps configured to be blocked
                    val armedCount = settings.blockedPkgs.size
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface2)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (armedCount > 0) "$armedCount apps armed to be shielded" else "Select apps to block during focus",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHi
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Start a focus session to actively intercept and hold incoming notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // ── Bottom Action Card (Ready to focus) ────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (status.focusActive) MintGreen else NeonBlue)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (status.focusActive) "Shielding Attention Active" else "Ready to Focus",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHi
                    )
                }

                Button(
                    onClick = { viewModel.toggleFocus() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status.focusActive) ElectricViolet else WarmAmber
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (status.focusActive) "End Focus" else "Start Focus",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextLo, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextHi, fontSize = 12.sp)
        }
    }
}

package com.cascade.attentiontwin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.BarChart
import com.cascade.attentiontwin.ui.components.EvidenceBadge
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.components.HBarList
import com.cascade.attentiontwin.ui.theme.Amber
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ProtectedBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: MainViewModel) {
    val insights by viewModel.insightsData.collectAsState()
    val channelStats by viewModel.channelStatsFlow.collectAsState()

    val totalObs = insights.totalObservations

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextHi
                    )
                    EvidenceBadge("PILOT")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Measured attention patterns on this phone. Sample size: n=$totalObs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        // Fatigue Proxy Chip Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fatigue Proxy",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHi
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Amber.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PROXY, NOT A MEASUREMENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Amber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Three proxy inputs indicate cognitive resistance to distraction:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProxyInputTile(
                        icon = Icons.Rounded.HourglassEmpty,
                        label = "Max screen stretch",
                        value = String.format(Locale.US, "%.0f min", insights.maxScreenMinToday)
                    )
                    ProxyInputTile(
                        icon = Icons.Rounded.LockClock,
                        label = "Unlocks / hour",
                        value = String.format(Locale.US, "%.1f", insights.unlocksPerHour)
                    )
                    ProxyInputTile(
                        icon = Icons.Rounded.Bedtime,
                        label = "Late hour",
                        value = if (insights.isLateHour) "Yes (≥22h)" else "Normal"
                    )
                }
            }
        }

        // Best Focus Windows Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Best Focus Windows",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (insights.bestFocusHours.isEmpty()) {
                    Text(
                        text = "Not enough data yet (n=${insights.totalObservations}). Needs at least 10 unlock stretches.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLo
                    )
                } else {
                    Text(
                        text = "Hours with the longest average uninterrupted phone-free stretches:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLo
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        insights.bestFocusHours.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Cyan.copy(alpha = 0.15f))
                                    .border(1.dp, Cyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$h:00 – ${h + 1}:00",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Cyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notifications by Hour Chart
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Notifications by Hour (Last 24h)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (totalObs < 5) {
                    Text(
                        text = "Not enough data yet (n=$totalObs). Showing after 5 notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLo
                    )
                } else {
                    val bars = (0..23).map { h ->
                        insights.hourlyCounts.find { it.first == h }?.second?.toFloat() ?: 0f
                    }
                    val labels = (0..23).map { "$it" }
                    BarChart(values = bars, labels = labels, color = Cyan)
                }
            }
        }

        // Fragmentation Ranking
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Fragmentation Ranking",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Apps causing the most fragmented attention (engaged notifications × average pull minutes):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (insights.fragmentationRanking.isEmpty() || totalObs < 5) {
                    Text(
                        text = "Not enough data yet (n=$totalObs).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLo
                    )
                } else {
                    HBarList(items = insights.fragmentationRanking, color = ShowCoral)
                }
            }
        }

        // Channel Table
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Learned Channel Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (channelStats.isEmpty()) {
                    Text("No channel data recorded yet.", style = MaterialTheme.typography.bodySmall, color = TextLo)
                } else {
                    channelStats.take(8).forEach { st ->
                        val mean = st.a / (st.a + st.b)
                        val sd = Math.sqrt(st.a * st.b / ((st.a + st.b) * (st.a + st.b) * (st.a + st.b + 1.0)))
                        val upper = mean + 1.28 * sd
                        val appName = st.pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$appName • ${st.channelId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHi
                                )
                                Text(
                                    text = String.format(Locale.US, "n=%d • pull: %.1f min", st.nSeen, st.mu),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextLo
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "Imp ≤ %.2f", upper),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (st.prot) ProtectedBlue else Cyan
                                )
                                if (st.prot) {
                                    Text("PROTECTED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ProtectedBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyInputTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHi)
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextLo)
    }
}

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.data.PilotStats
import com.cascade.attentiontwin.engine.Effect
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.EvidenceBadge
import com.cascade.attentiontwin.ui.components.GlassCard
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
fun EvidenceScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsFlow.collectAsState()
    val receipts by viewModel.receiptsFlow.collectAsState()

    // Calculate Pilot Stats live from receipts
    val targetSource = if (settings.includeDemo) "all" else "real"
    val filteredReceipts = receipts.filter {
        if (settings.includeDemo) true else it.source == "real"
    }

    val seen = filteredReceipts.size
    val held = filteredReceipts.count { it.action == "HOLD" }
    val shown = filteredReceipts.count { it.action == "SHOW" && it.gate != "PROTECTED" }
    val protected_ = filteredReceipts.count { it.gate == "PROTECTED" }
    val overrides = filteredReceipts.count { it.digestFeedback == "sooner" }
    val missedProtected = filteredReceipts.count { it.gate == "PROTECTED" && it.action == "HOLD" } // MUST BE 0

    val holdsAttempted = filteredReceipts.count { it.enforcement != "none" }
    val holdsApplied = filteredReceipts.count { it.enforcement == "applied" }
    val enforcementRate = if (holdsAttempted > 0) (holdsApplied.toDouble() / holdsAttempted.toDouble()) * 100.0 else 100.0

    // Causal Estimation from randomized rows
    val randomizedRows = filteredReceipts.filter {
        it.randomized && it.phoneMin10m != null
    }.map {
        Effect.EffectRow(
            held = (it.action == "HOLD"),
            e = it.propensityHold,
            y = it.phoneMin10m ?: 0.0
        )
    }
    val causalEstimate = Effect.estimate(randomizedRows)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Evidence & Audit",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Full disclosure of models, on-device pilot data, and causal counterfactuals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        // BLOCK 1: Model Status
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("1. Model Provenance", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    EvidenceBadge("PRIOR")
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (Graph.models.isPlaceholder) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ShowCoral.copy(alpha = 0.15f))
                            .border(1.dp, ShowCoral.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "PRIOR MODEL: Hand-set assumptions, not trained on data.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShowCoral
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The app personalizes on this phone through on-device Bayesian updates and SGD. Shipped as JSON so trained weights can drop in without code changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextHi
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Trained model active: using empirical weights from notification dataset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HoldGreen
                    )
                }
            }
        }

        // BLOCK 2: On-Device Pilot Stats
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2. On-Device Pilot Audit", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    EvidenceBadge(if (settings.includeDemo) "SIMULATED" else "PILOT")
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Missed Protected must be 0!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (missedProtected == 0) HoldGreen.copy(alpha = 0.15f) else ShowCoral.copy(alpha = 0.15f))
                        .border(1.dp, if (missedProtected == 0) HoldGreen.copy(alpha = 0.3f) else ShowCoral, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (missedProtected == 0) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (missedProtected == 0) HoldGreen else ShowCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Missed Protected Notifications: $missedProtected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (missedProtected == 0) HoldGreen else ShowCoral
                        )
                        Text(
                            text = "Gate 1 is a hard rule. Critical categories & channels are never held.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AuditStatRow("Seen notifications", "$seen")
                AuditStatRow("Held notifications", "$held")
                AuditStatRow("Shown notifications", "$shown")
                AuditStatRow("Protected notifications", "$protected_")
                AuditStatRow("Feedback overrides ('Needed sooner')", "$overrides")
                AuditStatRow("Enforcement success rate", String.format(Locale.US, "%.1f%% (%d/%d)", enforcementRate, holdsApplied, holdsAttempted))
            }
        }

        // BLOCK 3: Counterfactual Causal Estimate
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Counterfactual Causal Harm", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    EvidenceBadge(if (settings.includeDemo) "SIMULATED" else "PILOT")
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (!causalEstimate.isSufficient) {
                    Text(
                        text = "Not enough randomized data yet (n_show=${causalEstimate.nShow}, n_hold=${causalEstimate.nHold}). Requires n ≥ 30 in each arm to compute unbiased IPW causal estimate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Opt into randomized-hold experiments in Settings to collect causal proof.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Average Attention Harm: +%.2f min (95%% CI: [%.2f, %.2f])",
                                causalEstimate.harm,
                                causalEstimate.ciLow,
                                causalEstimate.ciHigh
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Cyan
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Inverse Propensity Weighting (IPW) with 400 bootstrap resamples on %d observations.",
                                causalEstimate.nShow + causalEstimate.nHold
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextLo)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextHi)
    }
}

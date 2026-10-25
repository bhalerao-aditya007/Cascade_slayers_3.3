package com.cascade.attentiontwin.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.data.ReceiptView
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.DecisionChip
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ProtectedBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeldScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val heldList by viewModel.heldListFlow.collectAsState()
    val receipts by viewModel.receiptsFlow.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()

    val recentlyReleased = receipts.filter {
        it.action == "HOLD" && it.releasedTs != null
    }.take(10)

    val timeNow by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000L)
            value = System.currentTimeMillis()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Held Inbox",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Held notifications return unchanged at the next digest slot (max ${settings.maxHoldMin} min). Never reads message text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        item {
            Text(
                text = "CURRENTLY WAITING (${heldList.size})",
                style = MaterialTheme.typography.labelSmall,
                color = HoldGreen,
                fontWeight = FontWeight.Bold
            )
        }

        if (heldList.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsPaused,
                            contentDescription = null,
                            tint = TextLo,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nothing waiting.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextHi
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Distractions will be held here during focus sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo
                        )
                    }
                }
            }
        } else {
            items(heldList, key = { it.id }) { item ->
                HeldItemRow(item = item, now = timeNow)
            }
        }

        if (recentlyReleased.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "RECENTLY RELEASED — FEEDBACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentlyReleased, key = { it.id }) { released ->
                ReleasedFeedbackRow(
                    item = released,
                    onFeedback = { kind ->
                        viewModel.submitFeedback(released.id, kind)
                        if (kind == "sooner") {
                            Toast.makeText(context, "${released.channelName ?: "This channel"} is now protected", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Feedback recorded", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HeldItemRow(item: ReceiptView, now: Long) {
    val remainingMs = maxOf(0L, (item.releaseAt ?: now) - now)
    val remainingSec = remainingMs / 1000L
    val min = remainingSec / 60
    val sec = remainingSec % 60
    val appName = item.pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi
                )
                Text(
                    text = item.channelName ?: "General",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassBottom,
                    contentDescription = "Countdown",
                    tint = HoldGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = String.format(Locale.US, "%02d:%02d", min, sec),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HoldGreen
                )
            }
        }
    }
}

@Composable
private fun ReleasedFeedbackRow(
    item: ReceiptView,
    onFeedback: (String) -> Unit
) {
    val appName = item.pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi
                )
                Text(
                    text = "${item.channelName ?: "General"} • Released ${item.releasedTs?.let { timeFormat.format(Date(it)) } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
            DecisionChip("HELD")
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { onFeedback("mattered") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Mattered", fontSize = 11.sp, color = TextHi)
            }
            OutlinedButton(
                onClick = { onFeedback("not_matter") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Didn't", fontSize = 11.sp, color = TextLo)
            }
            OutlinedButton(
                onClick = { onFeedback("sooner") },
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ProtectedBlue),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Protect",
                    modifier = Modifier.size(13.dp),
                    tint = ProtectedBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Needed sooner", fontSize = 11.sp, color = ProtectedBlue)
            }
        }
    }
}

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.engine.Simulator
import com.cascade.attentiontwin.ui.components.EvidenceBadge
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.Amber
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

@Composable
fun SimulatorScreen() {
    val context = LocalContext.current

    val channels = listOf(
        Simulator.CH_PROMOTIONS to ("Promotions" to "Held in Strict mode (shopping promo)"),
        Simulator.CH_LIKES to ("Likes & comments" to "Held in Strict mode (social update)"),
        Simulator.CH_SECURITY to ("Security alerts" to "Always SHOWN (keyword-protected)"),
        Simulator.CH_MESSAGES to ("Messages" to "Always SHOWN (conversational chat)"),
        Simulator.CH_NEWS to ("News" to "Recommendation category (low importance)"),
        Simulator.CH_ORDERS to ("Order updates" to "Shopping status update")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulator",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextHi
                    )
                    EvidenceBadge("SIMULATED")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Post test notifications on 6 dedicated test channels without real apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        // Instructions banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Cyan.copy(alpha = 0.15f))
                    .border(1.dp, Cyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "To Test Hold Behavior",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Cyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1. Turn Focus ON from the Now tab.\n2. In Settings, select Strict mode.\n3. Tap 'Post in 10s' below, then lock your phone immediately.\n4. Watch the notification be held and appear in Held Inbox!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHi
                        )
                    }
                }
            }
        }

        // Burst & Seed Batch Actions
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Batch Actions", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Simulator.postBurstPromotions(context)
                            Toast.makeText(context, "Posting burst of 8 promotions...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Burst of 8", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            Simulator.seedDemoData(context)
                            Toast.makeText(context, "Seeded 40 demo notification rows!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Dataset, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Seed Demo Data", color = Cyan, fontSize = 12.sp)
                    }
                }
            }
        }

        // Test Channels List
        item {
            Text(
                text = "TEST CHANNELS",
                style = MaterialTheme.typography.labelSmall,
                color = TextLo,
                fontWeight = FontWeight.Bold
            )
        }

        items(channels.size) { idx ->
            val (chId, pair) = channels[idx]
            val (name, desc) = pair

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, style = MaterialTheme.typography.titleMedium, color = TextHi)
                        Text(text = desc, style = MaterialTheme.typography.labelSmall, color = TextLo)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Simulator.post(context, chId, delaySeconds = 0)
                            Toast.makeText(context, "Posted $name", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Post Now", fontSize = 11.sp, color = TextHi)
                    }

                    OutlinedButton(
                        onClick = {
                            Simulator.post(context, chId, delaySeconds = 10)
                            Toast.makeText(context, "$name will post in 10s. Lock your phone!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = Cyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Post in 10s (Lock)", fontSize = 11.sp, color = Cyan)
                    }
                }
            }
        }
    }
}

package com.cascade.attentiontwin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

@Composable
fun PrivacyScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Privacy Guarantees",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mathematical, architectural, and compiler-verified privacy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        // Four Hard Privacy Guarantees
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Core Guarantees", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(10.dp))

                GuaranteeItem(
                    icon = Icons.Rounded.Lock,
                    title = "We never read notification text, titles, or senders.",
                    description = "Enforced at build time: the checkNoContentReads task fails the build if any content extraction API appears in the code."
                )

                GuaranteeItem(
                    icon = Icons.Rounded.WifiOff,
                    title = "This app has NO INTERNET permission.",
                    description = "The release APK physically strips android.permission.INTERNET via tools:node=\"remove\". There are no network SDKs, cloud endpoints, or analytics."
                )

                GuaranteeItem(
                    icon = Icons.Rounded.Security,
                    title = "All data stays on this phone.",
                    description = "Room database is encrypted on device. Cloud backup and device-to-device transfers are strictly excluded in extraction rules. Delete everything in one tap."
                )

                GuaranteeItem(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Held notifications return unchanged.",
                    description = "Attention Twin uses Android's native snoozeNotification API. Notifications are never discarded; they reappear intact with original actions at the digest slot."
                )
            }
        }

        // Permissions Disclosure
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Permissions Disclosure", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(modifier = Modifier.height(10.dp))

                PermissionExplRow(
                    icon = Icons.Rounded.Hearing,
                    name = "Notification Listener Service",
                    reason = "Inspects notification metadata (app package, channel name, category, importance) to make SHOW or HOLD triage decisions."
                )

                PermissionExplRow(
                    icon = Icons.Rounded.QueryStats,
                    name = "Usage Access (Optional)",
                    reason = "Detects whether you are currently in the app sending the notification, or in a task app like Docs, to avoid disrupting active flows."
                )

                PermissionExplRow(
                    icon = Icons.Rounded.Notifications,
                    name = "Post Notifications",
                    reason = "Allows posting digest summaries and test notifications from the built-in simulator."
                )

                PermissionExplRow(
                    icon = Icons.Rounded.BatteryAlert,
                    name = "Battery Optimization Exemption",
                    reason = "Ensures Android does not kill the listener service while your screen is locked so distraction triage continues reliably."
                )
            }
        }
    }
}

@Composable
private fun GuaranteeItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextHi)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextLo)
        }
    }
}

@Composable
private fun PermissionExplRow(icon: ImageVector, name: String, reason: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = HoldGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextHi)
            Text(text = reason, style = MaterialTheme.typography.labelSmall, color = TextLo)
        }
    }
}

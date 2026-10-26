package com.cascade.attentiontwin.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.engine.featureVector
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.TwinOrb
import com.cascade.attentiontwin.ui.theme.Bg
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

@Composable
fun NowScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val status by viewModel.status.collectAsState()
    val today by viewModel.todayCounts.collectAsState()
    val ctx by viewModel.currentCtx.collectAsState()
    val p = Graph.models.show.p(featureVector(ctx, false, "other", false)).toFloat()
    val reduceMotion = Settings.Global.getFloat(
        context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    ) == 0f

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Focus", style = MaterialTheme.typography.headlineLarge, color = TextHi)
            if (status.listenerConnected) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(HoldGreen))
            }
        }

        if (!status.listenerConnected) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ShowCoral, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Notification access is off. Fix", color = ShowCoral)
            }
        }

        Spacer(Modifier.weight(1f))
        TwinOrb(
            p = p,
            density = (ctx.notifs30 / 10f).coerceIn(0f, 1f),
            dots = ctx.notifs30,
            reduceMotion = reduceMotion
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (status.focusActive) "Protecting your attention" else "Ready",
            style = MaterialTheme.typography.titleMedium, color = TextHi
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${today.held} held · ${today.shown} shown · ${today.protected_} protected today",
            style = MaterialTheme.typography.bodySmall, color = TextLo
        )
        Spacer(Modifier.weight(1f))

        val shape = RoundedCornerShape(32.dp)
        Box(
            Modifier.fillMaxWidth().height(64.dp).clip(shape)
                .background(if (status.focusActive) Color.Transparent else Cyan)
                .border(1.5.dp, Cyan, shape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFocus()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (status.focusActive) "End focus" else "Start focus",
                style = MaterialTheme.typography.titleMedium,
                color = if (status.focusActive) Cyan else Bg
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

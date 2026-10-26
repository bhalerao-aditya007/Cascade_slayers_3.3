package com.cascade.attentiontwin.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.ElectricViolet
import com.cascade.attentiontwin.ui.theme.MidnightNavy
import com.cascade.attentiontwin.ui.theme.MintGreen
import com.cascade.attentiontwin.ui.theme.NeonBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.SoftLavender
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import com.cascade.attentiontwin.ui.theme.WarmAmber
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FocusScreen(viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current
    val status by viewModel.status.collectAsState()
    val today by viewModel.todayCounts.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()

    val totalDurationSeconds = 25 * 60
    var secondsLeft by remember { mutableIntStateOf(totalDurationSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var taskIntention by remember { mutableStateOf("Deep Work & Cognition") }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var selectedSoundscape by remember { mutableStateOf("Cosmic Void") }

    val soundscapes = listOf("Cosmic Void", "Gentle Rain", "Forest Stream", "Off")

    // Sync timer state with backend focusActive
    LaunchedEffect(status.focusActive) {
        if (status.focusActive && !isTimerRunning) {
            isTimerRunning = true
        } else if (!status.focusActive && isTimerRunning) {
            isTimerRunning = false
        }
    }

    // Countdown timer ticker
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
            if (secondsLeft == 0) {
                isTimerRunning = false
                viewModel.toggleFocus() // end focus when timer expires
            }
        }
    }

    // Infinite breathing glow for sanctuary platform
    val infiniteTransition = rememberInfiniteTransition(label = "sanctuary_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sanctuary_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top Bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sanctuary",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (status.focusActive) "Focus environment active" else "Distraction-free workspace",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.focusActive) ElectricViolet else TextLo
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .border(1.dp, if (status.focusActive) MintGreen.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (status.focusActive) MintGreen else TextLo)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (status.focusActive) "Shielding ${settings.blockedPkgs.size} apps" else "Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.focusActive) MintGreen else TextLo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Current Task / Intention Card ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface1)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .clickable { showEditTaskDialog = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Focus Intention", style = MaterialTheme.typography.labelSmall, color = TextLo, fontSize = 10.sp)
                    Text(taskIntention, style = MaterialTheme.typography.bodyMedium, color = TextHi, fontWeight = FontWeight.SemiBold)
                }
            }

            Icon(Icons.Rounded.Edit, contentDescription = "Edit task", tint = NeonBlue, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.weight(0.3f))

        // ── HERO: 3D Sanctuary Floating Island & Countdown Timer ───────────
        val timerProgress = (secondsLeft.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeDisplay = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        Box(
            modifier = Modifier.size(270.dp),
            contentAlignment = Alignment.Center
        ) {
            // Floating Sanctuary Island Visual Elements (Canvas)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2f - 16.dp.toPx()

                // Ambient Sanctuary Halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricViolet.copy(alpha = 0.25f * pulse),
                            NeonBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = c,
                        radius = r * 1.15f
                    ),
                    radius = r * 1.15f,
                    center = c
                )

                // Circular Progress Background Arc
                drawCircle(
                    color = Surface2,
                    radius = r,
                    center = c,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active Gradient Progress Arc
                val sweepAngle = 360f * (1f - timerProgress)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ElectricViolet, NeonBlue, SoftLavender, ElectricViolet),
                        center = c
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Atmospheric Sanctuary Island platform
                drawOval(
                    color = ElectricViolet.copy(alpha = 0.15f),
                    topLeft = Offset(c.x - r * 0.45f, c.y + r * 0.42f),
                    size = Size(r * 0.90f, r * 0.24f)
                )

                // Floating island atmospheric stars
                for (i in 0 until 12) {
                    val angle = (i / 12f) * 2f * PI.toFloat()
                    val stR = r * 0.75f + (sin(angle * 3f) * 8.dp.toPx())
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(c.x + stR * cos(angle), c.y + stR * sin(angle))
                    )
                }
            }

            // Big Timer in Center
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isTimerRunning) "Session in Progress" else "Ready to Begin",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTimerRunning) MintGreen else TextLo,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.weight(0.4f))

        // ── Soundscapes Selector ───────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = SoftLavender, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ambient Soundscape", style = MaterialTheme.typography.labelSmall, color = TextLo)
                }
                Text(selectedSoundscape, style = MaterialTheme.typography.labelSmall, color = NeonBlue, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(soundscapes) { sound ->
                    val isSelected = selectedSoundscape == sound
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSoundscape = sound },
                        label = { Text(sound, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                            selectedLabelColor = SoftLavender,
                            containerColor = Surface2,
                            labelColor = TextLo
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ElectricViolet else CardBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Focus Controls (Play/Pause & Reset) ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Secondary Reset button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(1.dp, CardBorder, CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isTimerRunning = false
                        secondsLeft = totalDurationSeconds
                        if (status.focusActive) {
                            viewModel.toggleFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = "Reset", tint = ShowCoral, modifier = Modifier.size(24.dp))
            }

            // Primary Start / Pause Sanctuary Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!status.focusActive) {
                        viewModel.toggleFocus()
                        isTimerRunning = true
                    } else {
                        isTimerRunning = !isTimerRunning
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTimerRunning) Surface1 else ElectricViolet
                ),
                border = if (isTimerRunning) androidx.compose.foundation.BorderStroke(1.5.dp, ElectricViolet) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isTimerRunning) ElectricViolet else MidnightNavy,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isTimerRunning) "Pause Sanctuary" else "Enter Sanctuary",
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRunning) ElectricViolet else MidnightNavy,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }

    // Task Intention Edit Dialog
    if (showEditTaskDialog) {
        var tempTask by remember { mutableStateOf(taskIntention) }
        AlertDialog(
            onDismissRequest = { showEditTaskDialog = false },
            containerColor = Surface1,
            title = { Text("Set Focus Intention", color = TextHi) },
            text = {
                OutlinedTextField(
                    value = tempTask,
                    onValueChange = { tempTask = it },
                    label = { Text("What are you focusing on?", color = TextLo) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextHi,
                        unfocusedTextColor = TextHi
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        taskIntention = tempTask
                        showEditTaskDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) { Text("Save", color = MidnightNavy) }
            },
            dismissButton = {
                TextButton(onClick = { showEditTaskDialog = false }) { Text("Cancel", color = TextLo) }
            }
        )
    }
}

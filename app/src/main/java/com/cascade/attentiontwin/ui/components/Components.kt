package com.cascade.attentiontwin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.data.ReceiptView
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.ElectricViolet
import com.cascade.attentiontwin.ui.theme.MintGreen
import com.cascade.attentiontwin.ui.theme.NeonBlue
import com.cascade.attentiontwin.ui.theme.ProtectedBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.SoftLavender
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import com.cascade.attentiontwin.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderColor: Color = CardBorder,
    backgroundColor: Color = Surface1,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun DecisionChip(kind: String, modifier: Modifier = Modifier) {
    val upper = kind.uppercase(Locale.US)
    val (icon, color, text) = when {
        upper == "HOLD" || upper == "HELD" || upper == "BLOCKED_APP" -> Triple(Icons.Rounded.PauseCircle, MintGreen, "HELD")
        upper == "PROTECTED" -> Triple(Icons.Rounded.Shield, ProtectedBlue, "PROTECTED")
        upper == "LEARNING" || upper == "COLD_START" -> Triple(Icons.Rounded.School, WarmAmber, "LEARNING")
        else -> Triple(Icons.Rounded.Visibility, ShowCoral, "SHOWN")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun EvidenceBadge(level: String, modifier: Modifier = Modifier) {
    val upper = level.uppercase(Locale.US)
    val color = when (upper) {
        "PRIOR" -> WarmAmber
        "PILOT" -> NeonBlue
        "SIMULATED" -> Color(0xFF9E9E9E)
        "OFFLINE" -> ElectricViolet
        else -> TextLo
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.75.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = upper,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────
   GALAXY TWIN ORB: Dispersed Starfield when Idle -> Spiral Galaxy on Focus
   ──────────────────────────────────────────────────────────────────────── */

private data class StarParticle(
    val dispersedX: Float, // -1 to 1 relative
    val dispersedY: Float,
    val arm: Int,          // 0, 1, 2
    val spiralDist: Float, // 0.12 to 0.92
    val spiralTheta: Float,// angle in arm
    val baseSize: Float,
    val speed: Float,
    val colorIdx: Int
)

@Composable
fun TwinOrb(
    p: Float = 0.5f,
    density: Float = 0.5f,
    dots: Int = 8,
    reduceMotion: Boolean = false,
    focusActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Animate transition between Dispersed (0f) and Spiral Galaxy (1f)
    val focusTransition by animateFloatAsState(
        targetValue = if (focusActive) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "focus_transition"
    )

    // Infinite rotation for Galaxy
    val infiniteTransition = rememberInfiniteTransition(label = "galaxy_rot")
    val galaxyRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "galaxy_rot_val"
    )

    // Breathing pulse for core
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Deterministic particles generated once
    val particles = remember {
        val list = mutableListOf<StarParticle>()
        val rnd = java.util.Random(1337)
        for (i in 0 until 90) {
            // Dispersed coordinates within outer circle
            val rDisp = Math.sqrt(rnd.nextDouble()).toFloat() * 0.92f
            val aDisp = rnd.nextFloat() * 2f * PI.toFloat()
            val dx = rDisp * cos(aDisp)
            val dy = rDisp * sin(aDisp)

            // Spiral parameters: 3 logarithmic spiral arms
            val arm = i % 3
            val spiralDist = 0.12f + (i / 90f) * 0.82f
            val spiralTheta = spiralDist * 3.8f + (rnd.nextFloat() - 0.5f) * 0.35f

            val size = 1.6f + rnd.nextFloat() * 2.4f
            val speed = 0.8f + rnd.nextFloat() * 0.5f
            val colorIdx = rnd.nextInt(4)

            list.add(StarParticle(dx, dy, arm, spiralDist, spiralTheta, size, speed, colorIdx))
        }
        list
    }

    val palette = listOf(ElectricViolet, NeonBlue, SoftLavender, Color(0xFFF5F7FF))

    Canvas(modifier = modifier.size(260.dp)) {
        val c = center
        val maxR = size.minDimension / 2f
        val currentPulse = if (reduceMotion) 1f else pulse

        // 1. Ambient Cosmic Background Glow
        val ambientColor = lerp(ElectricViolet, NeonBlue, focusTransition)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ambientColor.copy(alpha = 0.28f * (0.6f + 0.4f * focusTransition)),
                    ElectricViolet.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = c,
                radius = maxR * 1.05f
            ),
            radius = maxR * 1.05f,
            center = c
        )

        // 2. Center Luminous Core (Grows and shines brighter in focus mode)
        val coreR = maxR * (0.28f + 0.12f * focusTransition) * currentPulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    SoftLavender.copy(alpha = 0.80f),
                    ElectricViolet.copy(alpha = 0.65f),
                    NeonBlue.copy(alpha = 0.35f * focusTransition),
                    Color.Transparent
                ),
                center = c,
                radius = coreR
            ),
            radius = coreR,
            center = c
        )

        // 3. Orbital Rings (3D perspective tilt)
        val ringTilt = 0.36f // ellipse y scale
        val ringAngle = galaxyRotation * 0.5f
        for (ringIdx in 0..1) {
            val ringRadius = maxR * (0.60f + ringIdx * 0.28f)
            val ringColor = if (ringIdx == 0) ElectricViolet.copy(alpha = 0.35f) else NeonBlue.copy(alpha = 0.25f)

            drawOval(
                color = ringColor,
                topLeft = Offset(c.x - ringRadius, c.y - ringRadius * ringTilt),
                size = Size(ringRadius * 2f, ringRadius * 2f * ringTilt),
                style = Stroke(width = 1.25.dp.toPx())
            )
        }

        // 4. Particles (Transitioning between Dispersed and Spiral Galaxy)
        particles.forEachIndexed { idx, p ->
            // Dispersed coordinates with subtle wandering
            val wander = if (reduceMotion) 0f else sin(galaxyRotation * p.speed + idx) * 4.dp.toPx()
            val dispX = c.x + p.dispersedX * maxR + wander
            val dispY = c.y + p.dispersedY * maxR + wander

            // Spiral Galaxy coordinates (Differential rotation: inner faster than outer)
            val armBase = p.arm * (2f * PI.toFloat() / 3f)
            val rotSpeed = (1.4f - p.spiralDist * 0.7f) // differential rotation
            val currentTheta = armBase + p.spiralTheta + (if (reduceMotion) 0f else galaxyRotation * rotSpeed)
            val spiralRadius = maxR * p.spiralDist * currentPulse
            val spiralX = c.x + spiralRadius * cos(currentTheta)
            val spiralY = c.y + spiralRadius * sin(currentTheta)

            // Interpolate position based on focus transition
            val px = dispX + (spiralX - dispX) * focusTransition
            val py = dispY + (spiralY - dispY) * focusTransition

            val partColor = palette[p.colorIdx]
            val alpha = (0.55f + 0.45f * sin(galaxyRotation * 2f + idx)).coerceIn(0.4f, 1f)

            // Draw glowing particle
            drawCircle(
                color = partColor.copy(alpha = alpha),
                radius = p.baseSize.dp.toPx() * (if (focusTransition > 0.5f) 1.2f else 1.0f),
                center = Offset(px, py)
            )

            // Bright core dot for white/lavender stars
            if (p.colorIdx == 3 || p.colorIdx == 2) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = (p.baseSize * 0.55f).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────
   CURVED SPLINE CHART (Matching the Dashboard Reference Image)
   ──────────────────────────────────────────────────────────────────────── */

@Composable
fun CurvedSplineChart(
    values: List<Float>,
    labels: List<String>,
    lineColor: Color = WarmAmber,
    activeDotIndex: Int = values.lastIndex,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return

    val maxVal = maxOf(values.maxOrNull() ?: 1f, 1f)
    val minVal = minOf(values.minOrNull() ?: 0f, 0f)
    val valRange = if (maxVal - minVal > 0) maxVal - minVal else 1f

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height - 24.dp.toPx() // leave space for baseline
            val stepX = w / (values.size - 1)

            val points = values.mapIndexed { i, v ->
                val normY = (v - minVal) / valRange
                val x = i * stepX
                val y = h - (normY * (h - 20.dp.toPx())) - 10.dp.toPx()
                Offset(x, y)
            }

            // Build smooth cubic spline path
            val path = Path()
            val fillPath = Path()

            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[maxOf(0, i - 1)]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points[minOf(points.size - 1, i + 2)]

                val cp1x = p1.x + (p2.x - p0.x) / 5f
                val cp1y = p1.y + (p2.y - p0.y) / 5f
                val cp2x = p2.x - (p3.x - p1.x) / 5f
                val cp2y = p2.y - (p3.y - p1.y) / 5f

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                fillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }

            // Close fill path
            fillPath.lineTo(points.last().x, size.height)
            fillPath.lineTo(points.first().x, size.height)
            fillPath.close()

            // Draw glowing gradient fill under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.04f), Color.Transparent)
                )
            )

            // Draw smooth curve line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data points & active highlight
            points.forEachIndexed { i, pt ->
                val isActive = i == activeDotIndex
                if (isActive) {
                    // Outer halo
                    drawCircle(
                        color = lineColor.copy(alpha = 0.30f),
                        radius = 8.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                } else {
                    drawCircle(
                        color = Surface1,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = lineColor.copy(alpha = 0.85f),
                        radius = 3.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // X-axis day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { i, label ->
                val isToday = i == activeDotIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isToday) TextHi else TextLo,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────
   CIRCULAR PROGRESS GAUGE (Matching the Reference Image Device Parameters)
   ──────────────────────────────────────────────────────────────────────── */

@Composable
fun CircularGauge(
    progress: Float,
    label: String,
    valueText: String,
    accentColor: Color = WarmAmber,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(52.dp)) {
            val strokeW = 5.dp.toPx()
            val pad = strokeW / 2f
            val arcSize = Size(size.width - strokeW, size.height - strokeW)

            // Background track arc (270 degrees)
            drawArc(
                color = Surface1,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Active progress arc
            drawArc(
                color = accentColor,
                startAngle = 135f,
                sweepAngle = (270f * progress.coerceIn(0f, 1f)),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHi,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextLo,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = ElectricViolet,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextLo,
                fontSize = 12.sp
            )
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextHi,
            fontSize = 22.sp
        )
    }
}

@Composable
fun RangeBar(
    point: Float,
    low: Float,
    high: Float,
    max: Float,
    modifier: Modifier = Modifier
) {
    val safeMax = maxOf(max, high, 0.1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val y = size.height / 2f
        // Track
        drawRoundRect(
            color = Surface2,
            topLeft = Offset(0f, y - 3.dp.toPx()),
            size = Size(size.width, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Range Band
        val xLow = (low / safeMax).coerceIn(0f, 1f) * size.width
        val xHigh = (high / safeMax).coerceIn(0f, 1f) * size.width
        val bandWidth = maxOf(xHigh - xLow, 4.dp.toPx())
        drawRoundRect(
            color = ElectricViolet.copy(alpha = 0.35f),
            topLeft = Offset(xLow, y - 6.dp.toPx()),
            size = Size(bandWidth, 12.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Point dot
        val xPoint = (point / safeMax).coerceIn(0f, 1f) * size.width
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(xPoint, y)
        )
        drawCircle(
            color = NeonBlue,
            radius = 4.dp.toPx(),
            center = Offset(xPoint, y)
        )
    }
}

@Composable
fun ReceiptCard(
    receipt: ReceiptView,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val appName = receipt.pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHi,
                        fontWeight = FontWeight.SemiBold
                    )
                    receipt.channelName?.let {
                        Text(
                            text = "• $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo
                        )
                    }
                    if (receipt.source == "sim") {
                        EvidenceBadge("SIMULATED")
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormat.format(Date(receipt.postTs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLo
                )
            }
            DecisionChip(kind = receipt.action)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextLo,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder)
                )

                Text(
                    text = receipt.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHi
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gate: ${receipt.gate}", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    Text("Mode: ${receipt.mode}", style = MaterialTheme.typography.labelSmall, color = TextLo)
                    Text("Enforcement: ${receipt.enforcement}", style = MaterialTheme.typography.labelSmall, color = TextLo)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "Cost: %.1f min (Show) vs %.1f min (Hold)", receipt.costShow, receipt.costHold),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLo
                    )
                    Text(
                        text = String.format(Locale.US, "Benefit: +%.1f min", receipt.benefit),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (receipt.benefit > 0) MintGreen else ShowCoral
                    )
                }

                if (receipt.pImpMean > 0 || receipt.pImpUp > 0) {
                    Text(
                        text = String.format(Locale.US, "Importance: %.2f mean (≤ %.2f optimistic)", receipt.pImpMean, receipt.pImpUp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLo
                    )
                }

                if (receipt.engaged != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Outcome: ${if (receipt.engaged) "Engaged" else "Ignored"}" +
                                    (if (receipt.clicked == true) " • Clicked" else "") +
                                    (if (receipt.dismissed == true) " • Dismissed" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (receipt.engaged) WarmAmber else TextLo
                        )
                        receipt.phoneMin10m?.let {
                            Text(
                                text = String.format(Locale.US, "%.1f phone min", it),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHi
                            )
                        }
                    }
                }
            }
        }
    }
}

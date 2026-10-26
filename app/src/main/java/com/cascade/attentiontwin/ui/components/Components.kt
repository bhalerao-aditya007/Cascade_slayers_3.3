package com.cascade.attentiontwin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.data.ReceiptView
import com.cascade.attentiontwin.ui.theme.Amber
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.GlassBgBottom
import com.cascade.attentiontwin.ui.theme.GlassBgTop
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.ProtectedBlue
import com.cascade.attentiontwin.ui.theme.ShowCoral
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(Surface1)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun DecisionChip(kind: String, modifier: Modifier = Modifier) {
    val upper = kind.uppercase(Locale.US)
    val (icon, color, text) = when {
        upper == "HOLD" || upper == "HELD" -> Triple(Icons.Rounded.PauseCircle, HoldGreen, "HELD")
        upper == "PROTECTED" -> Triple(Icons.Rounded.Shield, ProtectedBlue, "PROTECTED")
        upper == "LEARNING" || upper == "COLD_START" -> Triple(Icons.Rounded.School, Amber, "LEARNING")
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
        "PRIOR" -> Amber
        "PILOT" -> Cyan
        "SIMULATED" -> Color(0xFF9E9E9E)
        "OFFLINE" -> ProtectedBlue
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

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = Cyan,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextLo
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
            color = TextHi
        )
    }
}

@Composable
fun TwinOrb(
    p: Float,
    density: Float,
    dots: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val t by rememberInfiniteTransition(label = "orb").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "t"
    )
    val phase = if (reduceMotion) 0f else t

    Canvas(modifier = modifier.size(240.dp)) {
        val c = center
        val maxR = size.minDimension / 2f
        val col = lerp(Cyan, Amber, density.coerceIn(0f, 1f))
        val r = maxR * (0.40f + 0.30f * p.coerceIn(0f, 1f)) * (1f + 0.03f * sin(phase * 2f * PI.toFloat()))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(col.copy(alpha = 0.30f), Color.Transparent),
                center = c,
                radius = maxR
            ),
            radius = maxR,
            center = c
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(col.copy(alpha = 0.95f), col.copy(alpha = 0.25f)),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )

        repeat(dots.coerceAtMost(12)) { i ->
            val a = (i / 12f + phase) * 2f * PI.toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 4.dp.toPx(),
                center = Offset(
                    c.x + maxR * 0.88f * cos(a),
                    c.y + maxR * 0.88f * sin(a)
                )
            )
        }
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color = Cyan,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodySmall, color = TextLo)
        return
    }
    val maxVal = maxOf(values.maxOrNull() ?: 1f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val barCount = values.size
            val spaceWidth = size.width / barCount
            val barWidth = spaceWidth * 0.65f

            values.forEachIndexed { i, v ->
                val barHeight = (v / maxVal) * (size.height - 20.dp.toPx())
                val left = i * spaceWidth + (spaceWidth - barWidth) / 2f
                val top = size.height - barHeight

                drawRoundRect(
                    color = color.copy(alpha = if (v > 0) 0.85f else 0.15f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, maxOf(barHeight, 3.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Fill
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { i, label ->
                if (i % 3 == 0 || i == labels.size - 1) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextLo
                    )
                }
            }
        }
    }
}

@Composable
fun HBarList(
    items: List<Triple<String, String, Float>>,
    color: Color = Cyan,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(items.maxOfOrNull { it.third } ?: 1f, 1f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (title, subtitle, value) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$title ($subtitle)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextHi
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f min", value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHi
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    // background track
                    drawRoundRect(
                        color = Surface2,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // filled bar
                    val fillWidth = (value / maxVal).coerceIn(0f, 1f) * size.width
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = Size(fillWidth, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
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

        // 80% Range Band
        val xLow = (low / safeMax).coerceIn(0f, 1f) * size.width
        val xHigh = (high / safeMax).coerceIn(0f, 1f) * size.width
        val bandWidth = maxOf(xHigh - xLow, 4.dp.toPx())
        drawRoundRect(
            color = Cyan.copy(alpha = 0.35f),
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
            color = Cyan,
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
                        color = TextHi
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
                        color = if (receipt.benefit > 0) HoldGreen else ShowCoral
                    )
                }

                if (receipt.pImpMean > 0 || receipt.pImpUp > 0) {
                    Text(
                        text = String.format(Locale.US, "Importance: %.2f mean (≤ %.2f optimistic)", receipt.pImpMean, receipt.pImpUp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLo
                    )
                }

                // Outcome info
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
                            color = if (receipt.engaged) Amber else TextLo
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

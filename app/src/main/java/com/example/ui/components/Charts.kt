package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import kotlin.math.max
import kotlin.math.min

data class BarChartPoint(
    val label: String,
    val value: Float,
    val isHighlighted: Boolean = false,
)

data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color,
)

data class HeatmapDay(
    val epochDay: Long,
    val intensity: Float, // 0f..1f
    val label: String = "",
)

@Composable
fun CompletionRingCanvas(
    progress: Float,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    strokeWidth: Dp = 9.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ringProgress",
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = centerLabel,
            style = MonoMetricMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun WeeklyBarChart(
    points: List<BarChartPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 136.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = LifeOsThemeTokens.colors.accent,
) {
    if (points.isEmpty()) return
    val maxVal = max(1f, points.maxOf { it.value })
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gridColor = LifeOsThemeTokens.colors.border

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val count = points.size
            val slotWidth = size.width / count
            val barWidth = min(slotWidth * 0.52f, 28.dp.toPx())
            val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

            // Subtle baseline and midpoint guide
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height - 1.dp.toPx()),
                end = Offset(size.width, size.height - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )

            points.forEachIndexed { index, point ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 4.dp.toPx()),
                    size = Size(barWidth, size.height - 6.dp.toPx()),
                    cornerRadius = radius,
                )
                val normalized = (point.value / maxVal).coerceIn(0f, 1f)
                val barH = (size.height - 6.dp.toPx()) * normalized
                if (barH > 2f) {
                    drawRoundRect(
                        color = if (point.isHighlighted) highlightColor else barColor,
                        topLeft = Offset(x, size.height - 2.dp.toPx() - barH),
                        size = Size(barWidth, barH),
                        cornerRadius = radius,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (point.isHighlighted) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
fun TrendLineChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (values.isEmpty()) return
    val minVal = values.minOrNull() ?: 0f
    val maxVal = values.maxOrNull() ?: 1f
    val span = max(0.5f, maxVal - minVal)
    val gridColor = LifeOsThemeTokens.colors.border

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val padY = 12.dp.toPx()
            val padX = 10.dp.toPx()
            val usableW = size.width - padX * 2
            val usableH = size.height - padY * 2

            drawLine(
                color = gridColor,
                start = Offset(0f, size.height - padY),
                end = Offset(size.width, size.height - padY),
                strokeWidth = 1.dp.toPx(),
            )

            val coords = values.mapIndexed { index, v ->
                val x = if (values.size == 1) size.width / 2f else padX + (usableW * index / (values.size - 1))
                val norm = (v - minVal) / span
                val y = padY + usableH * (1f - norm)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        val prev = coords[i - 1]
                        val curr = coords[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(coords.last().x, size.height - padY)
                    lineTo(coords.first().x, size.height - padY)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                    ),
                )
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            coords.forEach { pt ->
                drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                labels.forEach { lbl ->
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    slices: List<DonutSlice>,
    centerValue: String,
    centerSubtitle: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 18.dp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
                if (total <= 0f) {
                    drawArc(
                        color = fallbackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokePx),
                    )
                } else {
                    var startAngle = -90f
                    slices.filter { it.value > 0f }.forEach { slice ->
                        val sweep = (slice.value / total) * 360f
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = max(2f, sweep - 2f),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerValue,
                    style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = centerSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            slices.take(5).forEach { slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(slice.color),
                    )
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (total > 0f) "${((slice.value / total) * 100).toInt()}%" else "0%",
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeatmapGrid(
    days: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    onDaySelected: ((HeatmapDay) -> Unit)? = null,
) {
    val palette = LifeOsThemeTokens.colors
    // Group into columns of 7 days (weeks)
    val weeks = days.chunked(7)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            weeks.forEach { week ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { day ->
                        val cellColor = when {
                            day.intensity <= 0f -> palette.heatmapEmpty
                            day.intensity < 0.34f -> palette.heatmapLevel1
                            day.intensity < 0.67f -> palette.heatmapLevel2
                            day.intensity < 0.99f -> palette.heatmapLevel3
                            else -> palette.heatmapLevel4
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cellColor)
                                .let {
                                    if (onDaySelected != null) it.clickable { onDaySelected(day) } else it
                                },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
            listOf(
                palette.heatmapEmpty,
                palette.heatmapLevel1,
                palette.heatmapLevel2,
                palette.heatmapLevel3,
                palette.heatmapLevel4,
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

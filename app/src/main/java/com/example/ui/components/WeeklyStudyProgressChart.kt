package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyStudyXp
import com.example.ui.theme.GoldXP
import com.example.ui.theme.SuccessSage

enum class ChartMode {
    CUMULATIVE_XP,
    DAILY_XP
}

@Composable
fun WeeklyStudyProgressChart(
    weeklyData: List<DailyStudyXp>,
    totalXp: Int,
    streakDays: Int,
    modifier: Modifier = Modifier,
    title: String = "Weekly Study Progress"
) {
    if (weeklyData.isEmpty()) return

    var chartMode by remember { mutableStateOf(ChartMode.CUMULATIVE_XP) }
    var selectedDayIndex by remember {
        mutableStateOf(weeklyData.indexOfLast { it.isToday }.coerceAtLeast(weeklyData.lastIndex))
    }

    // Animation progress for chart reveal
    var animationPlayed by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "chartAnim"
    )

    LaunchedEffect(chartMode) {
        animationPlayed = false
        animationPlayed = true
    }

    val selectedDay = weeklyData.getOrNull(selectedDayIndex) ?: weeklyData.last()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val values = if (chartMode == ChartMode.CUMULATIVE_XP) {
        weeklyData.map { it.cumulativeXp.toFloat() }
    } else {
        weeklyData.map { it.xpGained.toFloat() }
    }

    val maxVal = maxOf(50f, (values.maxOrNull() ?: 100f) * 1.25f)
    val totalWeeklyGain = weeklyData.sumOf { it.xpGained }
    val avgDailyGain = totalWeeklyGain / weeklyData.size.coerceAtLeast(1)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                outlineColor.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .testTag("weekly_study_progress_chart"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Streak Badge, Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timeline,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Visualizing XP momentum over past 7 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }

                // Streak pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = GoldXP,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streakDays Day Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Summary Bar (Total Weekly XP, Daily Average, Selected Day Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEK GAIN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = "+$totalWeeklyGain XP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldXP
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(outlineColor.copy(alpha = 0.3f))
                )

                Column {
                    Text(
                        text = "DAILY AVG",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = "~$avgDailyGain XP/day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(outlineColor.copy(alpha = 0.3f))
                )

                // Chart mode toggle pills (Cumulative vs Daily)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    ChartPill(
                        label = "Total",
                        isSelected = chartMode == ChartMode.CUMULATIVE_XP,
                        onClick = { chartMode = ChartMode.CUMULATIVE_XP }
                    )
                    ChartPill(
                        label = "Daily",
                        isSelected = chartMode == ChartMode.DAILY_XP,
                        onClick = { chartMode = ChartMode.DAILY_XP }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Day Callout
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = primaryColor.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (selectedDay.isToday) SuccessSage else primaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedDay.dayLabel} (${selectedDay.dateLabel}) ${if (selectedDay.isToday) "• Today" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (chartMode == ChartMode.CUMULATIVE_XP)
                            "Total: ${selectedDay.cumulativeXp} XP (${if (selectedDay.xpGained >= 0) "+" else ""}${selectedDay.xpGained} XP)"
                        else
                            "+${selectedDay.xpGained} XP earned",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ══════════════════════════════════════════════════════════════
            // STANDARD COMPOSE CANVAS STUDY PROGRESS CHART
            // ══════════════════════════════════════════════════════════════
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(weeklyData) {
                        detectTapGestures { offset ->
                            val count = weeklyData.size
                            if (count > 0) {
                                val leftPad = 36.dp.toPx()
                                val rightPad = 16.dp.toPx()
                                val chartWidth = size.width - leftPad - rightPad
                                val stepX = chartWidth / (count - 1).coerceAtLeast(1)
                                val relativeX = (offset.x - leftPad).coerceIn(0f, chartWidth)
                                val nearestIndex = (relativeX / stepX + 0.5f).toInt().coerceIn(0, count - 1)
                                selectedDayIndex = nearestIndex
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 38.dp.toPx()
                    val rightPadding = 16.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 32.dp.toPx()

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    // 1. Horizontal Reference Grid Lines (0%, 50%, 100%)
                    val gridSteps = 3
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    for (step in 0..gridSteps) {
                        val fraction = step.toFloat() / gridSteps.toFloat()
                        val y = topPadding + chartHeight * (1f - fraction)

                        drawLine(
                            color = outlineColor.copy(alpha = 0.18f),
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashPathEffect
                        )

                        // Y-Axis label (XP level)
                        val xpValueAtLine = (maxVal * fraction).toInt()
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                            drawText(
                                "${xpValueAtLine}",
                                leftPadding - 6.dp.toPx(),
                                y + 4.dp.toPx(),
                                paint
                            )
                        }
                    }

                    // 2. Compute Point Coordinates
                    val points = mutableListOf<Offset>()
                    val count = weeklyData.size
                    val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth

                    for (i in 0 until count) {
                        val x = leftPadding + i * stepX
                        val normalizedY = (values[i] / maxVal).coerceIn(0f, 1f)
                        val animatedY = normalizedY * animProgress
                        val y = topPadding + chartHeight * (1f - animatedY)
                        points.add(Offset(x, y))
                    }

                    if (chartMode == ChartMode.DAILY_XP) {
                        // In DAILY mode: Draw aesthetic rounded bar columns for each day
                        val barWidth = 18.dp.toPx()
                        for (i in 0 until count) {
                            val pt = points[i]
                            val isSel = (i == selectedDayIndex)
                            val barTop = pt.y
                            val barBottom = topPadding + chartHeight
                            val barHeight = (barBottom - barTop).coerceAtLeast(4.dp.toPx())

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        if (isSel) GoldXP else primaryColor,
                                        primaryColor.copy(alpha = if (isSel) 0.6f else 0.25f)
                                    ),
                                    startY = barTop,
                                    endY = barBottom
                                ),
                                topLeft = Offset(pt.x - barWidth / 2, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    } else {
                        // In CUMULATIVE mode: Draw smooth Bezier spline line & gradient area under curve
                        if (points.size >= 2) {
                            val linePath = Path()
                            val fillPath = Path()

                            linePath.moveTo(points[0].x, points[0].y)
                            fillPath.moveTo(points[0].x, topPadding + chartHeight)
                            fillPath.lineTo(points[0].x, points[0].y)

                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                                val controlY1 = p0.y
                                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                                val controlY2 = p1.y

                                linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }

                            fillPath.lineTo(points.last().x, topPadding + chartHeight)
                            fillPath.close()

                            // Draw shaded gradient underneath curve
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.32f),
                                        secondaryColor.copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    startY = topPadding,
                                    endY = topPadding + chartHeight
                                )
                            )

                            // Draw main glowing trendline
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // 3. Draw Data Point Dots & Day of Week Labels
                    for (i in 0 until count) {
                        val pt = points[i]
                        val isSelected = (i == selectedDayIndex)
                        val isToday = weeklyData[i].isToday

                        // Highlight vertical marker line for selected day
                        if (isSelected) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.4f),
                                start = Offset(pt.x, topPadding),
                                end = Offset(pt.x, topPadding + chartHeight),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // Data point circle
                        val outerRadius = if (isSelected) 7.dp.toPx() else 4.5.dp.toPx()
                        val innerRadius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx()

                        drawCircle(
                            color = if (isSelected) GoldXP else primaryColor,
                            radius = outerRadius,
                            center = pt
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = innerRadius,
                            center = pt
                        )

                        // 4. X-Axis Day Labels below the chart
                        val dayText = weeklyData[i].dayLabel
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (isToday) {
                                    android.graphics.Color.parseColor("#388E3C") // Emerald Green
                                } else if (isSelected) {
                                    android.graphics.Color.DKGRAY
                                } else {
                                    android.graphics.Color.GRAY
                                }
                                textSize = if (isSelected || isToday) 11.sp.toPx() else 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = isSelected || isToday
                                isAntiAlias = true
                            }
                            drawText(
                                dayText,
                                pt.x,
                                topPadding + chartHeight + 18.dp.toPx(),
                                paint
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day selector pill buttons along the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weeklyData.forEachIndexed { index, day ->
                    val isSelected = (index == selectedDayIndex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.15f)
                                else if (day.isToday) SuccessSage.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { selectedDayIndex = index }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (day.isToday) "• Today" else day.dayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else if (day.isToday) SuccessSage else onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

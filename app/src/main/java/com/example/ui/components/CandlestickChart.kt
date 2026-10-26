package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.Timeframe
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.max

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedIndex by remember(candles) { mutableStateOf<Int?>(null) }
    val activeCandle = inspectedIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianBlack, RoundedCornerShape(14.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // OHLC Telemetry Header
        if (activeCandle != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "O: $${formatCompact(activeCandle.open)}  H: $${formatCompact(activeCandle.high)}  L: $${formatCompact(activeCandle.low)}  C: $${formatCompact(activeCandle.close)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Vol: $${formatCompact(activeCandle.volume)}",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Canvas OHLC + Volume + Moving Average
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            if (candles.isEmpty()) {
                Text(
                    text = "Gathering simulated market ticks...",
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(candles) {
                            detectTapGestures { offset ->
                                if (size.width > 0 && candles.isNotEmpty()) {
                                    val idx = ((offset.x / size.width) * candles.size)
                                        .toInt()
                                        .coerceIn(0, candles.lastIndex)
                                    inspectedIndex = idx
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val priceChartHeight = height * 0.76f
                    val volumeTop = height * 0.80f
                    val volumeHeight = height * 0.18f

                    val maxHigh = candles.maxOf { it.high }
                    val minLow = candles.minOf { it.low }
                    val priceRange = max(0.0001, maxHigh - minLow)
                    val maxVol = max(1.0, candles.maxOf { it.volume })

                    // Horizontal grid lines
                    for (i in 0..3) {
                        val y = (priceChartHeight / 3f) * i
                        drawLine(
                            color = GlassBorder.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    val count = candles.size
                    val slotWidth = width / count.toFloat()
                    val bodyWidth = (slotWidth * 0.65f).coerceAtLeast(3f)

                    fun priceToY(price: Double): Float {
                        val normalized = ((price - minLow) / priceRange).toFloat().coerceIn(0f, 1f)
                        return priceChartHeight - (normalized * (priceChartHeight - 12f)) - 6f
                    }

                    val maPath = Path()
                    var maStarted = false

                    candles.forEachIndexed { index, candle ->
                        val centerX = index * slotWidth + slotWidth / 2f
                        val highY = priceToY(candle.high)
                        val lowY = priceToY(candle.low)
                        val openY = priceToY(candle.open)
                        val closeY = priceToY(candle.close)

                        val isBull = candle.close >= candle.open
                        val color = if (isBull) BullGreen else BearRed

                        // Wick
                        drawLine(
                            color = color,
                            start = Offset(centerX, highY),
                            end = Offset(centerX, lowY),
                            strokeWidth = 2f
                        )

                        // Candle Body
                        val topY = minOf(openY, closeY)
                        val rawBodyH = abs(closeY - openY).coerceAtLeast(3f)
                        drawRect(
                            color = color,
                            topLeft = Offset(centerX - bodyWidth / 2f, topY),
                            size = Size(bodyWidth, rawBodyH)
                        )

                        // Volume bar
                        val volBarH = ((candle.volume / maxVol).toFloat() * volumeHeight).coerceAtLeast(2f)
                        drawRect(
                            color = color.copy(alpha = 0.35f),
                            topLeft = Offset(centerX - bodyWidth / 2f, volumeTop + (volumeHeight - volBarH)),
                            size = Size(bodyWidth, volBarH)
                        )

                        // 5-period Simple Moving Average
                        val startIdx = max(0, index - 4)
                        val sub = candles.subList(startIdx, index + 1)
                        val avgClose = sub.sumOf { it.close } / sub.size
                        val maY = priceToY(avgClose)
                        if (!maStarted) {
                            maPath.moveTo(centerX, maY)
                            maStarted = true
                        } else {
                            maPath.lineTo(centerX, maY)
                        }
                    }

                    drawPath(
                        path = maPath,
                        color = MetallicGold.copy(alpha = 0.85f),
                        style = Stroke(width = 2.2f)
                    )

                    // Crosshair if user tapped a candle
                    inspectedIndex?.let { idx ->
                        if (idx in candles.indices) {
                            val cx = idx * slotWidth + slotWidth / 2f
                            drawLine(
                                color = CyberCyan.copy(alpha = 0.7f),
                                start = Offset(cx, 0f),
                                end = Offset(cx, height),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selectable Timeframes: 1m, 5m, 15m, 1h, 4h, 1d, 1w, 1mo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Timeframe.entries.forEach { tf ->
                val selected = tf == selectedTimeframe
                FilterChip(
                    selected = selected,
                    onClick = { onTimeframeSelected(tf) },
                    label = {
                        Text(
                            text = tf.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ElevatedPanel,
                        labelColor = TextSecondary,
                        selectedContainerColor = MetallicGold,
                        selectedLabelColor = ObsidianBlack
                    )
                )
            }
        }
    }
}

private fun formatCompact(value: Double): String {
    return when {
        value >= 1_000_000_000.0 -> String.format("%.2fB", value / 1_000_000_000.0)
        value >= 1_000_000.0 -> String.format("%.2fM", value / 1_000_000.0)
        value >= 10_000.0 -> String.format("%,.0f", value)
        else -> String.format("%,.2f", value)
    }
}

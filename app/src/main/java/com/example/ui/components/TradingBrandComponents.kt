package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.sin

val EmeraldAccent = Color(0xFF10E584)
val DarkNavyBg = Color(0xFF070D17)
val DarkCardBg = Color(0xFF101926)
val DarkCardElevated = Color(0xFF162233)
val SubtleCardBorder = Color(0xFF21324A)

@Composable
fun LifeAssetsLogoMark(
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Left vertical bar of 'N'
        val strokeW = w * 0.16f
        val greenBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF00C853), Color(0xFF10E584), Color(0xFF69F0AE)),
            start = Offset(0f, h),
            end = Offset(w, 0f)
        )

        val nPath = Path().apply {
            moveTo(w * 0.18f, h * 0.85f)
            lineTo(w * 0.18f, h * 0.32f)
            lineTo(w * 0.54f, h * 0.72f)
            lineTo(w * 0.84f, h * 0.16f)
        }
        drawPath(
            path = nPath,
            brush = greenBrush,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Upward arrowhead at top right
        val arrowHead = Path().apply {
            moveTo(w * 0.62f, h * 0.14f)
            lineTo(w * 0.88f, h * 0.12f)
            lineTo(w * 0.89f, h * 0.38f)
            close()
        }
        drawPath(path = arrowHead, brush = greenBrush)
    }
}

@Composable
fun LifeAssetsBrandHeader(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (compact) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LifeAssetsLogoMark(size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Life Assets",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 17.sp
                )
                Text(
                    text = "Simulator",
                    color = EmeraldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LifeAssetsLogoMark(size = 68.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Life Assets",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Simulator",
                color = EmeraldAccent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun shortAssetDisplayName(asset: Asset): String {
    return when (asset.id) {
        "crypto_01" -> "Bitcoin"
        "crypto_02" -> "Ethereum"
        "crypto_03" -> "Solana"
        "crypto_04" -> "BNB"
        "crypto_05" -> "XRP"
        "crypto_06" -> "Cardano"
        "crypto_07" -> "Dogecoin"
        "crypto_08" -> "Avalanche"
        "crypto_09" -> "Chainlink"
        "crypto_10" -> "Polkadot"
        "stk_01" -> "Apple"
        "stk_02" -> "Microsoft"
        "stk_03" -> "NVIDIA"
        "stk_04" -> "Amazon"
        "stk_05" -> "Google"
        "stk_06" -> "Meta"
        "stk_07" -> "Tesla"
        "stk_08" -> "Berkshire"
        "stk_09" -> "TSMC"
        "stk_10" -> "Broadcom"
        "met_01" -> "Gold"
        "met_02" -> "Silver"
        "met_03" -> "Platinum"
        "met_04" -> "Palladium"
        "met_05" -> "Copper"
        "met_06" -> "Crude Oil"
        else -> asset.name.substringBefore(" (").substringBefore(" —")
    }
}

@Composable
fun AssetBrandBadge(
    asset: Asset,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    AssetThumbnailPicture(
        asset = asset,
        size = size,
        modifier = modifier.clip(RoundedCornerShape(10.dp))
    )
}

@Composable
fun MiniSparklineChart(
    assetId: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
        .width(74.dp)
        .height(28.dp)
) {
    val points = remember(assetId, isPositive) {
        val seed = abs(assetId.hashCode())
        val count = 12
        List(count) { i ->
            val progress = i.toFloat() / (count - 1)
            val trend = if (isPositive) progress * 0.65f else (1f - progress) * 0.65f
            val wave = (sin((i + (seed % 7)) * 1.15).toFloat() * 0.18f)
            (0.18f + trend + wave).coerceIn(0.08f, 0.94f)
        }
    }

    val lineColor = if (isPositive) EmeraldAccent else BearRed

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f || points.size < 2) return@Canvas

        val path = Path()
        val stepX = w / (points.size - 1)

        points.forEachIndexed { idx, normY ->
            val x = idx * stepX
            val y = h - (normY * h)
            if (idx == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (idx - 1) * stepX
                val prevY = h - (points[idx - 1] * h)
                val ctrlX = (prevX + x) / 2f
                path.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

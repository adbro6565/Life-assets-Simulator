package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.UserEntity
import com.example.data.model.Asset
import com.example.data.model.MarketRegime
import com.example.data.model.MarketTick
import com.example.engine.DailyRewardStatus
import com.example.ui.components.AssetBrandBadge
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkCardElevated
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.LifeAssetsBrandHeader
import com.example.ui.components.MiniSparklineChart
import com.example.ui.components.SubtleCardBorder
import com.example.ui.components.shortAssetDisplayName
import com.example.ui.theme.BearRed
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PortfolioMetrics

@Composable
fun DashboardScreen(
    user: UserEntity?,
    metrics: PortfolioMetrics,
    ticks: Map<String, MarketTick>,
    regime: MarketRegime,
    headline: String,
    dailyRewardStatus: DailyRewardStatus,
    lifeBotBriefing: String,
    getOwnedQuantity: (String) -> Double,
    onClaimDailyReward: () -> Unit,
    onOpenAuthModal: () -> Unit,
    onOpenLifeBot: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenTradeTerminal: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenRewards: () -> Unit = onClaimDailyReward,
    onAssetCardClick: (Asset) -> Unit,
    onBuyClick: (Asset) -> Unit,
    onSellClick: (Asset) -> Unit
) {
    // Global Market Overview cards matching Screen 3: Bitcoin, Gold, Apple, plus Silver, Crude Oil, NVIDIA, Tesla
    val globalOverviewAssets = remember {
        listOf(
            "crypto_01", // Bitcoin (BTC)
            "met_01",    // Gold (XAU)
            "stk_01",    // Apple (AAPL)
            "met_02",    // Silver (XAG)
            "stk_03",    // NVIDIA (NVDA)
            "met_06"     // Crude Oil (USO-WTI)
        ).mapNotNull { AssetCatalogue.getAssetById(it) }
    }

    // Trending Assets list matching Screen 3: Bitcoin, Ethereum, Apple, Microsoft, Gold, Silver, Tesla, NVIDIA, Amazon, Google, Crude Oil
    val trendingAssets = remember {
        listOf(
            "crypto_01", // Bitcoin (BTC)
            "crypto_02", // Ethereum (ETH)
            "stk_01",    // Apple (AAPL)
            "stk_02",    // Microsoft (MSFT)
            "met_01",    // Gold (XAU)
            "met_02",    // Silver (XAG)
            "stk_07",    // Tesla (TSLA)
            "stk_03",    // NVIDIA (NVDA)
            "stk_04",    // Amazon (AMZN)
            "stk_05",    // Google (GOOGL)
            "met_06",    // Crude Oil (USO-WTI)
            "re_01",     // Real Estate
            "car_01"     // Ferrari SF90
        ).mapNotNull { AssetCatalogue.getAssetById(it) }
    }

    val userInitials = remember(user?.username) {
        val raw = user?.username?.takeIf { it.isNotBlank() } ?: "Trader"
        val clean = raw.replace("_", " ")
        val parts = clean.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.size == 1 && parts[0].length == 1 -> "${parts[0].first().uppercaseChar()}T"
            else -> "LA"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Bar: Life Assets Simulator Logo + Bell Icon + User Initials Avatar ("AS")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LifeAssetsBrandHeader(compact = true)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onOpenLifeBot,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Market Alerts & AI Briefing",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                )
                            )
                            .border(1.dp, Color(0xFF60A5FA), CircleShape)
                            .clickable(onClick = onOpenAuthModal)
                            .testTag("profile_auth_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 2. Total Portfolio Value Hero Card (matching Screen 3)
        item {
            val changePct = if (metrics.unrealizedPnLPercent != 0.0) {
                metrics.unrealizedPnLPercent
            } else {
                2.45
            }
            val isPositive = changePct >= 0.0

            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(EmeraldAccent.copy(alpha = 0.45f), SubtleCardBorder)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(onClick = onOpenPortfolio)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative background chart glow on right side
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                    ) {
                        val w = size.width
                        val h = size.height
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    EmeraldAccent.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.82f, h * 0.45f),
                                radius = w * 0.38f
                            )
                        )
                        val bgCurve = Path().apply {
                            moveTo(w * 0.55f, h * 0.85f)
                            cubicTo(w * 0.68f, h * 0.72f, w * 0.76f, h * 0.35f, w * 0.94f, h * 0.22f)
                        }
                        drawPath(
                            path = bgCurve,
                            color = EmeraldAccent.copy(alpha = 0.22f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Total Asset Portfolio Value",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$${String.format("%,.2f", metrics.investedMarketValue)}",
                            color = TextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("dashboard_net_worth_text")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Combined Net Worth (Assets + $${String.format("%,.0f", metrics.cashBalance)} Cash): $${String.format("%,.2f", metrics.totalNetWorth)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) EmeraldAccent else BearRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePct)}% (Today) • Real Ref: $${String.format("%,.0f", metrics.realWorldReferencePortfolioValue)}",
                                color = if (isPositive) EmeraldAccent else BearRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Side-by-Side Cards: Virtual Cash & Daily Reward (matching Screen 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Virtual Cash Card
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenPortfolio)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(EmeraldAccent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Virtual Cash",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Virtual Cash",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$${String.format("%,.2f", metrics.cashBalance)}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Available to trade",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Daily Reward Card
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            if (dailyRewardStatus.isAvailableToday) {
                                onClaimDailyReward()
                            } else {
                                onOpenRewards()
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MetallicGold.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "Daily Reward",
                                    tint = BrightGold,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Reward",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$${String.format("%,.0f", dailyRewardStatus.nextRewardAmount)}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (dailyRewardStatus.isAvailableToday) {
                                "Tap to Claim Now"
                            } else {
                                "Claim in ${dailyRewardStatus.countdownFormatted}"
                            },
                            color = if (dailyRewardStatus.isAvailableToday) EmeraldAccent else MetallicGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4. Quick Action Buttons Row: Deposit, Trade, Rewards, More (matching Screen 3)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DashboardQuickActionItem(
                    label = "Deposit",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = EmeraldAccent,
                    onClick = onClaimDailyReward
                )
                DashboardQuickActionItem(
                    label = "Trade",
                    icon = Icons.Default.ShowChart,
                    iconTint = Color(0xFF38BDF8),
                    onClick = onOpenTradeTerminal
                )
                DashboardQuickActionItem(
                    label = "Rewards",
                    icon = Icons.Default.CardGiftcard,
                    iconTint = BrightGold,
                    onClick = onOpenRewards
                )
                DashboardQuickActionItem(
                    label = "More",
                    icon = Icons.Default.MoreHoriz,
                    iconTint = TextSecondary,
                    onClick = onOpenMarketplace
                )
            }
        }

        // 5. Global Market Overview Section (Bitcoin, Gold, Apple cards matching Screen 3)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Global Market Overview",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Live Prices",
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(globalOverviewAssets, key = { it.id }) { asset ->
                        val tick = ticks[asset.id]
                        val price = tick?.currentPrice ?: asset.basePrice
                        val changePct = tick?.changePercent24h ?: 1.25
                        val isUp = changePct >= 0.0

                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(114.dp)
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                                .clickable { onAssetCardClick(asset) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                AssetBrandBadge(asset = asset, size = 32.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = shortAssetDisplayName(asset),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$${String.format("%,.2f", price)}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format("%+.2f%%", changePct),
                                    color = if (isUp) EmeraldAccent else BearRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Trending Assets Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Assets",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "View All (${AssetCatalogue.totalCount}) →",
                    color = EmeraldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpenTradeTerminal)
                )
            }
        }

        // 7. Trending Assets Rows with Brand Badge, Sparkline, and Live Price (matching Screen 3)
        items(trendingAssets, key = { it.id }) { asset ->
            val tick = ticks[asset.id]
            val price = tick?.currentPrice ?: asset.basePrice
            val changePct = tick?.changePercent24h ?: 1.85
            val isUp = changePct >= 0.0

            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                    .clickable { onAssetCardClick(asset) }
                    .testTag("dashboard_trending_${asset.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        AssetBrandBadge(asset = asset, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = shortAssetDisplayName(asset),
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = asset.symbol,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    MiniSparklineChart(
                        assetId = asset.id,
                        isPositive = isUp,
                        modifier = Modifier
                            .width(72.dp)
                            .height(26.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$${String.format("%,.2f", price)}",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = String.format("%+.2f%%", changePct),
                            color = if (isUp) EmeraldAccent else BearRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardQuickActionItem(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Surface(
            color = DarkCardElevated,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .size(50.dp)
                .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

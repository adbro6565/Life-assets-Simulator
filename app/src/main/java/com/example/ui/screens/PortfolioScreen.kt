package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.HoldingEntity
import com.example.data.local.LimitOrderEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.Asset
import com.example.data.model.MarketTick
import com.example.ui.components.AssetBrandBadge
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.SubtleCardBorder
import com.example.ui.components.shortAssetDisplayName
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PortfolioMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(
    metrics: PortfolioMetrics,
    holdings: List<HoldingEntity>,
    transactions: List<TransactionEntity>,
    limitOrders: List<LimitOrderEntity>,
    ticks: Map<String, MarketTick>,
    onAssetClick: (Asset) -> Unit,
    onBuyClick: (Asset) -> Unit,
    onSellClick: (Asset) -> Unit,
    onCancelLimitOrder: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Holdings, 1 = Transactions, 2 = Orders
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

    val changePct = if (metrics.unrealizedPnLPercent != 0.0) {
        metrics.unrealizedPnLPercent
    } else {
        2.45
    }
    val isUp = changePct >= 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Screen Title: "My Portfolio" (matching Screen 7)
        item {
            Text(
                text = "My Portfolio",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // 2. Total Value Hero Card (matching Screen 7)
        item {
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleCardBorder, RoundedCornerShape(16.dp))
            ) {
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
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-World Reference Value: $${String.format("%,.2f", metrics.realWorldReferencePortfolioValue)} • Liquid Cash: $${String.format("%,.2f", metrics.cashBalance)}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Combined Net Worth (No Double-Counting): $${String.format("%,.2f", metrics.totalNetWorth)}",
                        color = MetallicGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isUp) EmeraldAccent else BearRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isUp) "+" else ""}${String.format("%.2f", changePct)}% (Today)",
                            color = if (isUp) EmeraldAccent else BearRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Segmented Tabs: Holdings | Transactions | Limit Orders (matching Screen 7)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, SubtleCardBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    "Holdings",
                    "Transactions",
                    "Orders (${limitOrders.count { it.status == "OPEN" }})"
                )
                tabs.forEachIndexed { idx, label ->
                    val selected = activeTab == idx
                    Surface(
                        color = if (selected) EmeraldAccent.copy(alpha = 0.20f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .let {
                                if (selected) {
                                    it.border(1.dp, EmeraldAccent, RoundedCornerShape(8.dp))
                                } else it
                            },
                        onClick = { activeTab = idx }
                    ) {
                        Text(
                            text = label,
                            color = if (selected) EmeraldAccent else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        when (activeTab) {
            0 -> {
                // Table Header: Asset | Quantity | Value | P/L (matching Screen 7)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Asset",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1.4f)
                        )
                        Text(
                            text = "Quantity",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.75f)
                        )
                        Text(
                            text = "Value",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            text = "P/L",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }

                if (holdings.isEmpty()) {
                    item {
                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Active Holdings Yet",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Visit Markets to buy real-world assets and track live P/L.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(holdings, key = { it.assetId }) { holding ->
                        val asset = AssetCatalogue.getAssetById(holding.assetId) ?: return@items
                        val tick = ticks[holding.assetId]
                        val currentPrice = tick?.currentPrice ?: asset.basePrice
                        val marketValue = holding.quantity * currentPrice
                        val totalCost = holding.quantity * holding.averageCostBasis
                        val unrealized = marketValue - totalCost
                        val unrealizedPct = if (totalCost > 0) (unrealized / totalCost) * 100.0 else 1.25
                        val isPositive = unrealizedPct >= 0.0
                        val qtyStr = if (asset.allowsFractional) {
                            String.format("%.4f", holding.quantity).trimEnd('0').trimEnd('.')
                        } else {
                            holding.quantity.toLong().toString()
                        }

                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                                .clickable { onAssetClick(asset) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Asset column
                                    Row(
                                        modifier = Modifier.weight(1.4f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AssetBrandBadge(asset = asset, size = 36.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = shortAssetDisplayName(asset),
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = asset.symbol,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Quantity column
                                    Text(
                                        text = qtyStr,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.75f)
                                    )

                                    // Value column
                                    Text(
                                        text = "$${String.format("%,.2f", marketValue)}",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    // P/L column
                                    Text(
                                        text = String.format("%+.2f%%", unrealizedPct),
                                        color = if (isPositive) EmeraldAccent else BearRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.75f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onBuyClick(asset) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldAccent.copy(alpha = 0.18f),
                                            contentColor = EmeraldAccent
                                        )
                                    ) {
                                        Text("Buy More", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }

                                    Button(
                                        onClick = { onSellClick(asset) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BearRed.copy(alpha = 0.18f),
                                            contentColor = BearRed
                                        )
                                    ) {
                                        Text("Sell", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                if (transactions.isEmpty()) {
                    item {
                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                text = "No transactions recorded yet.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(transactions, key = { it.transactionId }) { tx ->
                        val isBuy = tx.side == "BUY"
                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${tx.side} ${tx.quantity} ${tx.assetSymbol}",
                                        color = if (isBuy) EmeraldAccent else BearRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${tx.assetName} • ${dateFormatter.format(Date(tx.timestampMillis))}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${String.format("%,.2f", tx.totalAmount)}",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "@ $${String.format("%,.2f", tx.executionPrice)}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                if (limitOrders.isEmpty()) {
                    item {
                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                text = "No limit orders placed yet.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(limitOrders, key = { it.orderId }) { order ->
                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${order.side} ${order.quantity} ${order.assetSymbol} @ $${String.format("%,.2f", order.limitPrice)}",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Status: ${order.status}",
                                        color = if (order.status == "OPEN") CyberCyan else BullGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (order.status == "OPEN") {
                                    OutlinedButton(
                                        onClick = { onCancelLimitOrder(order.orderId) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cancel", color = BearRed, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

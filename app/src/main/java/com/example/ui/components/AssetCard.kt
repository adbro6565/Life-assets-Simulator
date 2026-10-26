package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.MarketTick
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AssetCard(
    asset: Asset,
    tick: MarketTick?,
    ownedQuantity: Double,
    onCardClick: () -> Unit,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPrice = tick?.currentPrice ?: asset.basePrice
    val changePct = tick?.changePercent24h ?: 0.0
    val isUp = changePct >= 0.0

    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onCardClick)
            .testTag("asset_card_${asset.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AssetThumbnailPicture(
                        asset = asset,
                        size = 52.dp,
                        imageIndex = 0
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AssetThumbnailPicture(
                        asset = asset,
                        size = 42.dp,
                        imageIndex = 1
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.symbol,
                                color = MetallicGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${asset.subcategory}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = asset.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$${String.format("%,.2f", currentPrice)}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("%+.2f%%", changePct),
                            color = if (isUp) BullGreen else BearRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = asset.priceSourceLabel,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                if (ownedQuantity > 0.0) {
                    Surface(
                        color = CyberCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        val qtyStr = if (asset.allowsFractional) {
                            String.format("%.3f", ownedQuantity).trimEnd('0').trimEnd('.')
                        } else {
                            ownedQuantity.toLong().toString()
                        }
                        Text(
                            text = "Owned: $qtyStr",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Non-instant Buy & Sell Triggers -> Opens QuantitySelector BottomSheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("buy_button_${asset.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BullGreen,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Text("BUY (SELECT QTY)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSellClick,
                    enabled = ownedQuantity > 0.0,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("sell_button_${asset.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (ownedQuantity > 0.0) BearRed else TextMuted
                    )
                ) {
                    Text("SELL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

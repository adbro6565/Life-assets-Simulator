package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.HoldingEntity
import com.example.data.local.TradeOfferEntity
import com.example.data.model.BillionairePlayer
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
fun MultiplayerScreen(
    leaderboard: List<BillionairePlayer>,
    holdings: List<HoldingEntity>,
    tradeOffers: List<TradeOfferEntity>,
    onCreateOffer: (recipientUsername: String, assetId: String, quantity: Double, requestedCash: Double) -> Unit,
    onAcceptOffer: (String) -> Unit,
    onDeclineOffer: (String) -> Unit
) {
    var recipientInput by remember { mutableStateOf("Satoshi_Phantom") }
    var selectedAssetId by remember(holdings) {
        mutableStateOf(holdings.firstOrNull()?.assetId ?: "watch_02")
    }
    var qtyInput by remember { mutableStateOf("1") }
    var requestedCashInput by remember { mutableStateOf("45000") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Global Billionaire Leaderboard Header
        item {
            Text(
                text = "GLOBAL BILLIONAIRE WEALTH ARENA & P2P ESCROW",
                color = MetallicGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Sovereign Syndicate Leaderboard",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        itemsIndexed(leaderboard, key = { _, p -> p.playerId }) { index, player ->
            Surface(
                color = if (player.isCurrentUser) ElevatedPanel else CarbonSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (player.isCurrentUser) 1.5.dp else 1.dp,
                        color = if (player.isCurrentUser) MetallicGold else GlassBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            color = if (index == 0) MetallicGold else CyberCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "@${player.username}" + if (player.isCurrentUser) " (YOU)" else "",
                                color = if (player.isCurrentUser) MetallicGold else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${player.title} • Core: ${player.topAssetName}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%,.0f", player.netWorth)}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = String.format("%+.1f%% (24h)", player.change24hPercent),
                            color = if (player.change24hPercent >= 0) BullGreen else BearRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Active P2P Trade Offers (Escrow Settlement)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "P2P ESCROW TRADE OFFERS (${tradeOffers.size})",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        items(tradeOffers, key = { it.offerId }) { offer ->
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "From @${offer.senderUsername} → @${offer.recipientUsername}",
                            color = MetallicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = offer.status,
                            color = when (offer.status) {
                                "ACCEPTED" -> BullGreen
                                "DECLINED" -> BearRed
                                else -> CyberCyan
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${offer.offeredQuantity} × ${offer.offeredAssetName}",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Escrow Settlement Price: $${String.format("%,.2f", offer.requestedCash)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    if (offer.status == "PENDING") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onAcceptOffer(offer.offerId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("accept_offer_${offer.offerId}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BullGreen,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text("ACCEPT & SETTLE", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { onDeclineOffer(offer.offerId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("DECLINE", color = BearRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Create Outgoing P2P Trade Offer Card
        item {
            Surface(
                color = ElevatedPanel,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MetallicGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CREATE P2P OTC ESCROW OFFER",
                        color = MetallicGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text("Counterparty Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetallicGold,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val currentSelectedAsset = AssetCatalogue.getAssetById(selectedAssetId)
                    Text(
                        text = "Selected Holding: ${currentSelectedAsset?.name ?: selectedAssetId}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        holdings.take(3).forEach { h ->
                            val a = AssetCatalogue.getAssetById(h.assetId) ?: return@forEach
                            val isSel = selectedAssetId == h.assetId
                            Surface(
                                color = if (isSel) MetallicGold else CarbonSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedAssetId = h.assetId }
                            ) {
                                Text(
                                    text = "${a.symbol} (${h.quantity.toInt()})",
                                    color = if (isSel) ObsidianBlack else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = requestedCashInput,
                            onValueChange = { requestedCashInput = it },
                            label = { Text("Ask Cash (USD)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val q = qtyInput.toDoubleOrNull() ?: 1.0
                            val cash = requestedCashInput.toDoubleOrNull() ?: 10000.0
                            onCreateOffer(recipientInput, selectedAssetId, q, cash)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("send_p2p_offer_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MetallicGold,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("LOCK INVENTORY IN ESCROW & SEND OFFER", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

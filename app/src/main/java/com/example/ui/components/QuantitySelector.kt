package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.OrderSide
import com.example.data.model.OrderType
import com.example.domain.trading.OrderIntent
import com.example.engine.TradingService
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
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuantitySelector(
    asset: Asset,
    initialSide: OrderSide = OrderSide.BUY,
    referencePrice: Double,
    priceChangePercent24h: Double = 0.0,
    minQuantity: Double = asset.minQuantity,
    maxQuantity: Double = asset.maxOrderQuantity,
    step: Double = if (asset.allowsFractional) 0.1 else 1.0,
    tradingFeeRate: Double = TradingService.DEFAULT_TRADING_FEE_RATE,
    fixedFee: Double = 0.0,
    currency: String = "USD",
    availableCash: Double,
    ownedQuantity: Double,
    averageCostBasis: Double = 0.0,
    isSubmitting: Boolean = false,
    isOnline: Boolean = true,
    quoteFreshnessStatus: com.example.data.model.QuoteFreshnessStatus = com.example.data.model.QuoteFreshnessStatus.FRESH,
    onConfirm: (OrderIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var side by remember(initialSide) { mutableStateOf(initialSide) }
    var orderType by remember { mutableStateOf(OrderType.MARKET) }
    var quantityText by remember(asset.id, side) {
        val initialVal = if (side == OrderSide.SELL && ownedQuantity > 0.0) {
            val q = if (ownedQuantity >= 1.0) 1.0 else ownedQuantity
            if (asset.allowsFractional && q % 1.0 != 0.0) {
                String.format(Locale.US, "%.4f", q).trimEnd('0').trimEnd('.')
            } else {
                q.toLong().toString()
            }
        } else "1"
        mutableStateOf(initialVal)
    }
    var limitPriceText by remember(asset.id) {
        mutableStateOf(String.format(Locale.US, "%.2f", referencePrice))
    }

    val parsedQty = quantityText.toDoubleOrNull() ?: 0.0
    val parsedLimitPrice = limitPriceText.toDoubleOrNull() ?: referencePrice
    val effectivePrice = if (orderType == OrderType.LIMIT) parsedLimitPrice else referencePrice

    val calc = remember(
        asset, side, parsedQty, effectivePrice, availableCash, ownedQuantity, averageCostBasis, tradingFeeRate, fixedFee, isOnline, quoteFreshnessStatus
    ) {
        TradingService.calculateOrder(
            asset = asset,
            side = side,
            quantityInput = parsedQty,
            unitPrice = effectivePrice,
            availableCash = availableCash,
            ownedQuantity = ownedQuantity,
            averageCostBasis = averageCostBasis,
            feeRate = tradingFeeRate,
            fixedFee = fixedFee,
            isOnline = isOnline,
            quoteFreshnessStatus = quoteFreshnessStatus
        )
    }

    val maxAllowedForSide = if (side == OrderSide.BUY) {
        calc.maxAffordableQuantity.coerceAtMost(maxQuantity)
    } else {
        calc.maxSellableQuantity.coerceAtMost(maxQuantity)
    }

    fun updateQty(newQty: Double) {
        val clamped = newQty.coerceIn(0.0, maxQuantity)
        quantityText = if (!asset.allowsFractional || clamped % 1.0 == 0.0) {
            clamped.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", clamped).trimEnd('0').trimEnd('.')
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface,
        scrimColor = Color.Black.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("quantity_selector_sheet")
        ) {
            // Header: Asset Identity & Live Simulated Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetThumbnailPicture(
                        asset = asset,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${asset.symbol} • ${asset.category.displayName}",
                            color = MetallicGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = asset.name,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (asset.allowsFractional) "Fractional Asset (Min $minQuantity)" else "Indivisible Asset (Whole Units Only)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%,.2f", referencePrice)}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val isUp = priceChangePercent24h >= 0.0
                    Text(
                        text = String.format("%+.2f%% (24h)", priceChangePercent24h),
                        color = if (isUp) BullGreen else BearRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Buy / Sell Side Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBlack, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isBuy = side == OrderSide.BUY
                Button(
                    onClick = { side = OrderSide.BUY },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("selector_tab_buy"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBuy) BullGreen else Color.Transparent,
                        contentColor = if (isBuy) ObsidianBlack else TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("BUY ASSET", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { side = OrderSide.SELL },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("selector_tab_sell"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isBuy) BearRed else Color.Transparent,
                        contentColor = if (!isBuy) Color.White else TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SELL ASSET", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Market vs Limit Order Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = orderType == OrderType.MARKET,
                    onClick = { orderType = OrderType.MARKET },
                    label = { Text("Market Order (Instant)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGold.copy(alpha = 0.22f),
                        selectedLabelColor = MetallicGold
                    )
                )
                FilterChip(
                    selected = orderType == OrderType.LIMIT,
                    onClick = { orderType = OrderType.LIMIT },
                    label = { Text("Limit Order (Target Price)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                        selectedLabelColor = CyberCyan
                    )
                )
            }

            if (orderType == OrderType.LIMIT) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = limitPriceText,
                    onValueChange = { limitPriceText = it },
                    label = { Text("Limit Execution Price ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("limit_price_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stepper Controls: < - > Quantity < + >
            Surface(
                color = ElevatedPanel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SELECT QUANTITY (1 – 100+ UNITS)",
                        color = MetallicGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                val next = (parsedQty - step).coerceAtLeast(minQuantity)
                                updateQty(next)
                            },
                            enabled = parsedQty > minQuantity && !isSubmitting,
                            modifier = Modifier
                                .size(48.dp)
                                .background(ObsidianBlack, CircleShape)
                                .border(1.dp, MetallicGold.copy(alpha = 0.6f), CircleShape)
                                .testTag("quantity_minus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Quantity",
                                tint = MetallicGold
                            )
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { input ->
                                quantityText = input.filter { ch -> ch.isDigit() || ch == '.' }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (asset.allowsFractional) KeyboardType.Decimal else KeyboardType.Number
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .testTag("quantity_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = GlassBorder
                            )
                        )

                        IconButton(
                            onClick = {
                                val next = parsedQty + step
                                updateQty(next)
                            },
                            enabled = (maxAllowedForSide <= 0.0 || parsedQty + step <= maxAllowedForSide + 0.0001) && !isSubmitting,
                            modifier = Modifier
                                .size(48.dp)
                                .background(ObsidianBlack, CircleShape)
                                .border(1.dp, MetallicGold.copy(alpha = 0.6f), CircleShape)
                                .testTag("quantity_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Quantity",
                                tint = MetallicGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick-select quantity buttons: 1, 5, 10, 25, 50, 100, MAX
                    val quickQuantities = listOf(1, 5, 10, 25, 50, 100)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickQuantities.forEach { preset ->
                            val isSelected = parsedQty == preset.toDouble()
                            FilterChip(
                                selected = isSelected,
                                onClick = { updateQty(preset.toDouble()) },
                                label = {
                                    Text(
                                        text = preset.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.testTag("quick_qty_$preset"),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianBlack,
                                    labelColor = TextPrimary,
                                    selectedContainerColor = MetallicGold,
                                    selectedLabelColor = ObsidianBlack
                                )
                            )
                        }
                        FilterChip(
                            selected = maxAllowedForSide > 0 && parsedQty == maxAllowedForSide,
                            onClick = {
                                if (maxAllowedForSide > 0.0) {
                                    updateQty(maxAllowedForSide)
                                }
                            },
                            enabled = maxAllowedForSide > 0.0,
                            label = {
                                Text(
                                    text = "MAX",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            },
                            modifier = Modifier.testTag("quick_qty_max"),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ObsidianBlack,
                                labelColor = CyberCyan,
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = ObsidianBlack
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick percentage row matching Screen 6: 25% | 50% | 75% | 100%
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { pct ->
                            Surface(
                                color = ObsidianBlack,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                                onClick = {
                                    if (maxAllowedForSide > 0.0) {
                                        val raw = maxAllowedForSide * (pct / 100.0)
                                        val target = if (asset.allowsFractional) {
                                            raw.coerceAtLeast(minQuantity)
                                        } else {
                                            kotlin.math.floor(raw).coerceAtLeast(1.0)
                                        }
                                        updateQty(target)
                                    }
                                }
                            ) {
                                Text(
                                    text = "$pct%",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Max Affordable: ${formatUnit(calc.maxAffordableQuantity, asset.allowsFractional)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Owned / Sellable: ${formatUnit(calc.maxSellableQuantity, asset.allowsFractional)} ${asset.symbol}",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Order Value Calculator Breakdown
            Surface(
                color = ObsidianBlack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE ORDER VALUATION SUMMARY (VIRTUAL EXECUTION)",
                        color = MetallicGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    calc.realWorldReferencePrice?.let { refPrice ->
                        SummaryRow(
                            "Real-World Reference (${asset.valuationMetadata.priceType.label})",
                            "$${String.format("%,.2f", refPrice)}"
                        )
                    }
                    SummaryRow("Virtual Execution Price", "$${String.format("%,.2f", calc.unitPrice)}")
                    SummaryRow("Quote Freshness Status", calc.quoteFreshnessStatus.label)
                    SummaryRow("Selected Quantity", "${formatUnit(calc.quantity, asset.allowsFractional)} ${asset.unit}")
                    SummaryRow("Order Value (Subtotal)", "$${String.format("%,.2f", calc.orderValue)}")
                    SummaryRow(
                        "Brokerage Fee (${String.format("%.2f%%", tradingFeeRate * 100.0)})",
                        "$${String.format("%,.2f", calc.feeAmount)}"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = GlassBorder
                    )

                    if (side == OrderSide.BUY) {
                        SummaryRow(
                            label = "Total Buy Cost",
                            value = "$${String.format("%,.2f", calc.totalBuyCost)}",
                            valueColor = MetallicGold,
                            isBold = true
                        )
                        SummaryRow("Available Cash", "$${String.format("%,.2f", calc.availableCash)}")
                        SummaryRow(
                            label = "Remaining Cash After Purchase",
                            value = "$${String.format("%,.2f", calc.remainingCashAfterBuy)}",
                            valueColor = if (calc.remainingCashAfterBuy >= 0.0) BullGreen else BearRed,
                            isBold = true
                        )
                    } else {
                        SummaryRow(
                            label = "Net Sell Proceeds",
                            value = "$${String.format("%,.2f", calc.netSellProceeds)}",
                            valueColor = MetallicGold,
                            isBold = true
                        )
                        SummaryRow(
                            label = "Avg Cost Basis / Unit",
                            value = "$${String.format("%,.2f", averageCostBasis)}"
                        )
                        val pnlPositive = calc.estimatedRealizedPnL >= 0.0
                        val pnlSign = if (pnlPositive) "+" else "-"
                        val pnlFormatted = "$pnlSign$${String.format("%,.2f", kotlin.math.abs(calc.estimatedRealizedPnL))}"
                        SummaryRow(
                            label = "Estimated Realized P&L",
                            value = pnlFormatted,
                            valueColor = if (pnlPositive) BullGreen else BearRed,
                            isBold = true
                        )
                    }
                }
            }

            // Validation Banner
            if (calc.validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = BearRed.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BearRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("quantity_validation_error")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Validation Warning",
                            tint = BearRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = calc.validationError,
                            color = BearRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: [Cancel] [Confirm Buy / Confirm Sell]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (!isSubmitting) {
                            onDismiss()
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quantity_cancel_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (calc.isValid && !isSubmitting) {
                            val intent = OrderIntent(
                                idempotencyKey = UUID.randomUUID().toString(),
                                assetId = asset.id,
                                assetSymbol = asset.symbol,
                                assetName = asset.name,
                                side = side,
                                orderType = orderType,
                                quantity = calc.quantity,
                                referencePrice = calc.unitPrice,
                                limitPrice = if (orderType == OrderType.LIMIT) parsedLimitPrice else null,
                                subtotal = calc.orderValue,
                                tradingFeeRate = tradingFeeRate,
                                fixedFee = fixedFee,
                                feeAmount = calc.feeAmount,
                                estimatedTotalOrNetProceeds = if (side == OrderSide.BUY) calc.totalBuyCost else calc.netSellProceeds,
                                currency = currency
                            )
                            onConfirm(intent)
                        }
                    },
                    enabled = calc.isValid && !isSubmitting,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("quantity_confirm_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (side == OrderSide.BUY) BullGreen else BearRed,
                        contentColor = if (side == OrderSide.BUY) ObsidianBlack else Color.White,
                        disabledContainerColor = GlassBorder
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = ObsidianBlack
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val actionLabel = if (orderType == OrderType.LIMIT) {
                            "Place Limit ${if (side == OrderSide.BUY) "Buy" else "Sell"}"
                        } else {
                            "Confirm ${if (side == OrderSide.BUY) "Buy" else "Sell"}"
                        }
                        Text(actionLabel, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = if (isBold) 14.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

private fun formatUnit(value: Double, allowsFractional: Boolean): String {
    return if (!allowsFractional || value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
    }
}

package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.model.Asset
import com.example.data.model.MarketTick
import com.example.data.model.OrderSide
import com.example.data.model.QuoteFreshnessStatus
import com.example.domain.trading.OrderIntent

@Composable
fun QuantitySelectorModal(
    asset: Asset,
    tick: MarketTick?,
    initialSide: OrderSide,
    availableCash: Double,
    ownedQuantity: Double,
    averageCostBasis: Double,
    isSubmitting: Boolean,
    onConfirmOrder: (OrderIntent) -> Unit,
    onDismiss: () -> Unit,
    isOnline: Boolean = true
) {
    val currentPrice = tick?.currentPrice ?: asset.basePrice
    val changePct = tick?.changePercent24h ?: 0.0
    val freshness = tick?.freshnessStatus ?: QuoteFreshnessStatus.FRESH

    QuantitySelector(
        asset = asset,
        initialSide = initialSide,
        referencePrice = currentPrice,
        priceChangePercent24h = changePct,
        availableCash = availableCash,
        ownedQuantity = ownedQuantity,
        averageCostBasis = averageCostBasis,
        isSubmitting = isSubmitting,
        isOnline = isOnline,
        quoteFreshnessStatus = freshness,
        onConfirm = onConfirmOrder,
        onDismiss = onDismiss
    )
}

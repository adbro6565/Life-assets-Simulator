package com.example.domain.trading

import com.example.data.model.OrderSide
import com.example.data.model.OrderType

data class OrderIntent(
    val idempotencyKey: String,
    val assetId: String,
    val assetSymbol: String,
    val assetName: String,
    val side: OrderSide,
    val orderType: OrderType,
    val quantity: Double,
    val referencePrice: Double,
    val limitPrice: Double? = null,
    val subtotal: Double,
    val tradingFeeRate: Double,
    val fixedFee: Double,
    val feeAmount: Double,
    val estimatedTotalOrNetProceeds: Double,
    val currency: String = "USD",
    val timestampMillis: Long = System.currentTimeMillis()
)

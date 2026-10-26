package com.example.engine

import androidx.room.withTransaction
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.HoldingEntity
import com.example.data.local.LifeAssetsDatabase
import com.example.data.local.LimitOrderEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.Asset
import com.example.data.model.OrderCalculation
import com.example.data.model.OrderSide
import com.example.data.model.QuoteFreshnessStatus
import com.example.data.model.RarityTier
import com.example.data.model.ReferencePriceType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.floor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TradingService(
    private val db: LifeAssetsDatabase,
    private val marketEngine: MarketEngine
) {
    private val orderExecutionMutex = Mutex()

    companion object {
        const val DEFAULT_TRADING_FEE_RATE = 0.005 // 0.5% brokerage fee

        fun calculateOrder(
            asset: Asset,
            side: OrderSide,
            quantityInput: Double,
            unitPrice: Double,
            availableCash: Double,
            ownedQuantity: Double,
            averageCostBasis: Double,
            feeRate: Double = DEFAULT_TRADING_FEE_RATE,
            fixedFee: Double = 0.0,
            isOnline: Boolean = true,
            quoteFreshnessStatus: QuoteFreshnessStatus = QuoteFreshnessStatus.FRESH
        ): OrderCalculation {
            val metadata = asset.valuationMetadata
            val safePrice = if (unitPrice <= 0.0) asset.basePrice else unitPrice
            val priceBd = BigDecimal.valueOf(safePrice.coerceAtLeast(0.0))
            val feeMultiplier = BigDecimal.ONE.add(BigDecimal.valueOf(feeRate))
            val cashAfterFixed = (availableCash - fixedFee).coerceAtLeast(0.0)
            val rawMaxAffordable = if (safePrice > 0.0) {
                BigDecimal.valueOf(cashAfterFixed)
                    .divide(priceBd.multiply(feeMultiplier), 6, RoundingMode.DOWN)
                    .toDouble()
            } else 0.0

            val maxAffordable = if (asset.allowsFractional) {
                floor(rawMaxAffordable * 10000.0) / 10000.0
            } else {
                floor(rawMaxAffordable)
            }

            val maxSellable = if (asset.allowsFractional) {
                floor(ownedQuantity * 10000.0) / 10000.0
            } else {
                floor(ownedQuantity)
            }

            val safeQty = if (quantityInput.isNaN() || quantityInput.isInfinite() || quantityInput < 0.0) 0.0 else quantityInput
            val qtyBd = BigDecimal.valueOf(safeQty)
            val orderValueBd = qtyBd.multiply(priceBd).setScale(2, RoundingMode.HALF_UP)
            val feeBd = orderValueBd.multiply(BigDecimal.valueOf(feeRate))
                .add(BigDecimal.valueOf(fixedFee))
                .setScale(2, RoundingMode.HALF_UP)
            val totalBuyBd = orderValueBd.add(feeBd).setScale(2, RoundingMode.HALF_UP)
            val netSellBd = orderValueBd.subtract(feeBd).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)

            val orderValue = orderValueBd.toDouble()
            val feeAmount = feeBd.toDouble()
            val totalBuyCost = totalBuyBd.toDouble()
            val netSellProceeds = netSellBd.toDouble()
            val remainingCash = BigDecimal.valueOf(availableCash).subtract(totalBuyBd).toDouble()

            val costBasisOfSold = BigDecimal.valueOf(averageCostBasis)
                .multiply(qtyBd)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            val estimatedPnL = if (side == OrderSide.SELL) {
                BigDecimal.valueOf(netSellProceeds).subtract(BigDecimal.valueOf(costBasisOfSold)).toDouble()
            } else 0.0

            val validationError = when {
                !isOnline ->
                    "Internet connection required: Offline trading is disabled. Reconnect to verify live market prices."
                metadata.priceType == ReferencePriceType.REFERENCE_PRICE_UNAVAILABLE || metadata.realWorldReferencePrice == null ->
                    "PRICE UNAVAILABLE (Reference Price Unavailable): ${asset.name} has no verified real-world valuation source and cannot be traded."
                !asset.isTradable || asset.rarity == RarityTier.HISTORIC_REFERENCE || !metadata.isPurchasableInRealWorld ->
                    "${asset.name} is a non-purchasable sovereign/museum heritage reference (${metadata.priceType.label}) and cannot be bought or sold."
                quoteFreshnessStatus == QuoteFreshnessStatus.STALE ->
                    "Market quote for ${asset.symbol} is STALE. Please refresh live market data before executing a trade."
                quantityInput.isNaN() || quantityInput.isInfinite() || quantityInput <= 0.0 ->
                    "Enter a valid quantity greater than zero."
                !asset.allowsFractional && (quantityInput % 1.0 != 0.0) ->
                    "${asset.name} is an indivisible asset and requires whole-number units (1, 2, 10, 100)."
                quantityInput < asset.minQuantity ->
                    "Minimum order quantity for ${asset.symbol} is ${asset.minQuantity}."
                quantityInput > asset.maxOrderQuantity ->
                    "Maximum single-order size is ${asset.maxOrderQuantity.toInt()} units."
                side == OrderSide.BUY && totalBuyCost > availableCash + 0.001 ->
                    "Insufficient liquid cash. Required: $${formatMoney(totalBuyCost)}, Available: $${formatMoney(availableCash)}."
                side == OrderSide.SELL && quantityInput > ownedQuantity + 0.000001 ->
                    "Insufficient holdings. You own ${formatQty(ownedQuantity, asset.allowsFractional)} ${asset.symbol}."
                else -> null
            }

            return OrderCalculation(
                assetId = asset.id,
                side = side,
                quantity = quantityInput,
                unitPrice = safePrice,
                realWorldReferencePrice = metadata.realWorldReferencePrice,
                orderValue = orderValue,
                feeRate = feeRate,
                feeAmount = feeAmount,
                totalBuyCost = totalBuyCost,
                netSellProceeds = netSellProceeds,
                availableCash = availableCash,
                remainingCashAfterBuy = remainingCash,
                ownedQuantity = ownedQuantity,
                maxAffordableQuantity = maxAffordable,
                maxSellableQuantity = maxSellable,
                estimatedRealizedPnL = estimatedPnL,
                quoteFreshnessStatus = quoteFreshnessStatus,
                isValid = validationError == null,
                validationError = validationError
            )
        }

        fun computeNewWeightedAverageCost(
            existingQty: Double,
            existingAvgCost: Double,
            addedQty: Double,
            executionPrice: Double
        ): Double {
            val totalQty = existingQty + addedQty
            if (totalQty <= 0.0) return 0.0
            val totalCost = BigDecimal.valueOf(existingQty).multiply(BigDecimal.valueOf(existingAvgCost))
                .add(BigDecimal.valueOf(addedQty).multiply(BigDecimal.valueOf(executionPrice)))
            return totalCost.divide(BigDecimal.valueOf(totalQty), 4, RoundingMode.HALF_UP).toDouble()
        }

        private fun formatMoney(amount: Double): String = String.format("%,.2f", amount)
        private fun formatQty(qty: Double, fractional: Boolean): String =
            if (fractional) String.format("%.4f", qty) else qty.toLong().toString()
    }

    private fun isTradingOnlineAllowed(): Boolean {
        val status = marketEngine.backendService.connectionStatus.value
        val health = marketEngine.backendService.backendHealth.value
        return status.allowsTradingAndClaiming && health != BackendHealthState.BACKEND_DOWN_503
    }

    suspend fun executeMarketOrder(
        userId: String,
        assetId: String,
        side: OrderSide,
        quantity: Double,
        idempotencyKey: String = UUID.randomUUID().toString(),
        overrideExecutionPrice: Double? = null
    ): Result<TransactionEntity> = orderExecutionMutex.withLock {
        if (!isTradingOnlineAllowed()) {
            return@withLock Result.failure(
                IllegalStateException("Internet connection required: Offline trading is disabled. Reconnect to verify live market prices.")
            )
        }

        val asset = db.assetDao().getAssetById(assetId)?.toAsset()
            ?: AssetCatalogue.getAssetById(assetId)
            ?: return@withLock Result.failure(IllegalArgumentException("Unknown asset ID: $assetId"))

        val tickFreshness = marketEngine.ticks.value[asset.id]?.freshnessStatus
            ?: marketEngine.getQuoteFreshness(asset.id)

        return@withLock try {
            val tx = db.withTransaction {
                val existingTx = db.transactionDao().getByIdempotencyKey(idempotencyKey)
                if (existingTx != null) {
                    return@withTransaction existingTx
                }

                val user = db.userDao().getUserById(userId)
                    ?: throw IllegalStateException("Authenticated user record not found.")
                val holding = db.holdingDao().getHolding(userId, assetId)
                val ownedQty = (holding?.quantity ?: 0.0) - (holding?.reservedQuantity ?: 0.0)
                val avgCost = holding?.averageCostBasis ?: 0.0
                val execPrice = overrideExecutionPrice ?: marketEngine.getAuthoritativePrice(assetId)

                val calc = calculateOrder(
                    asset = asset,
                    side = side,
                    quantityInput = quantity,
                    unitPrice = execPrice,
                    availableCash = user.cashBalance,
                    ownedQuantity = ownedQty.coerceAtLeast(0.0),
                    averageCostBasis = avgCost,
                    isOnline = isTradingOnlineAllowed(),
                    quoteFreshnessStatus = tickFreshness
                )

                if (!calc.isValid) {
                    throw IllegalArgumentException(calc.validationError ?: "Invalid order parameters.")
                }

                if (side == OrderSide.BUY) {
                    val newCash = BigDecimal.valueOf(user.cashBalance)
                        .subtract(BigDecimal.valueOf(calc.totalBuyCost))
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
                    val currentTotalQty = holding?.quantity ?: 0.0
                    val newQty = if (asset.allowsFractional) {
                        MarketEngine.roundQuantity(currentTotalQty + quantity)
                    } else {
                        (currentTotalQty.toLong() + quantity.toLong()).toDouble()
                    }
                    val newAvgCost = computeNewWeightedAverageCost(
                        existingQty = currentTotalQty,
                        existingAvgCost = avgCost,
                        addedQty = quantity,
                        executionPrice = execPrice
                    )

                    db.userDao().updateUser(user.copy(cashBalance = newCash))
                    db.holdingDao().upsertHolding(
                        HoldingEntity(
                            userId = userId,
                            assetId = assetId,
                            quantity = newQty,
                            averageCostBasis = newAvgCost,
                            reservedQuantity = holding?.reservedQuantity ?: 0.0,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )

                    val record = TransactionEntity(
                        transactionId = UUID.randomUUID().toString(),
                        idempotencyKey = idempotencyKey,
                        userId = userId,
                        assetId = asset.id,
                        assetName = asset.name,
                        assetSymbol = asset.symbol,
                        side = "BUY",
                        orderType = "MARKET",
                        quantity = quantity,
                        executionPrice = execPrice,
                        orderValue = calc.orderValue,
                        feeAmount = calc.feeAmount,
                        totalAmount = calc.totalBuyCost,
                        realizedPnL = 0.0,
                        timestampMillis = System.currentTimeMillis()
                    )
                    db.transactionDao().insertTransaction(record)
                    record
                } else {
                    val currentHolding = holding
                        ?: throw IllegalArgumentException("You do not own any ${asset.symbol} to sell.")
                    val newCash = BigDecimal.valueOf(user.cashBalance)
                        .add(BigDecimal.valueOf(calc.netSellProceeds))
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
                    val remainingQty = if (asset.allowsFractional) {
                        MarketEngine.roundQuantity(currentHolding.quantity - quantity)
                    } else {
                        (currentHolding.quantity.toLong() - quantity.toLong()).toDouble()
                    }
                    val newRealizedPnL = BigDecimal.valueOf(user.realizedPnL)
                        .add(BigDecimal.valueOf(calc.estimatedRealizedPnL))
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()

                    db.userDao().updateUser(
                        user.copy(
                            cashBalance = newCash,
                            realizedPnL = newRealizedPnL
                        )
                    )
                    if (remainingQty <= 0.00001) {
                        db.holdingDao().deleteHolding(userId, assetId)
                    } else {
                        db.holdingDao().upsertHolding(
                            currentHolding.copy(
                                quantity = remainingQty,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }

                    val record = TransactionEntity(
                        transactionId = UUID.randomUUID().toString(),
                        idempotencyKey = idempotencyKey,
                        userId = userId,
                        assetId = asset.id,
                        assetName = asset.name,
                        assetSymbol = asset.symbol,
                        side = "SELL",
                        orderType = "MARKET",
                        quantity = quantity,
                        executionPrice = execPrice,
                        orderValue = calc.orderValue,
                        feeAmount = calc.feeAmount,
                        totalAmount = calc.netSellProceeds,
                        realizedPnL = calc.estimatedRealizedPnL,
                        timestampMillis = System.currentTimeMillis()
                    )
                    db.transactionDao().insertTransaction(record)
                    record
                }
            }
            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun placeLimitOrder(
        userId: String,
        assetId: String,
        side: OrderSide,
        quantity: Double,
        limitPrice: Double
    ): Result<LimitOrderEntity> {
        if (!isTradingOnlineAllowed()) {
            return Result.failure(
                IllegalStateException("Internet connection required: Offline limit orders are disabled.")
            )
        }

        val asset = db.assetDao().getAssetById(assetId)?.toAsset()
            ?: AssetCatalogue.getAssetById(assetId)
            ?: return Result.failure(IllegalArgumentException("Unknown asset ID: $assetId"))
        if (limitPrice <= 0.0) {
            return Result.failure(IllegalArgumentException("Limit price must be greater than zero."))
        }

        return try {
            val order = db.withTransaction {
                val user = db.userDao().getUserById(userId)
                    ?: throw IllegalStateException("Authenticated user not found.")
                val holding = db.holdingDao().getHolding(userId, assetId)
                val availableQty = (holding?.quantity ?: 0.0) - (holding?.reservedQuantity ?: 0.0)

                val calc = calculateOrder(
                    asset = asset,
                    side = side,
                    quantityInput = quantity,
                    unitPrice = limitPrice,
                    availableCash = user.cashBalance,
                    ownedQuantity = availableQty.coerceAtLeast(0.0),
                    averageCostBasis = holding?.averageCostBasis ?: 0.0,
                    isOnline = isTradingOnlineAllowed()
                )
                if (!calc.isValid) {
                    throw IllegalArgumentException(calc.validationError ?: "Invalid limit order.")
                }

                if (side == OrderSide.BUY) {
                    db.userDao().updateUser(
                        user.copy(cashBalance = (user.cashBalance - calc.totalBuyCost).coerceAtLeast(0.0))
                    )
                } else {
                    val h = holding ?: throw IllegalArgumentException("Insufficient holdings.")
                    db.holdingDao().upsertHolding(
                        h.copy(reservedQuantity = h.reservedQuantity + quantity)
                    )
                }

                val limitOrder = LimitOrderEntity(
                    orderId = UUID.randomUUID().toString(),
                    userId = userId,
                    assetId = asset.id,
                    assetName = asset.name,
                    assetSymbol = asset.symbol,
                    side = side.name,
                    quantity = quantity,
                    limitPrice = limitPrice,
                    reservedCash = if (side == OrderSide.BUY) calc.totalBuyCost else 0.0,
                    reservedQuantity = if (side == OrderSide.SELL) quantity else 0.0,
                    status = "OPEN"
                )
                db.limitOrderDao().insertOrder(limitOrder)
                limitOrder
            }
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelLimitOrder(userId: String, orderId: String): Result<Unit> {
        if (!isTradingOnlineAllowed()) {
            return Result.failure(IllegalStateException("Internet connection required to modify orders."))
        }
        return try {
            db.withTransaction {
                val order = db.limitOrderDao().getOrderById(orderId)
                    ?: throw IllegalArgumentException("Order not found.")
                if (order.userId != userId || order.status != "OPEN") {
                    throw IllegalStateException("Order cannot be cancelled.")
                }
                if (order.side == "BUY") {
                    val user = db.userDao().getUserById(userId) ?: return@withTransaction
                    db.userDao().updateUser(user.copy(cashBalance = user.cashBalance + order.reservedCash))
                } else {
                    val holding = db.holdingDao().getHolding(userId, order.assetId)
                    if (holding != null) {
                        db.holdingDao().upsertHolding(
                            holding.copy(
                                reservedQuantity = (holding.reservedQuantity - order.reservedQuantity).coerceAtLeast(0.0)
                            )
                        )
                    }
                }
                db.limitOrderDao().updateOrder(
                    order.copy(status = "CANCELLED", updatedAtMillis = System.currentTimeMillis())
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun evaluateOpenLimitOrders(): List<TransactionEntity> {
        if (!isTradingOnlineAllowed()) return emptyList()
        val openOrders = db.limitOrderDao().getAllOpenOrders()
        val filled = mutableListOf<TransactionEntity>()
        for (order in openOrders) {
            val tick = marketEngine.ticks.value[order.assetId]
            if (tick?.freshnessStatus == QuoteFreshnessStatus.STALE) continue
            val currentPrice = marketEngine.getAuthoritativePrice(order.assetId)
            val shouldFill = (order.side == "BUY" && currentPrice <= order.limitPrice) ||
                (order.side == "SELL" && currentPrice >= order.limitPrice)
            if (!shouldFill) continue

            try {
                db.withTransaction {
                    val freshOrder = db.limitOrderDao().getOrderById(order.orderId) ?: return@withTransaction
                    if (freshOrder.status != "OPEN") return@withTransaction

                    if (freshOrder.side == "BUY") {
                        val user = db.userDao().getUserById(freshOrder.userId) ?: return@withTransaction
                        db.userDao().updateUser(user.copy(cashBalance = user.cashBalance + freshOrder.reservedCash))
                    } else {
                        val holding = db.holdingDao().getHolding(freshOrder.userId, freshOrder.assetId)
                        if (holding != null) {
                            db.holdingDao().upsertHolding(
                                holding.copy(
                                    reservedQuantity = (holding.reservedQuantity - freshOrder.reservedQuantity).coerceAtLeast(0.0)
                                )
                            )
                        }
                    }

                    db.limitOrderDao().updateOrder(
                        freshOrder.copy(status = "FILLED", updatedAtMillis = System.currentTimeMillis())
                    )
                }

                val execResult = executeMarketOrder(
                    userId = order.userId,
                    assetId = order.assetId,
                    side = OrderSide.valueOf(order.side),
                    quantity = order.quantity,
                    idempotencyKey = "limit_${order.orderId}",
                    overrideExecutionPrice = currentPrice
                )
                execResult.getOrNull()?.let { filled.add(it) }
            } catch (_: Exception) {
            }
        }
        return filled
    }
}

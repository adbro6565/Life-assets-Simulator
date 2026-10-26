package com.example.engine

import androidx.room.withTransaction
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.HoldingEntity
import com.example.data.local.LifeAssetsDatabase
import com.example.data.local.TradeOfferEntity
import com.example.data.model.BillionairePlayer
import java.util.UUID

class MultiplayerService(
    private val db: LifeAssetsDatabase,
    private val marketEngine: MarketEngine
) {
    private val rivalBillionaires = listOf(
        BillionairePlayer("bot_satoshi", "Satoshi_Phantom", "Crypto Sovereign", 4_850_000_000.0, 620_000_000.0, "Bitcoin & Quantum Pods", 4.2),
        BillionairePlayer("bot_aurelius", "Lord_Aurelius", "Geneva Heritage Syndicate", 2_940_000_000.0, 310_000_000.0, "Patek Grandmaster & Monaco Penthouse", 1.8),
        BillionairePlayer("bot_nova", "Valkyrie_Nova", "Orbital & Fusion Titan", 1_780_000_000.0, 240_000_000.0, "LEO Space Station & SMR Reactor", 5.6),
        BillionairePlayer("bot_kaito", "Kaito_Takahashi", "Tokyo Compute & Wagyu Baron", 890_000_000.0, 145_000_000.0, "Blackwell B200 Pod & Azabudai Penthouse", -0.9),
        BillionairePlayer("bot_elena", "Duchess_Elena", "Fine Art & Riviera Magnate", 640_000_000.0, 95_000_000.0, "Salvator Mundi & Lürssen 142m Yacht", 2.4)
    )

    fun getGlobalLeaderboard(
        currentUserId: String,
        currentUsername: String,
        currentCash: Double,
        currentNetWorth: Double
    ): List<BillionairePlayer> {
        val userEntry = BillionairePlayer(
            playerId = currentUserId,
            username = currentUsername,
            title = when {
                currentNetWorth >= 1_000_000_000.0 -> "Global Deca-Billionaire"
                currentNetWorth >= 100_000_000.0 -> "Centimillionaire Tycoon"
                currentNetWorth >= 10_000_000.0 -> "Hedge Fund Magnate"
                else -> "Rising Wealth Builder"
            },
            netWorth = currentNetWorth,
            cashBalance = currentCash,
            topAssetName = "Active Portfolio",
            change24hPercent = 2.8,
            isCurrentUser = true
        )
        return (rivalBillionaires + userEntry).sortedByDescending { it.netWorth }
    }

    suspend fun seedInitialOffersIfNeeded(userId: String, username: String) {
        val existing = db.tradeOfferDao().getOfferById("welcome_offer_$userId")
        if (existing == null) {
            db.tradeOfferDao().upsertOffer(
                TradeOfferEntity(
                    offerId = "welcome_offer_$userId",
                    senderUserId = "bot_aurelius",
                    senderUsername = "Lord_Aurelius",
                    recipientUserId = userId,
                    recipientUsername = username,
                    offeredAssetId = "watch_02",
                    offeredAssetName = "Rolex Submariner Date 18k Yellow Gold",
                    offeredQuantity = 1.0,
                    requestedCash = 39_500.0, // Discounted P2P offer
                    status = "PENDING"
                )
            )
        }
    }

    suspend fun createP2PTradeOffer(
        senderUserId: String,
        senderUsername: String,
        recipientUsername: String,
        offeredAssetId: String,
        offeredQuantity: Double,
        requestedCash: Double
    ): Result<TradeOfferEntity> {
        val asset = AssetCatalogue.getAssetById(offeredAssetId)
            ?: return Result.failure(IllegalArgumentException("Asset not found."))
        if (offeredQuantity <= 0.0 || requestedCash <= 0.0) {
            return Result.failure(IllegalArgumentException("Quantity and requested cash must be positive."))
        }

        return try {
            val offer = db.withTransaction {
                val holding = db.holdingDao().getHolding(senderUserId, offeredAssetId)
                    ?: throw IllegalArgumentException("You do not own ${asset.name}.")
                val available = holding.quantity - holding.reservedQuantity
                if (available < offeredQuantity) {
                    throw IllegalArgumentException("Insufficient unreserved quantity of ${asset.symbol}.")
                }

                // Reserve the offered inventory
                db.holdingDao().upsertHolding(
                    holding.copy(reservedQuantity = holding.reservedQuantity + offeredQuantity)
                )

                val entity = TradeOfferEntity(
                    offerId = UUID.randomUUID().toString(),
                    senderUserId = senderUserId,
                    senderUsername = senderUsername,
                    recipientUserId = "bot_satoshi",
                    recipientUsername = recipientUsername.ifBlank { "Satoshi_Phantom" },
                    offeredAssetId = asset.id,
                    offeredAssetName = asset.name,
                    offeredQuantity = offeredQuantity,
                    requestedCash = requestedCash,
                    status = "PENDING"
                )
                db.tradeOfferDao().upsertOffer(entity)
                entity
            }
            Result.success(offer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptTradeOffer(userId: String, offerId: String): Result<TradeOfferEntity> {
        return try {
            val settled = db.withTransaction {
                val offer = db.tradeOfferDao().getOfferById(offerId)
                    ?: throw IllegalArgumentException("Trade offer not found.")
                if (offer.status != "PENDING") {
                    throw IllegalStateException("This trade offer has already been ${offer.status.lowercase()}.")
                }

                val user = db.userDao().getUserById(userId)
                    ?: throw IllegalStateException("Authenticated user not found.")

                if (offer.recipientUserId == userId) {
                    // Current user is buying the offered asset for requestedCash
                    if (user.cashBalance < offer.requestedCash) {
                        throw IllegalArgumentException("Insufficient cash ($${String.format("%,.2f", user.cashBalance)}) to accept offer ($${String.format("%,.2f", offer.requestedCash)}).")
                    }
                    val existingHolding = db.holdingDao().getHolding(userId, offer.offeredAssetId)
                    val unitCost = offer.requestedCash / offer.offeredQuantity
                    val newQty = (existingHolding?.quantity ?: 0.0) + offer.offeredQuantity
                    val newAvg = TradingService.computeNewWeightedAverageCost(
                        existingQty = existingHolding?.quantity ?: 0.0,
                        existingAvgCost = existingHolding?.averageCostBasis ?: 0.0,
                        addedQty = offer.offeredQuantity,
                        executionPrice = unitCost
                    )

                    db.userDao().updateUser(user.copy(cashBalance = user.cashBalance - offer.requestedCash))
                    db.holdingDao().upsertHolding(
                        HoldingEntity(
                            userId = userId,
                            assetId = offer.offeredAssetId,
                            quantity = newQty,
                            averageCostBasis = newAvg,
                            reservedQuantity = existingHolding?.reservedQuantity ?: 0.0
                        )
                    )
                } else if (offer.senderUserId == userId) {
                    // Counterparty accepted user's outgoing offer
                    val holding = db.holdingDao().getHolding(userId, offer.offeredAssetId)
                        ?: throw IllegalStateException("Offered asset holding missing.")
                    val remainingQty = holding.quantity - offer.offeredQuantity
                    val remainingRes = (holding.reservedQuantity - offer.offeredQuantity).coerceAtLeast(0.0)
                    if (remainingQty <= 0.00001) {
                        db.holdingDao().deleteHolding(userId, offer.offeredAssetId)
                    } else {
                        db.holdingDao().upsertHolding(
                            holding.copy(quantity = remainingQty, reservedQuantity = remainingRes)
                        )
                    }
                    db.userDao().updateUser(user.copy(cashBalance = user.cashBalance + offer.requestedCash))
                }

                val updated = offer.copy(status = "ACCEPTED")
                db.tradeOfferDao().upsertOffer(updated)
                updated
            }
            Result.success(settled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineTradeOffer(userId: String, offerId: String): Result<Unit> {
        return try {
            db.withTransaction {
                val offer = db.tradeOfferDao().getOfferById(offerId) ?: return@withTransaction
                if (offer.status != "PENDING") return@withTransaction
                if (offer.senderUserId == userId) {
                    val holding = db.holdingDao().getHolding(userId, offer.offeredAssetId)
                    if (holding != null) {
                        db.holdingDao().upsertHolding(
                            holding.copy(reservedQuantity = (holding.reservedQuantity - offer.offeredQuantity).coerceAtLeast(0.0))
                        )
                    }
                }
                db.tradeOfferDao().upsertOffer(offer.copy(status = "DECLINED"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

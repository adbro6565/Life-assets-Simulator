package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["normalizedUsername"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val normalizedUsername: String,
    val email: String,
    val passwordHash: String,
    val cashBalance: Double,
    val realizedPnL: Double = 0.0,
    val streakDays: Int = 0,
    val lastClaimDateKathmandu: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "holdings",
    primaryKeys = ["userId", "assetId"],
    indices = [Index(value = ["userId"]), Index(value = ["assetId"])]
)
data class HoldingEntity(
    val userId: String,
    val assetId: String,
    val quantity: Double,
    val averageCostBasis: Double,
    val reservedQuantity: Double = 0.0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["userId"]), Index(value = ["idempotencyKey"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val idempotencyKey: String,
    val userId: String,
    val assetId: String,
    val assetName: String,
    val assetSymbol: String,
    val side: String, // "BUY" or "SELL"
    val orderType: String, // "MARKET" or "LIMIT"
    val quantity: Double,
    val executionPrice: Double,
    val orderValue: Double,
    val feeAmount: Double,
    val totalAmount: Double,
    val realizedPnL: Double,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "limit_orders",
    indices = [Index(value = ["userId"]), Index(value = ["status"])]
)
data class LimitOrderEntity(
    @PrimaryKey val orderId: String,
    val userId: String,
    val assetId: String,
    val assetName: String,
    val assetSymbol: String,
    val side: String, // "BUY" or "SELL"
    val quantity: Double,
    val limitPrice: Double,
    val reservedCash: Double,
    val reservedQuantity: Double,
    val status: String, // "OPEN", "FILLED", "CANCELLED"
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_rewards",
    primaryKeys = ["userId", "claimDateKathmandu"],
    indices = [Index(value = ["userId"])]
)
data class DailyRewardEntity(
    val userId: String,
    val claimDateKathmandu: String,
    val streakDay: Int,
    val rewardAmount: Double,
    val claimedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "trade_offers",
    indices = [Index(value = ["recipientUserId"]), Index(value = ["senderUserId"])]
)
data class TradeOfferEntity(
    @PrimaryKey val offerId: String,
    val senderUserId: String,
    val senderUsername: String,
    val recipientUserId: String,
    val recipientUsername: String,
    val offeredAssetId: String,
    val offeredAssetName: String,
    val offeredQuantity: Double,
    val requestedCash: Double,
    val status: String, // "PENDING", "ACCEPTED", "DECLINED"
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["category"]),
        Index(value = ["symbol"])
    ]
)
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val symbol: String,
    val category: String,
    val description: String,
    val basePrice: Double,
    val subcategory: String = "",
    val unit: String = "Unit",
    val allowsFractional: Boolean = false,
    val minQuantity: Double = 1.0,
    val maxOrderQuantity: Double = 10000.0,
    val rarity: String = "RARE",
    val maintenanceRateAnnual: Double = 0.01,
    val isTradable: Boolean = true,
    val priceSourceLabel: String = "Simulated Market",
    val location: String = "Global",
    val imageUrl1: String = "",
    val imageUrl2: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val imageSource: String = "Wikimedia Commons / Authorized Editorial CDN",
    val licenseInfo: String = "Public Domain / CC BY-SA 4.0 / Unsplash License",
    val verificationStatus: String = com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO.name,
    val onlinePriceUsd: Double = basePrice,
    val priceSource: String = priceSourceLabel,
    val priceType: String = com.example.data.model.ReferencePriceType.LIVE_QUOTE.name,
    val priceUpdatedAt: Long = 1761200000000L,
    val updatedAt: Long = 1761200000000L,
    val isWatched: Boolean = false
) {
    fun toAsset(): com.example.data.model.Asset {
        val catEnum = try {
            com.example.data.model.AssetCategory.valueOf(category)
        } catch (_: Exception) {
            com.example.data.model.AssetCategory.entries.firstOrNull {
                it.name.equals(category, ignoreCase = true) || it.code.equals(category, ignoreCase = true)
            } ?: com.example.data.model.AssetCategory.FINE_ART
        }
        val rarityEnum = try {
            com.example.data.model.RarityTier.valueOf(rarity)
        } catch (_: Exception) {
            com.example.data.model.RarityTier.RARE
        }
        val verifiedMeta = com.example.data.catalogue.AssetImageRegistry.getVerifiedImageMetadata(
            id,
            name,
            catEnum,
            subcategory
        )
        val statusEnum = runCatching {
            if (verificationStatus.isNotBlank() && id !in com.example.data.catalogue.AssetImageRegistry.verifiedAssetUrlMap.keys) {
                com.example.data.model.ImageVerificationStatus.valueOf(verificationStatus)
            } else {
                verifiedMeta.verificationStatus
            }
        }.getOrDefault(verifiedMeta.verificationStatus)

        val resolvedPriceType = runCatching {
            com.example.data.model.ReferencePriceType.valueOf(priceType)
        }.getOrDefault(com.example.data.model.ReferencePriceType.LIVE_QUOTE)

        val resolvedUrl1 = if (imageUrl1.startsWith("https://")) imageUrl1 else verifiedMeta.imageUrl
        val resolvedUrl2 = if (imageUrl2.startsWith("https://")) imageUrl2 else verifiedMeta.secondaryImageUrl
        val resolvedPrimary = if (imageUrl.startsWith("https://")) imageUrl else resolvedUrl1
        val resolvedThumb = if (thumbnailUrl.startsWith("https://")) thumbnailUrl else verifiedMeta.thumbnailUrl

        val effectiveTradable = isTradable &&
            rarityEnum != com.example.data.model.RarityTier.HISTORIC_REFERENCE &&
            !id.startsWith("unsupported_", ignoreCase = true) &&
            !priceSourceLabel.equals("UNAVAILABLE", ignoreCase = true)
        return com.example.data.model.Asset(
            id = id,
            name = name,
            symbol = symbol,
            category = catEnum,
            subcategory = subcategory,
            description = description,
            basePrice = basePrice,
            unit = unit,
            allowsFractional = allowsFractional,
            minQuantity = minQuantity,
            maxOrderQuantity = maxOrderQuantity,
            rarity = rarityEnum,
            maintenanceRateAnnual = maintenanceRateAnnual,
            isTradable = effectiveTradable,
            priceSourceLabel = priceSourceLabel,
            location = location,
            imageUrl1 = resolvedUrl1,
            imageUrl2 = resolvedUrl2,
            imageUrl = resolvedPrimary,
            thumbnailUrl = resolvedThumb,
            imageSource = imageSource.ifBlank { verifiedMeta.imageSource },
            licenseInfo = licenseInfo.ifBlank { verifiedMeta.licenseInfo },
            verificationStatus = statusEnum,
            onlinePriceUsd = if (onlinePriceUsd > 0.0) onlinePriceUsd else basePrice,
            priceSource = priceSource.ifBlank { priceSourceLabel },
            priceType = resolvedPriceType,
            priceUpdatedAt = if (priceUpdatedAt > 0L) priceUpdatedAt else updatedAt,
            updatedAt = if (updatedAt > 0L) updatedAt else verifiedMeta.updatedAt,
            isWatched = isWatched
        )
    }

    companion object {
        fun fromAsset(asset: com.example.data.model.Asset): AssetEntity {
            val verifiedMeta = com.example.data.catalogue.AssetImageRegistry.getVerifiedImageMetadata(
                asset.id,
                asset.name,
                asset.category,
                asset.subcategory
            )
            val valuationMeta = asset.valuationMetadata
            val resolvedUrl1 = if (asset.imageUrl1.startsWith("https://")) asset.imageUrl1 else verifiedMeta.imageUrl
            val resolvedUrl2 = if (asset.imageUrl2.startsWith("https://")) asset.imageUrl2 else verifiedMeta.secondaryImageUrl
            val resolvedPrimary = if (asset.imageUrl.startsWith("https://")) asset.imageUrl else resolvedUrl1
            val resolvedThumb = if (asset.thumbnailUrl.startsWith("https://")) asset.thumbnailUrl else verifiedMeta.thumbnailUrl

            val effectiveTradable = asset.isTradable &&
                asset.rarity != com.example.data.model.RarityTier.HISTORIC_REFERENCE &&
                !asset.id.startsWith("unsupported_", ignoreCase = true) &&
                !asset.priceSourceLabel.equals("UNAVAILABLE", ignoreCase = true)
            val resolvedOnlinePrice = valuationMeta.realWorldReferencePrice ?: asset.onlinePriceUsd.takeIf { it > 0.0 } ?: asset.basePrice
            return AssetEntity(
                id = asset.id,
                name = asset.name,
                symbol = asset.symbol,
                category = asset.category.name,
                description = asset.description,
                basePrice = asset.basePrice,
                subcategory = asset.subcategory,
                unit = asset.unit,
                allowsFractional = asset.allowsFractional,
                minQuantity = asset.minQuantity,
                maxOrderQuantity = asset.maxOrderQuantity,
                rarity = asset.rarity.name,
                maintenanceRateAnnual = asset.maintenanceRateAnnual,
                isTradable = effectiveTradable,
                priceSourceLabel = asset.priceSourceLabel,
                location = asset.location,
                imageUrl1 = resolvedUrl1,
                imageUrl2 = resolvedUrl2,
                imageUrl = resolvedPrimary,
                thumbnailUrl = resolvedThumb,
                imageSource = asset.imageSource.ifBlank { verifiedMeta.imageSource },
                licenseInfo = asset.licenseInfo.ifBlank { verifiedMeta.licenseInfo },
                verificationStatus = asset.verificationStatus.name,
                onlinePriceUsd = resolvedOnlinePrice,
                priceSource = valuationMeta.dataProvider.ifBlank { asset.priceSource },
                priceType = valuationMeta.priceType.name,
                priceUpdatedAt = if (asset.priceUpdatedAt > 0L) asset.priceUpdatedAt else verifiedMeta.updatedAt,
                updatedAt = if (asset.updatedAt > 0L) asset.updatedAt else verifiedMeta.updatedAt,
                isWatched = asset.isWatched
            )
        }
    }
}

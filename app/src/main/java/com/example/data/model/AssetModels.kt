package com.example.data.model

import com.example.data.catalogue.VerifiedAssetValuationRegistry

enum class AssetCategory(
    val displayName: String,
    val code: String,
    val defaultAllowsFractional: Boolean,
    val defaultVolatility: Double,
    val defaultFreshnessThresholdMs: Long = 180_000L
) {
    FINE_ART("Fine Art & Paintings", "FINE_ART", false, 0.004, 86_400_000L),
    LUXURY_WATCHES("Luxury Watches", "LUXURY_WATCHES", false, 0.006, 3_600_000L),
    SUPERCARS("Supercars & Hypercars", "SUPERCARS", false, 0.005, 3_600_000L),
    PRIVATE_JETS("Private Jets & Helicopters", "PRIVATE_JETS", false, 0.005, 86_400_000L),
    YACHTS_SHIPS("Yachts, Ships & Submarines", "YACHTS_SHIPS", false, 0.005, 86_400_000L),
    LUXURY_REAL_ESTATE("Mansions & Luxury Real Estate", "LUXURY_REAL_ESTATE", false, 0.004, 86_400_000L),
    PRIVATE_ISLANDS("Private Islands & Land", "PRIVATE_ISLANDS", false, 0.004, 86_400_000L),
    JEWELRY_DIAMONDS("Jewelry, Diamonds & Stones", "JEWELRY_DIAMONDS", false, 0.006, 3_600_000L),
    DESIGNER_FASHION("Designer Fashion & Handbags", "DESIGNER_FASHION", false, 0.007, 3_600_000L),
    PRECIOUS_METALS("Gold, Silver & Precious Metals", "PRECIOUS_METALS", true, 0.012, 120_000L),
    RARE_COLLECTIBLES("Rare Collectibles & Antiques", "RARE_COLLECTIBLES", false, 0.006, 86_400_000L),
    AGRICULTURAL("Food & Agricultural Commodities", "AGRICULTURAL", true, 0.014, 300_000L),
    LUXURY_FOOD("Luxury Food & Caviar", "LUXURY_FOOD", false, 0.010, 3_600_000L),
    CRYPTOCURRENCIES("Cryptocurrencies", "CRYPTOCURRENCIES", true, 0.032, 90_000L),
    STOCKS_INDICES("Stocks, ETFs & Global Indices", "STOCKS_INDICES", true, 0.018, 300_000L),
    BUSINESSES("Businesses & Franchises", "BUSINESSES", false, 0.008, 86_400_000L),
    INDUSTRIAL("Industrial & Construction Machinery", "INDUSTRIAL", false, 0.007, 86_400_000L),
    TRANSPORT_LOGISTICS("Maritime & Aviation Infrastructure", "TRANSPORT_LOGISTICS", false, 0.007, 86_400_000L),
    TECH_COMPUTING("AI Clusters & Quantum Computing", "TECH_COMPUTING", false, 0.015, 3_600_000L),
    SPACE_ENERGY("Space & Clean Energy Assets", "SPACE_ENERGY", false, 0.016, 3_600_000L)
}

enum class RarityTier(val label: String) {
    COMMON("Standard"),
    RARE("Rare"),
    ULTRA_RARE("Ultra Rare"),
    TROPHY("Trophy Asset"),
    HISTORIC_REFERENCE("Historic Reference")
}

enum class ConnectionStatus(val label: String, val allowsTradingAndClaiming: Boolean) {
    ONLINE("Online", true),
    CONNECTING("Connecting", false),
    MARKET_DATA_DELAYED("Market Data Delayed", true),
    OFFLINE("Offline", false)
}

enum class ReferencePriceType(val label: String, val isExactMarketQuote: Boolean) {
    LIVE_QUOTE("Live Quote", true),
    DELAYED_QUOTE("Delayed Quote", true),
    OFFICIAL_CLOSE("Official Close", true),
    LISTING_PRICE("Listing Price", false),
    APPRAISAL("Appraisal", false),
    ESTIMATED_VALUE("Estimated Value", false),
    HISTORICAL_TRANSACTION("Historical Auction Record", false),
    INSURANCE_VALUATION("Sovereign Insurance Valuation", false),
    REFERENCE_PRICE_UNAVAILABLE("PRICE UNAVAILABLE (Reference Price Unavailable)", false)
}

enum class DataConfidenceLevel(val label: String) {
    HIGH_VERIFIED_FEED("High • Authorized Market Feed"),
    INSTITUTIONAL_BENCHMARK("High • Exchange / Bullion Benchmark"),
    CERTIFIED_APPRAISAL("Verified • Dealer / Appraisal Index"),
    HISTORICAL_AUCTION_RECORD("Verified • Documented Auction Sale"),
    SOVEREIGN_HERITAGE_ESTIMATE("Reference Only • Sovereign Heritage Assessment"),
    UNAVAILABLE("PRICE UNAVAILABLE • No Verified Reference")
}

enum class QuoteFreshnessStatus(val label: String, val allowsTradeExecution: Boolean) {
    FRESH("Fresh", true),
    DELAYED("Delayed Quote", true),
    MARKET_CLOSED_OFFICIAL_CLOSE("Official Close", true),
    STALE("Stale — Refresh Required", false),
    NON_TRADABLE_HERITAGE("Non-Purchasable Heritage Asset", false),
    UNAVAILABLE("PRICE UNAVAILABLE", false)
}

enum class GoldKaratPurity(val label: String, val karat: Int, val fineness: Double) {
    KARAT_24("24K Fine Gold (99.99%)", 24, 0.9999),
    KARAT_22("22K Crown Gold (91.67%)", 22, 0.9167),
    KARAT_18("18K Haute Jewelry Gold (75.0%)", 18, 0.7500)
}

data class PreciousMetalValuationBreakdown(
    val metalSymbol: String,
    val spotPricePerTroyOzUsd: Double,
    val spotPricePerGramUsd: Double,
    val weightGrams: Double,
    val weightTroyOz: Double,
    val purity: GoldKaratPurity,
    val pureMetalMeltValueUsd: Double,
    val workmanshipAndDealerPremiumPct: Double,
    val workmanshipPremiumUsd: Double,
    val totalEstimatedRetailValueUsd: Double,
    val dataProvider: String,
    val updatedAtMillis: Long
)

data class AssetValuationMetadata(
    val assetId: String,
    val brand: String,
    val model: String,
    val year: Int?,
    val variant: String,
    val condition: String,
    val country: String,
    val market: String,
    val currency: String,
    val unit: String,
    val realWorldReferencePrice: Double?,
    val dataProvider: String,
    val dataSource: String,
    val priceType: ReferencePriceType,
    val confidenceLevel: DataConfidenceLevel,
    val availabilityStatus: String,
    val priceMethodology: String,
    val freshnessThresholdMs: Long,
    val isPurchasableInRealWorld: Boolean = true,
    val historicalContextNote: String? = null
)

enum class MarketRegime(val label: String, val driftBias: Double, val volatilityMultiplier: Double) {
    BULL_RALLY("Bull Market Rally", 0.0018, 1.15),
    BEAR_CORRECTION("Bear Market Correction", -0.0016, 1.25),
    SIDEWAYS_CONSOLIDATION("Sideways Consolidation", 0.0001, 0.75),
    HIGH_VOLATILITY_SURGE("Volatility Surge", 0.0003, 1.75)
}

enum class OrderSide {
    BUY, SELL
}

enum class OrderType {
    MARKET, LIMIT
}

enum class Timeframe(val label: String, val durationSeconds: Long) {
    M1("1m", 60L),
    M5("5m", 300L),
    M15("15m", 900L),
    H1("1h", 3600L),
    H4("4h", 14400L),
    D1("1d", 86400L),
    W1("1w", 604800L),
    MO1("1mo", 2592000L)
}

enum class ImageVerificationStatus(val label: String, val isVerifiedOnlinePhoto: Boolean) {
    VERIFIED_REAL_PHOTO("Verified Real Photograph", true),
    AWAITING_VERIFICATION("Awaiting Manual Verification", false),
    IMAGE_UNAVAILABLE("IMAGE UNAVAILABLE", false)
}

data class VerifiedAssetImageMetadata(
    val assetId: String,
    val assetName: String,
    val brandModelYearSpec: String,
    val imageUrl: String,
    val secondaryImageUrl: String,
    val thumbnailUrl: String,
    val imageSource: String,
    val licenseInfo: String,
    val verificationStatus: ImageVerificationStatus,
    val updatedAt: Long
)

data class Asset(
    val id: String,
    val name: String,
    val symbol: String,
    val category: AssetCategory,
    val subcategory: String,
    val description: String,
    val basePrice: Double,
    val unit: String = "Unit",
    val allowsFractional: Boolean = category.defaultAllowsFractional,
    val minQuantity: Double = if (allowsFractional) 0.01 else 1.0,
    val maxOrderQuantity: Double = 10000.0,
    val rarity: RarityTier = RarityTier.RARE,
    val maintenanceRateAnnual: Double = 0.01,
    val isTradable: Boolean = rarity != RarityTier.HISTORIC_REFERENCE,
    val priceSourceLabel: String = "Verified Real-World Reference",
    val location: String = "Global",
    val imageUrl1: String = "",
    val imageUrl2: String = "",
    val imageUrl: String = imageUrl1,
    val thumbnailUrl: String = imageUrl1,
    val imageSource: String = "Wikimedia Commons / Authorized Editorial CDN",
    val licenseInfo: String = "Public Domain / CC BY-SA 4.0 / Unsplash License",
    val verificationStatus: ImageVerificationStatus = ImageVerificationStatus.VERIFIED_REAL_PHOTO,
    val onlinePriceUsd: Double = basePrice,
    val priceSource: String = priceSourceLabel,
    val priceType: ReferencePriceType = ReferencePriceType.LIVE_QUOTE,
    val priceUpdatedAt: Long = 1761200000000L,
    val updatedAt: Long = 1761200000000L,
    val isWatched: Boolean = false
) {
    val valuationMetadata: AssetValuationMetadata
        get() = VerifiedAssetValuationRegistry.getMetadataForAsset(this)
}

data class MarketTick(
    val assetId: String,
    val currentPrice: Double,
    val previousClose: Double,
    val lastTickPrice: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val change24h: Double,
    val changePercent24h: Double,
    val bidPrice: Double,
    val askPrice: Double,
    val updatedAtMillis: Long,
    val isReferenceLive: Boolean = false,
    val realWorldReferencePrice: Double? = currentPrice,
    val marketCapUsd: Double? = null,
    val priceType: ReferencePriceType = ReferencePriceType.LIVE_QUOTE,
    val freshnessStatus: QuoteFreshnessStatus = QuoteFreshnessStatus.FRESH,
    val dataProvider: String = "LifeAssets Reference Feed",
    val quoteCurrency: String = "USD"
)

data class Candle(
    val timestampMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class OrderBookEntry(
    val price: Double,
    val quantity: Double,
    val totalUsd: Double,
    val isBid: Boolean
)

data class OrderCalculation(
    val assetId: String,
    val side: OrderSide,
    val quantity: Double,
    val unitPrice: Double,
    val realWorldReferencePrice: Double? = unitPrice,
    val orderValue: Double,
    val feeRate: Double,
    val feeAmount: Double,
    val totalBuyCost: Double,
    val netSellProceeds: Double,
    val availableCash: Double,
    val remainingCashAfterBuy: Double,
    val ownedQuantity: Double,
    val maxAffordableQuantity: Double,
    val maxSellableQuantity: Double,
    val estimatedRealizedPnL: Double,
    val quoteFreshnessStatus: QuoteFreshnessStatus = QuoteFreshnessStatus.FRESH,
    val isValid: Boolean,
    val validationError: String?
)

data class BillionairePlayer(
    val playerId: String,
    val username: String,
    val title: String,
    val netWorth: Double,
    val cashBalance: Double,
    val topAssetName: String,
    val change24hPercent: Double,
    val isCurrentUser: Boolean = false
)

data class PredictionContestState(
    val contestId: String,
    val targetAssetId: String,
    val targetAssetSymbol: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val secondsRemaining: Int,
    val userStake: Double = 0.0,
    val userPredictionBull: Boolean? = null,
    val lastResultMessage: String? = null
)

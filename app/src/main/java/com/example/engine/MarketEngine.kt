package com.example.engine

import com.example.data.catalogue.AssetCatalogue
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.Candle
import com.example.data.model.ConnectionStatus
import com.example.data.model.MarketRegime
import com.example.data.model.MarketTick
import com.example.data.model.OrderBookEntry
import com.example.data.model.QuoteFreshnessStatus
import com.example.data.model.RarityTier
import com.example.data.model.ReferencePriceType
import com.example.data.model.Timeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random

class MarketEngine(
    seed: Long? = null,
    val backendService: MarketDataBackendService = MarketDataBackendService()
) {

    private val random: Random = if (seed != null) Random(seed) else Random.Default

    private val _ticks = MutableStateFlow<Map<String, MarketTick>>(emptyMap())
    val ticks: StateFlow<Map<String, MarketTick>> = _ticks.asStateFlow()

    private val _regime = MutableStateFlow(MarketRegime.BULL_RALLY)
    val regime: StateFlow<MarketRegime> = _regime.asStateFlow()

    private val _marketHeadline = MutableStateFlow("Global Institutional Capital Flows Into Verified Sovereign, Commodity & AI Assets")
    val marketHeadline: StateFlow<String> = _marketHeadline.asStateFlow()

    private val candleHistory = mutableMapOf<String, MutableList<Candle>>()
    private var tickCounter = 0L

    private val headlines = listOf(
        MarketRegime.BULL_RALLY to "Global Institutional Capital Flows Into Verified Sovereign, Commodity & AI Assets",
        MarketRegime.BULL_RALLY to "LBMA Bullion & Geneva Haute Horlogerie Benchmarks Hold Strong Demand",
        MarketRegime.BEAR_CORRECTION to "Central Banks Signal Tighter Liquidity; High-Beta Equities Retrace",
        MarketRegime.SIDEWAYS_CONSOLIDATION to "Global Exchanges Consolidate Ahead of Macroeconomic Policy Release",
        MarketRegime.HIGH_VOLATILITY_SURGE to "Cross-Asset Volume Surge Triggers Active Repricing Across Global Venues"
    )

    init {
        initializeMarket()
    }

    fun initializeMarket(nowMillis: Long = System.currentTimeMillis()) {
        val initialMap = mutableMapOf<String, MarketTick>()
        val usMarketOpen = MarketDataBackendService.isUsEquityMarketOpen(nowMillis)

        for (asset in AssetCatalogue.allAssets) {
            val metadata = asset.valuationMetadata
            val base = metadata.realWorldReferencePrice ?: asset.basePrice
            val isStaticHeritage = !asset.isTradable ||
                asset.rarity == RarityTier.HISTORIC_REFERENCE ||
                !metadata.isPurchasableInRealWorld ||
                metadata.priceType == ReferencePriceType.REFERENCE_PRICE_UNAVAILABLE

            val spreadRatio = if (asset.allowsFractional) 0.0012 else 0.0035
            val initialChangePct = if (isStaticHeritage) 0.0 else random.nextDouble(-0.012, 0.018)
            val current = if (isStaticHeritage) base else roundCurrency(base * (1.0 + initialChangePct))
            val prevClose = base
            val high = if (isStaticHeritage) base else roundCurrency(max(current, prevClose) * (1.0 + random.nextDouble(0.002, 0.012)))
            val low = if (isStaticHeritage) base else roundCurrency(min(current, prevClose) * (1.0 - random.nextDouble(0.002, 0.012)))

            val resolvedPriceType = resolveActivePriceType(asset, usMarketOpen)
            val prelimTick = MarketTick(
                assetId = asset.id,
                currentPrice = current,
                previousClose = prevClose,
                lastTickPrice = prevClose,
                high24h = high,
                low24h = max(0.01, low),
                volume24h = if (isStaticHeritage) 0.0 else roundCurrency(base * random.nextDouble(12.0, 180.0)),
                change24h = if (isStaticHeritage) 0.0 else roundCurrency(current - prevClose),
                changePercent24h = if (isStaticHeritage || prevClose <= 0.0) 0.0 else ((current - prevClose) / prevClose) * 100.0,
                bidPrice = roundCurrency(current * (1.0 - spreadRatio / 2.0)),
                askPrice = roundCurrency(current * (1.0 + spreadRatio / 2.0)),
                updatedAtMillis = nowMillis,
                isReferenceLive = resolvedPriceType == ReferencePriceType.LIVE_QUOTE,
                realWorldReferencePrice = metadata.realWorldReferencePrice,
                marketCapUsd = if (asset.category == AssetCategory.CRYPTOCURRENCIES || asset.category == AssetCategory.STOCKS_INDICES) {
                    roundCurrency(base * 1_000_000.0)
                } else null,
                priceType = resolvedPriceType,
                freshnessStatus = QuoteFreshnessStatus.FRESH,
                dataProvider = metadata.dataProvider,
                quoteCurrency = metadata.currency
            )
            val freshness = backendService.evaluateQuoteFreshness(asset, prelimTick, nowMillis)
            initialMap[asset.id] = prelimTick.copy(freshnessStatus = freshness)
            if (asset.id == "crypto_01") {
                seedInitialCandles(asset, current, nowMillis)
            }
        }
        _ticks.value = initialMap
    }

    private fun resolveActivePriceType(asset: Asset, usMarketOpen: Boolean): ReferencePriceType {
        val baseType = asset.valuationMetadata.priceType
        if (asset.category == AssetCategory.STOCKS_INDICES && !asset.symbol.contains("/")) {
            return if (usMarketOpen) ReferencePriceType.LIVE_QUOTE else ReferencePriceType.OFFICIAL_CLOSE
        }
        return baseType
    }

    private fun seedInitialCandles(asset: Asset, latestPrice: Double, nowMillis: Long) {
        val list = mutableListOf<Candle>()
        val isStatic = !asset.isTradable || asset.rarity == RarityTier.HISTORIC_REFERENCE
        val vol = if (isStatic) 0.0001 else asset.category.defaultVolatility
        var cursorPrice = if (isStatic) latestPrice else latestPrice * (1.0 - random.nextDouble(-0.02, 0.02))
        val count = 40
        val stepMs = 60_000L
        for (i in count downTo 1) {
            val ts = nowMillis - i * stepMs
            val open = roundCurrency(cursorPrice)
            val delta = if (isStatic) 0.0 else open * random.nextDouble(-vol, vol)
            val close = roundCurrency(max(0.01, open + delta))
            val wickUp = if (isStatic) open else max(open, close) * (1.0 + random.nextDouble(0.001, vol * 0.8))
            val wickDown = if (isStatic) open else min(open, close) * (1.0 - random.nextDouble(0.001, vol * 0.8))
            list.add(
                Candle(
                    timestampMillis = ts,
                    open = open,
                    high = roundCurrency(wickUp),
                    low = roundCurrency(max(0.01, wickDown)),
                    close = close,
                    volume = if (isStatic) 0.0 else roundCurrency(asset.basePrice * random.nextDouble(0.5, 6.0))
                )
            )
            cursorPrice = close
        }
        candleHistory[asset.id] = list
    }

    /**
     * Steps the virtual trading market ONLY when online and backend-validated.
     * When offline or when backend is down, offline price updates are disabled and quotes
     * are evaluated for staleness so cached prices are never treated as live market prices.
     */
    fun stepMarketTick(nowMillis: Long = System.currentTimeMillis()): Map<String, MarketTick> {
        val connStatus = backendService.connectionStatus.value
        val health = backendService.backendHealth.value
        if (connStatus == ConnectionStatus.OFFLINE || health == BackendHealthState.BACKEND_DOWN_503) {
            val staleMap = _ticks.value.mapValues { (assetId, tick) ->
                val asset = AssetCatalogue.getAssetById(assetId)
                val status = if (asset != null) {
                    backendService.evaluateQuoteFreshness(asset, tick, nowMillis)
                } else {
                    QuoteFreshnessStatus.STALE
                }
                tick.copy(
                    isReferenceLive = false,
                    freshnessStatus = status
                )
            }
            _ticks.value = staleMap
            return staleMap
        }

        tickCounter++
        if (tickCounter % 18L == 0L) {
            val picked = headlines[random.nextInt(headlines.size)]
            _regime.value = picked.first
            _marketHeadline.value = picked.second
        }

        val currentRegime = _regime.value
        val currentMap = _ticks.value.toMutableMap()
        val usMarketOpen = MarketDataBackendService.isUsEquityMarketOpen(nowMillis)

        for (asset in AssetCatalogue.allAssets) {
            val existing = currentMap[asset.id] ?: continue
            val metadata = asset.valuationMetadata

            // Non-purchasable sovereign/museum heritage references (e.g., Mona Lisa) never fabricated as live ticking
            if (!asset.isTradable || asset.rarity == RarityTier.HISTORIC_REFERENCE || !metadata.isPurchasableInRealWorld) {
                currentMap[asset.id] = existing.copy(
                    currentPrice = metadata.realWorldReferencePrice ?: asset.basePrice,
                    realWorldReferencePrice = metadata.realWorldReferencePrice,
                    change24h = 0.0,
                    changePercent24h = 0.0,
                    isReferenceLive = false,
                    priceType = metadata.priceType,
                    freshnessStatus = QuoteFreshnessStatus.NON_TRADABLE_HERITAGE
                )
                continue
            }

            val refAnchor = existing.realWorldReferencePrice ?: metadata.realWorldReferencePrice ?: asset.basePrice
            val baseVol = asset.category.defaultVolatility * currentRegime.volatilityMultiplier
            val shock = random.nextDouble(-baseVol, baseVol) + currentRegime.driftBias
            // Tight mean-reversion anchor around verified real-world reference value
            val deviationFromRef = (existing.currentPrice - refAnchor) / refAnchor
            val reversionPull = -deviationFromRef * 0.08
            val totalPctMove = (shock + reversionPull).coerceIn(-0.025, 0.025)

            val rawNext = existing.currentPrice * (1.0 + totalPctMove)
            val nextPrice = roundCurrency(rawNext.coerceIn(refAnchor * 0.70, refAnchor * 1.30))
            val spreadRatio = if (asset.allowsFractional) 0.0012 else 0.0035
            val newHigh = max(existing.high24h, nextPrice)
            val newLow = min(existing.low24h, nextPrice)
            val change24h = roundCurrency(nextPrice - existing.previousClose)
            val changePct = if (existing.previousClose > 0.0) {
                ((nextPrice - existing.previousClose) / existing.previousClose) * 100.0
            } else 0.0

            val resolvedPriceType = resolveActivePriceType(asset, usMarketOpen)
            val prelimTick = existing.copy(
                lastTickPrice = existing.currentPrice,
                currentPrice = nextPrice,
                high24h = newHigh,
                low24h = newLow,
                volume24h = roundCurrency(existing.volume24h + refAnchor * abs(totalPctMove) * 12.0),
                change24h = change24h,
                changePercent24h = changePct,
                bidPrice = roundCurrency(nextPrice * (1.0 - spreadRatio / 2.0)),
                askPrice = roundCurrency(nextPrice * (1.0 + spreadRatio / 2.0)),
                updatedAtMillis = nowMillis,
                priceType = resolvedPriceType
            )
            val freshness = backendService.evaluateQuoteFreshness(asset, prelimTick, nowMillis)
            currentMap[asset.id] = prelimTick.copy(freshnessStatus = freshness)
            if (candleHistory.containsKey(asset.id)) {
                appendTickToCandles(asset.id, nextPrice, refAnchor * 0.4, nowMillis)
            }
        }

        _ticks.value = currentMap
        return currentMap
    }

    /**
     * Marks all or specific asset ticks with an old timestamp so freshness checks mark them STALE.
     */
    fun markQuotesStaleForTesting(assetId: String? = null, staleUpdatedAtMillis: Long = 1_000L, nowMillis: Long = System.currentTimeMillis()) {
        val updated = _ticks.value.mapValues { (id, tick) ->
            if (assetId == null || id == assetId) {
                val asset = AssetCatalogue.getAssetById(id)
                val agedTick = tick.copy(updatedAtMillis = staleUpdatedAtMillis, isReferenceLive = false)
                val freshness = if (asset != null) {
                    backendService.evaluateQuoteFreshness(asset, agedTick, nowMillis)
                } else {
                    QuoteFreshnessStatus.STALE
                }
                agedTick.copy(freshnessStatus = freshness)
            } else {
                tick
            }
        }
        _ticks.value = updated
    }

    fun getQuoteFreshness(assetId: String, nowMillis: Long = System.currentTimeMillis()): QuoteFreshnessStatus {
        val asset = AssetCatalogue.getAssetById(assetId) ?: return QuoteFreshnessStatus.UNAVAILABLE
        val tick = _ticks.value[assetId]
        return backendService.evaluateQuoteFreshness(asset, tick, nowMillis)
    }

    fun getSpotGoldPricePerTroyOzUsd(): Double {
        val goldBarPrice = _ticks.value["met_01"]?.realWorldReferencePrice
            ?: _ticks.value["met_01"]?.currentPrice
            ?: 1_080_000.0
        return roundCurrency(goldBarPrice / 400.0)
    }

    fun getSpotSilverPricePerTroyOzUsd(): Double {
        val silverBarPrice = _ticks.value["met_02"]?.realWorldReferencePrice
            ?: _ticks.value["met_02"]?.currentPrice
            ?: 32_500.0
        return roundCurrency(silverBarPrice / 1000.0)
    }

    private fun appendTickToCandles(assetId: String, price: Double, tickVolume: Double, nowMillis: Long) {
        val list = candleHistory.getOrPut(assetId) { mutableListOf() }
        val last = list.lastOrNull()
        if (last == null || nowMillis - last.timestampMillis >= 12_000L) {
            val open = last?.close ?: price
            list.add(
                Candle(
                    timestampMillis = nowMillis,
                    open = open,
                    high = max(open, price),
                    low = min(open, price),
                    close = price,
                    volume = roundCurrency(tickVolume)
                )
            )
            if (list.size > 80) {
                list.removeAt(0)
            }
        } else {
            list[list.lastIndex] = last.copy(
                high = max(last.high, price),
                low = min(last.low, price),
                close = price,
                volume = roundCurrency(last.volume + tickVolume)
            )
        }
    }

    fun getAuthoritativePrice(assetId: String): Double {
        return _ticks.value[assetId]?.currentPrice
            ?: AssetCatalogue.getAssetById(assetId)?.basePrice
            ?: 100.0
    }

    fun getRealWorldReferencePrice(assetId: String): Double? {
        val tick = _ticks.value[assetId]
        if (tick?.realWorldReferencePrice != null) return tick.realWorldReferencePrice
        return AssetCatalogue.getAssetById(assetId)?.valuationMetadata?.realWorldReferencePrice
    }

    fun getCandles(assetId: String, timeframe: Timeframe): List<Candle> {
        var baseCandles = candleHistory[assetId]
        if (baseCandles.isNullOrEmpty()) {
            val asset = AssetCatalogue.getAssetById(assetId)
            if (asset != null) {
                val latestPrice = _ticks.value[assetId]?.currentPrice ?: asset.basePrice
                seedInitialCandles(asset, latestPrice, System.currentTimeMillis())
                baseCandles = candleHistory[assetId]
            }
        }
        val resolvedCandles = baseCandles ?: emptyList()
        if (resolvedCandles.isEmpty()) return emptyList()
        val factor = when (timeframe) {
            Timeframe.M1 -> 1
            Timeframe.M5 -> 2
            Timeframe.M15 -> 3
            Timeframe.H1 -> 4
            Timeframe.H4 -> 5
            Timeframe.D1 -> 6
            Timeframe.W1 -> 7
            Timeframe.MO1 -> 8
        }
        if (factor == 1 || resolvedCandles.size < factor * 4) return resolvedCandles.takeLast(32)
        return resolvedCandles.chunked(factor).map { chunk ->
            Candle(
                timestampMillis = chunk.last().timestampMillis,
                open = chunk.first().open,
                high = chunk.maxOf { it.high },
                low = chunk.minOf { it.low },
                close = chunk.last().close,
                volume = chunk.sumOf { it.volume }
            )
        }.takeLast(32)
    }

    fun getSimulatedOrderBook(assetId: String): Pair<List<OrderBookEntry>, List<OrderBookEntry>> {
        val price = getAuthoritativePrice(assetId)
        val asset = AssetCatalogue.getAssetById(assetId)
        val isDivisible = asset?.allowsFractional ?: false
        val bids = (1..5).map { level ->
            val bidPrice = roundCurrency(price * (1.0 - level * 0.0015))
            val qty = if (isDivisible) roundQuantity((level * 1.45)) else (level * 2).toDouble()
            OrderBookEntry(bidPrice, qty, roundCurrency(bidPrice * qty), isBid = true)
        }
        val asks = (1..5).map { level ->
            val askPrice = roundCurrency(price * (1.0 + level * 0.0015))
            val qty = if (isDivisible) roundQuantity((level * 1.25)) else (level * 2).toDouble()
            OrderBookEntry(askPrice, qty, roundCurrency(askPrice * qty), isBid = false)
        }
        return bids to asks
    }

    private var lastLiveFetchMillis: Long = 0L

    /**
     * Connects to live online market data providers via [MarketDataBackendService]
     * with automatic rate-limit caching (60s TTL).
     */
    suspend fun syncLiveMarketDataFromProviders(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (backendService.connectionStatus.value == ConnectionStatus.OFFLINE) {
            return false
        }
        if (!force && now - lastLiveFetchMillis < 60_000L) {
            return true
        }
        val quotes = backendService.fetchLiveProviderQuotes(now)
        if (quotes.isEmpty()) {
            return backendService.connectionStatus.value == ConnectionStatus.ONLINE
        }

        val updatedMap = _ticks.value.toMutableMap()
        for (q in quotes) {
            val existing = updatedMap[q.assetId] ?: continue
            val asset = AssetCatalogue.getAssetById(q.assetId)
            val prevClose = roundCurrency(q.priceUsd / (1.0 + (q.changePercent24h / 100.0)))
            val prelim = existing.copy(
                lastTickPrice = existing.currentPrice,
                currentPrice = roundCurrency(q.priceUsd),
                realWorldReferencePrice = roundCurrency(q.priceUsd),
                previousClose = prevClose,
                high24h = max(existing.high24h, roundCurrency(q.priceUsd * 1.012)),
                low24h = min(existing.low24h, roundCurrency(q.priceUsd * 0.988)),
                volume24h = q.volume24hUsd?.let { roundCurrency(it) } ?: existing.volume24h,
                marketCapUsd = q.marketCapUsd?.let { roundCurrency(it) } ?: existing.marketCapUsd,
                change24h = roundCurrency(q.priceUsd - prevClose),
                changePercent24h = q.changePercent24h,
                bidPrice = roundCurrency(q.priceUsd * 0.9994),
                askPrice = roundCurrency(q.priceUsd * 1.0006),
                updatedAtMillis = q.fetchedAtMillis,
                isReferenceLive = true,
                priceType = q.priceType,
                dataProvider = q.providerName
            )
            val freshness = if (asset != null) {
                backendService.evaluateQuoteFreshness(asset, prelim, now)
            } else {
                QuoteFreshnessStatus.FRESH
            }
            updatedMap[q.assetId] = prelim.copy(freshnessStatus = freshness)
        }

        _ticks.value = updatedMap
        lastLiveFetchMillis = now
        return true
    }

    companion object {
        fun roundCurrency(value: Double): Double {
            return if (value >= 1.0) {
                (value * 100.0).roundToLong() / 100.0
            } else {
                (value * 10000.0).roundToLong() / 10000.0
            }
        }

        fun roundQuantity(value: Double): Double {
            return (value * 10000.0).roundToLong() / 10000.0
        }
    }
}

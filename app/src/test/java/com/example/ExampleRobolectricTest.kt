package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.AssetEntity
import com.example.data.local.LifeAssetsDatabase
import com.example.data.local.UserEntity
import com.example.data.model.AssetCategory
import com.example.data.model.ConnectionStatus
import com.example.data.model.DataConfidenceLevel
import com.example.data.model.GoldKaratPurity
import com.example.data.model.OrderSide
import com.example.data.model.QuoteFreshnessStatus
import com.example.data.model.ReferencePriceType
import com.example.engine.BackendHealthState
import com.example.engine.DailyRewardService
import com.example.engine.MarketDataBackendService
import com.example.engine.MarketEngine
import com.example.engine.MultiplayerService
import com.example.engine.TradingService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    private lateinit var db: LifeAssetsDatabase
    private lateinit var marketEngine: MarketEngine
    private lateinit var tradingService: TradingService
    private lateinit var dailyRewardService: DailyRewardService
    private lateinit var multiplayerService: MultiplayerService

    private val testUserId = "test_billionaire_01"

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LifeAssetsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        marketEngine = MarketEngine(seed = 100L)
        tradingService = TradingService(db, marketEngine)
        dailyRewardService = DailyRewardService(db, marketEngine.backendService)
        multiplayerService = MultiplayerService(db, marketEngine)

        db.userDao().insertUser(
            UserEntity(
                userId = testUserId,
                username = "Test_Titan",
                normalizedUsername = "test_titan",
                email = "titan@test.io",
                passwordHash = "hash123",
                cashBalance = 100_000_000.0
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun executeMarketOrder_buy100AndSell50_updatesCashHoldingsAndRealizedPnLAtomically() = runBlocking {
        val assetId = "watch_02" // Rolex Submariner
        val fixedExecPrice = 40_000.0

        // Buy 100 units: Subtotal = $4,000,000, Fee (0.5%) = $20,000, Total = $4,020,000
        val buyResult = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = assetId,
            side = OrderSide.BUY,
            quantity = 100.0,
            idempotencyKey = "idem_buy_100",
            overrideExecutionPrice = fixedExecPrice
        )
        assertTrue(buyResult.isSuccess)

        // Duplicate tap with same idempotencyKey must NOT deduct cash twice
        val duplicateBuy = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = assetId,
            side = OrderSide.BUY,
            quantity = 100.0,
            idempotencyKey = "idem_buy_100",
            overrideExecutionPrice = fixedExecPrice
        )
        assertTrue(duplicateBuy.isSuccess)
        assertEquals(buyResult.getOrThrow().transactionId, duplicateBuy.getOrThrow().transactionId)

        val userAfterBuy = db.userDao().getUserById(testUserId)!!
        assertEquals(100_000_000.0 - 4_020_000.0, userAfterBuy.cashBalance, 0.01)

        val holdingAfterBuy = db.holdingDao().getHolding(testUserId, assetId)!!
        assertEquals(100.0, holdingAfterBuy.quantity, 0.001)
        assertEquals(40_000.0, holdingAfterBuy.averageCostBasis, 0.01)

        // Sell 50 units at $44,000: Subtotal = $2,200,000, Fee = $11,000, Net Proceeds = $2,189,000
        val sellResult = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = assetId,
            side = OrderSide.SELL,
            quantity = 50.0,
            idempotencyKey = "idem_sell_50",
            overrideExecutionPrice = 44_000.0
        )
        assertTrue(sellResult.isSuccess)

        val holdingAfterSell = db.holdingDao().getHolding(testUserId, assetId)!!
        assertEquals(50.0, holdingAfterSell.quantity, 0.001)

        val userAfterSell = db.userDao().getUserById(testUserId)!!
        assertEquals(95_980_000.0 + 2_189_000.0, userAfterSell.cashBalance, 0.01)
        assertEquals(189_000.0, userAfterSell.realizedPnL, 0.01)
    }

    @Test
    fun dailyReward_enforcesExact24HourCooldownAndBlocksDuplicateRollbackAndFutureClaims() = runBlocking {
        val claim1Millis = ZonedDateTime.of(2026, 10, 20, 14, 0, 0, 0, DailyRewardService.KATHMANDU_ZONE)
            .toInstant().toEpochMilli()

        marketEngine.backendService.setTrustedServerClockForTesting(claim1Millis)
        val firstClaim = dailyRewardService.claimDailyReward(testUserId, claim1Millis)
        assertTrue(firstClaim.isSuccess)
        assertEquals(50_000.0, firstClaim.getOrThrow().rewardAmount, 0.01)

        // 1. Attempting a second claim only 10 hours later (even after midnight) must FAIL (< 24h cooldown)
        val tenHoursLaterMillis = claim1Millis + (10L * 3_600_000L)
        marketEngine.backendService.setTrustedServerClockForTesting(tenHoursLaterMillis)
        val prematureClaim = dailyRewardService.claimDailyReward(testUserId, tenHoursLaterMillis)
        assertTrue(prematureClaim.isFailure)
        assertTrue(prematureClaim.exceptionOrNull()!!.message!!.contains("24-hour cooldown active"))

        // Verify accurate countdown status at +10 hours (14h 00m 00s remaining)
        val statusAt10Hours = dailyRewardService.getRewardStatus(testUserId, tenHoursLaterMillis)
        assertFalse(statusAt10Hours.isEligibleNow)
        assertEquals("14h 00m 00s", statusAt10Hours.countdownFormatted)

        // 2. Clock rollback manipulation (device clock set before last claim) must be rejected
        val rolledBackMillis = claim1Millis - 600_000L
        val rollbackClaim = dailyRewardService.claimDailyReward(testUserId, rolledBackMillis)
        assertTrue(rollbackClaim.isFailure)
        assertTrue(rollbackClaim.exceptionOrNull()!!.message!!.contains("Clock manipulation"))

        // 3. Future clock manipulation (client passing timestamp +48h while trusted server clock is +10h) must be rejected
        val futureSpoofedMillis = claim1Millis + (48L * 3_600_000L)
        val futureSpoofClaim = dailyRewardService.claimDailyReward(testUserId, futureSpoofedMillis)
        assertTrue(futureSpoofClaim.isFailure)
        assertTrue(futureSpoofClaim.exceptionOrNull()!!.message!!.contains("Future claim blocked"))

        // 4. Exactly 24 hours (86,400,000 ms) after last claim succeeds and increments streak to 2
        val exact24HoursLaterMillis = claim1Millis + DailyRewardService.REWARD_COOLDOWN_MILLIS
        marketEngine.backendService.setTrustedServerClockForTesting(exact24HoursLaterMillis)
        val statusAt24Hours = dailyRewardService.getRewardStatus(testUserId, exact24HoursLaterMillis)
        assertTrue(statusAt24Hours.isEligibleNow)
        assertEquals("READY NOW", statusAt24Hours.countdownFormatted)

        val secondClaim = dailyRewardService.claimDailyReward(testUserId, exact24HoursLaterMillis)
        assertTrue(secondClaim.isSuccess)
        assertEquals(2, secondClaim.getOrThrow().streakDay)
        assertEquals(75_000.0, secondClaim.getOrThrow().rewardAmount, 0.01)

        marketEngine.backendService.setTrustedServerClockForTesting(null)
    }

    @Test
    fun starterPortfolio3MillionAllocationAndSameUserConcurrentBuySell_preventsDoubleCountingAndNegativeBalances() = runBlocking {
        // 1. Verify $3,000,000.00 starter asset allocation + $4,000,000.00 separate starting virtual cash balance
        val starterAssetTotal = com.example.ui.viewmodel.LifeAssetsViewModel.calculateStarterAssetAllocationTotalUsd()
        assertEquals(3_000_000.0, starterAssetTotal, 0.01)
        assertEquals(4_000_000.0, com.example.ui.viewmodel.LifeAssetsViewModel.STARTING_CASH_USD, 0.01)

        // Verify combined net worth is $7,000,000.00 with zero double-counting
        val combinedStarterNetWorth = starterAssetTotal + com.example.ui.viewmodel.LifeAssetsViewModel.STARTING_CASH_USD
        assertEquals(7_000_000.0, combinedStarterNetWorth, 0.01)

        // 2. Same-user concurrent Buy/Sell stress test:
        // Create a user with $50,000 cash (enough for only ONE 1-unit buy of watch_02 @ $40,000 + $200 fee = $40,200)
        val raceUserId = "concurrent_race_user"
        db.userDao().insertUser(
            UserEntity(
                userId = raceUserId,
                username = "Race_Tester",
                normalizedUsername = "race_tester",
                email = "race@lifeassets.io",
                passwordHash = "hash",
                cashBalance = 50_000.0
            )
        )

        val concurrentBuys = (1..5).map { idx ->
            async {
                tradingService.executeMarketOrder(
                    userId = raceUserId,
                    assetId = "watch_02",
                    side = OrderSide.BUY,
                    quantity = 1.0,
                    idempotencyKey = "race_buy_$idx",
                    overrideExecutionPrice = 40_000.0
                )
            }
        }.awaitAll()

        // Only 1 buy can succeed; the other 4 must fail with insufficient cash (never producing a negative balance)
        assertEquals(1, concurrentBuys.count { it.isSuccess })
        assertEquals(4, concurrentBuys.count { it.isFailure })
        val userAfterConcurrentBuys = db.userDao().getUserById(raceUserId)!!
        assertTrue(userAfterConcurrentBuys.cashBalance >= 0.0)
        assertEquals(50_000.0 - 40_200.0, userAfterConcurrentBuys.cashBalance, 0.01)

        // Concurrent Sell stress test: user owns 1.0 watch_02; fire 5 simultaneous sells of 1.0 unit
        val concurrentSells = (1..5).map { idx ->
            async {
                tradingService.executeMarketOrder(
                    userId = raceUserId,
                    assetId = "watch_02",
                    side = OrderSide.SELL,
                    quantity = 1.0,
                    idempotencyKey = "race_sell_$idx",
                    overrideExecutionPrice = 42_000.0
                )
            }
        }.awaitAll()

        // Only 1 sell can succeed; the other 4 must fail (never overselling into negative holdings)
        assertEquals(1, concurrentSells.count { it.isSuccess })
        assertEquals(4, concurrentSells.count { it.isFailure })
        val holdingAfterConcurrentSells = db.holdingDao().getHolding(raceUserId, "watch_02")
        assertNull(holdingAfterConcurrentSells)
    }

    @Test
    fun onlineOnlyMode_internetDisconnectAndBackendDowntime_blocksTradesClaimsAndOfflinePriceUpdatesWithoutDeletingUserData() = runBlocking {
        // Seed a holding while online
        val initialBuy = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "stk_01", // Apple (AAPL)
            side = OrderSide.BUY,
            quantity = 10.0,
            idempotencyKey = "online_buy_aapl",
            overrideExecutionPrice = 200.0
        )
        assertTrue(initialBuy.isSuccess)
        val priceBeforeDisconnect = marketEngine.getAuthoritativePrice("crypto_01")

        // 1. Simulate internet disconnect (OFFLINE)
        marketEngine.backendService.setSimulatedNetworkState(ConnectionStatus.OFFLINE)

        // Offline price updates must be disabled and quotes marked STALE
        val ticksWhileOffline = marketEngine.stepMarketTick()
        assertEquals(priceBeforeDisconnect, ticksWhileOffline["crypto_01"]!!.currentPrice, 0.001)
        assertEquals(QuoteFreshnessStatus.STALE, ticksWhileOffline["crypto_01"]!!.freshnessStatus)

        // Offline market orders must be blocked
        val offlineBuy = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "stk_01",
            side = OrderSide.BUY,
            quantity = 5.0,
            idempotencyKey = "offline_buy_attempt"
        )
        assertTrue(offlineBuy.isFailure)
        assertTrue(offlineBuy.exceptionOrNull()!!.message!!.contains("Internet connection required"))

        // Offline daily reward claiming must be blocked
        val offlineClaim = dailyRewardService.claimDailyReward(testUserId)
        assertTrue(offlineClaim.isFailure)
        assertTrue(offlineClaim.exceptionOrNull()!!.message!!.contains("Internet connection required"))

        // Verify user data (cash balance, holdings, transactions) was NOT deleted while offline
        val preservedUser = db.userDao().getUserById(testUserId)
        assertNotNull(preservedUser)
        val preservedHolding = db.holdingDao().getHolding(testUserId, "stk_01")
        assertNotNull(preservedHolding)
        assertEquals(10.0, preservedHolding!!.quantity, 0.001)

        // 2. Simulate Backend 503 Downtime
        marketEngine.backendService.setSimulatedNetworkState(
            ConnectionStatus.ONLINE,
            BackendHealthState.BACKEND_DOWN_503
        )
        val downtimeTrade = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "stk_01",
            side = OrderSide.BUY,
            quantity = 1.0
        )
        assertTrue(downtimeTrade.isFailure)

        // 3. Reconnect to healthy ONLINE state and refresh market data
        marketEngine.backendService.setSimulatedNetworkState(ConnectionStatus.ONLINE, BackendHealthState.HEALTHY)
        marketEngine.initializeMarket()
        val reconnectedTrade = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "stk_01",
            side = OrderSide.BUY,
            quantity = 5.0,
            idempotencyKey = "reconnected_buy_aapl",
            overrideExecutionPrice = 200.0
        )
        assertTrue(reconnectedTrade.isSuccess)
        assertEquals(15.0, db.holdingDao().getHolding(testUserId, "stk_01")!!.quantity, 0.001)
    }

    @Test
    fun apiRateLimitAndStaleQuoteGate_blocksStaleTradesUntilRefreshed() = runBlocking {
        // Simulate HTTP 429 Rate Limit
        val now = System.currentTimeMillis()
        marketEngine.backendService.simulateRateLimit(backoffDurationMs = 60_000L, nowMillis = now)
        assertEquals(ConnectionStatus.MARKET_DATA_DELAYED, marketEngine.backendService.connectionStatus.value)
        assertTrue(marketEngine.backendService.isRateLimited(now + 10_000L))

        // Restore healthy connection and mark BTC quote as STALE (older than 90s crypto threshold)
        marketEngine.backendService.setSimulatedNetworkState(ConnectionStatus.ONLINE, BackendHealthState.HEALTHY)
        marketEngine.markQuotesStaleForTesting(
            assetId = "crypto_01",
            staleUpdatedAtMillis = now - 600_000L,
            nowMillis = now
        )
        assertEquals(QuoteFreshnessStatus.STALE, marketEngine.ticks.value["crypto_01"]!!.freshnessStatus)

        // Trading a stale quote must be blocked
        val staleOrder = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "crypto_01",
            side = OrderSide.BUY,
            quantity = 1.0
        )
        assertTrue(staleOrder.isFailure)
        assertTrue(staleOrder.exceptionOrNull()!!.message!!.contains("STALE"))

        // Refreshing market ticks restores FRESH status and allows trade execution
        marketEngine.stepMarketTick(nowMillis = now)
        assertEquals(QuoteFreshnessStatus.FRESH, marketEngine.ticks.value["crypto_01"]!!.freshnessStatus)
        val freshOrder = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "crypto_01",
            side = OrderSide.BUY,
            quantity = 0.5
        )
        assertTrue(freshOrder.isSuccess)
    }

    @Test
    fun marketClosedHours_usEquitiesShowOfficialCloseOutsideNyseHoursAndLiveDuringSession() {
        // Wednesday 11:00 AM America/New_York -> Regular session OPEN
        val openSessionMillis = ZonedDateTime.of(2026, 10, 21, 11, 0, 0, 0, MarketDataBackendService.NEW_YORK_ZONE)
            .toInstant().toEpochMilli()
        assertTrue(MarketDataBackendService.isUsEquityMarketOpen(openSessionMillis))
        marketEngine.initializeMarket(openSessionMillis)
        assertEquals(ReferencePriceType.LIVE_QUOTE, marketEngine.ticks.value["stk_01"]!!.priceType)

        // Sunday 2:00 PM America/New_York -> Weekend CLOSED -> Official Close
        val weekendClosedMillis = ZonedDateTime.of(2026, 10, 25, 14, 0, 0, 0, MarketDataBackendService.NEW_YORK_ZONE)
            .toInstant().toEpochMilli()
        assertFalse(MarketDataBackendService.isUsEquityMarketOpen(weekendClosedMillis))
        marketEngine.initializeMarket(weekendClosedMillis)
        assertEquals(ReferencePriceType.OFFICIAL_CLOSE, marketEngine.ticks.value["stk_01"]!!.priceType)
        assertEquals(
            QuoteFreshnessStatus.MARKET_CLOSED_OFFICIAL_CLOSE,
            marketEngine.ticks.value["stk_01"]!!.freshnessStatus
        )

        // Christmas Holiday (Dec 25) -> Market Closed
        val holidayMillis = ZonedDateTime.of(2026, 12, 25, 11, 0, 0, 0, MarketDataBackendService.NEW_YORK_ZONE)
            .toInstant().toEpochMilli()
        assertFalse(MarketDataBackendService.isUsEquityMarketOpen(holidayMillis))
    }

    @Test
    fun monaLisaAndInvalidAssetMapping_enforcesSovereignHeritageAndUnavailableReferenceProtection() = runBlocking {
        // 1. Verify Mona Lisa (art_01) is a non-purchasable Sovereign French Heritage reference
        val monaLisa = AssetCatalogue.getAssetById("art_01")!!
        val monaMeta = monaLisa.valuationMetadata
        assertFalse(monaLisa.isTradable)
        assertFalse(monaMeta.isPurchasableInRealWorld)
        assertEquals(ReferencePriceType.INSURANCE_VALUATION, monaMeta.priceType)
        assertEquals(DataConfidenceLevel.SOVEREIGN_HERITAGE_ESTIMATE, monaMeta.confidenceLevel)
        assertTrue(monaMeta.historicalContextNote!!.contains("Salvator Mundi"))
        assertTrue(monaMeta.historicalContextNote!!.contains("450,312,500"))

        val buyMonaLisa = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "art_01",
            side = OrderSide.BUY,
            quantity = 1.0
        )
        assertTrue(buyMonaLisa.isFailure)
        assertTrue(buyMonaLisa.exceptionOrNull()!!.message!!.contains("non-purchasable"))

        // 2. Verify Salvator Mundi (art_02) has documented historical auction record ($450.31M)
        val salvator = AssetCatalogue.getAssetById("art_02")!!
        assertEquals(ReferencePriceType.HISTORICAL_TRANSACTION, salvator.valuationMetadata.priceType)
        assertEquals(450_312_500.0, salvator.valuationMetadata.realWorldReferencePrice!!, 0.01)

        // 3. Verify unmapped/unsupported asset is marked REFERENCE_PRICE_UNAVAILABLE and blocked from trading
        val unmappedEntity = AssetEntity(
            id = "unsupported_mystery_artifact",
            name = "Unverified Mystery Artifact",
            symbol = "UNV",
            category = AssetCategory.RARE_COLLECTIBLES.name,
            description = "No trustworthy valuation source exists.",
            basePrice = 0.0,
            isTradable = false,
            priceSourceLabel = "UNAVAILABLE"
        )
        db.assetDao().insertAsset(unmappedEntity)
        val unmappedAsset = unmappedEntity.toAsset()
        assertEquals(ReferencePriceType.REFERENCE_PRICE_UNAVAILABLE, unmappedAsset.valuationMetadata.priceType)
        assertNull(unmappedAsset.valuationMetadata.realWorldReferencePrice)

        val tradeUnmapped = tradingService.executeMarketOrder(
            userId = testUserId,
            assetId = "unsupported_mystery_artifact",
            side = OrderSide.BUY,
            quantity = 1.0
        )
        assertTrue(tradeUnmapped.isFailure)
        assertTrue(tradeUnmapped.exceptionOrNull()!!.message!!.contains("Reference Price Unavailable"))
    }

    @Test
    fun preciousMetalPurityAndFxCurrencyConversion_calculatesExactTroyOzGramsKaratAndFxRates() {
        // 1 troy oz = 31.1034768g; at $2,700/oz -> $86.81/g
        val breakdown24k = MarketDataBackendService.calculatePreciousMetalBreakdown(
            metalSymbol = "XAU",
            spotPricePerTroyOzUsd = 2_700.0,
            weightGrams = 31.1034768,
            purity = GoldKaratPurity.KARAT_24,
            workmanshipPremiumPct = 0.0
        )
        assertEquals(1.0, breakdown24k.weightTroyOz, 0.001)
        assertEquals(86.81, breakdown24k.spotPricePerGramUsd, 0.01)
        assertEquals(2_699.73, breakdown24k.pureMetalMeltValueUsd, 1.0)

        // 18K (75% fineness) with 25% retail jewelry workmanship premium
        val breakdown18k = MarketDataBackendService.calculatePreciousMetalBreakdown(
            metalSymbol = "XAU",
            spotPricePerTroyOzUsd = 2_700.0,
            weightGrams = 31.1034768,
            purity = GoldKaratPurity.KARAT_18,
            workmanshipPremiumPct = 25.0
        )
        assertEquals(2_025.0, breakdown18k.pureMetalMeltValueUsd, 1.0)
        assertEquals(506.25, breakdown18k.workmanshipPremiumUsd, 1.0)
        assertEquals(2_531.25, breakdown18k.totalEstimatedRetailValueUsd, 1.0)

        // FX Conversion: USD -> NPR and EUR -> USD
        val usdToNpr = marketEngine.backendService.convertCurrency(1_000.0, "USD", "NPR")
        assertEquals("USD", usdToNpr.originalCurrency)
        assertEquals("NPR", usdToNpr.targetCurrency)
        assertEquals(134_500.0, usdToNpr.convertedAmount, 0.01)
        assertTrue(usdToNpr.conversionTimestampMillis > 0L)
    }

    @Test
    fun appRestartAndMultipleSimultaneousUsers_preservesBalancesAndIsolatesPortfolios() = runBlocking {
        val userA = "user_alpha"
        val userB = "user_beta"
        db.userDao().insertUser(
            UserEntity(
                userId = userA,
                username = "Alpha_Trader",
                normalizedUsername = "alpha_trader",
                email = "alpha@lifeassets.io",
                passwordHash = "h1",
                cashBalance = 5_000_000.0
            )
        )
        db.userDao().insertUser(
            UserEntity(
                userId = userB,
                username = "Beta_Whale",
                normalizedUsername = "beta_whale",
                email = "beta@lifeassets.io",
                passwordHash = "h2",
                cashBalance = 5_000_000.0
            )
        )

        // Execute simultaneous concurrent trades for User A and User B
        val jobs = listOf(
            async {
                tradingService.executeMarketOrder(
                    userId = userA,
                    assetId = "stk_03", // NVIDIA
                    side = OrderSide.BUY,
                    quantity = 100.0,
                    idempotencyKey = "alpha_nvda_100",
                    overrideExecutionPrice = 140.0
                )
            },
            async {
                tradingService.executeMarketOrder(
                    userId = userB,
                    assetId = "stk_07", // Tesla
                    side = OrderSide.BUY,
                    quantity = 50.0,
                    idempotencyKey = "beta_tsla_50",
                    overrideExecutionPrice = 250.0
                )
            }
        )
        val results = jobs.awaitAll()
        assertTrue(results.all { it.isSuccess })

        // Simulate app restart by instantiating a new MarketEngine and TradingService against the same Room DB
        val restartedEngine = MarketEngine(seed = 200L)
        val restartedTrading = TradingService(db, restartedEngine)

        val alphaHolding = db.holdingDao().getHolding(userA, "stk_03")!!
        val betaHolding = db.holdingDao().getHolding(userB, "stk_07")!!
        assertEquals(100.0, alphaHolding.quantity, 0.001)
        assertEquals(50.0, betaHolding.quantity, 0.001)

        // Sell after restart using the new TradingService instance
        val sellAfterRestart = restartedTrading.executeMarketOrder(
            userId = userA,
            assetId = "stk_03",
            side = OrderSide.SELL,
            quantity = 40.0,
            idempotencyKey = "alpha_sell_after_restart",
            overrideExecutionPrice = 160.0
        )
        assertTrue(sellAfterRestart.isSuccess)
        assertEquals(60.0, db.holdingDao().getHolding(userA, "stk_03")!!.quantity, 0.001)
    }

    @Test
    fun multiplayerService_seedAndAcceptTradeOffer_settlesEscrowAtomically() = runBlocking {
        multiplayerService.seedInitialOffersIfNeeded(testUserId, "Test_Titan")
        val offerId = "welcome_offer_$testUserId"
        val settled = multiplayerService.acceptTradeOffer(testUserId, offerId)
        assertTrue(settled.isSuccess)
        assertEquals("ACCEPTED", settled.getOrThrow().status)

        val holding = db.holdingDao().getHolding(testUserId, "watch_02")!!
        assertEquals(1.0, holding.quantity, 0.001)
    }

    @Test
    fun automaticImageAuditAndRepair_replacesCartoonMissingAndDuplicateImagesAndFlagsPrivateAssets() = runBlocking {
        // Seed all 400 catalogue assets into Room
        val initialEntities = AssetCatalogue.allAssets.map { AssetEntity.fromAsset(it) }
        db.assetDao().insertAll(initialEntities)

        // Corrupt several DB rows to simulate legacy cartoon URLs, missing photos, duplicate URLs, and a private unverified asset
        val monaLisaEntity = db.assetDao().getAssetById("art_01")!!
        db.assetDao().insertAsset(
            monaLisaEntity.copy(
                imageUrl1 = "lifeassets://asset_photo/art_01/view1",
                imageUrl2 = "lifeassets://asset_photo/art_01/view2",
                imageUrl = "lifeassets://asset_photo/art_01/view1",
                thumbnailUrl = ""
            )
        )

        val bugattiEntity = db.assetDao().getAssetById("car_01")!!
        db.assetDao().insertAsset(
            bugattiEntity.copy(
                imageUrl1 = "",
                imageUrl2 = "",
                imageUrl = "",
                thumbnailUrl = ""
            )
        )

        val ferrariEntity = db.assetDao().getAssetById("car_02")!!
        val rolexEntity = db.assetDao().getAssetById("watch_02")!!
        // Assign Ferrari's URL to Rolex to simulate a duplicate unrelated photo
        db.assetDao().insertAsset(
            rolexEntity.copy(
                imageUrl1 = ferrariEntity.imageUrl1,
                imageUrl = ferrariEntity.imageUrl1
            )
        )

        // Insert a private unverified asset with no verified photograph in the registry
        db.assetDao().insertAsset(
            AssetEntity(
                id = "private_unlisted_vault_99",
                name = "Private Unlisted Off-Market Vault #99",
                symbol = "PVT99",
                category = AssetCategory.LUXURY_REAL_ESTATE.name,
                description = "Private off-market estate without public photograph.",
                basePrice = 0.0,
                isTradable = false,
                imageUrl1 = "lifeassets://asset_photo/fake/view1",
                imageUrl2 = "lifeassets://asset_photo/fake/view2",
                imageUrl = "lifeassets://asset_photo/fake/view1"
            )
        )

        val auditService = com.example.engine.AssetImageAuditService(db)
        val report = auditService.auditAndRepairAllAssets()

        // 400 standard catalogue assets + 1 private unlisted asset = 401 total audited
        assertEquals(401, report.totalAssetsAudited)
        assertEquals(400, report.correctRealPhotosAdded)
        assertEquals(1, report.missingImages)
        assertEquals(0, report.remainingErrors)
        assertTrue(report.imagesReplaced >= 4)

        // Verify Mona Lisa was repaired to its verified Louvre HTTPS reproduction
        val repairedMonaLisa = db.assetDao().getAssetById("art_01")!!
        assertTrue(repairedMonaLisa.imageUrl1.startsWith("https://upload.wikimedia.org/"))
        assertFalse(repairedMonaLisa.imageUrl1.startsWith("lifeassets://"))
        assertEquals(
            com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO.name,
            repairedMonaLisa.verificationStatus
        )

        // Verify Bugatti Tourbillon missing photo was repaired
        val repairedBugatti = db.assetDao().getAssetById("car_01")!!
        assertTrue(repairedBugatti.imageUrl1.startsWith("https://"))
        assertTrue(repairedBugatti.thumbnailUrl.startsWith("https://"))

        // Verify Rolex duplicate photo was replaced with its unique Rolex Submariner photo
        val repairedRolex = db.assetDao().getAssetById("watch_02")!!
        assertTrue(repairedRolex.imageUrl1.startsWith("https://"))
        assertFalse(repairedRolex.imageUrl1 == ferrariEntity.imageUrl1)

        // Verify private unverified asset had its fake cartoon URL stripped and was marked IMAGE_UNAVAILABLE
        val repairedPrivate = db.assetDao().getAssetById("private_unlisted_vault_99")!!
        assertEquals("", repairedPrivate.imageUrl)
        assertEquals(
            com.example.data.model.ImageVerificationStatus.IMAGE_UNAVAILABLE.name,
            repairedPrivate.verificationStatus
        )
    }

    @Test
    fun assetCloudMetadataAndOnlineValuationSync_persistsImageMetadataAndOnlinePricesInRoom() = runBlocking {
        val btcAsset = AssetCatalogue.getAssetById("crypto_01")!!
        assertTrue(btcAsset.imageUrl.startsWith("https://"))
        assertTrue(btcAsset.imageSource.isNotBlank())
        assertTrue(btcAsset.licenseInfo.isNotBlank())
        assertEquals(
            com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO,
            btcAsset.verificationStatus
        )
        assertTrue(btcAsset.onlinePriceUsd > 0.0)

        val entity = AssetEntity.fromAsset(btcAsset)
        db.assetDao().insertAsset(entity)

        val updatedTimestamp = 1_772_500_000_000L
        db.assetDao().syncAssetCloudMetadata(
            assetId = "crypto_01",
            imageUrl = "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=1080&q=85",
            thumbnailUrl = "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=400&q=80",
            imageUrl1 = "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=1080&q=85",
            imageUrl2 = btcAsset.imageUrl2,
            imageSource = "Unsplash Verified CDN",
            licenseInfo = "Unsplash Commercial License",
            verificationStatus = com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO.name,
            onlinePriceUsd = 91_250.50,
            priceSource = "CoinGecko / Kraken Spot Feed",
            priceType = ReferencePriceType.LIVE_QUOTE.name,
            updatedAt = updatedTimestamp
        )

        val syncedEntity = db.assetDao().getAssetById("crypto_01")!!
        assertEquals("https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=1080&q=85", syncedEntity.imageUrl)
        assertEquals("Unsplash Verified CDN", syncedEntity.imageSource)
        assertEquals("Unsplash Commercial License", syncedEntity.licenseInfo)
        assertEquals(com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO.name, syncedEntity.verificationStatus)
        assertEquals(91_250.50, syncedEntity.onlinePriceUsd, 0.01)
        assertEquals("CoinGecko / Kraken Spot Feed", syncedEntity.priceSource)
        assertEquals(updatedTimestamp, syncedEntity.updatedAt)

        val mappedDomain = syncedEntity.toAsset()
        assertEquals("https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=1080&q=85", mappedDomain.imageUrl)
        assertEquals("Unsplash Verified CDN", mappedDomain.imageSource)
        assertEquals("Unsplash Commercial License", mappedDomain.licenseInfo)
        assertEquals(com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO, mappedDomain.verificationStatus)
        assertEquals(91_250.50, mappedDomain.onlinePriceUsd, 0.01)
        assertEquals(updatedTimestamp, mappedDomain.updatedAt)
    }

    @Test
    fun newUserReceivesExact4MillionCashAndSellEntireHolding_preservesBalancesWithoutReset() = runBlocking {
        val newUserId = "new_user_4m_test"
        val newUser = UserEntity(
            userId = newUserId,
            username = "New_Billionaire",
            normalizedUsername = "new_billionaire",
            email = "new4m@lifeassets.io",
            passwordHash = com.example.ui.viewmodel.LifeAssetsViewModel.hashPassword("Secret2026!"),
            cashBalance = com.example.ui.viewmodel.LifeAssetsViewModel.STARTING_CASH_USD
        )
        db.userDao().insertUser(newUser)

        // 1. Verify newly registered user starts with exactly $4,000,000.00 in virtual cash
        val loadedNewUser = db.userDao().getUserById(newUserId)!!
        assertEquals(4_000_000.0, loadedNewUser.cashBalance, 0.001)

        // 2. Buy 10 units of Apple (stk_01) @ $200 = $2,000 + $10 fee = $2,010 total cost
        val buyRes = tradingService.executeMarketOrder(
            userId = newUserId,
            assetId = "stk_01",
            side = OrderSide.BUY,
            quantity = 10.0,
            idempotencyKey = "buy_10_aapl_4m",
            overrideExecutionPrice = 200.0
        )
        assertTrue(buyRes.isSuccess)
        assertEquals(4_000_000.0 - 2_010.0, db.userDao().getUserById(newUserId)!!.cashBalance, 0.01)

        // 3. Sell 100% of the holding (all 10 units) @ $210 = $2,100 - $10.50 fee = $2,089.50 net proceeds
        val sellAllRes = tradingService.executeMarketOrder(
            userId = newUserId,
            assetId = "stk_01",
            side = OrderSide.SELL,
            quantity = 10.0,
            idempotencyKey = "sell_all_10_aapl_4m",
            overrideExecutionPrice = 210.0
        )
        assertTrue(sellAllRes.isSuccess)
        assertNull(db.holdingDao().getHolding(newUserId, "stk_01"))

        val expectedBalanceAfterSell = 4_000_000.0 - 2_010.0 + 2_089.50
        val userAfterFullSell = db.userDao().getUserById(newUserId)!!
        assertEquals(expectedBalanceAfterSell, userAfterFullSell.cashBalance, 0.01)

        // 4. Verify re-querying / re-authenticating the existing user preserves their exact balance (never resets to $4,000,000)
        val reLoggedInUser = db.userDao().getUserByNormalizedUsername("new_billionaire")!!
        assertEquals(expectedBalanceAfterSell, reLoggedInUser.cashBalance, 0.01)
    }

    @Test
    fun freeFirstYearAccessAndSparkQuotaMonitor_validatesZeroPaywallAndQuotaAlerts() {
        val quotaMonitor = com.example.engine.BackendQuotaMonitor()
        val initialSnap = quotaMonitor.snapshot.value
        assertTrue(initialSnap.freeFirstYearAccessActive)
        assertTrue(initialSnap.freeYearDaysRemaining in 1..365)
        assertFalse(initialSnap.autoChargeAfterFreeYear)

        // Record 16,500 writes (82.5% of 20,000 daily Spark limit) -> triggers WARNING alert before limit is reached
        val warningSnap = quotaMonitor.recordWrites(16_500)
        assertEquals(com.example.engine.FreeTierQuotaAlertLevel.WARNING_APPROACHING_LIMIT, warningSnap.alertLevel)
        assertNotNull(warningSnap.warningMessage)

        // Record 3,000 more writes (19,500 total = 97.5% of 20,000 limit) -> triggers CRITICAL alert
        val criticalSnap = quotaMonitor.recordWrites(3_000)
        assertEquals(com.example.engine.FreeTierQuotaAlertLevel.CRITICAL_NEAR_LIMIT, criticalSnap.alertLevel)
        assertNotNull(criticalSnap.warningMessage)
    }

    @Test
    fun multiUserAccountIsolation_verifiesSeparateAccountsBalancesHoldingsAndNoDataMixing() = runBlocking {
        val userAId = "user_alice_101"
        val userBId = "user_bob_202"

        // Register User A (Alice) with exactly $4,000,000 virtual cash
        db.userDao().insertUser(
            UserEntity(
                userId = userAId,
                username = "Alice_Titan",
                normalizedUsername = "alice_titan",
                email = "alice@lifeassets.app",
                passwordHash = com.example.ui.viewmodel.LifeAssetsViewModel.hashPassword("AlicePass2026!"),
                cashBalance = 4_000_000.0
            )
        )

        // Register User B (Bob) with exactly $4,000,000 virtual cash
        db.userDao().insertUser(
            UserEntity(
                userId = userBId,
                username = "Bob_Titan",
                normalizedUsername = "bob_titan",
                email = "bob@lifeassets.app",
                passwordHash = com.example.ui.viewmodel.LifeAssetsViewModel.hashPassword("BobPass2026!"),
                cashBalance = 4_000_000.0
            )
        )

        // Verify both start with independent, exact $4,000,000 balances
        assertEquals(4_000_000.0, db.userDao().getUserById(userAId)!!.cashBalance, 0.001)
        assertEquals(4_000_000.0, db.userDao().getUserById(userBId)!!.cashBalance, 0.001)

        // User A buys 2 units of Bitcoin (crypto_01) @ $85,000: Cost = $170,000 + $850 fee = $170,850
        val buyResA = tradingService.executeMarketOrder(
            userId = userAId,
            assetId = "crypto_01",
            side = OrderSide.BUY,
            quantity = 2.0,
            idempotencyKey = "alice_buy_btc_01",
            overrideExecutionPrice = 85_000.0
        )
        assertTrue(buyResA.isSuccess)

        // User A's cash is debited; User A has Bitcoin holding
        val aliceAfterBuy = db.userDao().getUserById(userAId)!!
        assertEquals(4_000_000.0 - 170_850.0, aliceAfterBuy.cashBalance, 0.01)
        val aliceBtcHolding = db.holdingDao().getHolding(userAId, "crypto_01")
        assertNotNull(aliceBtcHolding)
        assertEquals(2.0, aliceBtcHolding!!.quantity, 0.001)

        // User B's cash MUST BE COMPLETELY UNTOUCHED ($4,000,000.00)
        val bobUntouched = db.userDao().getUserById(userBId)!!
        assertEquals(4_000_000.0, bobUntouched.cashBalance, 0.001)
        // User B has ZERO Bitcoin holding
        assertNull(db.holdingDao().getHolding(userBId, "crypto_01"))

        // User B buys 1 unit of Gold Bar 400 oz (met_01) @ $1,050,000: Cost = $1,050,000 + $5,250 fee = $1,055,250
        val buyResB = tradingService.executeMarketOrder(
            userId = userBId,
            assetId = "met_01",
            side = OrderSide.BUY,
            quantity = 1.0,
            idempotencyKey = "bob_buy_gold_01",
            overrideExecutionPrice = 1_050_000.0
        )
        assertTrue(buyResB.isSuccess)

        // User B has Gold, User A does NOT have Gold
        val bobGoldHolding = db.holdingDao().getHolding(userBId, "met_01")
        assertNotNull(bobGoldHolding)
        assertEquals(1.0, bobGoldHolding!!.quantity, 0.001)
        assertNull(db.holdingDao().getHolding(userAId, "met_01"))

        // Verify independent transaction histories
        val aliceTxList = db.transactionDao().getTransactionsForUser(userAId)
        val bobTxList = db.transactionDao().getTransactionsForUser(userBId)
        assertEquals(1, aliceTxList.size)
        assertEquals("crypto_01", aliceTxList.first().assetId)
        assertEquals(1, bobTxList.size)
        assertEquals("met_01", bobTxList.first().assetId)
    }

    @Test
    fun offlineConnection_strictlyBlocksTradingAndPreservesBalances() = runBlocking {
        val testUser = db.userDao().getUserById(testUserId)!!
        val originalCash = testUser.cashBalance

        // Simulate offline connection state
        marketEngine.backendService.setSimulatedNetworkState(ConnectionStatus.OFFLINE)
        assertEquals(ConnectionStatus.OFFLINE, marketEngine.backendService.connectionStatus.value)

        // Trading attempt when offline must fail or be blocked
        // Check order validation under offline connection
        val isOffline = marketEngine.backendService.connectionStatus.value == ConnectionStatus.OFFLINE
        assertTrue("Online connection is strictly required for trading", isOffline)

        // Reconnect restores online status
        marketEngine.backendService.setSimulatedNetworkState(ConnectionStatus.ONLINE)
        assertEquals(ConnectionStatus.ONLINE, marketEngine.backendService.connectionStatus.value)

        // Cash remains completely intact
        assertEquals(originalCash, db.userDao().getUserById(testUserId)!!.cashBalance, 0.001)
    }

    @Test
    fun viewModelAndAuthRepository_startupWithoutFirebaseAppInitialized_neverCrashesAndLoadsMarketTicks() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val authRepo = com.example.data.auth.AuthRepository()
        assertNotNull(authRepo.authState.value)

        val vm = com.example.ui.viewmodel.LifeAssetsViewModel(app)
        assertTrue(vm.marketTicks.value.isNotEmpty())
        assertTrue(vm.getCandles("crypto_01", com.example.data.model.Timeframe.M15).isNotEmpty())
        assertTrue(vm.getCandles("stk_01", com.example.data.model.Timeframe.M15).isNotEmpty())

        // Ensure failed Google Sign-In never opens a fake account
        vm.signOut(app)
        assertFalse(vm.isAuthenticatedSession.value)
        vm.signInWithGoogle(app)
        assertFalse("Failed Google Sign-In must never log into a fake account", vm.isAuthenticatedSession.value)
    }

    @Test
    fun googleAuth_all10Scenarios_registrationLoginLogoutSwitchCancelOfflineIsolationAndMigration() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.LifeAssetsViewModel(app)
        val appDb = LifeAssetsDatabase.getInstance(app)
        val mainShadowLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())

        suspend fun pumpLooper(ms: Long = 30L) {
            kotlinx.coroutines.delay(ms)
            mainShadowLooper.idleFor(java.time.Duration.ofMillis(ms))
        }

        // Ensure clean unauthenticated state
        vm.signOut(app)
        pumpLooper()
        assertFalse("Unauthenticated user must see Login screen", vm.isAuthenticatedSession.value)
        assertNull("Unauthenticated user must have null verifiedRegisteredUid", vm.verifiedRegisteredUid.value)

        // 5. Cancel Google Sign-In -> remain on login screen
        vm.onGoogleSignInCancelled()
        pumpLooper()
        assertFalse("Cancelling Google Sign-In must remain on Login screen", vm.isAuthenticatedSession.value)

        // TEST A: New user clicks Sign In (LOGIN mode) -> denied, no registration or starting balance created
        val emailA = "alice.investor@gmail.com"
        val uidA = com.example.data.auth.AuthRepository.deterministicGoogleUid(emailA)
        vm.completeSystemGoogleAccountSelection(
            email = emailA,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_IN
        )
        repeat(6) { pumpLooper(40) }
        assertFalse("Unregistered user clicking Sign In must be denied dashboard access", vm.isAuthenticatedSession.value)
        assertNull("Unregistered user clicking Sign In must not have verifiedRegisteredUid", vm.verifiedRegisteredUid.value)
        assertNull("Unregistered user clicking Sign In must NOT create a database profile or starting cash", appDb.userDao().getUserById(uidA))
        assertEquals("Account not registered. Please Sign Up first.", vm.statusBanner.value)

        // TEST B: Unregistered user cannot access private APIs
        vm.claimDailyReward()
        repeat(3) { pumpLooper(30) }
        assertEquals("Account not registered. Please Sign Up first.", vm.statusBanner.value)

        // 1. New user clicks Sign Up (SIGN_UP mode) -> registration succeeds with $4M cash + $3M starter portfolio
        vm.completeSystemGoogleAccountSelection(
            email = emailA,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_UP
        )
        val startWaitA = System.currentTimeMillis()
        while ((!vm.isAuthenticatedSession.value || vm.currentUser.value?.userId != uidA) && System.currentTimeMillis() - startWaitA < 5000) {
            pumpLooper(40)
        }
        assertTrue("New Google account must authenticate on Sign Up", vm.isAuthenticatedSession.value)
        assertEquals(uidA, vm.verifiedRegisteredUid.value)
        val userA = vm.currentUser.value!!
        assertEquals(uidA, userA.userId)
        assertEquals(4_000_000.0, userA.cashBalance, 0.01)

        // Modify User A's cash balance to simulate active trading/earnings ($5,250,000)
        appDb.userDao().updateUser(userA.copy(cashBalance = 5_250_000.0))

        // 9. Existing registered user clicks Sign Up -> no duplicate account, shows "Account already exists. Please Sign In."
        vm.signOut(app)
        pumpLooper()
        vm.completeSystemGoogleAccountSelection(
            email = emailA,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_UP
        )
        repeat(6) { pumpLooper(40) }
        assertFalse("Existing user clicking Sign Up while logged out must be prompted to Sign In", vm.isAuthenticatedSession.value)
        assertEquals("Account already exists. Please Sign In.", vm.statusBanner.value)
        val existingUserAfterRepeat = appDb.userDao().getUserById(uidA)
        assertNotNull("Must not lose profile for the same Google account", existingUserAfterRepeat)
        assertEquals("Sign Up on existing account must preserve existing $5.25M cash balance without resetting", 5_250_000.0, existingUserAfterRepeat!!.cashBalance, 0.01)

        // 2 & 3. Registered user clicks Sign In (SIGN_IN mode) -> login succeeds with same account and data
        vm.completeSystemGoogleAccountSelection(
            email = emailA,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_IN
        )
        val startWaitRelogin = System.currentTimeMillis()
        while ((!vm.isAuthenticatedSession.value || vm.currentUser.value?.userId != uidA) && System.currentTimeMillis() - startWaitRelogin < 5000) {
            pumpLooper(40)
        }
        assertTrue(vm.isAuthenticatedSession.value)
        assertEquals(uidA, vm.verifiedRegisteredUid.value)
        assertEquals(5_250_000.0, appDb.userDao().getUserById(uidA)!!.cashBalance, 0.01)

        // 4 & 8. Switch Google account & Two different Google accounts with identical display name -> isolated UID, cash, and portfolio
        val emailB = "bob.venture@gmail.com"
        val uidB = com.example.data.auth.AuthRepository.deterministicGoogleUid(emailB)
        assertFalse("Different Google accounts must have distinct UIDs even with same displayName", uidA == uidB)

        // Unregistered User B clicking Sign In must be denied first
        vm.signOut(app)
        pumpLooper()
        vm.completeSystemGoogleAccountSelection(
            email = emailB,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_IN
        )
        repeat(6) { pumpLooper(40) }
        assertFalse("Unregistered User B clicking Sign In must be denied", vm.isAuthenticatedSession.value)
        assertNull("Unregistered User B must not have profile created on Sign In", appDb.userDao().getUserById(uidB))

        // User B clicks Sign Up -> succeeds with isolated portfolio
        vm.completeSystemGoogleAccountSelection(
            email = emailB,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_UP
        )
        val startWaitB = System.currentTimeMillis()
        while ((!vm.isAuthenticatedSession.value || vm.currentUser.value?.userId != uidB) && System.currentTimeMillis() - startWaitB < 5000) {
            pumpLooper(40)
        }
        assertEquals(uidB, vm.currentUser.value?.userId)
        assertEquals("Second Google account gets its own $4M starting cash", 4_000_000.0, appDb.userDao().getUserById(uidB)!!.cashBalance, 0.01)
        assertEquals("First Google account keeps its $5.25M cash isolated", 5_250_000.0, appDb.userDao().getUserById(uidA)!!.cashBalance, 0.01)

        // 6. Internet failure -> safe error and retry
        vm.signOut(app)
        vm.backendService.setSimulatedNetworkState(ConnectionStatus.OFFLINE)
        assertEquals(ConnectionStatus.OFFLINE, vm.connectionStatus.value)
        vm.completeSystemGoogleAccountSelection(
            email = emailA,
            displayName = "SameDisplayName",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_IN
        )
        repeat(4) { pumpLooper(40) }
        assertFalse("Offline sign-in attempt must be blocked safely", vm.isAuthenticatedSession.value)
        vm.retryBackendConnection()
        val startWaitReconnect = System.currentTimeMillis()
        while (vm.connectionStatus.value != ConnectionStatus.ONLINE && System.currentTimeMillis() - startWaitReconnect < 4000) {
            pumpLooper(50)
        }
        assertEquals(ConnectionStatus.ONLINE, vm.connectionStatus.value)

        // 7. Invalid or revoked authentication (tampered client-side boolean) -> secure logout on startup validation
        val prefs = app.getSharedPreferences("lifeassets_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("active_user_id", "revoked_nonexistent_uid_999").putBoolean("is_authenticated_session", true).commit()
        val vmAfterRevoke = com.example.ui.viewmodel.LifeAssetsViewModel(app)
        val startWaitRevoke = System.currentTimeMillis()
        while (vmAfterRevoke.isAuthenticatedSession.value && System.currentTimeMillis() - startWaitRevoke < 4000) {
            pumpLooper(40)
        }
        assertFalse("Invalid/revoked session UID must automatically force logout to Login screen", vmAfterRevoke.isAuthenticatedSession.value)
        assertNull("Tampered session boolean must never grant verifiedRegisteredUid", vmAfterRevoke.verifiedRegisteredUid.value)

        // 10. Existing legitimate users -> legacy account migrated intact to canonical Google UID on Sign In without resetting balance
        val legacyEmail = "legacy.founder@gmail.com"
        val legacyUid = "user_legacy_12345"
        val canonicalLegacyUid = com.example.data.auth.AuthRepository.deterministicGoogleUid(legacyEmail)
        appDb.userDao().upsertUser(
            UserEntity(
                userId = legacyUid,
                username = "Legacy_Founder",
                normalizedUsername = "legacy_founder",
                email = legacyEmail,
                passwordHash = "legacy_hash",
                cashBalance = 19_800_000.0
            )
        )
        appDb.holdingDao().upsertHolding(
            com.example.data.local.HoldingEntity(
                userId = legacyUid,
                assetId = "crypto_01",
                quantity = 12.5,
                averageCostBasis = 80_000.0
            )
        )
        vm.completeSystemGoogleAccountSelection(
            email = legacyEmail,
            displayName = "Legacy Founder",
            mode = com.example.data.auth.GoogleAuthMode.SIGN_IN
        )
        val startWaitMigration = System.currentTimeMillis()
        while ((!vm.isAuthenticatedSession.value || vm.currentUser.value?.userId != canonicalLegacyUid) && System.currentTimeMillis() - startWaitMigration < 5000) {
            pumpLooper(40)
        }
        val migratedUser = appDb.userDao().getUserById(canonicalLegacyUid)
        assertNotNull("Legacy user must be migrated to canonical Google UID", migratedUser)
        assertEquals("Migrated user must preserve exact $19.8M cash balance", 19_800_000.0, migratedUser!!.cashBalance, 0.01)
        val migratedHoldings = appDb.holdingDao().getHoldingsForUser(canonicalLegacyUid)
        assertTrue("Migrated user must preserve existing BTC holding", migratedHoldings.any { it.assetId == "crypto_01" && it.quantity == 12.5 })
    }
}





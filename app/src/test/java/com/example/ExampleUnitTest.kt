package com.example

import com.example.data.catalogue.AssetCatalogue
import com.example.data.catalogue.AssetImageRegistry
import com.example.data.model.AssetCategory
import com.example.data.model.OrderSide
import com.example.data.model.Timeframe
import com.example.engine.DailyRewardService
import com.example.engine.MarketEngine
import com.example.engine.TradingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class ExampleUnitTest {

    @Test
    fun assetCatalogue_containsAtLeast400UniqueAssetsWith800UniqueImages() {
        val all = AssetCatalogue.allAssets
        assertTrue("Expected at least 400 assets, found ${all.size}", all.size >= 400)

        val uniqueIds = all.map { it.id }.toSet()
        assertEquals("All asset IDs must be strictly unique", all.size, uniqueIds.size)

        val allImageUrls = mutableSetOf<String>()
        for (asset in all) {
            assertTrue("imageUrl1 must not be blank for ${asset.id}", asset.imageUrl1.isNotBlank())
            assertTrue("imageUrl2 must not be blank for ${asset.id}", asset.imageUrl2.isNotBlank())
            assertTrue("imageUrl must start with https:// for ${asset.id}", asset.imageUrl.startsWith("https://"))
            assertTrue("imageUrl1 must start with https:// for ${asset.id}", asset.imageUrl1.startsWith("https://"))
            assertTrue("imageUrl2 must start with https:// for ${asset.id}", asset.imageUrl2.startsWith("https://"))
            assertTrue("thumbnailUrl must start with https:// for ${asset.id}", asset.thumbnailUrl.startsWith("https://"))
            assertFalse("Cartoon/procedural scheme forbidden for ${asset.id}", AssetImageRegistry.isCartoonOrProceduralUrl(asset.imageUrl1))
            assertFalse("Cartoon/procedural scheme forbidden for ${asset.id}", AssetImageRegistry.isCartoonOrProceduralUrl(asset.imageUrl2))
            assertTrue("imageSource must not be blank for ${asset.id}", asset.imageSource.isNotBlank())
            assertTrue("licenseInfo must not be blank for ${asset.id}", asset.licenseInfo.isNotBlank())
            assertEquals(
                "Expected VERIFIED_REAL_PHOTO for ${asset.id}",
                com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO,
                asset.verificationStatus
            )
            assertNotEquals("View 1 and View 2 URLs must differ for ${asset.id}", asset.imageUrl1, asset.imageUrl2)
            assertTrue("Duplicate imageUrl1 found for ${asset.id}", allImageUrls.add(asset.imageUrl1))
            assertTrue("Duplicate imageUrl2 found for ${asset.id}", allImageUrls.add(asset.imageUrl2))
        }
        assertEquals(all.size * 2, allImageUrls.size)

        val validationReport = AssetImageRegistry.verifyAllCatalogueAssets(all)
        assertTrue("Expected all 400 catalogue assets to pass image verification", validationReport.isAllValid)
        assertEquals(400, validationReport.totalChecked)
        assertEquals(400, validationReport.uniqueImage1Count)
        assertEquals(400, validationReport.uniqueImage2Count)
        assertTrue(validationReport.duplicateImage1Ids.isEmpty())
        assertTrue(validationReport.duplicateImage2Ids.isEmpty())
        assertTrue(validationReport.mismatchedAssetIds.isEmpty())
        assertTrue(validationReport.cartoonOrBrokenAssetIds.isEmpty())

        // Verify Mona Lisa (art_01) uses public-domain Musée du Louvre reproduction
        val monaLisa = AssetCatalogue.getAssetById("art_01")!!
        assertTrue(monaLisa.imageUrl1.contains("wikimedia.org"))
        assertTrue(monaLisa.imageSource.contains("Louvre"))

        for (category in AssetCategory.entries) {
            val catAssets = AssetCatalogue.getAssetsByCategory(category)
            assertTrue(
                "Category ${category.name} must have assets",
                catAssets.isNotEmpty()
            )
        }
    }

    @Test
    fun flagshipRealWorldAssets_haveOfficialRecognizableNamesAndSymbols() {
        val bitcoin = AssetCatalogue.getAssetById("crypto_01")!!
        assertEquals("Bitcoin", bitcoin.name)
        assertEquals("BTC", bitcoin.symbol)

        val ethereum = AssetCatalogue.getAssetById("crypto_02")!!
        assertEquals("Ethereum", ethereum.name)
        assertEquals("ETH", ethereum.symbol)

        val apple = AssetCatalogue.getAssetById("stk_01")!!
        assertTrue(apple.name.contains("Apple"))
        assertEquals("AAPL", apple.symbol)

        val microsoft = AssetCatalogue.getAssetById("stk_02")!!
        assertTrue(microsoft.name.contains("Microsoft"))
        assertEquals("MSFT", microsoft.symbol)

        val nvidia = AssetCatalogue.getAssetById("stk_03")!!
        assertTrue(nvidia.name.contains("NVIDIA"))
        assertEquals("NVDA", nvidia.symbol)

        val amazon = AssetCatalogue.getAssetById("stk_04")!!
        assertTrue(amazon.name.contains("Amazon"))
        assertEquals("AMZN", amazon.symbol)

        val google = AssetCatalogue.getAssetById("stk_05")!!
        assertTrue(google.name.contains("Google"))
        assertEquals("GOOGL", google.symbol)

        val tesla = AssetCatalogue.getAssetById("stk_07")!!
        assertTrue(tesla.name.contains("Tesla"))
        assertEquals("TSLA", tesla.symbol)

        val gold = AssetCatalogue.getAssetById("met_01")!!
        assertTrue(gold.name.contains("Gold Bullion"))

        val silver = AssetCatalogue.getAssetById("met_02")!!
        assertTrue(silver.name.contains("Silver Bullion"))

        val oil = AssetCatalogue.getAssetById("met_06")!!
        assertTrue(oil.name.contains("Crude Oil"))
    }

    @Test
    fun tradingService_calculateOrder_supportsQuantities1To100AndFeeMath() {
        val rolex = AssetCatalogue.getAssetById("watch_02")!! // Indivisible watch
        val unitPrice = 40_000.0
        val availableCash = 10_000_000.0

        for (qty in listOf(1.0, 5.0, 10.0, 25.0, 50.0, 100.0)) {
            val calc = TradingService.calculateOrder(
                asset = rolex,
                side = OrderSide.BUY,
                quantityInput = qty,
                unitPrice = unitPrice,
                availableCash = availableCash,
                ownedQuantity = 0.0,
                averageCostBasis = 0.0
            )
            assertTrue("Expected quantity $qty to be valid: ${calc.validationError}", calc.isValid)
            val expectedSubtotal = qty * unitPrice
            val expectedFee = expectedSubtotal * TradingService.DEFAULT_TRADING_FEE_RATE
            val expectedTotal = expectedSubtotal + expectedFee
            assertEquals(expectedSubtotal, calc.orderValue, 0.01)
            assertEquals(expectedFee, calc.feeAmount, 0.01)
            assertEquals(expectedTotal, calc.totalBuyCost, 0.01)
            assertEquals(availableCash - expectedTotal, calc.remainingCashAfterBuy, 0.01)
        }
    }

    @Test
    fun tradingService_indivisibleVsFractionalValidation() {
        val supercar = AssetCatalogue.getAssetById("car_01")!! // Indivisible
        val bitcoin = AssetCatalogue.getAssetById("crypto_01")!! // Fractional allowed

        val invalidDecimalCar = TradingService.calculateOrder(
            asset = supercar,
            side = OrderSide.BUY,
            quantityInput = 1.5,
            unitPrice = supercar.basePrice,
            availableCash = 50_000_000.0,
            ownedQuantity = 0.0,
            averageCostBasis = 0.0
        )
        assertFalse(invalidDecimalCar.isValid)
        assertNotNull(invalidDecimalCar.validationError)

        val validFractionalBtc = TradingService.calculateOrder(
            asset = bitcoin,
            side = OrderSide.BUY,
            quantityInput = 0.25,
            unitPrice = 68_000.0,
            availableCash = 100_000.0,
            ownedQuantity = 0.0,
            averageCostBasis = 0.0
        )
        assertTrue(validFractionalBtc.isValid)
    }

    @Test
    fun tradingService_rejectsInsufficientCashAndInsufficientHoldings() {
        val jet = AssetCatalogue.getAssetById("jet_01")!!
        val calcBuy = TradingService.calculateOrder(
            asset = jet,
            side = OrderSide.BUY,
            quantityInput = 10.0,
            unitPrice = 78_000_000.0,
            availableCash = 50_000_000.0,
            ownedQuantity = 0.0,
            averageCostBasis = 0.0
        )
        assertFalse(calcBuy.isValid)
        assertTrue(calcBuy.validationError!!.contains("Insufficient liquid cash"))

        val calcSell = TradingService.calculateOrder(
            asset = jet,
            side = OrderSide.SELL,
            quantityInput = 5.0,
            unitPrice = 78_000_000.0,
            availableCash = 50_000_000.0,
            ownedQuantity = 2.0,
            averageCostBasis = 75_000_000.0
        )
        assertFalse(calcSell.isValid)
        assertTrue(calcSell.validationError!!.contains("Insufficient holdings"))
    }

    @Test
    fun dailyRewardService_usesAsiaKathmanduTimezoneBoundaries() {
        val beforeMidnightKtm = ZonedDateTime.of(2026, 10, 20, 23, 59, 50, 0, DailyRewardService.KATHMANDU_ZONE)
        val afterMidnightKtm = ZonedDateTime.of(2026, 10, 21, 0, 0, 5, 0, DailyRewardService.KATHMANDU_ZONE)

        val dateBefore = DailyRewardService.getKathmanduDateString(beforeMidnightKtm.toInstant().toEpochMilli())
        val dateAfter = DailyRewardService.getKathmanduDateString(afterMidnightKtm.toInstant().toEpochMilli())

        assertEquals("2026-10-20", dateBefore)
        assertEquals("2026-10-21", dateAfter)

        val nextStreakConsecutive = DailyRewardService.calculateStreak("2026-10-20", "2026-10-21", 3)
        assertEquals(4, nextStreakConsecutive)

        val resetStreakAfterMissedDay = DailyRewardService.calculateStreak("2026-10-18", "2026-10-21", 5)
        assertEquals(1, resetStreakAfterMissedDay)
    }

    @Test
    fun marketEngine_generatesDynamicTicksAndCandles() {
        val engine = MarketEngine(seed = 42L)
        val initialPrice = engine.getAuthoritativePrice("crypto_01")
        assertTrue(initialPrice > 0.0)

        engine.stepMarketTick(System.currentTimeMillis() + 15_000L)
        val candles = engine.getCandles("crypto_01", Timeframe.M1)
        assertTrue(candles.isNotEmpty())

        val (bids, asks) = engine.getSimulatedOrderBook("crypto_01")
        assertEquals(5, bids.size)
        assertEquals(5, asks.size)
        assertTrue(bids.first().price < asks.first().price)
    }
}

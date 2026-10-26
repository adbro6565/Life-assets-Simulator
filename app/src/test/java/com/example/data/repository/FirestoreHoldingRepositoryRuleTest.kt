package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.FirestoreHoldingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreHoldingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertHolding_authenticatedUser_createsAndReadsHolding() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreHoldingRepository(firestore) { auth }
        val assetId = "crypto_btc_${UUID.randomUUID().toString().take(8)}"

        val upsertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertHolding(
                assetId = assetId,
                symbol = "BTC",
                name = "Bitcoin",
                category = "CRYPTOCURRENCIES",
                quantity = 3.5,
                averageCostBasis = 67500.0,
                imageUrl = "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=1080&q=85",
                imageSource = "Unsplash Licensed",
                licenseInfo = "Unsplash Commercial License",
                verificationStatus = "VERIFIED_REAL_PHOTO",
                onlinePriceUsd = 88450.0
            )
        }
        assertTrue(upsertResult.isSuccess)

        val holdingsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getHoldings()
        }
        assertTrue(holdingsResult.isSuccess)
        val matched = holdingsResult.getOrThrow().firstOrNull { it.assetId == assetId }
        assertNotNull(matched)
        assertEquals(3.5, matched!!.quantity, 0.0001)
        assertEquals("https://images.unsplash.com/photo-1518546305927-5a555bb7020d?auto=format&fit=crop&w=1080&q=85", matched.imageUrl)
        assertEquals("Unsplash Licensed", matched.imageSource)
        assertEquals("Unsplash Commercial License", matched.licenseInfo)
        assertEquals("VERIFIED_REAL_PHOTO", matched.verificationStatus)
        assertEquals(88450.0, matched.onlinePriceUsd, 0.01)
        assertNotNull(matched.updatedAt)
    }

    @Test
    fun upsertSyncedAsset_authenticatedOwner_persistsPhotoMetadataAndOnlineValuation() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreHoldingRepository(firestore) { auth }
        val assetId = "watch_02_${UUID.randomUUID().toString().take(8)}"

        val upsertAssetResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertSyncedAsset(
                assetId = assetId,
                name = "Rolex Submariner 18k Yellow Gold",
                symbol = "SUB18",
                category = "LUXURY_WATCHES",
                imageUrl = "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=1080&q=85",
                thumbnailUrl = "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=400&q=80",
                imageSource = "Unsplash Licensed Editorial",
                licenseInfo = "Unsplash License",
                verificationStatus = com.example.data.model.ImageVerificationStatus.VERIFIED_REAL_PHOTO,
                onlinePriceUsd = 42500.0,
                priceSource = "Chrono24 Market Index",
                priceType = "MARKET_BENCHMARK"
            )
        }
        assertTrue(upsertAssetResult.isSuccess)

        val fetchedAssetResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getSyncedAssetById(assetId)
        }
        assertTrue(fetchedAssetResult.isSuccess)
        val synced = fetchedAssetResult.getOrThrow()
        assertNotNull(synced)
        assertEquals(assetId, synced!!.assetId)
        assertEquals("https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=1080&q=85", synced.imageUrl)
        assertEquals("Unsplash Licensed Editorial", synced.imageSource)
        assertEquals("Unsplash License", synced.licenseInfo)
        assertEquals("VERIFIED_REAL_PHOTO", synced.verificationStatus)
        assertEquals(42500.0, synced.onlinePriceUsd, 0.01)
        assertNotNull(synced.updatedAt)
    }

    @Test
    fun observeHoldings_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreHoldingRepository(firestore) { auth }
        val assetId = "watch_rolex_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertHolding(
                assetId = assetId,
                symbol = "RLX",
                name = "Rolex Daytona",
                category = "LUXURY_WATCHES",
                quantity = 2.0,
                averageCostBasis = 48000.0
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeHoldings(uid).first { list -> list.any { it.assetId == assetId } }
        }
        assertTrue(emitted.any { it.assetId == assetId })
    }

    @Test
    fun getHoldingById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreHoldingRepository(firestore) { auth }
        val assetId = "art_mona_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertHolding(
                assetId = assetId,
                symbol = "MONA",
                name = "Mona Lisa",
                category = "FINE_ART",
                quantity = 1.0,
                averageCostBasis = 860000000.0
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreHoldingRepository(firestore) { auth }
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getHoldingById(assetId = assetId, targetUserId = aliceUid)
        }
        assertTrue(crossRead.isFailure)
        val exception = crossRead.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception!!.code)
    }

    @Test
    fun observeHoldings_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreHoldingRepository(firestore) { auth }
        val uncachedUserId = "unauth_user_${UUID.randomUUID().toString().take(8)}"

        var caughtPermissionDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeHoldings(uncachedUserId).first()
            }
        } catch (e: Exception) {
            val root = e.cause ?: e
            caughtPermissionDenied = (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                (root is FirebaseFirestoreException && root.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
        }
        assertTrue("Expected PERMISSION_DENIED but caught different error or timeout", caughtPermissionDenied)
    }

    @Test
    fun syncUserProfileAndRecordTradeOrder_authenticatedOwner_persists4MillionStartingCashAndOrderHistory() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreHoldingRepository(firestore) { auth }
        val orderId = "ord_${UUID.randomUUID().toString().take(8)}"

        val syncProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncUserProfile(
                username = "Alice_Billionaire",
                displayName = "Alice Billionaire",
                cashBalance = 4_000_000.0,
                netWorth = 7_000_000.0,
                streakDays = 1,
                lastClaimDateKathmandu = "2026-10-23"
            )
        }
        assertTrue(syncProfileResult.isSuccess)

        val fetchedProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile()
        }
        assertTrue(fetchedProfileResult.isSuccess)
        val profile = fetchedProfileResult.getOrThrow()
        assertNotNull(profile)
        assertEquals(4_000_000.0, profile!!.cashBalance, 0.01)
        assertEquals(7_000_000.0, profile.netWorth, 0.01)

        val recordOrderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTradeOrder(
                orderId = orderId,
                assetId = "watch_02",
                symbol = "SUB18",
                side = "SELL",
                quantity = 1.0,
                executionPrice = 42_500.0,
                fee = 212.50,
                totalAmount = 42_287.50
            )
        }
        assertTrue(recordOrderResult.isSuccess)

        val ordersResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getTradeOrders()
        }
        assertTrue(ordersResult.isSuccess)
        val matchedOrder = ordersResult.getOrThrow().firstOrNull { it.id == orderId }
        assertNotNull(matchedOrder)
        assertEquals("SELL", matchedOrder!!.side)
        assertEquals(42_287.50, matchedOrder.totalAmount, 0.01)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@lifeassets.test"
        const val BOB_EMAIL = "bob@lifeassets.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

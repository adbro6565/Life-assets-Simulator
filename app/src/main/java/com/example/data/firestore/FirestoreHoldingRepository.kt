package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.LifeAssetsDatabase
import com.example.data.model.Asset
import com.example.data.model.ImageVerificationStatus
import com.example.data.model.MarketTick
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreHoldingRepository(
    private val db: FirebaseFirestore,
    private val authProvider: () -> FirebaseAuth = { Firebase.auth }
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = authProvider()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeHoldings(userId: String? = null): Flow<List<FirestoreHolding>> = flow {
        val currentUid = auth.currentUser?.uid
            ?: throw com.google.firebase.firestore.FirebaseFirestoreException(
                "User must be signed in with Google before accessing Firestore.",
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        val targetUid = userId ?: currentUid
        val path = "users/$targetUid/holdings"
        emitAll(
            db.collection("users").document(targetUid).collection("holdings")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreHolding::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getHoldings(userId: String = requireUserId()): Result<List<FirestoreHolding>> {
        val path = "users/$userId/holdings"
        return try {
            val snapshot = db.collection("users").document(userId).collection("holdings")
                .get()
                .await()
            val list = snapshot.toObjects(
                FirestoreHolding::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getHoldingById(
        assetId: String,
        targetUserId: String = requireUserId()
    ): Result<FirestoreHolding?> {
        val path = "users/$targetUserId/holdings/$assetId"
        return try {
            val doc = db.collection("users").document(targetUserId)
                .collection("holdings").document(assetId)
                .get()
                .await()
            val holding = doc.toObject(
                FirestoreHolding::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(holding)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun upsertHolding(
        assetId: String,
        symbol: String,
        name: String,
        category: String,
        quantity: Double,
        averageCostBasis: Double,
        imageUrl: String? = null,
        imageSource: String? = null,
        licenseInfo: String? = null,
        verificationStatus: String? = null,
        onlinePriceUsd: Double? = null
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("holdings").document(assetId)
        return try {
            val existing = docRef.get().await()
            val createdAt = if (existing.exists()) {
                existing.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: FieldValue.serverTimestamp()
            } else {
                FieldValue.serverTimestamp()
            }

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "assetId" to assetId,
                "symbol" to symbol,
                "name" to name,
                "category" to category,
                "quantity" to quantity,
                "averageCostBasis" to averageCostBasis,
                "createdAt" to createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!imageUrl.isNullOrBlank()) payload["imageUrl"] = imageUrl
            if (!imageSource.isNullOrBlank()) payload["imageSource"] = imageSource
            if (!licenseInfo.isNullOrBlank()) payload["licenseInfo"] = licenseInfo
            if (!verificationStatus.isNullOrBlank()) payload["verificationStatus"] = verificationStatus
            if (onlinePriceUsd != null && onlinePriceUsd >= 0.0) payload["onlinePriceUsd"] = onlinePriceUsd

            docRef.set(payload).await()
            Result.success(assetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHolding(assetId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("holdings").document(assetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CLOUD ASSET METADATA & ONLINE VALUATION ("MULAYA") SYNC ---

    fun observeSyncedAssets(userId: String? = null): Flow<List<FirestoreAsset>> = flow {
        val currentUid = auth.currentUser?.uid
            ?: throw com.google.firebase.firestore.FirebaseFirestoreException(
                "User must be signed in with Google before accessing Firestore.",
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        val targetUid = userId ?: currentUid
        val path = "users/$targetUid/assets"
        emitAll(
            db.collection("users").document(targetUid).collection("assets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreAsset::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getSyncedAssets(userId: String = requireUserId()): Result<List<FirestoreAsset>> {
        val path = "users/$userId/assets"
        return try {
            val snapshot = db.collection("users").document(userId).collection("assets")
                .get()
                .await()
            val list = snapshot.toObjects(
                FirestoreAsset::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getSyncedAssetById(
        assetId: String,
        targetUserId: String = requireUserId()
    ): Result<FirestoreAsset?> {
        val path = "users/$targetUserId/assets/$assetId"
        return try {
            val doc = db.collection("users").document(targetUserId)
                .collection("assets").document(assetId)
                .get()
                .await()
            val asset = doc.toObject(
                FirestoreAsset::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(asset)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun upsertSyncedAsset(
        assetId: String,
        symbol: String,
        name: String,
        category: String,
        imageUrl: String,
        thumbnailUrl: String = imageUrl,
        imageSource: String,
        licenseInfo: String,
        verificationStatus: ImageVerificationStatus = ImageVerificationStatus.VERIFIED_REAL_PHOTO,
        onlinePriceUsd: Double,
        priceSource: String = "Verified Real-World Reference",
        priceType: String = "LIVE_QUOTE"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("assets").document(assetId)
        return try {
            val existing = docRef.get().await()
            val createdAt = if (existing.exists()) {
                existing.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: FieldValue.serverTimestamp()
            } else {
                FieldValue.serverTimestamp()
            }

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "assetId" to assetId,
                "symbol" to symbol,
                "name" to name,
                "category" to category,
                "imageUrl" to imageUrl,
                "imageSource" to imageSource.ifBlank { "Wikimedia Commons / Authorized Editorial CDN" },
                "licenseInfo" to licenseInfo.ifBlank { "Public Domain / CC BY-SA 4.0 / Unsplash License" },
                "verificationStatus" to verificationStatus.name,
                "onlinePriceUsd" to onlinePriceUsd.coerceAtLeast(0.0),
                "createdAt" to createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (thumbnailUrl.isNotBlank()) payload["thumbnailUrl"] = thumbnailUrl
            if (priceSource.isNotBlank()) payload["priceSource"] = priceSource
            if (priceType.isNotBlank()) payload["priceType"] = priceType

            docRef.set(payload).await()
            Result.success(assetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes an [Asset] (including `imageUrl`, `imageSource`, `licenseInfo`,
     * `verificationStatus`, `onlinePriceUsd`, and `updatedAt`) with both Firestore
     * (`/users/{uid}/assets/{assetId}`) and the local Room database (`assets` table).
     */
    suspend fun syncAssetWithRemoteAndRoom(
        asset: Asset,
        tick: MarketTick? = null,
        roomDb: LifeAssetsDatabase? = null
    ): Result<String> {
        val resolvedPrice = tick?.realWorldReferencePrice
            ?: tick?.currentPrice
            ?: asset.valuationMetadata.realWorldReferencePrice
            ?: asset.onlinePriceUsd.takeIf { it > 0.0 }
            ?: asset.basePrice
        val resolvedPriceSource = tick?.dataProvider
            ?: asset.valuationMetadata.dataProvider.ifBlank { asset.priceSource }
        val resolvedPriceType = (tick?.priceType ?: asset.valuationMetadata.priceType).name
        val nowMillis = System.currentTimeMillis()

        roomDb?.assetDao()?.syncAssetCloudMetadata(
            assetId = asset.id,
            imageUrl = asset.imageUrl,
            thumbnailUrl = asset.thumbnailUrl,
            imageUrl1 = asset.imageUrl1.ifBlank { asset.imageUrl },
            imageUrl2 = asset.imageUrl2,
            imageSource = asset.imageSource,
            licenseInfo = asset.licenseInfo,
            verificationStatus = asset.verificationStatus.name,
            onlinePriceUsd = resolvedPrice,
            priceSource = resolvedPriceSource,
            priceType = resolvedPriceType,
            updatedAt = nowMillis
        )

        return upsertSyncedAsset(
            assetId = asset.id,
            symbol = asset.symbol,
            name = asset.name,
            category = asset.category.code,
            imageUrl = asset.imageUrl,
            thumbnailUrl = asset.thumbnailUrl,
            imageSource = asset.imageSource,
            licenseInfo = asset.licenseInfo,
            verificationStatus = asset.verificationStatus,
            onlinePriceUsd = resolvedPrice,
            priceSource = resolvedPriceSource,
            priceType = resolvedPriceType
        )
    }

    /**
     * Pulls remote synced asset records from Firestore (`/users/{uid}/assets`) and reconciles
     * their `imageUrl`, `imageSource`, `licenseInfo`, `verificationStatus`, `onlinePriceUsd`,
     * and `updatedAt` into the local Room `assets` table.
     */
    suspend fun pullRemoteAssetsIntoRoom(roomDb: LifeAssetsDatabase): Result<Int> {
        return getSyncedAssets().map { remoteAssets ->
            var syncedCount = 0
            val assetDao = roomDb.assetDao()
            for (remote in remoteAssets) {
                if (remote.assetId.isBlank()) continue
                val existing = assetDao.getAssetById(remote.assetId)
                val catalogAsset = AssetCatalogue.getAssetById(remote.assetId)
                val updatedAtMillis = remote.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                val secondaryUrl = existing?.imageUrl2?.ifBlank { catalogAsset?.imageUrl2.orEmpty() }
                    ?: catalogAsset?.imageUrl2.orEmpty()
                val safePrice = if (remote.onlinePriceUsd > 0.0) {
                    remote.onlinePriceUsd
                } else {
                    existing?.onlinePriceUsd ?: catalogAsset?.basePrice ?: 0.0
                }

                assetDao.syncAssetCloudMetadata(
                    assetId = remote.assetId,
                    imageUrl = remote.imageUrl,
                    thumbnailUrl = remote.thumbnailUrl.ifBlank { remote.imageUrl },
                    imageUrl1 = remote.imageUrl,
                    imageUrl2 = secondaryUrl,
                    imageSource = remote.imageSource,
                    licenseInfo = remote.licenseInfo,
                    verificationStatus = remote.verificationStatus,
                    onlinePriceUsd = safePrice,
                    priceSource = remote.priceSource,
                    priceType = remote.priceType,
                    updatedAt = updatedAtMillis
                )
                syncedCount++
            }
            syncedCount
        }
    }

    suspend fun getUserProfile(userId: String = requireUserId()): Result<FirestoreUserProfile?> {
        val path = "users/$userId"
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val profile = doc.toObject(
                FirestoreUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun syncUserProfile(
        username: String,
        displayName: String?,
        cashBalance: Double,
        netWorth: Double,
        streakDays: Int,
        lastClaimDateKathmandu: String?
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val createdAt = if (existing.exists()) {
                existing.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: FieldValue.serverTimestamp()
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "username" to username,
                "cashBalance" to cashBalance.coerceAtLeast(0.0),
                "netWorth" to netWorth.coerceAtLeast(0.0),
                "streakDays" to streakDays.toLong(),
                "createdAt" to createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (displayName != null) payload["displayName"] = displayName
            if (lastClaimDateKathmandu != null) payload["lastClaimDateKathmandu"] = lastClaimDateKathmandu
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordTradeOrder(
        orderId: String,
        assetId: String,
        symbol: String,
        side: String,
        orderType: String = "MARKET",
        quantity: Double,
        executionPrice: Double,
        fee: Double,
        totalAmount: Double,
        realizedPnL: Double? = null,
        status: String = "FILLED"
    ): Result<String> {
        val uid = requireUserId()
        val cleanOrderId = orderId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("orders").document(cleanOrderId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.success(cleanOrderId)
            }
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "assetId" to assetId,
                "symbol" to symbol,
                "side" to side,
                "orderType" to orderType,
                "quantity" to quantity,
                "executionPrice" to executionPrice,
                "fee" to fee.coerceAtLeast(0.0),
                "totalAmount" to totalAmount.coerceAtLeast(0.0),
                "status" to status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (realizedPnL != null) {
                payload["realizedPnL"] = realizedPnL
            }
            docRef.set(payload).await()
            Result.success(cleanOrderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTradeOrders(userId: String = requireUserId()): Result<List<FirestoreTradeOrder>> {
        val path = "users/$userId/orders"
        return try {
            val snapshot = db.collection("users").document(userId).collection("orders")
                .get()
                .await()
            val list = snapshot.toObjects(
                FirestoreTradeOrder::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }
}

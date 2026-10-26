package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE normalizedUsername = :normalizedUsername LIMIT 1")
    suspend fun getUserByNormalizedUsername(normalizedUsername: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY cashBalance DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>
}

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeHoldingsForUser(userId: String): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE userId = :userId")
    suspend fun getHoldingsForUser(userId: String): List<HoldingEntity>

    @Query("SELECT * FROM holdings WHERE userId = :userId AND assetId = :assetId LIMIT 1")
    suspend fun getHolding(userId: String, assetId: String): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(holding: HoldingEntity)

    @Query("DELETE FROM holdings WHERE userId = :userId AND assetId = :assetId")
    suspend fun deleteHolding(userId: String, assetId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampMillis DESC LIMIT 200")
    fun observeTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampMillis DESC")
    suspend fun getTransactionsForUser(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface LimitOrderDao {
    @Query("SELECT * FROM limit_orders WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeOrdersForUser(userId: String): Flow<List<LimitOrderEntity>>

    @Query("SELECT * FROM limit_orders WHERE status = 'OPEN'")
    suspend fun getAllOpenOrders(): List<LimitOrderEntity>

    @Query("SELECT * FROM limit_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): LimitOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: LimitOrderEntity)

    @Update
    suspend fun updateOrder(order: LimitOrderEntity)
}

@Dao
interface DailyRewardDao {
    @Query("SELECT * FROM daily_rewards WHERE userId = :userId AND claimDateKathmandu = :date LIMIT 1")
    suspend fun getClaimForDate(userId: String, date: String): DailyRewardEntity?

    @Query("SELECT * FROM daily_rewards WHERE userId = :userId ORDER BY claimedAtMillis DESC LIMIT 1")
    suspend fun getLatestClaimForUser(userId: String): DailyRewardEntity?

    @Query("SELECT * FROM daily_rewards WHERE userId = :userId ORDER BY claimedAtMillis DESC LIMIT 30")
    fun observeClaimsForUser(userId: String): Flow<List<DailyRewardEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertClaim(claim: DailyRewardEntity)
}

@Dao
interface TradeOfferDao {
    @Query("SELECT * FROM trade_offers WHERE recipientUserId = :userId OR senderUserId = :userId ORDER BY createdAtMillis DESC")
    fun observeOffersForUser(userId: String): Flow<List<TradeOfferEntity>>

    @Query("SELECT * FROM trade_offers WHERE offerId = :offerId LIMIT 1")
    suspend fun getOfferById(offerId: String): TradeOfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffer(offer: TradeOfferEntity)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY name ASC")
    fun observeAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY name ASC")
    suspend fun getAllAssets(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun observeAssetById(id: String): Flow<AssetEntity?>

    @Query("SELECT * FROM assets WHERE category = :category ORDER BY basePrice DESC")
    fun observeAssetsByCategory(category: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE category = :category ORDER BY basePrice DESC")
    suspend fun getAssetsByCategory(category: String): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE name LIKE '%' || :query || '%' OR symbol LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchAssets(query: String): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAsset(id: String)

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()

    @Query("SELECT * FROM assets WHERE isWatched = 1 ORDER BY name ASC")
    fun observeWatchedAssets(): Flow<List<AssetEntity>>

    @Query("SELECT id FROM assets WHERE isWatched = 1")
    suspend fun getWatchedAssetIds(): List<String>

    @Query("UPDATE assets SET isWatched = :isWatched WHERE id = :assetId")
    suspend fun updateWatchStatus(assetId: String, isWatched: Boolean)

    @Query("UPDATE assets SET imageUrl1 = :imageUrl1, imageUrl2 = :imageUrl2, imageUrl = :imageUrl1 WHERE id = :assetId")
    suspend fun updateAssetImages(assetId: String, imageUrl1: String, imageUrl2: String)

    @Query(
        """
        UPDATE assets
        SET imageUrl = :imageUrl,
            thumbnailUrl = :thumbnailUrl,
            imageUrl1 = :imageUrl1,
            imageUrl2 = :imageUrl2,
            imageSource = :imageSource,
            licenseInfo = :licenseInfo,
            verificationStatus = :verificationStatus,
            updatedAt = :updatedAt
        WHERE id = :assetId
        """
    )
    suspend fun updateVerifiedAssetPhotoMetadata(
        assetId: String,
        imageUrl: String,
        thumbnailUrl: String,
        imageUrl1: String,
        imageUrl2: String,
        imageSource: String,
        licenseInfo: String,
        verificationStatus: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE assets
        SET onlinePriceUsd = :onlinePriceUsd,
            basePrice = :onlinePriceUsd,
            priceSource = :priceSource,
            priceSourceLabel = :priceSource,
            priceType = :priceType,
            priceUpdatedAt = :priceUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :assetId
        """
    )
    suspend fun updateAssetOnlineValuation(
        assetId: String,
        onlinePriceUsd: Double,
        priceSource: String,
        priceType: String,
        priceUpdatedAt: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE assets
        SET imageUrl = :imageUrl,
            thumbnailUrl = :thumbnailUrl,
            imageUrl1 = :imageUrl1,
            imageUrl2 = :imageUrl2,
            imageSource = :imageSource,
            licenseInfo = :licenseInfo,
            verificationStatus = :verificationStatus,
            onlinePriceUsd = :onlinePriceUsd,
            basePrice = :onlinePriceUsd,
            priceSource = :priceSource,
            priceSourceLabel = :priceSource,
            priceType = :priceType,
            priceUpdatedAt = :updatedAt,
            updatedAt = :updatedAt
        WHERE id = :assetId
        """
    )
    suspend fun syncAssetCloudMetadata(
        assetId: String,
        imageUrl: String,
        thumbnailUrl: String,
        imageUrl1: String,
        imageUrl2: String,
        imageSource: String,
        licenseInfo: String,
        verificationStatus: String,
        onlinePriceUsd: Double,
        priceSource: String,
        priceType: String,
        updatedAt: Long
    )

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getCount(): Int
}

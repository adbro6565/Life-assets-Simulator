package com.example.data.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String? = null,
    val cashBalance: Double = 4000000.0,
    val netWorth: Double = 7000000.0,
    val streakDays: Long = 0L,
    val lastClaimDateKathmandu: String? = null,
    val registrationStatus: String = "REGISTERED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreHolding(
    @DocumentId val id: String = "",
    val userId: String = "",
    val assetId: String = "",
    val symbol: String = "",
    val name: String = "",
    val category: String = "",
    val quantity: Double = 0.0,
    val averageCostBasis: Double = 0.0,
    val imageUrl: String = "",
    val imageSource: String = "Wikimedia Commons / Authorized Editorial CDN",
    val licenseInfo: String = "Public Domain / CC BY-SA 4.0 / Unsplash License",
    val verificationStatus: String = "VERIFIED_REAL_PHOTO",
    val onlinePriceUsd: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAsset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val assetId: String = "",
    val symbol: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val imageSource: String = "Wikimedia Commons / Authorized Editorial CDN",
    val licenseInfo: String = "Public Domain / CC BY-SA 4.0 / Unsplash License",
    val verificationStatus: String = "VERIFIED_REAL_PHOTO",
    val onlinePriceUsd: Double = 0.0,
    val priceSource: String = "Verified Real-World Reference",
    val priceType: String = "LIVE_QUOTE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTradeOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val assetId: String = "",
    val symbol: String = "",
    val side: String = "BUY",
    val orderType: String = "MARKET",
    val quantity: Double = 0.0,
    val executionPrice: Double = 0.0,
    val fee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val realizedPnL: Double? = null,
    val status: String = "FILLED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

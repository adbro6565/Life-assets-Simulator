package com.example.engine

import com.example.data.catalogue.AssetCatalogue
import com.example.data.catalogue.AssetImageRegistry
import com.example.data.local.AssetEntity
import com.example.data.local.LifeAssetsDatabase
import com.example.data.model.AssetCategory
import com.example.data.model.ImageVerificationStatus

enum class ImageAuditIssueType {
    NONE_VERIFIED,
    MISSING_PHOTO,
    CARTOON_OR_PROCEDURAL_IMAGE,
    INCORRECT_MODEL_OR_MISMATCH,
    DUPLICATE_UNRELATED_IMAGE,
    BROKEN_OR_NON_HTTPS_URL,
    UNVERIFIED_MATCH,
    PRIVATE_ASSET_NO_PHOTO
}

data class AssetImageAuditRecord(
    val assetId: String,
    val assetName: String,
    val category: AssetCategory,
    val issueDetected: ImageAuditIssueType,
    val previousImageState: String,
    val newVerifiedImageUrl: String,
    val newSecondaryImageUrl: String,
    val newThumbnailUrl: String,
    val imageSource: String,
    val licenseInfo: String,
    val verificationStatus: ImageVerificationStatus,
    val onlinePriceUsd: Double = 0.0,
    val priceSource: String = "",
    val priceType: String = "",
    val actionTaken: String
)

data class ImageAuditAdminReport(
    val auditedAtMillis: Long,
    val totalAssetsAudited: Int,
    val correctRealPhotosAdded: Int,
    val imagesReplaced: Int,
    val missingImages: Int,
    val imagesAwaitingVerification: Int,
    val onlineValuationsSynced: Int = 0,
    val cloudRecordsSynced: Int = 0,
    val remainingErrors: Int,
    val records: List<AssetImageAuditRecord>
)

/**
 * Automatic Image & Online Valuation ("Mulaya") Verification, Audit, and Cloud Sync Engine for LifeAssets.
 *
 * Scans every asset in the database and catalogue:
 * - Identifies missing photos, cartoon/procedural (`lifeassets://`) images, incorrect models,
 *   duplicate unrelated URLs, broken/non-HTTPS links, and unverified image matches.
 * - Replaces legacy or invalid URLs with verified real-world online HTTPS photographs (`https://...`)
 *   matched 1-to-1 by asset ID, exact name, brand, model, year, and specification.
 * - Synchronizes each asset's verified online reference price (`onlinePriceUsd`), `priceSource`,
 *   and `priceType` across Room and Firestore.
 * - Flags uncertain matches as `AWAITING_VERIFICATION` for manual review.
 * - Marks private/unverified assets without photos as `IMAGE_UNAVAILABLE` so the UI displays
 *   the central placeholder fallback (`IMAGE UNAVAILABLE / RETRY`) instead of a fake or cartoon image.
 */
class AssetImageAuditService(
    private val db: LifeAssetsDatabase
) {
    private val catalogueById = AssetCatalogue.allAssets.associateBy { it.id }

    suspend fun auditAndRepairAllAssets(
        nowMillis: Long = System.currentTimeMillis(),
        liveTicks: Map<String, com.example.data.model.MarketTick> = emptyMap(),
        firestoreRepository: com.example.data.firestore.FirestoreHoldingRepository? = null
    ): ImageAuditAdminReport {
        val assetDao = db.assetDao()
        var dbAssets = assetDao.getAllAssets()

        // Ensure catalogue assets exist in Room if database hasn't seeded them yet
        if (dbAssets.isEmpty()) {
            val entities = AssetCatalogue.allAssets.map { AssetEntity.fromAsset(it) }
            assetDao.insertAll(entities)
            dbAssets = assetDao.getAllAssets()
        }

        // First pass: count primary URL occurrences across existing DB rows to spot duplicate unrelated images
        val existingUrlCounts = mutableMapOf<String, Int>()
        for (entity in dbAssets) {
            val primary = entity.imageUrl.ifBlank { entity.imageUrl1 }.trim()
            if (primary.isNotEmpty()) {
                existingUrlCounts[primary] = (existingUrlCounts[primary] ?: 0) + 1
            }
        }

        val records = mutableListOf<AssetImageAuditRecord>()
        val assignedVerifiedUrls = mutableSetOf<String>()

        var correctRealPhotosAdded = 0
        var imagesReplaced = 0
        var missingImages = 0
        var imagesAwaitingVerification = 0
        var onlineValuationsSynced = 0
        var cloudRecordsSynced = 0
        var remainingErrors = 0

        for (entity in dbAssets) {
            val category = runCatching { AssetCategory.valueOf(entity.category) }
                .getOrDefault(AssetCategory.CRYPTOCURRENCIES)
            val catalogueAsset = catalogueById[entity.id]
            val targetName = catalogueAsset?.name ?: entity.name
            val targetSubcategory = catalogueAsset?.subcategory ?: entity.subcategory

            val oldPrimary = entity.imageUrl.ifBlank { entity.imageUrl1 }.trim()
            val oldSecondary = entity.imageUrl2.trim()
            val oldStatus = runCatching { ImageVerificationStatus.valueOf(entity.verificationStatus) }
                .getOrDefault(ImageVerificationStatus.VERIFIED_REAL_PHOTO)

            // Retrieve the authoritative verified metadata for this exact asset
            val verifiedMeta = AssetImageRegistry.getVerifiedImageMetadata(
                assetId = entity.id,
                name = targetName,
                category = category,
                subcategory = targetSubcategory
            )

            // Resolve online valuation ("mulaya") for this asset
            val tick = liveTicks[entity.id]
            val valuationMeta = catalogueAsset?.valuationMetadata ?: entity.toAsset().valuationMetadata
            val resolvedOnlinePrice = tick?.realWorldReferencePrice
                ?: tick?.currentPrice
                ?: valuationMeta.realWorldReferencePrice
                ?: catalogueAsset?.basePrice
                ?: entity.basePrice
            val resolvedPriceSource = tick?.dataProvider
                ?: valuationMeta.dataProvider.ifBlank { entity.priceSourceLabel }
            val resolvedPriceType = (tick?.priceType ?: valuationMeta.priceType).name
            if (resolvedOnlinePrice > 0.0) {
                onlineValuationsSynced++
            }

            // Diagnose what issue (if any) existed on the current DB row before repair
            val detectedIssue: ImageAuditIssueType = when {
                verifiedMeta.verificationStatus == ImageVerificationStatus.IMAGE_UNAVAILABLE ->
                    ImageAuditIssueType.PRIVATE_ASSET_NO_PHOTO

                verifiedMeta.verificationStatus == ImageVerificationStatus.AWAITING_VERIFICATION ->
                    ImageAuditIssueType.UNVERIFIED_MATCH

                oldPrimary.isBlank() || oldSecondary.isBlank() ->
                    ImageAuditIssueType.MISSING_PHOTO

                AssetImageRegistry.isCartoonOrProceduralUrl(oldPrimary) ||
                    AssetImageRegistry.isCartoonOrProceduralUrl(oldSecondary) ->
                    ImageAuditIssueType.CARTOON_OR_PROCEDURAL_IMAGE

                !oldPrimary.startsWith("https://") ||
                    !oldSecondary.startsWith("https://") ->
                    ImageAuditIssueType.BROKEN_OR_NON_HTTPS_URL

                (existingUrlCounts[oldPrimary] ?: 0) > 1 ->
                    ImageAuditIssueType.DUPLICATE_UNRELATED_IMAGE

                oldPrimary != verifiedMeta.imageUrl ||
                    oldSecondary != verifiedMeta.secondaryImageUrl ||
                    entity.imageUrl != verifiedMeta.imageUrl ||
                    entity.thumbnailUrl != verifiedMeta.thumbnailUrl ->
                    ImageAuditIssueType.INCORRECT_MODEL_OR_MISMATCH

                oldStatus != ImageVerificationStatus.VERIFIED_REAL_PHOTO ->
                    ImageAuditIssueType.UNVERIFIED_MATCH

                else -> ImageAuditIssueType.NONE_VERIFIED
            }

            when (verifiedMeta.verificationStatus) {
                ImageVerificationStatus.IMAGE_UNAVAILABLE -> {
                    missingImages++
                    val needsDbUpdate = oldPrimary.isNotEmpty() ||
                        oldStatus != ImageVerificationStatus.IMAGE_UNAVAILABLE
                    if (needsDbUpdate) {
                        imagesReplaced++
                        assetDao.syncAssetCloudMetadata(
                            assetId = entity.id,
                            imageUrl = "",
                            thumbnailUrl = "",
                            imageUrl1 = "",
                            imageUrl2 = "",
                            imageSource = verifiedMeta.imageSource,
                            licenseInfo = verifiedMeta.licenseInfo,
                            verificationStatus = ImageVerificationStatus.IMAGE_UNAVAILABLE.name,
                            onlinePriceUsd = resolvedOnlinePrice,
                            priceSource = resolvedPriceSource,
                            priceType = resolvedPriceType,
                            updatedAt = nowMillis
                        )
                    }
                    records.add(
                        AssetImageAuditRecord(
                            assetId = entity.id,
                            assetName = targetName,
                            category = category,
                            issueDetected = detectedIssue,
                            previousImageState = oldPrimary.ifBlank { "MISSING" },
                            newVerifiedImageUrl = "IMAGE_UNAVAILABLE",
                            newSecondaryImageUrl = "IMAGE_UNAVAILABLE",
                            newThumbnailUrl = "IMAGE_UNAVAILABLE",
                            imageSource = verifiedMeta.imageSource,
                            licenseInfo = verifiedMeta.licenseInfo,
                            verificationStatus = ImageVerificationStatus.IMAGE_UNAVAILABLE,
                            onlinePriceUsd = resolvedOnlinePrice,
                            priceSource = resolvedPriceSource,
                            priceType = resolvedPriceType,
                            actionTaken = "Marked IMAGE_UNAVAILABLE (No verified photo; blocked fake match)"
                        )
                    )
                }

                ImageVerificationStatus.AWAITING_VERIFICATION -> {
                    imagesAwaitingVerification++
                    assetDao.syncAssetCloudMetadata(
                        assetId = entity.id,
                        imageUrl = verifiedMeta.imageUrl,
                        thumbnailUrl = verifiedMeta.thumbnailUrl,
                        imageUrl1 = verifiedMeta.imageUrl,
                        imageUrl2 = verifiedMeta.secondaryImageUrl,
                        imageSource = verifiedMeta.imageSource,
                        licenseInfo = verifiedMeta.licenseInfo,
                        verificationStatus = ImageVerificationStatus.AWAITING_VERIFICATION.name,
                        onlinePriceUsd = resolvedOnlinePrice,
                        priceSource = resolvedPriceSource,
                        priceType = resolvedPriceType,
                        updatedAt = nowMillis
                    )
                    records.add(
                        AssetImageAuditRecord(
                            assetId = entity.id,
                            assetName = targetName,
                            category = category,
                            issueDetected = detectedIssue,
                            previousImageState = oldPrimary.ifBlank { "UNVERIFIED" },
                            newVerifiedImageUrl = verifiedMeta.imageUrl.ifBlank { "AWAITING_VERIFICATION" },
                            newSecondaryImageUrl = verifiedMeta.secondaryImageUrl.ifBlank { "AWAITING_VERIFICATION" },
                            newThumbnailUrl = verifiedMeta.thumbnailUrl.ifBlank { "AWAITING_VERIFICATION" },
                            imageSource = verifiedMeta.imageSource,
                            licenseInfo = verifiedMeta.licenseInfo,
                            verificationStatus = ImageVerificationStatus.AWAITING_VERIFICATION,
                            onlinePriceUsd = resolvedOnlinePrice,
                            priceSource = resolvedPriceSource,
                            priceType = resolvedPriceType,
                            actionTaken = "Flagged for manual verification review"
                        )
                    )
                }

                ImageVerificationStatus.VERIFIED_REAL_PHOTO -> {
                    val isValidNew = verifiedMeta.imageUrl.startsWith("https://") &&
                        verifiedMeta.secondaryImageUrl.startsWith("https://") &&
                        !AssetImageRegistry.isCartoonOrProceduralUrl(verifiedMeta.imageUrl) &&
                        !AssetImageRegistry.isCartoonOrProceduralUrl(verifiedMeta.secondaryImageUrl) &&
                        verifiedMeta.imageUrl != verifiedMeta.secondaryImageUrl &&
                        assignedVerifiedUrls.add(verifiedMeta.imageUrl) &&
                        assignedVerifiedUrls.add(verifiedMeta.secondaryImageUrl)

                    if (!isValidNew) {
                        remainingErrors++
                    } else {
                        correctRealPhotosAdded++
                    }

                    val needsReplacement = detectedIssue != ImageAuditIssueType.NONE_VERIFIED ||
                        entity.imageSource != verifiedMeta.imageSource ||
                        entity.licenseInfo != verifiedMeta.licenseInfo ||
                        entity.thumbnailUrl != verifiedMeta.thumbnailUrl ||
                        entity.imageUrl != verifiedMeta.imageUrl ||
                        entity.onlinePriceUsd != resolvedOnlinePrice

                    if (needsReplacement) {
                        if (detectedIssue != ImageAuditIssueType.NONE_VERIFIED) {
                            imagesReplaced++
                        }
                        assetDao.syncAssetCloudMetadata(
                            assetId = entity.id,
                            imageUrl = verifiedMeta.imageUrl,
                            thumbnailUrl = verifiedMeta.thumbnailUrl,
                            imageUrl1 = verifiedMeta.imageUrl,
                            imageUrl2 = verifiedMeta.secondaryImageUrl,
                            imageSource = verifiedMeta.imageSource,
                            licenseInfo = verifiedMeta.licenseInfo,
                            verificationStatus = ImageVerificationStatus.VERIFIED_REAL_PHOTO.name,
                            onlinePriceUsd = resolvedOnlinePrice,
                            priceSource = resolvedPriceSource,
                            priceType = resolvedPriceType,
                            updatedAt = nowMillis
                        )
                    }

                    val actionDescription = when (detectedIssue) {
                        ImageAuditIssueType.CARTOON_OR_PROCEDURAL_IMAGE ->
                            "Replaced cartoon/procedural URI with verified real photograph & synced online price"
                        ImageAuditIssueType.MISSING_PHOTO ->
                            "Added missing verified real-world photograph & synced online price"
                        ImageAuditIssueType.DUPLICATE_UNRELATED_IMAGE ->
                            "Replaced duplicate image with unique verified model photograph & synced online price"
                        ImageAuditIssueType.INCORRECT_MODEL_OR_MISMATCH ->
                            "Replaced mismatched image with exact model photograph & synced online price"
                        ImageAuditIssueType.BROKEN_OR_NON_HTTPS_URL ->
                            "Repaired broken URL with verified HTTPS photograph & synced online price"
                        ImageAuditIssueType.UNVERIFIED_MATCH ->
                            "Verified exact model & museum/manufacturer source"
                        else ->
                            "Verified exact real-life photograph & online valuation match"
                    }

                    records.add(
                        AssetImageAuditRecord(
                            assetId = entity.id,
                            assetName = targetName,
                            category = category,
                            issueDetected = detectedIssue,
                            previousImageState = oldPrimary.ifBlank { "NONE" },
                            newVerifiedImageUrl = verifiedMeta.imageUrl,
                            newSecondaryImageUrl = verifiedMeta.secondaryImageUrl,
                            newThumbnailUrl = verifiedMeta.thumbnailUrl,
                            imageSource = verifiedMeta.imageSource,
                            licenseInfo = verifiedMeta.licenseInfo,
                            verificationStatus = ImageVerificationStatus.VERIFIED_REAL_PHOTO,
                            onlinePriceUsd = resolvedOnlinePrice,
                            priceSource = resolvedPriceSource,
                            priceType = resolvedPriceType,
                            actionTaken = actionDescription
                        )
                    )
                }
            }
        }

        // If authenticated with Firestore, sync flagship/owned asset records to the cloud provider
        if (firestoreRepository != null) {
            for (record in records.take(25)) {
                val res = firestoreRepository.upsertSyncedAsset(
                    assetId = record.assetId,
                    symbol = catalogueById[record.assetId]?.symbol ?: record.assetId.uppercase().take(8),
                    name = record.assetName,
                    category = record.category.code,
                    imageUrl = if (record.newVerifiedImageUrl.startsWith("https://")) record.newVerifiedImageUrl else "",
                    thumbnailUrl = if (record.newThumbnailUrl.startsWith("https://")) record.newThumbnailUrl else "",
                    imageSource = record.imageSource,
                    licenseInfo = record.licenseInfo,
                    verificationStatus = record.verificationStatus,
                    onlinePriceUsd = record.onlinePriceUsd,
                    priceSource = record.priceSource,
                    priceType = record.priceType
                )
                if (res.isSuccess) {
                    cloudRecordsSynced++
                }
            }
        }

        return ImageAuditAdminReport(
            auditedAtMillis = nowMillis,
            totalAssetsAudited = dbAssets.size,
            correctRealPhotosAdded = correctRealPhotosAdded,
            imagesReplaced = imagesReplaced,
            missingImages = missingImages,
            imagesAwaitingVerification = imagesAwaitingVerification,
            onlineValuationsSynced = onlineValuationsSynced,
            cloudRecordsSynced = cloudRecordsSynced,
            remainingErrors = remainingErrors,
            records = records
        )
    }
}

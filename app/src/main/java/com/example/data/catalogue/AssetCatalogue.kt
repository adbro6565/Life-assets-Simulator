package com.example.data.catalogue

import com.example.data.model.Asset
import com.example.data.model.AssetCategory

object AssetCatalogue {

    val allAssets: List<Asset> by lazy {
        val rawList = buildList {
            addAll(ArtAndWatchesCatalogue.fineArtAssets)                  // 25
            addAll(ArtAndWatchesCatalogue.luxuryWatchAssets)              // 25
            addAll(VehiclesCatalogue.supercarAssets)                      // 35
            addAll(VehiclesCatalogue.privateJetAssets)                    // 15
            addAll(VehiclesCatalogue.yachtAndShipAssets)                  // 15
            addAll(RealEstateAndIslandsCatalogue.luxuryRealEstateAssets)  // 30
            addAll(RealEstateAndIslandsCatalogue.privateIslandAssets)     // 15
            addAll(LuxuryGoodsCatalogue.jewelryAndDiamondAssets)          // 20
            addAll(LuxuryGoodsCatalogue.designerFashionAssets)            // 20
            addAll(CommoditiesAndFoodCatalogue.preciousMetalAssets)       // 10
            addAll(LuxuryGoodsCatalogue.rareCollectibleAssets)            // 25
            addAll(CommoditiesAndFoodCatalogue.agriculturalAssets)        // 20
            addAll(CommoditiesAndFoodCatalogue.luxuryFoodAssets)          // 15
            addAll(FinancialMarketsCatalogue.cryptocurrencyAssets)        // 20
            addAll(FinancialMarketsCatalogue.stockAndIndexAssets)         // 25
            addAll(BusinessAndIndustrialCatalogue.businessAssets)         // 20
            addAll(BusinessAndIndustrialCatalogue.industrialAssets)       // 15
            addAll(BusinessAndIndustrialCatalogue.transportInfrastructureAssets) // 10
            addAll(TechAndSpaceCatalogue.techComputingAssets)             // 20
            addAll(TechAndSpaceCatalogue.spaceAndEnergyAssets)            // 20
        }
        rawList.map { asset ->
            val verifiedMeta = AssetImageRegistry.getVerifiedImageMetadata(
                asset.id,
                asset.name,
                asset.category,
                asset.subcategory
            )
            asset.copy(
                imageUrl1 = verifiedMeta.imageUrl,
                imageUrl2 = verifiedMeta.secondaryImageUrl,
                imageUrl = verifiedMeta.imageUrl,
                thumbnailUrl = verifiedMeta.thumbnailUrl,
                imageSource = verifiedMeta.imageSource,
                licenseInfo = verifiedMeta.licenseInfo,
                verificationStatus = verifiedMeta.verificationStatus,
                updatedAt = verifiedMeta.updatedAt
            )
        }
    }

    val totalCount: Int
        get() = allAssets.size

    private val legacyIdAliases = mapOf(
        "metal_01" to "met_01",
        "metal_02" to "met_02",
        "metal_03" to "met_03",
        "metal_06" to "met_06",
        "oil_01" to "met_06",
        "stock_01" to "stk_01",
        "stock_02" to "stk_02",
        "stock_03" to "stk_03",
        "stock_04" to "stk_04",
        "stock_05" to "stk_05",
        "stock_06" to "stk_06",
        "stock_07" to "stk_07"
    )

    private val assetByIdMap: Map<String, Asset> by lazy {
        allAssets.associateBy { it.id }
    }

    fun getAssetById(id: String): Asset? {
        return assetByIdMap[id] ?: legacyIdAliases[id]?.let { assetByIdMap[it] }
    }

    fun getAssetsByCategory(category: AssetCategory): List<Asset> =
        allAssets.filter { it.category == category }

    fun getCategoryCounts(): Map<AssetCategory, Int> =
        allAssets.groupingBy { it.category }.eachCount()
}

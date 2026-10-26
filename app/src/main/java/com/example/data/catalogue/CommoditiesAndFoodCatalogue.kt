package com.example.data.catalogue

import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.RarityTier

object CommoditiesAndFoodCatalogue {

    val preciousMetalAssets: List<Asset> = listOf(
        Asset("met_01", "Gold Bullion Bar (400 oz LBMA)", "XAU-400", AssetCategory.PRECIOUS_METALS, "Gold Bullion", "LBMA-accredited 999.9 fine 400 troy ounce physical gold bullion bar.", 1_080_000.0, unit = "400 oz Bar", rarity = RarityTier.RARE, priceSourceLabel = "LBMA Gold Reference / Simulated"),
        Asset("met_02", "Silver Bullion Bar (1,000 oz COMEX)", "XAG-1K", AssetCategory.PRECIOUS_METALS, "Silver Bullion", "999.0 fine 1,000 troy ounce institutional physical silver bullion bar.", 32_500.0, unit = "1000 oz Bar", rarity = RarityTier.COMMON, priceSourceLabel = "COMEX Silver Reference / Simulated"),
        Asset("met_03", "Platinum Bullion Ingot (50 oz LPPM)", "XPT-50", AssetCategory.PRECIOUS_METALS, "PGM Bullion", "999.5 pure platinum bullion ingot for catalyst and treasury reserves.", 51_000.0, unit = "50 oz Lot", rarity = RarityTier.RARE),
        Asset("met_04", "Palladium Bullion Lot (100 oz LPPM)", "XPD-100", AssetCategory.PRECIOUS_METALS, "PGM Bullion", "Rare platinum-group metal critical for aerospace and electronics.", 104_000.0, unit = "100 oz Lot", rarity = RarityTier.RARE),
        Asset("met_05", "LME Grade-A Copper Cathode (25 MT)", "HG-25T", AssetCategory.PRECIOUS_METALS, "Industrial Metal", "99.99% pure electrolytic copper cathode contract.", 238_000.0, unit = "25 MT Lot", rarity = RarityTier.COMMON),
        Asset("met_06", "WTI / Brent Crude Oil Barrels (1,000 Bbl)", "USO-1K", AssetCategory.PRECIOUS_METALS, "Crude Oil", "1,000-barrel physical light sweet crude oil barrels benchmark contract.", 76_500.0, unit = "1000 Barrels", rarity = RarityTier.COMMON, priceSourceLabel = "NYMEX/ICE Reference / Simulated"),
        Asset("met_07", "Henry Hub Natural Gas (10,000 MMBtu)", "NG-10K", AssetCategory.PRECIOUS_METALS, "Energy Commodity", "Pipeline-deliverable 10,000 MMBtu natural gas energy contract.", 28_500.0, unit = "10k MMBtu", rarity = RarityTier.COMMON),
        Asset("met_08", "LME Primary Aluminum Ingots (25 MT)", "ALU-25T", AssetCategory.PRECIOUS_METALS, "Industrial Metal", "99.7% purity primary aluminum ingots for aerospace and EV chassis.", 64_000.0, unit = "25 MT Lot", rarity = RarityTier.COMMON),
        Asset("met_09", "Battery-Grade Lithium Carbonate (5 MT)", "LIT-5T", AssetCategory.PRECIOUS_METALS, "Strategic Mineral", "EV battery-grade Li2CO3 refined chemical payload.", 72_000.0, unit = "5 MT Lot", rarity = RarityTier.RARE),
        Asset("met_10", "LME Class-1 Briquette Nickel (6 MT)", "NIC-6T", AssetCategory.PRECIOUS_METALS, "Strategic Mineral", "99.8% pure nickel briquettes for superalloys and high-nickel cathodes.", 102_000.0, unit = "6 MT Lot", rarity = RarityTier.RARE)
    )

    val agriculturalAssets: List<Asset> = listOf(
        Asset("agr_01", "No. 2 Hard Red Winter Wheat (100 Metric Tonnes)", "WHT-100", AssetCategory.AGRICULTURAL, "Grain", "Export-grade bread wheat stored in climate-controlled silos.", 26_500.0, unit = "100 MT", rarity = RarityTier.COMMON),
        Asset("agr_02", "Thai 100% Grade-B Milled Jasmine Rice (100 MT)", "RCE-100", AssetCategory.AGRICULTURAL, "Grain", "Containerized aromatic long-grain export rice.", 61_000.0, unit = "100 MT", rarity = RarityTier.COMMON),
        Asset("agr_03", "US No. 2 Yellow Corn (100 Metric Tonnes)", "CRN-100", AssetCategory.AGRICULTURAL, "Feed & Biofuel", "Midwest yellow dent corn bulk barge lot.", 19_800.0, unit = "100 MT", rarity = RarityTier.COMMON),
        Asset("agr_04", "No. 1 Yellow Soybeans (100 Metric Tonnes)", "SOY-100", AssetCategory.AGRICULTURAL, "Oilseed", "High-protein crush-grade soybean futures lot.", 43_000.0, unit = "100 MT", rarity = RarityTier.COMMON),
        Asset("agr_05", "ICE Arabica Washed Green Coffee (37,500 lbs Lot)", "KCF-LOT", AssetCategory.AGRICULTURAL, "Softs", "Colombian and Brazilian washed Arabica exchange-certified bags.", 89_000.0, unit = "37.5k lbs", rarity = RarityTier.RARE),
        Asset("agr_06", "Ivory Coast Main-Crop Cocoa Beans (10 Metric Tonnes)", "COC-10T", AssetCategory.AGRICULTURAL, "Softs", "Fermented and sun-dried fine chocolate-grade cocoa beans.", 78_000.0, unit = "10 MT", rarity = RarityTier.RARE),
        Asset("agr_07", "Raw Centrifugal Cane Sugar No. 11 (112,000 lbs)", "SGR-11", AssetCategory.AGRICULTURAL, "Softs", "Global raw cane sugar maritime contract.", 23_500.0, unit = "50 MT", rarity = RarityTier.COMMON),
        Asset("agr_08", "Strict Low Middling Upland Cotton (50,000 lbs)", "CTN-50K", AssetCategory.AGRICULTURAL, "Fiber", "100-bale textile mill grade cotton contract.", 39_000.0, unit = "50k lbs", rarity = RarityTier.COMMON),
        Asset("agr_09", "Assam & Darjeeling First-Flush Orthodox Tea (5 MT)", "TEA-5T", AssetCategory.AGRICULTURAL, "Beverage Crop", "Single-estate whole-leaf golden tip tea chests.", 44_000.0, unit = "5 MT", rarity = RarityTier.RARE),
        Asset("agr_10", "TSR-20 Natural Latex Rubber (20 Metric Tonnes)", "RUB-20T", AssetCategory.AGRICULTURAL, "Industrial Crop", "Technically Specified Rubber for aviation and high-speed tires.", 36_000.0, unit = "20 MT", rarity = RarityTier.COMMON),
        Asset("agr_11", "Andalusian Cold-Pressed Extra Virgin Olive Oil (10,000 L)", "OLV-10K", AssetCategory.AGRICULTURAL, "Edible Oil", "Stainless-steel nitrogen-sealed tank of Picual extra virgin olive oil.", 92_000.0, unit = "10,000 L", rarity = RarityTier.RARE),
        Asset("agr_12", "Refined Non-GMO Expeller Corn Oil (20 Metric Tonnes)", "COI-20T", AssetCategory.AGRICULTURAL, "Edible Oil", "Food-grade refined golden corn oil bulk flexitank.", 29_000.0, unit = "20 MT", rarity = RarityTier.COMMON),
        Asset("agr_13", "New Zealand Fonterra Whole Milk Powder (25 MT)", "WMP-25T", AssetCategory.AGRICULTURAL, "Dairy", "Export-grade spray-dried whole dairy powder.", 84_000.0, unit = "25 MT", rarity = RarityTier.COMMON),
        Asset("agr_14", "Parmigiano-Reggiano 36-Month Aged Cave Vault (200 Wheels)", "CHS-200", AssetCategory.AGRICULTURAL, "Artisan Dairy", "Bank-collateralized Emilia-Romagna DOP Parmesan wheel vault.", 260_000.0, unit = "200 Wheels", rarity = RarityTier.RARE),
        Asset("agr_15", "Kiln-Dried Structural Douglas Fir Lumber (110,000 bd ft)", "LMB-110", AssetCategory.AGRICULTURAL, "Forestry", "Railcar-deliverable dimensional construction timber.", 62_000.0, unit = "110k bd ft", rarity = RarityTier.COMMON),
        Asset("agr_16", "Khorasan Super Negin Red Saffron (10 Kilogram Vault)", "SAF-10K", AssetCategory.AGRICULTURAL, "Spice", "ISO 3632 Category I all-red stigma saffron threads.", 38_000.0, unit = "10 kg", rarity = RarityTier.RARE),
        Asset("agr_17", "Madagascar Sava Bourbon Cured Vanilla Beans (100 kg)", "VAN-100", AssetCategory.AGRICULTURAL, "Spice", "Vacuum-sealed gourmet black vanilla pods (1.8%+ vanillin).", 34_000.0, unit = "100 kg", rarity = RarityTier.RARE),
        Asset("agr_18", "Tellicherry Garbled Special Extra Bold Black Pepper (5 MT)", "PEP-5T", AssetCategory.AGRICULTURAL, "Spice", "Malabar Coast high-piperine whole black peppercorns.", 41_000.0, unit = "5 MT", rarity = RarityTier.COMMON),
        Asset("agr_19", "Guatemalan 8mm Fancy Green Cardamom Pods (2 MT)", "CRD-2T", AssetCategory.AGRICULTURAL, "Spice", "Sun-cured aromatic green cardamom export lot.", 52_000.0, unit = "2 MT", rarity = RarityTier.RARE),
        Asset("agr_20", "Ceylon Alba Grade True Cinnamon Quills (2 Metric Tonnes)", "CIN-2T", AssetCategory.AGRICULTURAL, "Spice", "Hand-rolled pencil-thin Sri Lankan Cinnamomum verum quills.", 48_000.0, unit = "2 MT", rarity = RarityTier.RARE)
    )

    val luxuryFoodAssets: List<Asset> = listOf(
        Asset("food_01", "Piemonte Alba White Truffle 'Tuber Magnatum' Trophy Lot", "TRF-WHT", AssetCategory.LUXURY_FOOD, "Haute Gastronomy", "1.2 kg twin specimen of wild Alba white truffle auctioned at Castle of Grinzane Cavour.", 145_000.0, unit = "1.2 kg Lot", rarity = RarityTier.ULTRA_RARE, location = "Alba, Italy"),
        Asset("food_02", "Périgord Black Winter Truffle 'Tuber Melanosporum' (10 kg)", "TRF-BLK", AssetCategory.LUXURY_FOOD, "Haute Gastronomy", "Freshly harvested French Périgord black winter truffles.", 22_000.0, unit = "10 kg", rarity = RarityTier.RARE, location = "Dordogne, France"),
        Asset("food_03", "Kashmiri Pampore Mongra Saffron Reserve (25 kg)", "SAF-KSH", AssetCategory.LUXURY_FOOD, "Specialty Spice", "GI-tagged high-crocin Kashmiri Mongra saffron in hermetic gold tins.", 96_000.0, unit = "25 kg", rarity = RarityTier.ULTRA_RARE, location = "Kashmir"),
        Asset("food_04", "Miyazaki A5 BMS 12 Champion Wagyu Whole Carcass Lot", "WGY-A5", AssetCategory.LUXURY_FOOD, "Artisan Beef", "Japanese Wagyu Olympics champion-grade A5 striploin & ribeye allocation.", 68_000.0, unit = "Carcass Lot", rarity = RarityTier.RARE, location = "Miyazaki, Japan"),
        Asset("food_05", "Certified Hyogo Tajima Kobe Beef A5 Export Allocation", "KOB-A5", AssetCategory.LUXURY_FOOD, "Artisan Beef", "Chrysanthemum-stamped authentic Hyogo Prefecture Kobe beef.", 85_000.0, unit = "Prime Allocation", rarity = RarityTier.ULTRA_RARE, location = "Kobe, Japan"),
        Asset("food_06", "Almas Golden Albino Iranian Beluga Caviar (5 kg 24k Tin)", "CAV-ALM", AssetCategory.LUXURY_FOOD, "Rare Caviar", "Harvested from 60-to-100-year-old albino Huso huso sturgeon.", 160_000.0, unit = "5 kg Vault", rarity = RarityTier.TROPHY, location = "Caspian / London"),
        Asset("food_07", "Gillardeau & Belon '0000' Royal Oyster Bed Concession", "OYS-BEL", AssetCategory.LUXURY_FOOD, "Aquaculture", "Annual harvest rights to premier Brittany flat oyster beds.", 48_000.0, unit = "Concession", rarity = RarityTier.RARE, location = "Brittany, France"),
        Asset("food_08", "Hacienda La Esmeralda Boquete Geisha Coffee Auction Lot", "COF-GSH", AssetCategory.LUXURY_FOOD, "Specialty Coffee", "Best of Panama 96.5-point natural process Geisha micro-lot.", 110_000.0, unit = "100 lb Lot", rarity = RarityTier.ULTRA_RARE, location = "Boquete, Panama"),
        Asset("food_09", "Wuyi Mountain Da Hong Pao Mother-Tree Tea Reserve (200g)", "TEA-DHP", AssetCategory.LUXURY_FOOD, "Imperial Oolong", "Pre-ban 1998 harvest from the original cliffside Wuyi mother bushes.", 240_000.0, unit = "200g Jar", rarity = RarityTier.TROPHY, location = "Fujian, China"),
        Asset("food_10", "Acetaia Tradizionale di Modena 100-Year Battery Cask Set", "VNG-100", AssetCategory.LUXURY_FOOD, "Aged Elixir", "Seven-wood antique battery of century-aged DOP Balsamic vinegar.", 74_000.0, unit = "Cask Battery", rarity = RarityTier.ULTRA_RARE, location = "Modena, Italy"),
        Asset("food_11", "Tahitian Pompona Grandiflorum Orchid Vanilla Vault (50 kg)", "VAN-TAH", AssetCategory.LUXURY_FOOD, "Rare Culinary", "Anise-and-cherry-scented Raiatea Tahitian gourmet pods.", 39_000.0, unit = "50 kg", rarity = RarityTier.RARE, location = "French Polynesia"),
        Asset("food_12", "Ancient Millenary Farga Olive Tree Private Harvest (500 L)", "OLV-FRG", AssetCategory.LUXURY_FOOD, "Liquid Gold", "Pressed from 1,000-year-old Maestrazgo Farga olive trees.", 32_000.0, unit = "500 L Reserve", rarity = RarityTier.RARE, location = "Valencia, Spain"),
        Asset("food_13", "To'ak Nacional Ancient Heirloom Cacao Cognac Cask Collection", "CHC-TOK", AssetCategory.LUXURY_FOOD, "Fine Chocolate", "100-bar vault of DNA-verified 100% pure Ancient Nacional Ecuadorian cacao.", 45_000.0, unit = "100-Bar Vault", rarity = RarityTier.RARE, location = "Ecuador"),
        Asset("food_14", "New Zealand UMF 30+ MGO 1600+ Rare Manuka Honey Reserve", "HNY-MNK", AssetCategory.LUXURY_FOOD, "Medicinal Nectar", "Helicopter-harvested remote North Island ultra-high-potency Manuka.", 56_000.0, unit = "100 Jar Lot", rarity = RarityTier.RARE, location = "New Zealand"),
        Asset("food_15", "Domaine de la Romanée-Conti 1945 & Macallan 1926 Cellar", "WIN-DRC", AssetCategory.LUXURY_FOOD, "Fine Wine & Whisky", "Climate-controlled Geneva cellar holding DRC 1945 and Macallan 1926 Fine & Rare.", 2_450_000.0, unit = "Trophy Cellar", rarity = RarityTier.TROPHY, location = "Geneva")
    )
}

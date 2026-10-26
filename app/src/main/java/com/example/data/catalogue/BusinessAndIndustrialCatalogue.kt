package com.example.data.catalogue

import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.RarityTier

object BusinessAndIndustrialCatalogue {

    val businessAssets: List<Asset> = listOf(
        Asset("biz_01", "Paris Michelin Three-Star Gastronomy Flagship", "BIZ-MCH", AssetCategory.BUSINESSES, "Hospitality", "Equity valuation based on net restaurant cash flows and 18-month tasting menu waitlist.", 18_500_000.0, rarity = RarityTier.RARE, location = "Paris"),
        Asset("biz_02", "Specialty Coffee Roastery & 45-Store Urban Chain", "BIZ-COF", AssetCategory.BUSINESSES, "Food & Beverage", "Direct-trade roastery and high-foot-traffic commuter espresso bars.", 32_000_000.0, rarity = RarityTier.RARE, location = "Seattle / Tokyo"),
        Asset("biz_03", "Regional Organic Supermarket Chain (28 Locations)", "BIZ-SUP", AssetCategory.BUSINESSES, "Grocery Retail", "High-margin organic grocer with proprietary private-label products.", 145_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "California"),
        Asset("biz_04", "Geneva & Bond Street Luxury Watch Boutique Group", "BIZ-LUX", AssetCategory.BUSINESSES, "Luxury Retail", "Authorized multi-brand haute horlogerie retail group.", 95_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Geneva / London"),
        Asset("biz_05", "Kyoto Ryokan & Onsen Heritage Hospitality Group", "BIZ-HTL", AssetCategory.BUSINESSES, "Boutique Hotel", "High-ADR luxury hot-spring hospitality business.", 54_000_000.0, rarity = RarityTier.RARE, location = "Kyoto"),
        Asset("biz_06", "St. Moritz Alpine Ski Resort & Gondola Operator", "BIZ-SKI", AssetCategory.BUSINESSES, "Resort Operation", "Lift pass, chalet hospitality, and private club operating company.", 260_000_000.0, rarity = RarityTier.TROPHY, location = "St. Moritz"),
        Asset("biz_07", "Premier IMAX & Dine-In Cinema Circuit (40 Multiplexes)", "BIZ-CIN", AssetCategory.BUSINESSES, "Entertainment", "Laser-projection luxury recliner theater circuit.", 88_000_000.0, rarity = RarityTier.RARE, location = "North America"),
        Asset("biz_08", "Transcontinental Cold-Chain Freight & 3PL Company", "BIZ-LOG", AssetCategory.BUSINESSES, "Logistics", "Pharmaceutical and fresh-food temperature-controlled logistics operator.", 175_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Frankfurt"),
        Asset("biz_09", "Civil Infrastructure & Tunneling Engineering Firm", "BIZ-CNS", AssetCategory.BUSINESSES, "Construction", "Backlog-supported heavy civil bridge and metro contractor.", 210_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Zurich"),
        Asset("biz_10", "Generative AI Foundation Model Research Lab Stake", "BIZ-AIL", AssetCategory.BUSINESSES, "AI Startup", "Series-C preferred equity stake in frontier multimodal AI lab.", 480_000_000.0, rarity = RarityTier.TROPHY, location = "San Francisco"),
        Asset("biz_11", "B2B Enterprise Cybersecurity SaaS Unicorn", "BIZ-SAS", AssetCategory.BUSINESSES, "Software Company", "$120M ARR zero-trust identity protection SaaS platform (118% NRR).", 920_000_000.0, rarity = RarityTier.TROPHY, location = "Palo Alto"),
        Asset("biz_12", "Sovereign FinTech Payment Gateway & Neobank", "BIZ-FIN", AssetCategory.BUSINESSES, "FinTech", "Licensed cross-border merchant acquiring and treasury platform.", 340_000_000.0, rarity = RarityTier.TROPHY, location = "London / Singapore"),
        Asset("biz_13", "Tier-IV Hyperscale Colocation Data Center Operator", "BIZ-DCO", AssetCategory.BUSINESSES, "Cloud Infrastructure", "Long-term triple-net AI hyperscaler colocation operating business.", 560_000_000.0, rarity = RarityTier.TROPHY, location = "Ashburn / Dublin"),
        Asset("biz_14", "Precision Aerospace Titanium Component Manufacturer", "BIZ-MFG", AssetCategory.BUSINESSES, "Aerospace Mfg", "AS9100-certified turbine blade and landing gear supplier.", 165_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Toulouse"),
        Asset("biz_15", "Automated Hydroponic Vertical Farming Enterprise", "BIZ-FRM", AssetCategory.BUSINESSES, "AgriTech", "Climate-independent leafy greens and berry indoor farming business.", 48_000_000.0, rarity = RarityTier.RARE, location = "Netherlands"),
        Asset("biz_16", "Western Australia Gold & Copper Open-Pit Mining Co.", "BIZ-MIN", AssetCategory.BUSINESSES, "Mining Operation", "Producing deposit with 2.4M oz proven gold reserves.", 390_000_000.0, rarity = RarityTier.TROPHY, location = "Kalgoorlie"),
        Asset("biz_17", "Atacama Utility-Scale Solar & Battery IPP", "BIZ-SOL", AssetCategory.BUSINESSES, "Clean Energy Co", "20-year power purchase agreement independent power producer.", 195_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Chile"),
        Asset("biz_18", "North Sea Offshore Wind Power Generation Consortium", "BIZ-WND", AssetCategory.BUSINESSES, "Clean Energy Co", "Yield-generating offshore wind power operating equity.", 310_000_000.0, rarity = RarityTier.TROPHY, location = "North Sea"),
        Asset("biz_19", "Formula 1 World Championship Racing Team Equity Stake", "BIZ-F1T", AssetCategory.BUSINESSES, "Sports Franchise", "Cost-cap profitable Formula 1 constructor minority controlling stake.", 450_000_000.0, rarity = RarityTier.TROPHY, location = "Silverstone"),
        Asset("biz_20", "European Top-Flight Football Club Ownership", "BIZ-FCB", AssetCategory.BUSINESSES, "Sports Franchise", "Champions League-contending club with stadium media rights.", 780_000_000.0, rarity = RarityTier.TROPHY, location = "London / Milan")
    )

    val industrialAssets: List<Asset> = listOf(
        Asset("ind_01", "800-Ton Hydraulic Mining Shovel Excavator", "IND-EXC", AssetCategory.INDUSTRIAL, "Heavy Earthmoving", "Twin-diesel 4,000 hp mining face shovel with 42 m³ bucket.", 18_500_000.0, rarity = RarityTier.RARE),
        Asset("ind_02", "Caterpillar D11T CD Heavy Mining Bulldozer Fleet (5x)", "IND-BLD", AssetCategory.INDUSTRIAL, "Earthmoving", "Five-unit fleet of 104-ton carrydozer mining tractors.", 12_500_000.0, rarity = RarityTier.RARE),
        Asset("ind_03", "Ultra-Class 400-Ton Autonomous Haul Truck Fleet (6x)", "IND-TRK", AssetCategory.INDUSTRIAL, "Mining Haulage", "LiDAR/GPS autonomous electric-drive dump trucks.", 36_000_000.0, rarity = RarityTier.ULTRA_RARE),
        Asset("ind_04", "Liebherr LR 13000 3,000-Ton Crawler Crane", "IND-CRN", AssetCategory.INDUSTRIAL, "Heavy Lifting", "World's most powerful conventional crawler crane for nuclear & offshore lifts.", 29_000_000.0, rarity = RarityTier.ULTRA_RARE),
        Asset("ind_05", "9,000-Ton Aluminum Megacasting Giga-Press Cell", "IND-PRS", AssetCategory.INDUSTRIAL, "Automotive Factory", "High-pressure die-casting cell producing single-piece EV rear underbodies.", 24_000_000.0, rarity = RarityTier.ULTRA_RARE),
        Asset("ind_06", "5-Axis Simultaneous Aerospace Titanium CNC Line (10x)", "IND-CNC", AssetCategory.INDUSTRIAL, "Precision Machining", "Automated pallet-pool 5-axis gantry milling centers.", 14_800_000.0, rarity = RarityTier.RARE),
        Asset("ind_07", "Autonomous RTK-GPS 650 hp Articulated Tractor Fleet (10x)", "IND-TRC", AssetCategory.INDUSTRIAL, "Agri Machinery", "Quad-track autonomous seeding and tillage tractor fleet.", 7_800_000.0, rarity = RarityTier.COMMON),
        Asset("ind_08", "Class-10 Rotary Twin-Rotor Combine Harvester Fleet (8x)", "IND-HRV", AssetCategory.INDUSTRIAL, "Agri Machinery", "50-foot draper header grain harvesting fleet with yield mapping.", 8_400_000.0, rarity = RarityTier.COMMON),
        Asset("ind_09", "AutoStore 500-Robot Cube Storage Fulfillment Grid", "IND-WHS", AssetCategory.INDUSTRIAL, "Warehouse Robotics", "High-density goods-to-person automated fulfillment system.", 22_000_000.0, rarity = RarityTier.RARE),
        Asset("ind_10", "6-Axis Heavy-Payload Automotive Welding Robot Line (60x)", "IND-RBT", AssetCategory.INDUSTRIAL, "Industrial Robotics", "Synchronized laser and spot-welding body-in-white assembly line.", 16_200_000.0, rarity = RarityTier.RARE),
        Asset("ind_11", "Herrenknecht 14-Meter Hard-Rock Tunnel Boring Machine", "IND-TBM", AssetCategory.INDUSTRIAL, "Subterranean Eng.", "120-meter-long EPB shield TBM for alpine rail and metro tunnels.", 58_000_000.0, rarity = RarityTier.ULTRA_RARE),
        Asset("ind_12", "ISO Class-1 Cleanroom Semiconductor Wafer Fab Module", "IND-CLN", AssetCategory.INDUSTRIAL, "Semiconductor Fab", "Sub-micron ULPA air-handling and vibration-isolated cleanroom bay.", 210_000_000.0, rarity = RarityTier.TROPHY),
        Asset("ind_13", "40 GWh Dry-Electrode Lithium-Ion Gigafactory Line", "IND-GIG", AssetCategory.INDUSTRIAL, "Battery Factory", "High-speed roll-to-roll coating, calendering, and formation cell line.", 185_000_000.0, rarity = RarityTier.TROPHY),
        Asset("ind_14", "Automated Aseptic Bioreactor & Vial Fill-Finish Line", "IND-BIO", AssetCategory.INDUSTRIAL, "Biomanufacturing", "20,000L stainless single-use mAb bioreactor and robotic isolator line.", 94_000_000.0, rarity = RarityTier.ULTRA_RARE),
        Asset("ind_15", "Reverse-Osmosis Coastal Seawater Desalination Plant", "IND-DSL", AssetCategory.INDUSTRIAL, "Water Infrastructure", "100,000 m³/day energy-recovery membrane desalination facility.", 140_000_000.0, rarity = RarityTier.TROPHY)
    )

    val transportInfrastructureAssets: List<Asset> = listOf(
        Asset("trn_01", "Boeing 777F Long-Range Dedicated Cargo Freighter", "TRN-777F", AssetCategory.TRANSPORT_LOGISTICS, "Air Cargo", "102-metric-ton payload transpacific widebody air freighter.", 195_000_000.0, rarity = RarityTier.TROPHY, location = "Anchorage / Hong Kong"),
        Asset("trn_02", "24,000 TEU Ultra-Large Container Vessel 'Megamax-24'", "TRN-24K", AssetCategory.TRANSPORT_LOGISTICS, "Ocean Liner", "400-meter Asia-Europe flagship container ship.", 235_000_000.0, rarity = RarityTier.TROPHY, location = "Singapore"),
        Asset("trn_03", "Deepwater Ship-to-Shore Automated Container Terminal Berth", "TRN-PRT", AssetCategory.TRANSPORT_LOGISTICS, "Port Concession", "8 tandem-lift Super-Post-Panamax gantry cranes and AGV yard.", 380_000_000.0, rarity = RarityTier.TROPHY, location = "Rotterdam"),
        Asset("trn_04", "FBO Private Aviation Hangar & Fuel Farm Complex", "TRN-FBO", AssetCategory.TRANSPORT_LOGISTICS, "Aviation Terminal", "Heated G700-capable hangars, VIP customs lounge, and SAF fuel depot.", 72_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Teterboro / Farnborough"),
        Asset("trn_05", "Tier-4 Freight Electric-Diesel Locomotive Consist (12x)", "TRN-LOC", AssetCategory.TRANSPORT_LOGISTICS, "Rail Freight", "4,400 hp AC-traction heavy-haul intermodal locomotives.", 54_000_000.0, rarity = RarityTier.RARE, location = "Chicago"),
        Asset("trn_06", "High-Speed 320 km/h Electric Passenger Trainset", "TRN-HSR", AssetCategory.TRANSPORT_LOGISTICS, "High-Speed Rail", "16-car distributed-traction bullet train leased to rail operator.", 48_000_000.0, rarity = RarityTier.RARE, location = "Tokyo / Paris"),
        Asset("trn_07", "250-Bus Zero-Emission Hydrogen & EV Transit Fleet", "TRN-BUS", AssetCategory.TRANSPORT_LOGISTICS, "Municipal Fleet", "Contracted metropolitan zero-emission transit fleet with depot.", 115_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "London"),
        Asset("trn_08", "100-Truck Class-8 Heavy Electric Semi & Megawatt Chargers", "TRN-SMI", AssetCategory.TRANSPORT_LOGISTICS, "Highway Logistics", "500-mile range heavy electric semi-tractors and MCS charging corridor.", 34_000_000.0, rarity = RarityTier.RARE, location = "Los Angeles"),
        Asset("trn_09", "Floating Storage and Regasification Unit (FSRU) Terminal", "TRN-FSR", AssetCategory.TRANSPORT_LOGISTICS, "Energy Logistics", "Offshore LNG regasification vessel connected to national gas grid.", 290_000_000.0, rarity = RarityTier.TROPHY, location = "Wilhelmshaven"),
        Asset("trn_10", "Automated Intermodal Rail-to-Port Dry Port Hub", "TRN-DRY", AssetCategory.TRANSPORT_LOGISTICS, "Intermodal Hub", "Dual-rail-served inland customs and container transfer terminal.", 155_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Duisburg")
    )
}

package com.example.data.catalogue

import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.RarityTier

object VehiclesCatalogue {

    val supercarAssets: List<Asset> = listOf(
        Asset("car_01", "Bugatti Chiron Super Sport 300+", "BUG-300", AssetCategory.SUPERCARS, "Hypercar", "Quad-turbo W16 1,600 hp longtail hypercar breaking the 300 mph barrier.", 5_400_000.0, rarity = RarityTier.TROPHY, location = "Molsheim"),
        Asset("car_02", "Bugatti Tourbillon V16 Hybrid", "BUG-TRB", AssetCategory.SUPERCARS, "Next-Gen Hypercar", "Naturally aspirated Cosworth 8.3L V16 with tri-motor hybrid powertrain and Swiss gauge cluster.", 4_600_000.0, rarity = RarityTier.TROPHY, location = "Molsheim"),
        Asset("car_03", "Bugatti Divo Coachbuilt", "BUG-DIV", AssetCategory.SUPERCARS, "Track Hypercar", "1-of-40 aerodynamic coachbuilt Bugatti focused on lateral G-force.", 8_200_000.0, rarity = RarityTier.TROPHY, location = "Monaco"),
        Asset("car_04", "Rolls-Royce Phantom Extended Series II", "RR-PHN", AssetCategory.SUPERCARS, "Ultra-Luxury Sedan", "Bespoke Starlight Headliner and Gallery flagship saloon.", 645_000.0, rarity = RarityTier.RARE, location = "Goodwood"),
        Asset("car_05", "Rolls-Royce Cullinan Black Badge", "RR-CUL", AssetCategory.SUPERCARS, "Ultra-Luxury SUV", "Twin-turbo V12 luxury SUV with Viewing Suite.", 495_000.0, rarity = RarityTier.RARE, location = "London"),
        Asset("car_06", "Rolls-Royce Spectre Ultra-Luxury EV", "RR-SPC", AssetCategory.SUPERCARS, "Electric Coupé", "All-electric spiritual successor to the Phantom Coupé.", 460_000.0, rarity = RarityTier.RARE, location = "Goodwood"),
        Asset("car_07", "Bentley Continental GT Speed First Edition", "BNT-CGT", AssetCategory.SUPERCARS, "Grand Tourer", "Ultra-Performance Hybrid V8 771 hp grand tourer.", 335_000.0, rarity = RarityTier.RARE, location = "Crewe"),
        Asset("car_08", "Bentley Bentayga Extended Wheelbase Mulliner", "BNT-BTY", AssetCategory.SUPERCARS, "Luxury SUV", "Airline Seat Specification flagship Mulliner SUV.", 365_000.0, rarity = RarityTier.RARE, location = "Crewe"),
        Asset("car_09", "Lamborghini Aventador SVJ Roadster", "LMB-SVJ", AssetCategory.SUPERCARS, "V12 Supercar", "Naturally aspirated 6.5L V12 with ALA 2.0 active aerodynamics.", 920_000.0, rarity = RarityTier.ULTRA_RARE, location = "Sant'Agata"),
        Asset("car_10", "Lamborghini Revuelto V12 HPEV", "LMB-REV", AssetCategory.SUPERCARS, "Hybrid V12", "1,001 cv plug-in hybrid V12 flagship supercar.", 690_000.0, rarity = RarityTier.RARE, location = "Sant'Agata"),
        Asset("car_11", "Lamborghini Urus SE Performante", "LMB-URS", AssetCategory.SUPERCARS, "Super SUV", "Twin-turbo V8 super-SUV with torque vectoring.", 310_000.0, rarity = RarityTier.COMMON, location = "Milan"),
        Asset("car_12", "Ferrari LaFerrari Aperta", "FER-LAF", AssetCategory.SUPERCARS, "Holy Trinity Hypercar", "Open-top HY-KERS V12 Ferrari collector pinnacle.", 5_800_000.0, rarity = RarityTier.TROPHY, location = "Maranello"),
        Asset("car_13", "Ferrari SF90 XX Stradale", "FER-SF90", AssetCategory.SUPERCARS, "Road-Legal XX", "1,016 cv road-homologated special series with fixed rear wing.", 1_150_000.0, rarity = RarityTier.ULTRA_RARE, location = "Maranello"),
        Asset("car_14", "Ferrari Purosangue V12", "FER-PRO", AssetCategory.SUPERCARS, "Four-Door V12", "Naturally aspirated 6.5L V12 four-door thoroughbred with active suspension.", 540_000.0, rarity = RarityTier.RARE, location = "Maranello"),
        Asset("car_15", "McLaren P1 GTR Road Legal Conversion", "MCL-P1", AssetCategory.SUPERCARS, "Hybrid Hypercar", "Lanzante road-converted P1 GTR in Papaya Spark.", 3_400_000.0, rarity = RarityTier.TROPHY, location = "Woking"),
        Asset("car_16", "McLaren Speedtail 3-Seat Hyper-GT", "MCL-SPD", AssetCategory.SUPERCARS, "Streamliner", "250 mph central-driving-position 1-of-106 hyper-GT.", 2_850_000.0, rarity = RarityTier.TROPHY, location = "Woking"),
        Asset("car_17", "McLaren 765LT Spider MSO", "MCL-765", AssetCategory.SUPERCARS, "Longtail", "Quad-exit titanium exhaust lightweight Longtail convertible.", 560_000.0, rarity = RarityTier.RARE, location = "Woking"),
        Asset("car_18", "Koenigsegg Jesko Absolut", "KNG-JSK", AssetCategory.SUPERCARS, "Low-Drag Hypercar", "1,600 hp twin-turbo V8 with 9-speed Light Speed Transmission.", 4_100_000.0, rarity = RarityTier.TROPHY, location = "Ängelholm"),
        Asset("car_19", "Koenigsegg Gemera HV8 2,300 hp", "KNG-GMR", AssetCategory.SUPERCARS, "Mega-GT", "Four-seat 2,300 hp Hot-V8 Dark Matter electric Mega-GT.", 2_600_000.0, rarity = RarityTier.ULTRA_RARE, location = "Ängelholm"),
        Asset("car_20", "Pagani Huayra Roadster BC", "PAG-HBC", AssetCategory.SUPERCARS, "Carbo-Titanium Art", "1-of-40 AMG twin-turbo V12 open-top sculpture.", 4_400_000.0, rarity = RarityTier.TROPHY, location = "Modena"),
        Asset("car_21", "Pagani Utopia 7-Speed Gated Manual", "PAG-UTP", AssetCategory.SUPERCARS, "Analog Hypercar", "864 hp twin-turbo V12 with pure mechanical 7-speed manual gearbox.", 3_600_000.0, rarity = RarityTier.TROPHY, location = "Modena"),
        Asset("car_22", "Mercedes-Benz S 680 Guard 4MATIC Armored", "MB-S680", AssetCategory.SUPERCARS, "VR10 Armored Sedan", "VR10 ballistic protection V12 state limousine.", 680_000.0, rarity = RarityTier.RARE, location = "Stuttgart"),
        Asset("car_23", "Mercedes-Maybach S 680 Haute Voiture", "MB-MAY", AssetCategory.SUPERCARS, "Chauffeur Saloon", "Two-tone bespoke Maybach V12 with champagne flutes.", 340_000.0, rarity = RarityTier.RARE, location = "Stuttgart"),
        Asset("car_24", "Mercedes-AMG G 63 4x4² Portal Axle", "MB-G63", AssetCategory.SUPERCARS, "Extreme Off-Roader", "Portal-axle high-clearance twin-turbo V8 G-Wagen.", 380_000.0, rarity = RarityTier.RARE, location = "Graz"),
        Asset("car_25", "BMW M760e xDrive Individual Protection", "BMW-7", AssetCategory.SUPERCARS, "Executive Saloon", "Flagship executive saloon with Theatre Screen.", 195_000.0, rarity = RarityTier.COMMON, location = "Munich"),
        Asset("car_26", "BMW XM Label Red 1-of-500", "BMW-XM", AssetCategory.SUPERCARS, "M Hybrid SUV", "738 hp M Hybrid V8 flagship performance SUV.", 190_000.0, rarity = RarityTier.COMMON, location = "Munich"),
        Asset("car_27", "Porsche 911 GT3 RS (992) Weissach Package", "POR-GT3", AssetCategory.SUPERCARS, "Track Weapon", "DRS active aerodynamics 9,000 rpm flat-six track icon.", 395_000.0, rarity = RarityTier.RARE, location = "Zuffenhausen"),
        Asset("car_28", "Porsche Taycan Turbo GT Weissach", "POR-TAY", AssetCategory.SUPERCARS, "Electric Track Sedan", "1,092 hp Attack Mode electric performance record holder.", 245_000.0, rarity = RarityTier.COMMON, location = "Zuffenhausen"),
        Asset("car_29", "Aston Martin Valkyrie AMR Pro", "AST-VLK", AssetCategory.SUPERCARS, " Adrian Newey Hypercar", "Cosworth 6.5L V12 screaming to 11,000 rpm.", 4_200_000.0, rarity = RarityTier.TROPHY, location = "Gaydon"),
        Asset("car_30", "Aston Martin DB12 Volante", "AST-DB12", AssetCategory.SUPERCARS, "Super Tourer", "671 bhp hand-built twin-turbo V8 open-top Super Tourer.", 275_000.0, rarity = RarityTier.COMMON, location = "Gaydon"),
        Asset("car_31", "Range Rover SV Carmel Edition", "RR-SV", AssetCategory.SUPERCARS, "Bespoke Luxury SUV", "1-of-17 long-wheelbase four-seat SV sanctuary.", 370_000.0, rarity = RarityTier.RARE, location = "Solihull"),
        Asset("car_32", "Tesla Model S Plaid Track Package", "TSL-PLD", AssetCategory.SUPERCARS, "Tri-Motor EV", "1,020 hp carbon-ceramic brake 200 mph electric saloon.", 115_000.0, rarity = RarityTier.COMMON, location = "Austin"),
        Asset("car_33", "Lucid Air Sapphire 1,234 hp", "LCD-SPH", AssetCategory.SUPERCARS, "Tri-Motor Luxury EV", "Sub-2-second 0-60 mph tri-motor luxury performance sedan.", 250_000.0, rarity = RarityTier.RARE, location = "Casa Grande"),
        Asset("car_34", "1962 Ferrari 250 GTO Scaglietti", "FER-250", AssetCategory.SUPERCARS, "Blue-Chip Collector", "1-of-36 Colombo V12 racing legend and auction crown jewel.", 52_000_000.0, rarity = RarityTier.TROPHY, location = "Monterey"),
        Asset("car_35", "1955 Mercedes-Benz 300 SLR Uhlenhaut Coupé Ref.", "MB-300SLR", AssetCategory.SUPERCARS, "Automotive History", "The most valuable automobile ever sold; 1-of-2 gullwing prototype.", 142_000_000.0, rarity = RarityTier.HISTORIC_REFERENCE, location = "Stuttgart")
    )

    val privateJetAssets: List<Asset> = listOf(
        Asset("jet_01", "Gulfstream G700 Flagship Ultra-Long-Range", "GLF-G700", AssetCategory.PRIVATE_JETS, "Ultra-Long-Range Jet", "7,750 nm range with five living areas and 20 panoramic oval windows.", 82_000_000.0, rarity = RarityTier.TROPHY, location = "Savannah"),
        Asset("jet_02", "Gulfstream G650ER Extended Range", "GLF-G650", AssetCategory.PRIVATE_JETS, "Executive Jet", "Proven Mach 0.925 transoceanic billionaire workhorse.", 70_500_000.0, rarity = RarityTier.ULTRA_RARE, location = "Teterboro"),
        Asset("jet_03", "Bombardier Global 7500 Four-Zone", "BMB-7500", AssetCategory.PRIVATE_JETS, "Ultra-Long-Range Jet", "7,700 nm Smooth Flx Wing flagship with master suite and permanent bed.", 78_000_000.0, rarity = RarityTier.TROPHY, location = "Montreal"),
        Asset("jet_04", "Bombardier Global 8000 Mach 0.94", "BMB-8000", AssetCategory.PRIVATE_JETS, "Fastest Civil Jet", "8,000 nm range and 2,900 ft cabin altitude flagship.", 84_000_000.0, rarity = RarityTier.TROPHY, location = "Montreal"),
        Asset("jet_05", "Dassault Falcon 10X Penthouse Cabin", "DSL-10X", AssetCategory.PRIVATE_JETS, "Ultra-Widebody Bizjet", "7,500 nm French flagship with modular stateroom and shower.", 79_000_000.0, rarity = RarityTier.TROPHY, location = "Bordeaux"),
        Asset("jet_06", "Dassault Falcon 8X Trijet", "DSL-8X", AssetCategory.PRIVATE_JETS, "Long-Range Trijet", "Short-runway London City and Gstaad-capable tri-engine jet.", 62_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Geneva"),
        Asset("jet_07", "Embraer Praetor 600 Super-Midsize", "EMB-P600", AssetCategory.PRIVATE_JETS, "Super-Midsize Jet", "4,018 nm range with full fly-by-wire and Ka-band satellite Wi-Fi.", 23_500_000.0, rarity = RarityTier.RARE, location = "São José dos Campos"),
        Asset("jet_08", "Cessna Citation Longitude", "CSN-LNG", AssetCategory.PRIVATE_JETS, "Super-Midsize Jet", "Quiet cabin transcontinental corporate jet.", 29_000_000.0, rarity = RarityTier.RARE, location = "Wichita"),
        Asset("jet_09", "Boeing Business Jet BBJ 787-9 Dreamliner VVIP", "BBJ-787", AssetCategory.PRIVATE_JETS, "VVIP Widebody", "2,775 sq ft flying palace with 9,945 nm nonstop global range.", 325_000_000.0, rarity = RarityTier.TROPHY, location = "Basel"),
        Asset("jet_10", "Airbus ACJ TwoTwenty Corporate Jet", "ACJ-220", AssetCategory.PRIVATE_JETS, "VIP Airliner", "785 sq ft six-zone cabin with 5,650 nm intercontinental range.", 85_000_000.0, rarity = RarityTier.TROPHY, location = "Toulouse"),
        Asset("jet_11", "Airbus ACH145 Mercedes-Benz Style Helicopter", "ACH-145", AssetCategory.PRIVATE_JETS, "Twin-Engine Rotorcraft", "Five-blade Fenestron superyacht-compatible luxury helicopter.", 11_800_000.0, rarity = RarityTier.RARE, location = "Monaco"),
        Asset("jet_12", "Airbus ACH160 Exclusive Edition", "ACH-160", AssetCategory.PRIVATE_JETS, "Next-Gen Helicopter", "Blue Edge blades and Helionix avionics VIP rotorcraft.", 16_500_000.0, rarity = RarityTier.ULTRA_RARE, location = "Marignane"),
        Asset("jet_13", "Sikorsky S-76D VVIP Executive", "SIK-S76", AssetCategory.PRIVATE_JETS, "Executive Helicopter", "Dual Pratt & Whitney turbine head-of-state transport helicopter.", 14_200_000.0, rarity = RarityTier.RARE, location = "New York"),
        Asset("jet_14", "Leonardo AW139 Agusta VIP", "LEO-139", AssetCategory.PRIVATE_JETS, "Twin-Turbine Helicopter", "High-speed 165-knot Italian executive helicopter.", 15_400_000.0, rarity = RarityTier.RARE, location = "Milan"),
        Asset("jet_15", "Robinson R66 Turbine Marine Edition", "ROB-R66", AssetCategory.PRIVATE_JETS, "Light Turbine Heli", "Rolls-Royce RR300 turbine helicopter with pop-out floats for island hops.", 1_250_000.0, rarity = RarityTier.COMMON, location = "Miami")
    )

    val yachtAndShipAssets: List<Asset> = listOf(
        Asset("yacht_01", "Lürssen 142m Custom Giga-Yacht", "LUR-142", AssetCategory.YACHTS_SHIPS, "Megayacht", "Two helipads, submarine garage, IMAX theater, and 36-guest spa deck.", 520_000_000.0, rarity = RarityTier.TROPHY, location = "Bremen / Monaco"),
        Asset("yacht_02", "Damen SeaXplorer 77 Polar Expedition Yacht", "DMN-77", AssetCategory.YACHTS_SHIPS, "Ice-Class Explorer", " Polar Code ice-breaker hull with heli-hangar and dive center.", 135_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Antarctica / Fjord"),
        Asset("yacht_03", "Oceanco 127m Three-Masted Sailing Superyacht", "OCN-127", AssetCategory.YACHTS_SHIPS, "Sailing Megayacht", "Towering carbon-fiber masts and hybrid regeneration propulsion.", 430_000_000.0, rarity = RarityTier.TROPHY, location = "Portofino"),
        Asset("yacht_04", "Sunreef 100 Eco Solar Catamaran", "SRF-100", AssetCategory.YACHTS_SHIPS, "Luxury Catamaran", "Composite solar-skin transoceanic catamaran with 12-meter beam.", 18_500_000.0, rarity = RarityTier.RARE, location = "St. Barths"),
        Asset("yacht_05", "Feadship 95m Hybrid Superyacht", "FDS-95", AssetCategory.YACHTS_SHIPS, "Dutch Pedigree Yacht", "Glass-bottom pool and diesel-electric silent cruising.", 245_000_000.0, rarity = RarityTier.TROPHY, location = "Antibes"),
        Asset("yacht_06", "Ultra-Luxury Boutique Cruise Ship (220 Suites)", "CRS-220", AssetCategory.YACHTS_SHIPS, "Cruise Liner", "All-balcony Mediterranean and Caribbean ultra-luxury cruise ship.", 680_000_000.0, rarity = RarityTier.TROPHY, location = "Monte Carlo"),
        Asset("yacht_07", "Panamax Dry Bulk Cargo Vessel (82,000 DWT)", "BLK-82K", AssetCategory.YACHTS_SHIPS, "Commercial Maritime", "Modern eco-design Kamsarmax grain and iron ore carrier.", 38_000_000.0, rarity = RarityTier.RARE, location = "Singapore"),
        Asset("yacht_08", "Neopanamax 15,000 TEU Container Ship", "CNT-15K", AssetCategory.YACHTS_SHIPS, "Container Vessel", "Dual-fuel LNG transpacific container liner.", 165_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Rotterdam"),
        Asset("yacht_09", "VLCC Very Large Crude Carrier (300,000 DWT)", "VLC-300", AssetCategory.YACHTS_SHIPS, "Oil Supertanker", "2-million-barrel capacity double-hull crude oil supertanker.", 128_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Fujairah"),
        Asset("yacht_10", "Q-Max 174,000 m³ LNG Membrane Carrier", "LNG-174", AssetCategory.YACHTS_SHIPS, "Cryogenic LNG Ship", "Cryogenic -162°C liquefied natural gas ocean carrier.", 255_000_000.0, rarity = RarityTier.TROPHY, location = "Doha"),
        Asset("yacht_11", "Capesize Ore Carrier (180,000 DWT)", "CAP-180", AssetCategory.YACHTS_SHIPS, "Bulk Carrier", "Heavy ocean bulk carrier chartered on global mineral routes.", 64_000_000.0, rarity = RarityTier.RARE, location = "Port Hedland"),
        Asset("yacht_12", "DP3 Offshore Subsea Construction Vessel", "OSV-DP3", AssetCategory.YACHTS_SHIPS, "Offshore Support", "Dynamic positioning vessel with 400-ton active heave-compensated crane.", 92_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Aberdeen"),
        Asset("yacht_13", "J-Class Classic Racing Yacht 'Endeavour' Spec", "JCL-130", AssetCategory.YACHTS_SHIPS, "Historic Sailing", "40-meter classic regatta sailing yacht restored to concours perfection.", 29_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Palma de Mallorca"),
        Asset("yacht_14", "85m Deep-Ocean Oceanographic Research Vessel", "RES-85M", AssetCategory.YACHTS_SHIPS, "Research Vessel", "Equipped with multibeam sonar, wet labs, and ROV launch hangar.", 110_000_000.0, rarity = RarityTier.ULTRA_RARE, location = "Woods Hole"),
        Asset("yacht_15", "Triton 36000/2 Full-Ocean-Depth Submersible", "SUB-36K", AssetCategory.YACHTS_SHIPS, "Deep Submersible", "Titanium-hull two-person submersible rated to 11,000 meters (Mariana Trench).", 48_000_000.0, rarity = RarityTier.TROPHY, location = "Bahamas")
    )
}

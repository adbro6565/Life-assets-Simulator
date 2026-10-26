package com.example.data.catalogue

import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.RarityTier

object FinancialMarketsCatalogue {

    val cryptocurrencyAssets: List<Asset> = listOf(
        Asset("crypto_01", "Bitcoin", "BTC", AssetCategory.CRYPTOCURRENCIES, "Layer-1 Digital Reserve", "Official Bitcoin (BTC) decentralized proof-of-work digital gold with a 21 million hard cap.", 88_450.0, unit = "BTC", rarity = RarityTier.RARE, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_02", "Ethereum", "ETH", AssetCategory.CRYPTOCURRENCIES, "Smart Contract Platform", "Official Ethereum (ETH) programmable proof-of-stake global settlement layer.", 3_420.0, unit = "ETH", rarity = RarityTier.RARE, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_03", "Solana", "SOL", AssetCategory.CRYPTOCURRENCIES, "High-Throughput L1", "Proof-of-History high-speed decentralized execution network.", 192.50, unit = "SOL", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_04", "XRP Ledger", "XRP", AssetCategory.CRYPTOCURRENCIES, "Cross-Border Settlement", "Institutional real-time gross settlement digital asset.", 1.48, unit = "XRP", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_05", "BNB Chain", "BNB", AssetCategory.CRYPTOCURRENCIES, "Exchange Ecosystem", "Native utility and gas token of the BNB Smart Chain ecosystem.", 645.0, unit = "BNB", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_06", "Dogecoin", "DOGE", AssetCategory.CRYPTOCURRENCIES, "Community Currency", "Peer-to-peer Scrypt proof-of-work digital currency.", 0.28, unit = "DOGE", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_07", "Cardano", "ADA", AssetCategory.CRYPTOCURRENCIES, "Ouroboros PoS", "Peer-reviewed eUTXO smart contract blockchain.", 0.84, unit = "ADA", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_08", "Avalanche", "AVAX", AssetCategory.CRYPTOCURRENCIES, "Subnet Architecture", "Sub-second finality multi-chain smart contract platform.", 38.60, unit = "AVAX", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_09", "Polkadot", "DOT", AssetCategory.CRYPTOCURRENCIES, "Parachain Relay", "Heterogeneous multi-chain interoperability protocol.", 8.95, unit = "DOT", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_10", "Chainlink", "LINK", AssetCategory.CRYPTOCURRENCIES, "Decentralized Oracles", "CCIP cross-chain interoperability and verifiable market data network.", 21.40, unit = "LINK", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_11", "Litecoin", "LTC", AssetCategory.CRYPTOCURRENCIES, "Digital Silver", "Low-fee Scrypt proof-of-work payment network.", 104.0, unit = "LTC", rarity = RarityTier.COMMON, priceSourceLabel = "Reference + Simulated"),
        Asset("crypto_12", "Stellar Lumens", "XLM", AssetCategory.CRYPTOCURRENCIES, "Payment Rails", "Decentralized network for fiat-to-crypto anchors and remittances.", 0.34, unit = "XLM", rarity = RarityTier.COMMON),
        Asset("crypto_13", "Monero", "XMR", AssetCategory.CRYPTOCURRENCIES, "Privacy Coin", "RingCT and stealth-address confidential cryptocurrency.", 185.0, unit = "XMR", rarity = RarityTier.RARE),
        Asset("crypto_14", "Uniswap", "UNI", AssetCategory.CRYPTOCURRENCIES, "DeFi Automated Market Maker", "Governance token of the flagship decentralized liquidity protocol.", 13.80, unit = "UNI", rarity = RarityTier.COMMON),
        Asset("crypto_15", "Aave Protocol", "AAVE", AssetCategory.CRYPTOCURRENCIES, "DeFi Money Market", "Decentralized non-custodial liquidity and flash-loan protocol.", 275.0, unit = "AAVE", rarity = RarityTier.RARE),
        Asset("crypto_16", "Cosmos Hub", "ATOM", AssetCategory.CRYPTOCURRENCIES, "Internet of Blockchains", "IBC inter-blockchain communication hub token.", 9.40, unit = "ATOM", rarity = RarityTier.COMMON),
        Asset("crypto_17", "NEAR Protocol", "NEAR", AssetCategory.CRYPTOCURRENCIES, "Sharded Layer-1", "Nightshade sharded developer-friendly blockchain.", 6.85, unit = "NEAR", rarity = RarityTier.COMMON),
        Asset("crypto_18", "Internet Computer", "ICP", AssetCategory.CRYPTOCURRENCIES, "Decentralized Cloud", "Chain-key cryptography canister smart contract compute network.", 12.60, unit = "ICP", rarity = RarityTier.COMMON),
        Asset("crypto_19", "Ethereum Classic", "ETC", AssetCategory.CRYPTOCURRENCIES, "Proof-of-Work EVM", "Original immutable proof-of-work Ethereum chain.", 29.80, unit = "ETC", rarity = RarityTier.COMMON),
        Asset("crypto_20", "Bittensor", "TAO", AssetCategory.CRYPTOCURRENCIES, "Decentralized AI Network", "Incentivized peer-to-peer machine intelligence subnet token.", 540.0, unit = "TAO", rarity = RarityTier.RARE)
    )

    val stockAndIndexAssets: List<Asset> = listOf(
        Asset("stk_01", "Apple Inc.", "AAPL", AssetCategory.STOCKS_INDICES, "Consumer Technology", "Global ecosystem of iPhone, Mac, iPad, Apple Silicon, and Services.", 228.50, unit = "Share", rarity = RarityTier.COMMON, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_02", "Microsoft Corp.", "MSFT", AssetCategory.STOCKS_INDICES, "Enterprise & Cloud", "Azure cloud infrastructure, Windows, Enterprise Office, and AI Copilot leader.", 445.00, unit = "Share", rarity = RarityTier.COMMON, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_03", "NVIDIA Corp.", "NVDA", AssetCategory.STOCKS_INDICES, "AI Semiconductors", "Blackwell and Hopper accelerated computing GPUs and CUDA AI platform.", 138.60, unit = "Share", rarity = RarityTier.RARE, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_04", "Amazon.com Inc.", "AMZN", AssetCategory.STOCKS_INDICES, "Cloud & E-Commerce", "AWS cloud computing, Prime e-commerce, and global logistics fulfillment.", 210.40, unit = "Share", rarity = RarityTier.COMMON, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_05", "Alphabet Inc. (Google)", "GOOGL", AssetCategory.STOCKS_INDICES, "Search, Cloud & AI", "Google Search, YouTube, Android, Google Cloud, and Waymo autonomous fleet.", 186.20, unit = "Share", rarity = RarityTier.COMMON, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_06", "Meta Platforms Inc.", "META", AssetCategory.STOCKS_INDICES, "Social & AI", "Global social graph across Instagram, WhatsApp, Facebook, and Llama AI.", 592.00, unit = "Share", rarity = RarityTier.COMMON, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_07", "Tesla Inc.", "TSLA", AssetCategory.STOCKS_INDICES, "EV, Autonomy & Energy", "Electric vehicles, Megapack utility grid storage, and autonomous robotics.", 258.00, unit = "Share", rarity = RarityTier.COMMON, priceSourceLabel = "NASDAQ Reference + Simulated"),
        Asset("stk_08", "Berkshire Hathaway Class A", "BRK.A", AssetCategory.STOCKS_INDICES, "Conglomerate Holding", "Insurance float, BNSF Railway, energy utilities, and flagship equity portfolio.", 685_000.00, unit = "Class A Share", rarity = RarityTier.TROPHY),
        Asset("stk_09", "JPMorgan Chase & Co.", "JPM", AssetCategory.STOCKS_INDICES, "Global Banking", "Tier-1 fortress balance sheet investment and commercial bank.", 232.00, unit = "Share", rarity = RarityTier.COMMON),
        Asset("stk_10", "Walmart Inc.", "WMT", AssetCategory.STOCKS_INDICES, "Global Retail", "Omnichannel hypermarket and global retail supply chain leader.", 84.50, unit = "Share", rarity = RarityTier.COMMON),
        Asset("stk_11", "Taiwan Semiconductor (TSMC)", "TSM", AssetCategory.STOCKS_INDICES, "Semiconductor Foundry", "2nm and 3nm advanced logic wafer fabrication leader.", 194.00, unit = "ADR", rarity = RarityTier.RARE),
        Asset("stk_12", "Samsung Electronics", "SMSN", AssetCategory.STOCKS_INDICES, "Memory & Displays", "HBM3e memory, NAND flash, and AMOLED display conglomerate.", 1_240.00, unit = "GDR", rarity = RarityTier.COMMON),
        Asset("stk_13", "Toyota Motor Corp.", "TM", AssetCategory.STOCKS_INDICES, "Automotive", "Global hybrid and solid-state battery automotive manufacturer.", 182.00, unit = "ADR", rarity = RarityTier.COMMON),
        Asset("stk_14", "Saudi Aramco Energy", "ARMCO", AssetCategory.STOCKS_INDICES, "Integrated Energy", "Lowest-lifting-cost hydrocarbon producer institutional basket.", 740.00, unit = "Basket", rarity = RarityTier.RARE),
        Asset("stk_15", "ASML Holding NV", "ASML", AssetCategory.STOCKS_INDICES, "Lithography", "Sole supplier of High-NA EUV extreme ultraviolet lithography machines.", 760.00, unit = "Share", rarity = RarityTier.RARE),
        Asset("stk_16", "LVMH Moët Hennessy Louis Vuitton", "LVMH", AssetCategory.STOCKS_INDICES, "Luxury Conglomerate", "75 prestige houses across fashion, watches, jewelry, and champagne.", 695.00, unit = "Share", rarity = RarityTier.RARE),
        Asset("stk_17", "Eli Lilly and Co.", "LLY", AssetCategory.STOCKS_INDICES, "Biopharma", "Metabolic health, oncology, and neuroscience biopharmaceutical leader.", 890.00, unit = "Share", rarity = RarityTier.RARE),
        Asset("stk_18", "S&P 500 Index ETF (SPY)", "SPY", AssetCategory.STOCKS_INDICES, "US Large-Cap Index", "500 leading publicly traded US corporations benchmark ETF.", 585.00, unit = "Unit", rarity = RarityTier.COMMON),
        Asset("stk_19", "Nasdaq-100 Tech Index ETF (QQQ)", "QQQ", AssetCategory.STOCKS_INDICES, "Global Tech Index", "100 largest non-financial companies listed on Nasdaq.", 505.00, unit = "Unit", rarity = RarityTier.COMMON),
        Asset("stk_20", "FTSE All-World Global Equity ETF", "VWRL", AssetCategory.STOCKS_INDICES, "Global Equity ETF", "Developed and emerging market global equity basket.", 138.00, unit = "Unit", rarity = RarityTier.COMMON),
        Asset("stk_21", "USD/NPR Forex Institutional Lot", "USD/NPR", AssetCategory.STOCKS_INDICES, "Foreign Exchange", "US Dollar to Nepalese Rupee FX reference contract (Rate ~134.5 NPR).", 100_000.00, unit = "FX Lot", rarity = RarityTier.COMMON, priceSourceLabel = "FX Reference / Simulated"),
        Asset("stk_22", "EUR/USD Major Forex Lot", "EUR/USD", AssetCategory.STOCKS_INDICES, "Foreign Exchange", "Euro to US Dollar interbank currency pair contract.", 108_400.00, unit = "FX Lot", rarity = RarityTier.COMMON, priceSourceLabel = "FX Reference / Simulated"),
        Asset("stk_23", "GBP/USD Cable Forex Lot", "GBP/USD", AssetCategory.STOCKS_INDICES, "Foreign Exchange", "British Pound Sterling to US Dollar currency pair contract.", 129_800.00, unit = "FX Lot", rarity = RarityTier.COMMON, priceSourceLabel = "FX Reference / Simulated"),
        Asset("stk_24", "USD/JPY Yen Carry Forex Lot", "USD/JPY", AssetCategory.STOCKS_INDICES, "Foreign Exchange", "US Dollar to Japanese Yen macro currency contract.", 100_000.00, unit = "FX Lot", rarity = RarityTier.COMMON, priceSourceLabel = "FX Reference / Simulated"),
        Asset("stk_25", "US 10-Year Treasury Bond Strip", "UST-10Y", AssetCategory.STOCKS_INDICES, "Sovereign Debt", "Sovereign US Treasury fixed-income institutional note.", 965_000.00, unit = "Note", rarity = RarityTier.RARE)
    )
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.LimitOrderEntity
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.Candle
import com.example.data.model.MarketTick
import com.example.data.model.OrderBookEntry
import com.example.data.model.QuoteFreshnessStatus
import com.example.data.model.ReferencePriceType
import com.example.data.model.Timeframe
import com.example.ui.components.AssetBrandBadge
import com.example.ui.components.AssetHeroPicture
import com.example.ui.components.CandlestickChart
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.MiniSparklineChart
import com.example.ui.components.SubtleCardBorder
import com.example.ui.components.shortAssetDisplayName
import com.example.ui.theme.BearRed
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PortfolioMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MarketCategoryFilter(val label: String) {
    ALL("All"),
    WATCHLIST("Watchlist ★"),
    CRYPTO("Crypto"),
    STOCKS("Stocks"),
    METALS("Metals"),
    COMMODITIES("Commodities"),
    REAL_ESTATE("Real Estate"),
    VEHICLES("Vehicles"),
    LUXURY("Luxury")
}

@Composable
fun MarketsScreen(
    selectedAssetId: String,
    selectedTimeframe: Timeframe,
    ticks: Map<String, MarketTick>,
    candles: List<Candle>,
    orderBook: Pair<List<OrderBookEntry>, List<OrderBookEntry>>,
    openLimitOrders: List<LimitOrderEntity>,
    ownedQuantity: Double,
    watchedAssetIds: Set<String> = emptySet(),
    portfolioMetrics: PortfolioMetrics = PortfolioMetrics(),
    onToggleWatchlist: (String) -> Unit = {},
    onSelectAsset: (String) -> Unit,
    onSelectTimeframe: (Timeframe) -> Unit,
    onBuyClick: (Asset) -> Unit,
    onSellClick: (Asset) -> Unit,
    onCancelLimitOrder: (String) -> Unit,
    onOpenAssetDetail: (Asset) -> Unit = { asset -> onSelectAsset(asset.id) }
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf(MarketCategoryFilter.ALL) }
    var showProChartMode by rememberSaveable { mutableStateOf(false) }

    val priorityIds = remember {
        listOf(
            "crypto_01", // Bitcoin (BTC)
            "crypto_02", // Ethereum (ETH)
            "stk_01",    // Apple (AAPL)
            "stk_02",    // Microsoft (MSFT)
            "met_01",    // Gold (XAU)
            "met_02",    // Silver (XAG)
            "stk_07",    // Tesla (TSLA)
            "stk_04",    // Amazon (AMZN)
            "stk_03",    // NVIDIA (NVDA)
            "stk_05",    // Google (GOOGL)
            "met_06"     // Crude Oil (USO-WTI)
        )
    }

    val filteredAssets = remember(searchQuery, activeFilter, watchedAssetIds) {
        val base = when (activeFilter) {
            MarketCategoryFilter.ALL -> {
                val priorityAssets = priorityIds.mapNotNull { AssetCatalogue.getAssetById(it) }
                val prioritySet = priorityIds.toSet()
                val rest = AssetCatalogue.allAssets.filter { it.id !in prioritySet }
                priorityAssets + rest
            }
            MarketCategoryFilter.WATCHLIST -> AssetCatalogue.allAssets.filter { it.id in watchedAssetIds }
            MarketCategoryFilter.CRYPTO -> AssetCatalogue.allAssets.filter { it.category == AssetCategory.CRYPTOCURRENCIES }
            MarketCategoryFilter.STOCKS -> AssetCatalogue.allAssets.filter { it.category == AssetCategory.STOCKS_INDICES }
            MarketCategoryFilter.METALS -> AssetCatalogue.allAssets.filter {
                it.category == AssetCategory.PRECIOUS_METALS && !it.id.startsWith("met_06") && !it.name.contains("Oil", ignoreCase = true)
            }
            MarketCategoryFilter.COMMODITIES -> AssetCatalogue.allAssets.filter {
                it.id == "met_06" || it.name.contains("Oil", ignoreCase = true) ||
                    it.name.contains("Gas", ignoreCase = true) ||
                    it.category == AssetCategory.AGRICULTURAL ||
                    it.category == AssetCategory.INDUSTRIAL ||
                    it.category == AssetCategory.PRECIOUS_METALS
            }
            MarketCategoryFilter.REAL_ESTATE -> AssetCatalogue.allAssets.filter {
                it.category == AssetCategory.LUXURY_REAL_ESTATE || it.category == AssetCategory.PRIVATE_ISLANDS
            }
            MarketCategoryFilter.VEHICLES -> AssetCatalogue.allAssets.filter {
                it.category == AssetCategory.SUPERCARS || it.category == AssetCategory.YACHTS_SHIPS ||
                    it.category == AssetCategory.PRIVATE_JETS || it.category == AssetCategory.TRANSPORT_LOGISTICS
            }
            MarketCategoryFilter.LUXURY -> AssetCatalogue.allAssets.filter {
                it.category == AssetCategory.LUXURY_WATCHES || it.category == AssetCategory.RARE_COLLECTIBLES ||
                    it.category == AssetCategory.FINE_ART || it.category == AssetCategory.JEWELRY_DIAMONDS ||
                    it.category == AssetCategory.DESIGNER_FASHION || it.category == AssetCategory.BUSINESSES
            }
        }

        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            base
        } else {
            base.filter { asset ->
                asset.name.lowercase().contains(q) ||
                    asset.symbol.lowercase().contains(q) ||
                    asset.category.displayName.lowercase().contains(q) ||
                    shortAssetDisplayName(asset).lowercase().contains(q)
            }
        }
    }

    val selectedAsset = AssetCatalogue.getAssetById(selectedAssetId) ?: AssetCatalogue.allAssets.first()
    val currentTick = ticks[selectedAsset.id]
    val selectedMetadata = selectedAsset.valuationMetadata
    val currentPrice = currentTick?.currentPrice ?: selectedAsset.basePrice
    val referencePrice = currentTick?.realWorldReferencePrice ?: selectedMetadata.realWorldReferencePrice
    val activePriceType = currentTick?.priceType ?: selectedMetadata.priceType
    val activeFreshness = currentTick?.freshnessStatus ?: QuoteFreshnessStatus.FRESH
    val changePct = currentTick?.changePercent24h ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Header: "2D Market Exchange" + Pro Terminal toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Markets & 2D Exchange",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Real Reference Value vs. Virtual Execution Price",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .border(1.dp, EmeraldAccent, RoundedCornerShape(10.dp))
                    .clickable { showProChartMode = !showProChartMode }
                    .testTag("toggle_pro_terminal_button")
            ) {
                Text(
                    text = if (showProChartMode) "Asset List" else "2D Terminal & Order Book",
                    color = EmeraldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Portfolio Performance Strip (Separates $3M Asset Value, Real Reference Value & $200K Cash)
        Surface(
            color = DarkCardBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SubtleCardBorder, RoundedCornerShape(12.dp))
                .testTag("markets_portfolio_performance_strip")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PORTFOLIO ASSETS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$${String.format("%,.0f", portfolioMetrics.investedMarketValue)}",
                        color = EmeraldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REAL REF VALUE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$${String.format("%,.0f", portfolioMetrics.realWorldReferencePortfolioValue)}",
                        color = MetallicGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("CASH BALANCE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$${String.format("%,.0f", portfolioMetrics.cashBalance)}",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search assets (name, ticker, variant, model...)",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search assets",
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("markets_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkCardBg,
                unfocusedContainerColor = DarkCardBg,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = SubtleCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Pills including Watchlist ★
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MarketCategoryFilter.entries) { filter ->
                val isSelected = activeFilter == filter
                Surface(
                    color = if (isSelected) EmeraldAccent else DarkCardBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) EmeraldAccent else SubtleCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeFilter = filter }
                        .testTag("markets_filter_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) ObsidianBlack else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!showProChartMode) {
            // 2D Exchange Asset List with Watchlist Star, Sparkline, Real Reference Value & Virtual Execution Price
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filteredAssets, key = { it.id }) { asset ->
                    val tick = ticks[asset.id]
                    val metadata = asset.valuationMetadata
                    val price = tick?.currentPrice ?: asset.basePrice
                    val refPrice = tick?.realWorldReferencePrice ?: metadata.realWorldReferencePrice
                    val pct = tick?.changePercent24h ?: 0.0
                    val isUp = pct >= 0.0
                    val priceType = tick?.priceType ?: metadata.priceType
                    val freshness = tick?.freshnessStatus ?: QuoteFreshnessStatus.FRESH
                    val isWatched = asset.id in watchedAssetIds

                    Surface(
                        color = DarkCardBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectAsset(asset.id)
                                onOpenAssetDetail(asset)
                            }
                            .testTag("market_asset_row_${asset.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1.45f)
                            ) {
                                IconButton(
                                    onClick = { onToggleWatchlist(asset.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("watchlist_star_${asset.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isWatched) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Toggle Watchlist",
                                        tint = if (isWatched) MetallicGold else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                AssetBrandBadge(asset = asset, size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = shortAssetDisplayName(asset),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${asset.symbol} • ${priceType.label}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (refPrice != null) {
                                            "Real Ref: $${String.format("%,.2f", refPrice)} (${metadata.dataProvider})"
                                        } else {
                                            "PRICE UNAVAILABLE"
                                        },
                                        color = if (freshness == QuoteFreshnessStatus.STALE || refPrice == null) BearRed else MetallicGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            MiniSparklineChart(
                                assetId = asset.id,
                                isPositive = isUp,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(24.dp)
                            )

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (refPrice != null) {
                                        "$${String.format("%,.2f", price)}"
                                    } else {
                                        "PRICE UNAVAILABLE"
                                    },
                                    color = if (refPrice != null) TextPrimary else BearRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (!asset.isTradable) {
                                        "Reference Only"
                                    } else {
                                        String.format("Virtual %+.2f%%", pct)
                                    },
                                    color = if (!asset.isTradable) MetallicGold else if (isUp) EmeraldAccent else BearRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Pro 2D Crypto-Exchange Terminal Mode: Real Asset Photo, Dual Valuation, Candlesticks, 2D Order Book & Limit Orders
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredAssets.take(30), key = { it.id }) { asset ->
                            val isSelected = asset.id == selectedAsset.id
                            Surface(
                                color = if (isSelected) ElevatedPanel else CarbonSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isSelected) EmeraldAccent else GlassBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectAsset(asset.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssetBrandBadge(asset = asset, size = 24.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = asset.symbol,
                                        color = if (isSelected) EmeraldAccent else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = DarkCardBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SubtleCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    AssetBrandBadge(asset = selectedAsset, size = 42.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${shortAssetDisplayName(selectedAsset)} (${selectedAsset.symbol})",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${selectedMetadata.brand} • ${selectedMetadata.model} • ${selectedMetadata.unit}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Source: ${currentTick?.dataProvider ?: selectedMetadata.dataProvider} • ${activePriceType.label} (${activeFreshness.label})",
                                            color = EmeraldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onToggleWatchlist(selectedAsset.id) }
                                ) {
                                    val isWatched = selectedAsset.id in watchedAssetIds
                                    Icon(
                                        imageVector = if (isWatched) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Watchlist",
                                        tint = if (isWatched) MetallicGold else TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Real Asset Photo Banner inside 2D Exchange Terminal
                            AssetHeroPicture(
                                asset = selectedAsset,
                                pictureIndex = 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Side-by-Side: REAL REFERENCE VALUE vs VIRTUAL GAME EXECUTION PRICE
                            Surface(
                                color = ObsidianBlack,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .testTag("terminal_dual_price_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "REAL REFERENCE VALUE",
                                            color = MetallicGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (referencePrice != null) {
                                                "$${String.format("%,.2f", referencePrice)} ${selectedMetadata.currency}"
                                            } else {
                                                "PRICE UNAVAILABLE"
                                            },
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${activePriceType.label} • ${selectedMetadata.confidenceLevel.label}",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "VIRTUAL GAME EXECUTION PRICE",
                                            color = CyberCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (selectedAsset.isTradable && referencePrice != null) {
                                                "$${String.format("%,.2f", currentPrice)} USD"
                                            } else {
                                                "Non-Tradable"
                                            },
                                            color = if (changePct >= 0) EmeraldAccent else BearRed,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = String.format("%+.2f%% (24h) • Held: %.2f", changePct, ownedQuantity),
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 24h Market Stats Strip (High / Low / Volume / Timestamp)
                            val updatedTimeStr = remember(currentTick?.updatedAtMillis) {
                                SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US)
                                    .format(Date(currentTick?.updatedAtMillis ?: System.currentTimeMillis()))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "24h High: $${String.format("%,.2f", currentTick?.high24h ?: currentPrice)}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "24h Low: $${String.format("%,.2f", currentTick?.low24h ?: currentPrice)}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Updated: $updatedTimeStr",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            CandlestickChart(
                                candles = candles,
                                selectedTimeframe = selectedTimeframe,
                                onTimeframeSelected = onSelectTimeframe,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2D ORDER BOOK (BIDS vs ASKS DEPTH)
                            val (bids, asks) = orderBook
                            Surface(
                                color = ObsidianBlack,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .testTag("terminal_order_book_panel")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "2D ORDER BOOK DEPTH (BIDS)",
                                            color = EmeraldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "ASKS (SELL WALL)",
                                            color = BearRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val rowCount = maxOf(bids.size, asks.size).coerceAtMost(5)
                                    for (i in 0 until rowCount) {
                                        val bid = bids.getOrNull(i)
                                        val ask = asks.getOrNull(i)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = bid?.let { "${String.format("%.2f", it.quantity)} @ $${String.format("%,.2f", it.price)}" } ?: "-",
                                                color = EmeraldAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = ask?.let { "$${String.format("%,.2f", it.price)} × ${String.format("%.2f", it.quantity)}" } ?: "-",
                                                color = BearRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            if (openLimitOrders.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = GlassBorder)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "OPEN LIMIT ORDERS (${openLimitOrders.size})",
                                    color = MetallicGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                openLimitOrders.take(4).forEach { order ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${order.side} ${order.quantity} ${order.assetSymbol} @ $${String.format("%,.2f", order.limitPrice)}",
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "CANCEL",
                                            color = BearRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.clickable { onCancelLimitOrder(order.orderId) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onBuyClick(selectedAsset) },
                                    enabled = selectedAsset.isTradable && referencePrice != null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("terminal_buy_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldAccent,
                                        contentColor = ObsidianBlack
                                    )
                                ) {
                                    Text("Buy", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                }

                                Button(
                                    onClick = { onSellClick(selectedAsset) },
                                    enabled = selectedAsset.isTradable && referencePrice != null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("terminal_sell_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BearRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Sell", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

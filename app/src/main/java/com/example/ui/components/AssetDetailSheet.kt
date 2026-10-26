package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.Candle
import com.example.data.model.GoldKaratPurity
import com.example.data.model.MarketTick
import com.example.data.model.OrderBookEntry
import com.example.data.model.QuoteFreshnessStatus
import com.example.data.model.RarityTier
import com.example.data.model.ReferencePriceType
import com.example.data.model.Timeframe
import com.example.engine.MarketDataBackendService
import com.example.ui.theme.BearRed
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DETAIL_TIMEFRAME_LABELS = listOf("1D", "1W", "1M", "3M", "1Y", "ALL")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailSheet(
    asset: Asset,
    tick: MarketTick?,
    ownedQuantity: Double,
    averageCostBasis: Double,
    getCandles: (Timeframe) -> List<Candle>,
    orderBook: Pair<List<OrderBookEntry>, List<OrderBookEntry>>,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onDismiss: () -> Unit,
    fxRates: Map<String, Double> = mapOf("USD" to 1.0, "NPR" to 134.50, "EUR" to 0.9225, "GBP" to 0.7710, "AED" to 3.6725),
    selectedDisplayCurrency: String = "USD",
    fxSourceLabel: String = "ECB / Open Exchange Rates Verified Reference",
    spotGoldPerOzUsd: Double = 2_700.0,
    spotSilverPerOzUsd: Double = 32.50,
    onRefreshQuote: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTfIndex by remember { mutableIntStateOf(0) }
    var selectedPictureIndex by remember { mutableIntStateOf(0) }
    var showRealWorldPhotos by remember { mutableStateOf(true) }
    var selectedKarat by remember { mutableStateOf(GoldKaratPurity.KARAT_24) }

    val metadata = remember(asset.id) { asset.valuationMetadata }
    val mappedTimeframe = when (selectedTfIndex) {
        0 -> Timeframe.M1
        1 -> Timeframe.M5
        2 -> Timeframe.M15
        else -> Timeframe.H1
    }

    val candles = remember(asset.id, tick?.updatedAtMillis, selectedTfIndex) {
        getCandles(mappedTimeframe)
    }
    val virtualPrice = tick?.currentPrice ?: asset.basePrice
    val realWorldReferencePrice = tick?.realWorldReferencePrice ?: metadata.realWorldReferencePrice
    val changePct = tick?.changePercent24h ?: 0.0
    val isUp = changePct >= 0.0

    val activePriceType = tick?.priceType ?: metadata.priceType
    val freshnessStatus = tick?.freshnessStatus ?: if (!asset.isTradable || asset.rarity == RarityTier.HISTORIC_REFERENCE) {
        QuoteFreshnessStatus.NON_TRADABLE_HERITAGE
    } else {
        QuoteFreshnessStatus.FRESH
    }

    val canTradeAsset = asset.isTradable &&
        asset.rarity != RarityTier.HISTORIC_REFERENCE &&
        metadata.isPurchasableInRealWorld &&
        metadata.priceType != ReferencePriceType.REFERENCE_PRICE_UNAVAILABLE &&
        freshnessStatus != QuoteFreshnessStatus.STALE

    val marketHoursLabel = remember(asset.category) {
        when (asset.category) {
            AssetCategory.CRYPTOCURRENCIES, AssetCategory.PRECIOUS_METALS -> "24/7 Continuous Spot Market"
            AssetCategory.STOCKS_INDICES -> {
                if (MarketDataBackendService.isUsEquityMarketOpen()) {
                    "NYSE/NASDAQ Regular Session Open (09:30–16:00 EST)"
                } else {
                    "Market Closed • Showing Official Close (09:30–16:00 EST)"
                }
            }
            else -> metadata.availabilityStatus
        }
    }

    val lastUpdatedLabel = remember(tick?.updatedAtMillis) {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss 'UTC'", Locale.US)
        formatter.format(Date(tick?.updatedAtMillis ?: System.currentTimeMillis()))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkNavyBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("asset_detail_sheet")
        ) {
            // Top Bar: Back Button + Quote Status Pill + Refresh / Photo Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // Price Type & Freshness Pill
                val badgeColor = when {
                    freshnessStatus == QuoteFreshnessStatus.STALE -> BearRed
                    freshnessStatus == QuoteFreshnessStatus.NON_TRADABLE_HERITAGE -> BrightGold
                    activePriceType == ReferencePriceType.LIVE_QUOTE -> EmeraldAccent
                    activePriceType == ReferencePriceType.OFFICIAL_CLOSE || activePriceType == ReferencePriceType.DELAYED_QUOTE -> CyberCyan
                    else -> MetallicGold
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .testTag("detail_quote_status_badge")
                ) {
                    Text(
                        text = "${activePriceType.label} • ${freshnessStatus.label}",
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onRefreshQuote()
                        showRealWorldPhotos = !showRealWorldPhotos
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Quote & Toggle Photos",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Asset Identity Header: Circular Brand Badge + Name & Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssetBrandBadge(asset = asset, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = shortAssetDisplayName(asset),
                            color = TextPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${asset.symbol})",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${metadata.brand} • ${metadata.unit} • ${metadata.country}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Price Display + 24h Change
            Text(
                text = if (realWorldReferencePrice != null) {
                    "$${String.format("%,.2f", virtualPrice)}"
                } else {
                    "PRICE UNAVAILABLE"
                },
                color = if (realWorldReferencePrice != null) TextPrimary else BearRed,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isUp) EmeraldAccent else BearRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (!asset.isTradable || asset.rarity == RarityTier.HISTORIC_REFERENCE) {
                        "${activePriceType.label} (${metadata.confidenceLevel.label})"
                    } else {
                        String.format("%+.2f%% (24h) • %s", changePct, activePriceType.label)
                    },
                    color = if (isUp) EmeraldAccent else BearRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DUAL VALUATION PANEL: (1) Real-World Reference Value vs. (2) LifeAssets Virtual Trading Value
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MetallicGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .testTag("dual_valuation_comparison_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MetallicGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REAL-WORLD REFERENCE VS. VIRTUAL GAME EXECUTION PRICE",
                            color = MetallicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Real-World Reference Value",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (realWorldReferencePrice != null) {
                                    "$${String.format("%,.2f", realWorldReferencePrice)} ${metadata.currency}"
                                } else {
                                    "PRICE UNAVAILABLE"
                                },
                                color = if (realWorldReferencePrice != null) TextPrimary else BearRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${activePriceType.label} • ${metadata.unit}",
                                color = EmeraldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "2. Virtual Game Execution Price",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (canTradeAsset) {
                                    "$${String.format("%,.2f", virtualPrice)} USD"
                                } else if (realWorldReferencePrice == null) {
                                    "PRICE UNAVAILABLE"
                                } else {
                                    "Non-Tradable Reference"
                                },
                                color = if (canTradeAsset) CyberCyan else BrightGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (canTradeAsset) "Virtual Execution + 0.50% Fee" else "Museum / Heritage Lock",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = GlassBorder)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Source: ${tick?.dataProvider ?: metadata.dataProvider} • ${metadata.dataSource}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Methodology: ${metadata.priceMethodology}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MarketDataBackendService.PROVIDER_COMPLIANCE_DISCLOSURE,
                        color = TextMuted,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            // MONA LISA / HISTORIC REFERENCE / NON-PURCHASABLE HERITAGE WARNING BANNER
            if (metadata.historicalContextNote != null || !canTradeAsset) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = BrightGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrightGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .testTag("heritage_reference_notice_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (freshnessStatus == QuoteFreshnessStatus.STALE) Icons.Default.Warning else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (freshnessStatus == QuoteFreshnessStatus.STALE) BearRed else BrightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    asset.id == "art_01" -> "SOVEREIGN MUSEUM HERITAGE • NON-PURCHASABLE"
                                    freshnessStatus == QuoteFreshnessStatus.STALE -> "STALE MARKET QUOTE • REFRESH REQUIRED"
                                    else -> "VERIFIED HISTORICAL / INSTITUTIONAL VALUATION"
                                },
                                color = if (freshnessStatus == QuoteFreshnessStatus.STALE) BearRed else BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = metadata.historicalContextNote
                                ?: "Quote status is ${freshnessStatus.label}. Refresh live market data to trade.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // GOLD & SILVER SPOT + PURITY (24K / 22K / 18K) & TROY OUNCE / GRAM CALCULATOR
            if (asset.category == AssetCategory.PRECIOUS_METALS || asset.category == AssetCategory.JEWELRY_DIAMONDS) {
                Spacer(modifier = Modifier.height(12.dp))
                val isSilverAsset = asset.id == "met_02" || asset.name.contains("Silver", ignoreCase = true)
                val spotOz = if (isSilverAsset) spotSilverPerOzUsd else spotGoldPerOzUsd
                val defaultGrams = when {
                    asset.id == "met_01" -> 400.0 * MarketDataBackendService.GRAMS_PER_TROY_OUNCE
                    asset.id == "met_02" -> 1000.0 * MarketDataBackendService.GRAMS_PER_TROY_OUNCE
                    asset.category == AssetCategory.JEWELRY_DIAMONDS -> 100.0
                    else -> MarketDataBackendService.GRAMS_PER_TROY_OUNCE
                }
                val workmanshipPct = if (asset.category == AssetCategory.JEWELRY_DIAMONDS) 35.0 else 0.0
                val metalBreakdown = remember(spotOz, selectedKarat, defaultGrams, workmanshipPct) {
                    MarketDataBackendService.calculatePreciousMetalBreakdown(
                        metalSymbol = if (isSilverAsset) "XAG" else "XAU",
                        spotPricePerTroyOzUsd = spotOz,
                        weightGrams = defaultGrams,
                        purity = selectedKarat,
                        workmanshipPremiumPct = workmanshipPct
                    )
                }

                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        .testTag("precious_metal_purity_calculator")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = MetallicGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SPOT BULLION & KARAT PURITY CALCULATOR (TROY OZ / GRAM)",
                                color = MetallicGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoldKaratPurity.entries.forEach { purity ->
                                val selected = selectedKarat == purity
                                Surface(
                                    color = if (selected) MetallicGold else ObsidianBlack,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedKarat = purity }
                                ) {
                                    Text(
                                        text = "${purity.karat}K (${String.format("%.1f%%", purity.fineness * 100)})",
                                        color = if (selected) ObsidianBlack else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Spot per Troy Oz (31.1035g): $${String.format("%,.2f", metalBreakdown.spotPricePerTroyOzUsd)} • Spot per Gram (24K): $${String.format("%,.2f", metalBreakdown.spotPricePerGramUsd)}/g",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selectedKarat.label} Pure Melt Value: $${String.format("%,.2f", metalBreakdown.pureMetalMeltValueUsd)}",
                            color = EmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (workmanshipPct > 0.0) {
                            Text(
                                text = "Retail Workmanship / Gem Setting Premium (+${workmanshipPct.toInt()}%): $${String.format("%,.2f", metalBreakdown.workmanshipPremiumUsd)} (Separated from raw metal spot value)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // FOREIGN EXCHANGE (FX) MULTI-CURRENCY REFERENCE CONVERSION ROW
            if (realWorldReferencePrice != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
                        .testTag("fx_conversion_panel")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FX MULTI-CURRENCY REFERENCE CONVERSION ($fxSourceLabel)",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val displayCurrencies = listOf("NPR", "EUR", "GBP", "AED", "JPY")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            displayCurrencies.forEach { code ->
                                val rate = fxRates[code] ?: 1.0
                                val converted = realWorldReferencePrice * rate
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = code,
                                        color = if (code == selectedDisplayCurrency) MetallicGold else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatCompactCurrency(converted),
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe Selector Row: 1D | 1W | 1M | 3M | 1Y | ALL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DETAIL_TIMEFRAME_LABELS.forEachIndexed { idx, label ->
                    val isSelected = selectedTfIndex == idx
                    Surface(
                        color = if (isSelected) Color(0xFF2563EB) else DarkCardBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF3B82F6) else SubtleCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTfIndex = idx }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Candlestick & Volume Chart
            CandlestickChart(
                candles = candles,
                selectedTimeframe = mappedTimeframe,
                onTimeframeSelected = { tf ->
                    selectedTfIndex = when (tf) {
                        Timeframe.M1 -> 0
                        Timeframe.M5 -> 1
                        Timeframe.M15 -> 2
                        else -> 3
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Real-World Asset Photos (View 1 & View 2)
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleCardBorder, RoundedCornerShape(14.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRealWorldPhotos = !showRealWorldPhotos }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Real-World Asset Photos (View 1 & View 2)",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Photo 1", "Photo 2").forEachIndexed { idx, label ->
                                val isSelected = selectedPictureIndex == idx
                                Surface(
                                    color = if (isSelected) EmeraldAccent else ObsidianBlack,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable {
                                        selectedPictureIndex = idx
                                        showRealWorldPhotos = true
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) ObsidianBlack else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showRealWorldPhotos) {
                        AssetHeroPicture(
                            asset = asset,
                            pictureIndex = selectedPictureIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About This Asset Section
            Text(
                text = "About This Asset & Provenance",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = asset.description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Verified Pricing Database Metadata Rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetMetadataRow(
                    icon = Icons.Default.Public,
                    label = "Brand / Model / Variant / Year",
                    value = "${metadata.brand} • ${metadata.model} (${metadata.variant}${metadata.year?.let { " • $it" } ?: ""})"
                )
                AssetMetadataRow(
                    icon = Icons.Default.Gavel,
                    label = "Market & Data Confidence",
                    value = "${metadata.market} • ${metadata.confidenceLevel.label}"
                )
                AssetMetadataRow(
                    icon = Icons.Default.AccessTime,
                    label = "Market Hours & Availability",
                    value = marketHoursLabel
                )
                AssetMetadataRow(
                    icon = Icons.Default.Update,
                    label = "Last Updated & Quote Currency",
                    value = "$lastUpdatedLabel • ${metadata.currency} per ${metadata.unit}"
                )
                AssetMetadataRow(
                    icon = Icons.Default.Verified,
                    label = "Photograph Source, License & Status",
                    value = "${asset.imageSource} • ${asset.licenseInfo} • ${asset.verificationStatus.label}"
                )
            }

            if (ownedQuantity > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MetallicGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your Position: $ownedQuantity ${asset.symbol}",
                            color = MetallicGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Avg Cost: $${String.format("%,.2f", averageCostBasis)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Action Buttons: Buy & Sell (Disabled for Non-Purchasable Heritage like Mona Lisa or Stale Quotes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (canTradeAsset) {
                            scope.launch {
                                sheetState.hide()
                                onBuyClick()
                            }
                        }
                    },
                    enabled = canTradeAsset,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("detail_buy_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = ObsidianBlack,
                        disabledContainerColor = GlassBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(
                        text = if (canTradeAsset) "Buy" else "Non-Tradable",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = {
                        if (canTradeAsset) {
                            scope.launch {
                                sheetState.hide()
                                onSellClick()
                            }
                        }
                    },
                    enabled = canTradeAsset,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("detail_sell_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BearRed,
                        contentColor = Color.White,
                        disabledContainerColor = GlassBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(
                        text = if (canTradeAsset) "Sell" else "Reference Only",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatCompactCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000.0 -> String.format("%.2fB", value / 1_000_000_000.0)
        value >= 1_000_000.0 -> String.format("%.2fM", value / 1_000_000.0)
        value >= 1_000.0 -> String.format("%,.0f", value)
        else -> String.format("%.2f", value)
    }
}

@Composable
private fun AssetMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        color = DarkCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtleCardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.engine.MarketDataBackendService
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.LifeAssetsBrandHeader
import com.example.ui.components.SubtleCardBorder
import com.example.ui.theme.BearRed
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun InternetRequiredScreen(
    connectionStatus: ConnectionStatus,
    username: String?,
    preservedNetWorthUsd: Double,
    onRetryConnection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("internet_required_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LifeAssetsBrandHeader(compact = true)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(BearRed.copy(alpha = 0.14f))
                    .border(1.5.dp, BearRed.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Internet Connection Required",
                    tint = BearRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                color = BearRed.copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, BearRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = "STATUS: ${connectionStatus.label.uppercase()}",
                    color = BearRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Internet Connection Required",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "LifeAssets operates on an Online-Only Real-World Asset Price System. Offline trading, offline daily reward claiming, and stale cached price execution are strictly disabled to guarantee pricing integrity.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Account & Data Preservation Assurance Card
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Account & Portfolio Safely Preserved",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Trader: @${username ?: "Sovereign_Titan"} • Preserved Net Worth: $${String.format("%,.2f", preservedNetWorthUsd)}",
                        color = MetallicGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No balances, holdings, or transactions are ever deleted while offline. Reconnecting will synchronize live market quotes safely without duplicate orders.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetryConnection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("internet_retry_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = ObsidianBlack
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry Connection",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (connectionStatus == ConnectionStatus.CONNECTING) {
                        "Connecting to Market Backend..."
                    } else {
                        "Retry Connection & Sync Live Prices"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Text(
            text = "CoinGecko • NYSE/NASDAQ • LBMA Gold/Silver • ECB FX Reference Feeds",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConnectionAndMarketStatusBar(
    connectionStatus: ConnectionStatus,
    selectedCurrency: String,
    fxRates: Map<String, Double>,
    onCycleCurrency: () -> Unit,
    onRefreshFeeds: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (connectionStatus) {
        ConnectionStatus.ONLINE -> EmeraldAccent
        ConnectionStatus.CONNECTING -> BrightGold
        ConnectionStatus.MARKET_DATA_DELAYED -> Color(0xFFF59E0B)
        ConnectionStatus.OFFLINE -> BearRed
    }
    val usEquitiesOpen = MarketDataBackendService.isUsEquityMarketOpen()
    val rate = fxRates[selectedCurrency] ?: 1.0

    Surface(
        color = DarkCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtleCardBorder, RoundedCornerShape(12.dp))
            .testTag("connection_market_status_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${connectionStatus.label} • Crypto/Spot 24/7",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (usEquitiesOpen) {
                            "US Equities: Live Session (09:30–16:00 EST)"
                        } else {
                            "US Equities: Official Close (After-Hours/Weekend)"
                        },
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = ObsidianBlack,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, MetallicGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCycleCurrency)
                        .testTag("currency_selector_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Display Currency",
                            tint = MetallicGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedCurrency == "USD") "USD (1.00)" else "$selectedCurrency (${String.format("%.2f", rate)})",
                            color = MetallicGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Surface(
                    color = ObsidianBlack,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, SubtleCardBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onRefreshFeeds)
                        .testTag("refresh_market_feeds_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Live Market Data",
                        tint = EmeraldAccent,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

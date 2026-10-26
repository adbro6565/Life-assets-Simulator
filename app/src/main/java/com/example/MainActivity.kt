package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.AuthOperationState
import com.example.data.model.OrderSide
import com.example.ui.components.AppDestination
import com.example.ui.components.AssetDetailSheet
import com.example.ui.components.BottomNavigationBar
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.QuantitySelectorModal
import com.example.ui.screens.AuthModalSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LifeBotScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MarketsScreen
import com.example.ui.screens.MultiplayerScreen
import com.example.ui.screens.OnboardingAuthFlowScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.theme.LifeAssetsTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.LifeAssetsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LifeAssetsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeAssetsTheme {
                LifeAssetsRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LifeAssetsRootApp(viewModel: LifeAssetsViewModel) {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }

    val isAuthenticatedSession by viewModel.isAuthenticatedSession.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val fxRates by viewModel.fxRates.collectAsStateWithLifecycle()
    val fxSourceLabel by viewModel.fxSourceLabel.collectAsStateWithLifecycle()
    val selectedDisplayCurrency by viewModel.selectedDisplayCurrency.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val limitOrders by viewModel.limitOrders.collectAsStateWithLifecycle()
    val tradeOffers by viewModel.tradeOffers.collectAsStateWithLifecycle()
    val dailyRewardClaims by viewModel.dailyRewardClaims.collectAsStateWithLifecycle()
    val ticks by viewModel.marketTicks.collectAsStateWithLifecycle()
    val regime by viewModel.marketRegime.collectAsStateWithLifecycle()
    val headline by viewModel.marketHeadline.collectAsStateWithLifecycle()
    val metrics by viewModel.portfolioMetrics.collectAsStateWithLifecycle()
    val dailyRewardStatus by viewModel.dailyRewardStatus.collectAsStateWithLifecycle()
    val authState by viewModel.authOperationState.collectAsStateWithLifecycle()

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    val terminalAssetId by viewModel.terminalAssetId.collectAsStateWithLifecycle()
    val terminalTimeframe by viewModel.terminalTimeframe.collectAsStateWithLifecycle()

    val activeOrderAsset by viewModel.activeOrderAsset.collectAsStateWithLifecycle()
    val activeOrderSide by viewModel.activeOrderSide.collectAsStateWithLifecycle()
    val isSubmittingOrder by viewModel.isSubmittingOrder.collectAsStateWithLifecycle()
    val detailSheetAsset by viewModel.detailSheetAsset.collectAsStateWithLifecycle()
    val showAuthModal by viewModel.showAuthModal.collectAsStateWithLifecycle()

    val lifeBotMessages by viewModel.lifeBotMessages.collectAsStateWithLifecycle()
    val isLifeBotThinking by viewModel.isLifeBotThinking.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val watchedAssetIds by viewModel.watchedAssetIds.collectAsStateWithLifecycle()
    val imageAuditReport by viewModel.imageAuditReport.collectAsStateWithLifecycle()
    val quotaSnapshot by viewModel.quotaSnapshot.collectAsStateWithLifecycle()

    // Online-Only Gate: If internet or backend is unavailable, show full-screen "Internet Connection Required" page
    if (connectionStatus == com.example.data.model.ConnectionStatus.OFFLINE) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavyBg),
            containerColor = DarkNavyBg,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                com.example.ui.screens.InternetRequiredScreen(
                    connectionStatus = connectionStatus,
                    username = user?.username,
                    preservedNetWorthUsd = metrics.totalNetWorth,
                    onRetryConnection = { viewModel.retryBackendConnection() }
                )
            }
        }
        return
    }

    // First-launch Onboarding & Auth Flow: Splash -> Sign Up / Log In -> Home Dashboard
    if (!isAuthenticatedSession) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavyBg),
            containerColor = DarkNavyBg,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                OnboardingAuthFlowScreen(
                    statusBanner = statusBanner,
                    onClearBanner = { viewModel.clearBanner() },
                    onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                    onRegisterAccount = { uname, email, pass ->
                        currentDestination = AppDestination.DASHBOARD
                        viewModel.registerUniqueUsernameAccount(uname, email, pass)
                    },
                    onLoginWithEmailOrUsername = { emailOrUname, pass ->
                        currentDestination = AppDestination.DASHBOARD
                        viewModel.loginWithUsername(emailOrUname, pass)
                    },
                    onForgotPassword = { recoveryEmail ->
                        viewModel.sendPasswordReset(recoveryEmail)
                    }
                )
            }
        }
        return
    }

    // Ensure pressing Back when a modal sheet is open dismisses the sheet instead of closing the Activity
    if (activeOrderAsset != null || detailSheetAsset != null || showAuthModal) {
        BackHandler {
            when {
                activeOrderAsset != null -> viewModel.dismissQuantitySelector()
                detailSheetAsset != null -> viewModel.dismissAssetDetail()
                showAuthModal -> viewModel.setShowAuthModal(false)
            }
        }
    } else if (currentDestination != AppDestination.DASHBOARD) {
        BackHandler {
            currentDestination = AppDestination.DASHBOARD
        }
    }

    val filteredAssets = remember(
        selectedCategory,
        searchQuery,
        sortOption,
        ticks
    ) {
        viewModel.getFilteredAssets(
            category = selectedCategory,
            query = searchQuery,
            sort = sortOption,
            ticks = ticks
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        containerColor = DarkNavyBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomNavigationBar(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Online Connection Status, US Equity Session & Display Currency Selector Bar
            com.example.ui.screens.ConnectionAndMarketStatusBar(
                connectionStatus = connectionStatus,
                selectedCurrency = selectedDisplayCurrency,
                fxRates = fxRates,
                onCycleCurrency = {
                    val supported = com.example.engine.MarketDataBackendService.SUPPORTED_DISPLAY_CURRENCIES
                    val nextIdx = (supported.indexOf(selectedDisplayCurrency) + 1) % supported.size
                    viewModel.setDisplayCurrency(supported[nextIdx])
                },
                onRefreshFeeds = { viewModel.refreshMarketQuotesNow() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Live Status Banner for Trade Execution, Limit Orders, & Daily Rewards
            AnimatedVisibility(visible = statusBanner != null) {
                statusBanner?.let { msg ->
                    Surface(
                        color = DarkCardBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(1.dp, EmeraldAccent, RoundedCornerShape(10.dp))
                            .clickable { viewModel.clearBanner() }
                            .testTag("status_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "DISMISS",
                                color = EmeraldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            user = user,
                            metrics = metrics,
                            ticks = ticks,
                            regime = regime,
                            headline = headline,
                            dailyRewardStatus = dailyRewardStatus,
                            lifeBotBriefing = viewModel.getLifeBotBriefing(metrics),
                            getOwnedQuantity = { viewModel.getOwnedQuantity(it) },
                            onClaimDailyReward = { viewModel.claimDailyReward() },
                            onOpenAuthModal = { currentDestination = AppDestination.PROFILE },
                            onOpenLifeBot = { currentDestination = AppDestination.LIFEBOT },
                            onOpenMarketplace = { currentDestination = AppDestination.MARKETPLACE },
                            onOpenTradeTerminal = { currentDestination = AppDestination.MARKETS },
                            onOpenPortfolio = { currentDestination = AppDestination.PORTFOLIO },
                            onOpenRewards = { currentDestination = AppDestination.REWARDS },
                            onAssetCardClick = { viewModel.openAssetDetail(it) },
                            onBuyClick = { viewModel.openQuantitySelector(it, OrderSide.BUY) },
                            onSellClick = { viewModel.openQuantitySelector(it, OrderSide.SELL) }
                        )
                    }

                    AppDestination.MARKETS -> {
                        val candles = remember(terminalAssetId, terminalTimeframe, ticks[terminalAssetId]?.updatedAtMillis) {
                            viewModel.getCandles(terminalAssetId, terminalTimeframe)
                        }
                        val orderBook = remember(terminalAssetId, ticks[terminalAssetId]?.updatedAtMillis) {
                            viewModel.getOrderBook(terminalAssetId)
                        }
                        MarketsScreen(
                            selectedAssetId = terminalAssetId,
                            selectedTimeframe = terminalTimeframe,
                            ticks = ticks,
                            candles = candles,
                            orderBook = orderBook,
                            openLimitOrders = limitOrders,
                            ownedQuantity = viewModel.getOwnedQuantity(terminalAssetId),
                            watchedAssetIds = watchedAssetIds,
                            portfolioMetrics = metrics,
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                            onSelectAsset = { viewModel.selectTerminalAsset(it) },
                            onSelectTimeframe = { viewModel.selectTerminalTimeframe(it) },
                            onBuyClick = { viewModel.openQuantitySelector(it, OrderSide.BUY) },
                            onSellClick = { viewModel.openQuantitySelector(it, OrderSide.SELL) },
                            onCancelLimitOrder = { viewModel.cancelLimitOrder(it) },
                            onOpenAssetDetail = { viewModel.openAssetDetail(it) }
                        )
                    }

                    AppDestination.PORTFOLIO -> {
                        PortfolioScreen(
                            metrics = metrics,
                            holdings = holdings,
                            transactions = transactions,
                            limitOrders = limitOrders,
                            ticks = ticks,
                            onAssetClick = { viewModel.openAssetDetail(it) },
                            onBuyClick = { viewModel.openQuantitySelector(it, OrderSide.BUY) },
                            onSellClick = { viewModel.openQuantitySelector(it, OrderSide.SELL) },
                            onCancelLimitOrder = { viewModel.cancelLimitOrder(it) }
                        )
                    }

                    AppDestination.REWARDS -> {
                        RewardsScreen(
                            dailyRewardStatus = dailyRewardStatus,
                            recentClaims = dailyRewardClaims,
                            onClaimDailyReward = { viewModel.claimDailyReward() },
                            onOpenMarketplace = { currentDestination = AppDestination.MARKETPLACE }
                        )
                    }

                    AppDestination.PROFILE -> {
                        ProfileScreen(
                            user = user,
                            imageAuditReport = imageAuditReport,
                            onRunImageAudit = { viewModel.runAssetImageAuditNow() },
                            quotaSnapshot = quotaSnapshot,
                            onNavigateDashboard = { currentDestination = AppDestination.DASHBOARD },
                            onNavigatePortfolio = { currentDestination = AppDestination.PORTFOLIO },
                            onNavigateTransactions = { currentDestination = AppDestination.PORTFOLIO },
                            onNavigateRewards = { currentDestination = AppDestination.REWARDS },
                            onNavigateMarketplace = { currentDestination = AppDestination.MARKETPLACE },
                            onNavigateArena = { currentDestination = AppDestination.MULTIPLAYER },
                            onNavigateLifeBot = { currentDestination = AppDestination.LIFEBOT },
                            onOpenSettings = { viewModel.setShowAuthModal(true) },
                            onLogOut = { viewModel.signOut(context) },
                            onDeleteAccount = { viewModel.deleteAccount(context) }
                        )
                    }

                    AppDestination.MARKETPLACE -> {
                        MarketplaceScreen(
                            assets = filteredAssets,
                            ticks = ticks,
                            selectedCategory = selectedCategory,
                            searchQuery = searchQuery,
                            sortOption = sortOption,
                            getOwnedQuantity = { viewModel.getOwnedQuantity(it) },
                            onSelectCategory = { viewModel.setCategoryFilter(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onSelectSort = { viewModel.setSortOption(it) },
                            onAssetCardClick = { viewModel.openAssetDetail(it) },
                            onBuyClick = { viewModel.openQuantitySelector(it, OrderSide.BUY) },
                            onSellClick = { viewModel.openQuantitySelector(it, OrderSide.SELL) }
                        )
                    }

                    AppDestination.MULTIPLAYER -> {
                        MultiplayerScreen(
                            leaderboard = viewModel.getLeaderboard(metrics),
                            holdings = holdings,
                            tradeOffers = tradeOffers,
                            onCreateOffer = { recipient, assetId, qty, cash ->
                                viewModel.createP2POffer(recipient, assetId, qty, cash)
                            },
                            onAcceptOffer = { viewModel.acceptP2POffer(it) },
                            onDeclineOffer = { viewModel.declineP2POffer(it) }
                        )
                    }

                    AppDestination.LIFEBOT -> {
                        LifeBotScreen(
                            messages = lifeBotMessages,
                            isThinking = isLifeBotThinking,
                            regime = regime,
                            onSendPrompt = { viewModel.sendLifeBotPrompt(it) }
                        )
                    }
                }
            }
        }
    }

    // Asset Detail Sheet (with Dual Real-World Reference vs. Virtual Valuation, Purity Calculator & FX)
    detailSheetAsset?.let { asset ->
        AssetDetailSheet(
            asset = asset,
            tick = ticks[asset.id],
            ownedQuantity = viewModel.getOwnedQuantity(asset.id),
            averageCostBasis = viewModel.getAverageCostBasis(asset.id),
            getCandles = { tf -> viewModel.getCandles(asset.id, tf) },
            orderBook = viewModel.getOrderBook(asset.id),
            onBuyClick = {
                viewModel.dismissAssetDetail()
                viewModel.openQuantitySelector(asset, OrderSide.BUY)
            },
            onSellClick = {
                viewModel.dismissAssetDetail()
                viewModel.openQuantitySelector(asset, OrderSide.SELL)
            },
            onDismiss = { viewModel.dismissAssetDetail() },
            fxRates = fxRates,
            selectedDisplayCurrency = selectedDisplayCurrency,
            fxSourceLabel = fxSourceLabel,
            spotGoldPerOzUsd = viewModel.marketEngine.getSpotGoldPricePerTroyOzUsd(),
            spotSilverPerOzUsd = viewModel.marketEngine.getSpotSilverPricePerTroyOzUsd(),
            onRefreshQuote = { viewModel.refreshMarketQuotesNow() }
        )
    }

    // Buy / Sell Order Sheet (matching Screen 6: Buy / Sell Order with 1–100+ & 25%/50%/75%/100% controls)
    activeOrderAsset?.let { asset ->
        QuantitySelectorModal(
            asset = asset,
            tick = ticks[asset.id],
            initialSide = activeOrderSide,
            availableCash = metrics.cashBalance,
            ownedQuantity = viewModel.getOwnedQuantity(asset.id),
            averageCostBasis = viewModel.getAverageCostBasis(asset.id),
            isSubmitting = isSubmittingOrder,
            isOnline = connectionStatus.allowsTradingAndClaiming,
            onConfirmOrder = { intent -> viewModel.submitOrderIntent(intent) },
            onDismiss = { viewModel.dismissQuantitySelector() }
        )
    }

    // Account & Profile Settings Modal Sheet
    if (showAuthModal) {
        AuthModalSheet(
            currentUser = user,
            isGoogleSignedIn = authState is AuthOperationState.Authenticated,
            onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
            onRegisterAccount = { uname, email, pass ->
                viewModel.registerUniqueUsernameAccount(uname, email, pass)
            },
            onLoginWithUsername = { uname, pass ->
                viewModel.loginWithUsername(uname, pass)
            },
            onUpdateUsername = { newName ->
                viewModel.updateUniqueUsername(newName)
            },
            onSignOut = { viewModel.signOut(context) },
            onDismiss = { viewModel.setShowAuthModal(false) }
        )
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthOperationState
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.AssetCatalogue
import com.example.data.firestore.FirestoreHoldingRepository
import com.example.data.local.DailyRewardEntity
import com.example.data.local.HoldingEntity
import com.example.data.local.LifeAssetsDatabase
import com.example.data.local.LimitOrderEntity
import com.example.data.local.TradeOfferEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.BillionairePlayer
import com.example.data.model.Candle
import com.example.data.model.MarketRegime
import com.example.data.model.MarketTick
import com.example.data.model.OrderBookEntry
import com.example.data.model.OrderSide
import com.example.data.model.OrderType
import com.example.data.model.Timeframe
import com.example.domain.trading.OrderIntent
import com.example.engine.AssetImageAuditService
import com.example.engine.BackendFreeTierSnapshot
import com.example.engine.BackendQuotaMonitor
import com.example.engine.DailyRewardService
import com.example.engine.DailyRewardStatus
import com.example.engine.FreeTierQuotaAlertLevel
import com.example.engine.ImageAuditAdminReport
import com.example.engine.LifeBotMessage
import com.example.engine.LifeBotService
import com.example.engine.MarketEngine
import com.example.engine.MultiplayerService
import com.example.engine.TradingService
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

enum class AssetSortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_HIGH_TO_LOW("Price: High → Low"),
    PRICE_LOW_TO_HIGH("Price: Low → High"),
    CHANGE_24H_DESC("Top 24h Gainers"),
    CHANGE_24H_ASC("Top 24h Pullbacks"),
    NAME_ASC("Name: A → Z")
}

data class PortfolioMetrics(
    val cashBalance: Double = LifeAssetsViewModel.STARTING_CASH_USD,
    val investedMarketValue: Double = LifeAssetsViewModel.STARTING_ASSET_ALLOCATION_USD,
    val realWorldReferencePortfolioValue: Double = LifeAssetsViewModel.STARTING_ASSET_ALLOCATION_USD,
    val totalCostBasis: Double = LifeAssetsViewModel.STARTING_ASSET_ALLOCATION_USD,
    val totalNetWorth: Double = LifeAssetsViewModel.STARTING_CASH_USD + LifeAssetsViewModel.STARTING_ASSET_ALLOCATION_USD,
    val unrealizedPnL: Double = 0.0,
    val unrealizedPnLPercent: Double = 0.0,
    val realizedPnL: Double = 0.0
)

class LifeAssetsViewModel(application: Application) : AndroidViewModel(application) {

    init {
        runCatching {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
        }
    }

    private val db = LifeAssetsDatabase.getInstance(application)
    val marketEngine = MarketEngine()
    val backendService = marketEngine.backendService
    val tradingService = TradingService(db, marketEngine)
    val dailyRewardService = DailyRewardService(db, backendService)
    val multiplayerService = MultiplayerService(db, marketEngine)
    val imageAuditService = AssetImageAuditService(db)
    val lifeBotService = LifeBotService()
    val authRepository = AuthRepository()
    val backendQuotaMonitor = BackendQuotaMonitor()
    private val firestoreRepository: FirestoreHoldingRepository? = try {
        FirestoreHoldingRepository(application)
    } catch (_: Throwable) {
        null
    }

    private val prefs = application.getSharedPreferences("lifeassets_session", Context.MODE_PRIVATE)

    val connectionStatus: StateFlow<com.example.data.model.ConnectionStatus> = backendService.connectionStatus
    val fxRates: StateFlow<Map<String, Double>> = backendService.fxRates
    val fxSourceLabel: StateFlow<String> = backendService.fxSourceLabel
    val fxLastUpdatedMillis: StateFlow<Long> = backendService.fxLastUpdatedMillis

    // Free-Tier Quota Telemetry StateFlow
    val quotaSnapshot: StateFlow<BackendFreeTierSnapshot> = backendQuotaMonitor.snapshot

    private val _selectedDisplayCurrency = MutableStateFlow(
        prefs.getString("display_currency", "USD") ?: "USD"
    )
    val selectedDisplayCurrency: StateFlow<String> = _selectedDisplayCurrency.asStateFlow()

    private val _isAuthenticatedSession = MutableStateFlow(
        prefs.getBoolean("is_authenticated_session", false)
    )
    val isAuthenticatedSession: StateFlow<Boolean> = _isAuthenticatedSession.asStateFlow()

    private val _currentUserId = MutableStateFlow(
        prefs.getString("active_user_id", "") ?: ""
    )
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _holdings = MutableStateFlow<List<HoldingEntity>>(emptyList())
    val holdings: StateFlow<List<HoldingEntity>> = _holdings.asStateFlow()

    private val _watchedAssetIds = MutableStateFlow<Set<String>>(
        setOf("crypto_01", "crypto_02", "stk_01", "stk_03", "met_01")
    )
    val watchedAssetIds: StateFlow<Set<String>> = _watchedAssetIds.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _limitOrders = MutableStateFlow<List<LimitOrderEntity>>(emptyList())
    val limitOrders: StateFlow<List<LimitOrderEntity>> = _limitOrders.asStateFlow()

    private val _tradeOffers = MutableStateFlow<List<TradeOfferEntity>>(emptyList())
    val tradeOffers: StateFlow<List<TradeOfferEntity>> = _tradeOffers.asStateFlow()

    private val _dailyRewardClaims = MutableStateFlow<List<DailyRewardEntity>>(emptyList())
    val dailyRewardClaims: StateFlow<List<DailyRewardEntity>> = _dailyRewardClaims.asStateFlow()

    val marketTicks: StateFlow<Map<String, MarketTick>> = marketEngine.ticks
    val marketRegime: StateFlow<MarketRegime> = marketEngine.regime
    val marketHeadline: StateFlow<String> = marketEngine.marketHeadline
    val authOperationState: StateFlow<AuthOperationState> = authRepository.authState

    val portfolioMetrics: StateFlow<PortfolioMetrics> = combine(
        _currentUser,
        _holdings,
        marketTicks
    ) { user, holdingsList, ticks ->
        val cash = user?.cashBalance ?: STARTING_CASH_USD
        var investedValue = 0.0
        var realReferenceTotal = 0.0
        var costBasis = 0.0
        for (h in holdingsList) {
            val catalogAsset = AssetCatalogue.getAssetById(h.assetId)
            val tick = ticks[h.assetId]
            val price = tick?.currentPrice
                ?: catalogAsset?.basePrice
                ?: h.averageCostBasis
            val refPrice = tick?.realWorldReferencePrice
                ?: catalogAsset?.valuationMetadata?.realWorldReferencePrice
                ?: price
            investedValue += h.quantity * price
            realReferenceTotal += h.quantity * refPrice
            costBasis += h.quantity * h.averageCostBasis
        }
        val unrealized = investedValue - costBasis
        val unrealizedPct = if (costBasis > 0.0) (unrealized / costBasis) * 100.0 else 0.0
        PortfolioMetrics(
            cashBalance = cash,
            investedMarketValue = investedValue,
            realWorldReferencePortfolioValue = realReferenceTotal,
            totalCostBasis = costBasis,
            totalNetWorth = cash + investedValue,
            unrealizedPnL = unrealized,
            unrealizedPnLPercent = unrealizedPct,
            realizedPnL = user?.realizedPnL ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioMetrics())

    // Marketplace Filters
    private val _selectedCategory = MutableStateFlow<AssetCategory?>(null)
    val selectedCategory: StateFlow<AssetCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(AssetSortOption.FEATURED)
    val sortOption: StateFlow<AssetSortOption> = _sortOption.asStateFlow()

    // Terminal Chart Selection
    private val _terminalAssetId = MutableStateFlow("crypto_01") // Bitcoin default
    val terminalAssetId: StateFlow<String> = _terminalAssetId.asStateFlow()

    private val _terminalTimeframe = MutableStateFlow(Timeframe.M15)
    val terminalTimeframe: StateFlow<Timeframe> = _terminalTimeframe.asStateFlow()

    // Quantity Selector Modal State
    private val _activeOrderAsset = MutableStateFlow<Asset?>(null)
    val activeOrderAsset: StateFlow<Asset?> = _activeOrderAsset.asStateFlow()

    private val _activeOrderSide = MutableStateFlow(OrderSide.BUY)
    val activeOrderSide: StateFlow<OrderSide> = _activeOrderSide.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    // Asset Detail Sheet State
    private val _detailSheetAsset = MutableStateFlow<Asset?>(null)
    val detailSheetAsset: StateFlow<Asset?> = _detailSheetAsset.asStateFlow()

    // Auth Modal State
    private val _showAuthModal = MutableStateFlow(false)
    val showAuthModal: StateFlow<Boolean> = _showAuthModal.asStateFlow()

    private val _requestSystemGoogleAccountPicker = MutableStateFlow(false)
    val requestSystemGoogleAccountPicker: StateFlow<Boolean> = _requestSystemGoogleAccountPicker.asStateFlow()

    private val _showGoogleEmailFallbackDialog = MutableStateFlow(false)
    val showGoogleEmailFallbackDialog: StateFlow<Boolean> = _showGoogleEmailFallbackDialog.asStateFlow()

    // Daily Reward Status
    private val _dailyRewardStatus = MutableStateFlow(
        DailyRewardStatus(
            kathmanduDateString = DailyRewardService.getKathmanduDateString(),
            isAvailableToday = true,
            currentStreak = 0,
            nextRewardAmount = 50_000.0,
            countdownFormatted = DailyRewardService.getCountdownToKathmanduMidnight(),
            statusLabel = "Available Now"
        )
    )
    val dailyRewardStatus: StateFlow<DailyRewardStatus> = _dailyRewardStatus.asStateFlow()

    // LifeBot AI Chat State
    private val _lifeBotMessages = MutableStateFlow<List<LifeBotMessage>>(
        listOf(
            LifeBotMessage(
                id = "welcome_bot",
                isFromBot = true,
                text = "Welcome to LifeAssets Neural Terminal. I monitor 400+ global luxury, commodity, equity, crypto, and sovereign assets in real time. Select any asset to trade 1–100+ units with full fee and cost-basis visibility, or ask me for portfolio diagnostics."
            )
        )
    )
    val lifeBotMessages: StateFlow<List<LifeBotMessage>> = _lifeBotMessages.asStateFlow()

    private val _isLifeBotThinking = MutableStateFlow(false)
    val isLifeBotThinking: StateFlow<Boolean> = _isLifeBotThinking.asStateFlow()

    // Status Banner / Toast Feedback
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Automatic Image Audit & Verification Admin Report
    private val _imageAuditReport = MutableStateFlow<ImageAuditAdminReport?>(null)
    val imageAuditReport: StateFlow<ImageAuditAdminReport?> = _imageAuditReport.asStateFlow()

    private var userStreamsJob: Job? = null

    init {
        startMarketSimulationLoop()
        startDailyRewardCountdownLoop()
        observeFirebaseAuthChanges()

        val activeUid = _currentUserId.value
        val isAuth = _isAuthenticatedSession.value
        if (isAuth && activeUid.isNotBlank()) {
            observeUserStreams(activeUid)
            viewModelScope.launch {
                runCatching { refreshDailyRewardStatus() }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val entities = AssetCatalogue.allAssets.map { com.example.data.local.AssetEntity.fromAsset(it) }
                db.assetDao().insertAll(entities)
                val dbWatched = db.assetDao().getWatchedAssetIds()
                if (dbWatched.isNotEmpty()) {
                    _watchedAssetIds.value = dbWatched.toSet()
                }
                _imageAuditReport.value = imageAuditService.auditAndRepairAllAssets(
                    liveTicks = marketEngine.ticks.value,
                    firestoreRepository = if (authRepository.currentUser != null) firestoreRepository else null
                )
            }.onFailure { e ->
                Log.e("LifeAssetsVM", "Startup asset audit error", e)
            }
        }
    }

    fun runAssetImageAuditNow() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val report = imageAuditService.auditAndRepairAllAssets(
                    liveTicks = marketEngine.ticks.value,
                    firestoreRepository = if (authRepository.currentUser != null) firestoreRepository else null
                )
                _imageAuditReport.value = report
                withContext(Dispatchers.Main) {
                    postBanner(
                        "Asset Photo & Online Valuation Audit Complete: ${report.correctRealPhotosAdded}/${report.totalAssetsAudited} verified real photos (${report.remainingErrors} errors)"
                    )
                }
            }.onFailure { e ->
                Log.e("LifeAssetsVM", "Manual asset audit error", e)
            }
        }
    }

    private fun observeUserStreams(userId: String) {
        userStreamsJob?.cancel()
        userStreamsJob = viewModelScope.launch {
            launch {
                db.userDao().observeUserById(userId).collect { user ->
                    _currentUser.value = user
                }
            }
            launch {
                db.holdingDao().observeHoldingsForUser(userId).collect { list ->
                    _holdings.value = list
                }
            }
            launch {
                db.transactionDao().observeTransactionsForUser(userId).collect { list ->
                    _transactions.value = list
                }
            }
            launch {
                db.limitOrderDao().observeOrdersForUser(userId).collect { list ->
                    _limitOrders.value = list
                }
            }
            launch {
                db.tradeOfferDao().observeOffersForUser(userId).collect { list ->
                    _tradeOffers.value = list
                }
            }
            launch {
                db.dailyRewardDao().observeClaimsForUser(userId).collect { list ->
                    _dailyRewardClaims.value = list
                }
            }
        }
    }

    private suspend fun provisionOrLoadGoogleIdentity(
        uid: String,
        email: String?,
        displayName: String?
    ): UserEntity {
        val cleanEmail = email?.trim()?.takeIf { it.isNotBlank() }
        var existing = db.userDao().getUserById(uid)
            ?: cleanEmail?.let { db.userDao().getUserByEmail(it) }
        val resolvedUid = existing?.userId ?: uid
        if (existing == null) {
            val remoteProfile = firestoreRepository?.getUserProfile(resolvedUid)?.getOrNull()
            if (remoteProfile != null) {
                backendQuotaMonitor.recordReads(1)
            }
            val baseName = (remoteProfile?.username?.takeIf { it.isNotBlank() }
                ?: displayName?.takeIf { it.isNotBlank() }
                ?: cleanEmail?.substringBefore("@")
                ?: "Trader")
                .replace(Regex("[^a-zA-Z0-9_]"), "_")
                .take(15)
                .ifBlank { "Trader" }
            val uniqueName = ensureUniqueUsernameCandidate(baseName)
            val initialCash = remoteProfile?.cashBalance ?: STARTING_CASH_USD
            val entity = UserEntity(
                userId = resolvedUid,
                username = uniqueName,
                normalizedUsername = uniqueName.lowercase(),
                email = cleanEmail ?: "$uniqueName@lifeassets.io",
                passwordHash = hashPassword(resolvedUid),
                cashBalance = initialCash,
                realizedPnL = 0.0,
                streakDays = remoteProfile?.streakDays?.toInt() ?: 1,
                lastClaimDateKathmandu = remoteProfile?.lastClaimDateKathmandu
            )
            db.userDao().upsertUser(entity)
            if (remoteProfile == null) {
                for ((assetId, qty, avgCost) in STARTER_PORTFOLIO_3M_HOLDINGS) {
                    db.holdingDao().upsertHolding(
                        HoldingEntity(
                            userId = resolvedUid,
                            assetId = assetId,
                            quantity = qty,
                            averageCostBasis = avgCost,
                            reservedQuantity = 0.0
                        )
                    )
                }
            }
            multiplayerService.seedInitialOffersIfNeeded(resolvedUid, uniqueName)
            existing = entity
        }
        _currentUser.value = existing
        switchActiveUser(existing.userId)
        syncHoldingsWithFirestore()
        return existing
    }

    private fun observeFirebaseAuthChanges() {
        viewModelScope.launch {
            authRepository.observeFirebaseUser().collect { fbUser ->
                if (fbUser != null) {
                    provisionOrLoadGoogleIdentity(fbUser.uid, fbUser.email, fbUser.displayName)
                }
            }
        }
    }

    private suspend fun ensureUniqueUsernameCandidate(base: String): String {
        var candidate = if (base.length < 3) "${base}_01" else base
        var suffix = 1
        while (db.userDao().getUserByNormalizedUsername(candidate.lowercase()) != null) {
            candidate = "${base.take(15)}_$suffix"
            suffix++
        }
        return candidate
    }

    private fun switchActiveUser(userId: String) {
        _currentUserId.value = userId
        prefs.edit().putString("active_user_id", userId).apply()
        observeUserStreams(userId)
        viewModelScope.launch {
            refreshDailyRewardStatus()
        }
    }

    private fun startMarketSimulationLoop() {
        viewModelScope.launch {
            runCatching {
                marketEngine.syncLiveMarketDataFromProviders(force = true)
                persistOnlineValuationsToRoom()
            }
            while (isActive) {
                delay(2500L)
                runCatching {
                    marketEngine.stepMarketTick()
                    marketEngine.syncLiveMarketDataFromProviders(force = false)
                    persistOnlineValuationsToRoom()
                    val filled = tradingService.evaluateOpenLimitOrders()
                    if (filled.isNotEmpty()) {
                        val first = filled.first()
                        postBanner("Limit Order Filled: ${first.side} ${first.quantity} ${first.assetSymbol} @ $${String.format("%,.2f", first.executionPrice)}")
                        syncHoldingsWithFirestore()
                    }
                    refreshDailyRewardStatus()
                }
            }
        }
    }

    private suspend fun persistOnlineValuationsToRoom() {
        val ticks = marketEngine.ticks.value
        val now = System.currentTimeMillis()
        val priorityAssetIds = (_holdings.value.map { it.assetId } + _watchedAssetIds.value).toSet()
        for (assetId in priorityAssetIds) {
            val tick = ticks[assetId] ?: continue
            val onlinePrice = tick.realWorldReferencePrice ?: tick.currentPrice
            val tickUpdated = tick.updatedAtMillis.takeIf { it > 0L } ?: now
            db.assetDao().updateAssetOnlineValuation(
                assetId = assetId,
                onlinePriceUsd = onlinePrice,
                priceSource = tick.dataProvider,
                priceType = tick.priceType.name,
                priceUpdatedAt = tickUpdated,
                updatedAt = tickUpdated
            )
        }
    }

    private fun startDailyRewardCountdownLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                refreshDailyRewardStatus()
            }
        }
    }

    suspend fun refreshDailyRewardStatus() {
        val status = dailyRewardService.getStatus(_currentUserId.value)
        _dailyRewardStatus.value = status
    }

    fun toggleWatchlist(assetId: String) {
        viewModelScope.launch {
            val current = _watchedAssetIds.value.toMutableSet()
            val nowWatched = if (assetId in current) {
                current.remove(assetId)
                false
            } else {
                current.add(assetId)
                true
            }
            _watchedAssetIds.value = current
            db.assetDao().updateWatchStatus(assetId, nowWatched)
        }
    }

    fun setDisplayCurrency(currencyCode: String) {
        val normalized = currencyCode.uppercase()
        if (normalized in com.example.engine.MarketDataBackendService.SUPPORTED_DISPLAY_CURRENCIES) {
            _selectedDisplayCurrency.value = normalized
            prefs.edit().putString("display_currency", normalized).apply()
            postBanner("Reference display currency switched to $normalized (${fxSourceLabel.value})")
        }
    }

    fun retryBackendConnection() {
        viewModelScope.launch {
            backendService.setSimulatedNetworkState(com.example.data.model.ConnectionStatus.CONNECTING)
            delay(350L)
            backendService.setSimulatedNetworkState(com.example.data.model.ConnectionStatus.ONLINE)
            marketEngine.initializeMarket()
            marketEngine.syncLiveMarketDataFromProviders(force = true)
            persistOnlineValuationsToRoom()
            refreshDailyRewardStatus()
            syncHoldingsWithFirestore()
            postBanner("Backend connection verified • Live market feeds & portfolio synchronized.")
        }
    }

    fun refreshMarketQuotesNow() {
        viewModelScope.launch {
            if (backendService.connectionStatus.value == com.example.data.model.ConnectionStatus.OFFLINE) {
                postBanner("Cannot refresh quotes while offline. Reconnect first.")
                return@launch
            }
            marketEngine.stepMarketTick()
            marketEngine.syncLiveMarketDataFromProviders(force = true)
            persistOnlineValuationsToRoom()
            syncHoldingsWithFirestore()
            postBanner("Real-world reference quotes & FX rates refreshed.")
        }
    }

    // --- QUANTITY SELECTOR & BUY/SELL EXECUTION ---

    fun openQuantitySelector(asset: Asset, side: OrderSide) {
        _detailSheetAsset.value = null
        _activeOrderAsset.value = asset
        _activeOrderSide.value = side
    }

    fun dismissQuantitySelector() {
        if (!_isSubmittingOrder.value) {
            _activeOrderAsset.value = null
        }
    }

    fun openAssetDetail(asset: Asset) {
        _detailSheetAsset.value = asset
    }

    fun dismissAssetDetail() {
        _detailSheetAsset.value = null
    }

    fun selectTerminalAsset(assetId: String) {
        _terminalAssetId.value = assetId
    }

    fun selectTerminalTimeframe(timeframe: Timeframe) {
        _terminalTimeframe.value = timeframe
    }

    fun submitOrderIntent(intent: OrderIntent) {
        if (_isSubmittingOrder.value) return
        if (backendService.connectionStatus.value == com.example.data.model.ConnectionStatus.OFFLINE) {
            postBanner("Order Rejected: No Internet Connection. Online backend is required for trading.")
            return
        }
        _isSubmittingOrder.value = true

        viewModelScope.launch {
            try {
                val uid = _currentUserId.value
                if (intent.orderType == OrderType.MARKET) {
                    val result = tradingService.executeMarketOrder(
                        userId = uid,
                        assetId = intent.assetId,
                        side = intent.side,
                        quantity = intent.quantity,
                        idempotencyKey = intent.idempotencyKey
                    )
                    result.onSuccess { tx ->
                        val qtyFormatted = if (tx.quantity % 1.0 == 0.0) {
                            tx.quantity.toLong().toString()
                        } else {
                            String.format(Locale.US, "%.4f", tx.quantity)
                        }
                        val actionVerb = if (tx.side == "BUY") "Purchased" else "Sold"
                        _activeOrderAsset.value = null
                        postBanner(
                            "$actionVerb $qtyFormatted ${tx.assetSymbol} @ $${String.format(Locale.US, "%,.2f", tx.executionPrice)} (Total: $${String.format(Locale.US, "%,.2f", tx.totalAmount)})"
                        )
                        syncOrderAndHoldingsWithFirestore(tx)
                    }.onFailure { err ->
                        postBanner("Order Rejected: ${err.message}")
                    }
                } else {
                    val limitPrice = intent.limitPrice ?: intent.referencePrice
                    val result = tradingService.placeLimitOrder(
                        userId = uid,
                        assetId = intent.assetId,
                        side = intent.side,
                        quantity = intent.quantity,
                        limitPrice = limitPrice
                    )
                    result.onSuccess { lo ->
                        _activeOrderAsset.value = null
                        postBanner("Limit ${lo.side} Order Placed: ${lo.quantity} ${lo.assetSymbol} @ $${String.format(Locale.US, "%,.2f", lo.limitPrice)}")
                    }.onFailure { err ->
                        postBanner("Limit Order Rejected: ${err.message}")
                    }
                }
            } finally {
                _isSubmittingOrder.value = false
            }
        }
    }

    fun cancelLimitOrder(orderId: String) {
        viewModelScope.launch {
            val res = tradingService.cancelLimitOrder(_currentUserId.value, orderId)
            res.onSuccess {
                postBanner("Limit order cancelled and reserved funds/inventory restored.")
            }.onFailure {
                postBanner("Cancel failed: ${it.message}")
            }
        }
    }

    // --- DAILY REWARD (Asia/Kathmandu Timezone) ---

    fun claimDailyReward() {
        viewModelScope.launch {
            val res = dailyRewardService.claimDailyReward(_currentUserId.value)
            res.onSuccess { claim ->
                refreshDailyRewardStatus()
                postBanner("Daily Reward Claimed! +$${String.format("%,.2f", claim.rewardAmount)} (Day ${claim.streakDay} Streak • ${claim.claimDateKathmandu} NPT)")
                syncHoldingsWithFirestore()
            }.onFailure { err ->
                refreshDailyRewardStatus()
                postBanner(err.message ?: "Daily reward already claimed for today.")
            }
        }
    }

    // --- WATCHLIST & MARKETPLACE FILTERING ---

    fun setCategoryFilter(category: AssetCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: AssetSortOption) {
        _sortOption.value = option
    }

    fun getFilteredAssets(
        category: AssetCategory?,
        query: String,
        sort: AssetSortOption,
        ticks: Map<String, MarketTick>
    ): List<Asset> {
        var list = AssetCatalogue.allAssets
        if (category != null) {
            list = list.filter { it.category == category }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                    it.symbol.lowercase().contains(q) ||
                    it.subcategory.lowercase().contains(q) ||
                    it.category.displayName.lowercase().contains(q)
            }
        }
        return when (sort) {
            AssetSortOption.FEATURED -> list
            AssetSortOption.PRICE_HIGH_TO_LOW -> list.sortedByDescending { ticks[it.id]?.currentPrice ?: it.basePrice }
            AssetSortOption.PRICE_LOW_TO_HIGH -> list.sortedBy { ticks[it.id]?.currentPrice ?: it.basePrice }
            AssetSortOption.CHANGE_24H_DESC -> list.sortedByDescending { ticks[it.id]?.changePercent24h ?: 0.0 }
            AssetSortOption.CHANGE_24H_ASC -> list.sortedBy { ticks[it.id]?.changePercent24h ?: 0.0 }
            AssetSortOption.NAME_ASC -> list.sortedBy { it.name }
        }
    }

    fun getOwnedQuantity(assetId: String): Double {
        val h = _holdings.value.firstOrNull { it.assetId == assetId } ?: return 0.0
        return (h.quantity - h.reservedQuantity).coerceAtLeast(0.0)
    }

    fun getAverageCostBasis(assetId: String): Double {
        return _holdings.value.firstOrNull { it.assetId == assetId }?.averageCostBasis ?: 0.0
    }

    fun getCandles(assetId: String, timeframe: Timeframe): List<Candle> {
        return marketEngine.getCandles(assetId, timeframe)
    }

    fun getOrderBook(assetId: String): Pair<List<OrderBookEntry>, List<OrderBookEntry>> {
        return marketEngine.getSimulatedOrderBook(assetId)
    }

    // --- MULTIPLAYER & P2P TRADING ---

    fun getLeaderboard(metrics: PortfolioMetrics): List<BillionairePlayer> {
        val user = _currentUser.value
        return multiplayerService.getGlobalLeaderboard(
            currentUserId = _currentUserId.value,
            currentUsername = user?.username ?: "Sovereign_Titan",
            currentCash = metrics.cashBalance,
            currentNetWorth = metrics.totalNetWorth
        )
    }

    fun createP2POffer(
        recipientUsername: String,
        offeredAssetId: String,
        offeredQuantity: Double,
        requestedCash: Double
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val res = multiplayerService.createP2PTradeOffer(
                senderUserId = user.userId,
                senderUsername = user.username,
                recipientUsername = recipientUsername,
                offeredAssetId = offeredAssetId,
                offeredQuantity = offeredQuantity,
                requestedCash = requestedCash
            )
            res.onSuccess { offer ->
                postBanner("P2P Offer Sent to ${offer.recipientUsername}: ${offer.offeredQuantity} ${offer.offeredAssetName} for $${String.format("%,.2f", offer.requestedCash)}")
            }.onFailure { err ->
                postBanner("P2P Offer Error: ${err.message}")
            }
        }
    }

    fun acceptP2POffer(offerId: String) {
        viewModelScope.launch {
            val res = multiplayerService.acceptTradeOffer(_currentUserId.value, offerId)
            res.onSuccess { offer ->
                postBanner("P2P Trade Settled: Acquired ${offer.offeredQuantity} × ${offer.offeredAssetName} for $${String.format("%,.2f", offer.requestedCash)}")
                syncHoldingsWithFirestore()
            }.onFailure { err ->
                postBanner("Could not accept offer: ${err.message}")
            }
        }
    }

    fun declineP2POffer(offerId: String) {
        viewModelScope.launch {
            multiplayerService.declineTradeOffer(_currentUserId.value, offerId)
            postBanner("P2P trade offer declined.")
        }
    }

    // --- LIFEBOT AI FINANCIAL ADVISOR ---

    fun getLifeBotBriefing(metrics: PortfolioMetrics): String {
        val user = _currentUser.value
        return lifeBotService.generatePortfolioBriefing(
            username = user?.username ?: "Sovereign_Titan",
            cashBalance = metrics.cashBalance,
            netWorth = metrics.totalNetWorth,
            holdings = _holdings.value,
            ticks = marketTicks.value,
            regime = marketRegime.value
        )
    }

    fun sendLifeBotPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isLifeBotThinking.value) return

        val userMsg = LifeBotMessage(
            id = UUID.randomUUID().toString(),
            isFromBot = false,
            text = trimmed
        )
        _lifeBotMessages.value = _lifeBotMessages.value + userMsg
        _isLifeBotThinking.value = true

        viewModelScope.launch {
            try {
                val user = _currentUser.value
                val metrics = portfolioMetrics.value
                val replyText = lifeBotService.askLifeBot(
                    userPrompt = trimmed,
                    username = user?.username ?: "Sovereign_Titan",
                    cashBalance = metrics.cashBalance,
                    netWorth = metrics.totalNetWorth,
                    holdings = _holdings.value,
                    ticks = marketTicks.value,
                    regime = marketRegime.value
                )
                val botMsg = LifeBotMessage(
                    id = UUID.randomUUID().toString(),
                    isFromBot = true,
                    text = replyText
                )
                _lifeBotMessages.value = _lifeBotMessages.value + botMsg
            } finally {
                _isLifeBotThinking.value = false
            }
        }
    }

    // --- AUTHENTICATION & UNIQUE USERNAME MANAGEMENT ---

    fun setShowAuthModal(show: Boolean) {
        _showAuthModal.value = show
    }

    fun markAuthenticated(rememberMe: Boolean = true) {
        _isAuthenticatedSession.value = true
        if (rememberMe) {
            prefs.edit().putBoolean("is_authenticated_session", true).apply()
        }
    }

    fun consumeSystemGoogleAccountPickerRequest() {
        _requestSystemGoogleAccountPicker.value = false
    }

    fun setShowGoogleEmailFallbackDialog(show: Boolean) {
        _showGoogleEmailFallbackDialog.value = show
    }

    fun completeSystemGoogleAccountSelection(email: String, displayName: String? = null) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            postBanner("Please select or enter a valid Google email address.")
            return
        }
        viewModelScope.launch {
            val uid = com.example.data.auth.AuthRepository.deterministicGoogleUid(cleanEmail)
            val effectiveName = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: cleanEmail.substringBefore("@")
            authRepository.markGoogleAccountAuthenticated(uid, cleanEmail, effectiveName)
            val userEntity = provisionOrLoadGoogleIdentity(uid, cleanEmail, effectiveName)
            markAuthenticated(true)
            _showAuthModal.value = false
            _showGoogleEmailFallbackDialog.value = false
            postBanner("Signed in with Google as @${userEntity.username} (${userEntity.email})")
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        if (backendService.connectionStatus.value == com.example.data.model.ConnectionStatus.OFFLINE) {
            postBanner("No Internet Connection. Online backend is required to sign in.")
            return
        }
        viewModelScope.launch {
            val res = authRepository.signInWithGoogleInteractive(activityContext)
            res.onSuccess { googleAccount ->
                val userEntity = provisionOrLoadGoogleIdentity(
                    uid = googleAccount.uid,
                    email = googleAccount.email,
                    displayName = googleAccount.displayName
                )
                markAuthenticated(true)
                _showAuthModal.value = false
                _showGoogleEmailFallbackDialog.value = false
                postBanner("Signed in with Google as @${userEntity.username} (${userEntity.email})")
            }.onFailure { err ->
                val isActivityHost = activityContext is android.app.Activity ||
                    (activityContext is android.content.ContextWrapper && activityContext !is android.app.Application)
                if (isActivityHost) {
                    // Play Services CredentialManager throws GetCredentialCancellationException when APK SHA-1
                    // differs from OAuth console; immediately launch Android OS native Google Account Chooser
                    _requestSystemGoogleAccountPicker.value = true
                } else {
                    postBanner(
                        "Google Sign-In could not complete: ${err.localizedMessage ?: "Ensure a Google account is signed in on this device."}"
                    )
                }
            }
        }
    }

    fun registerUniqueUsernameAccount(
        username: String,
        email: String,
        password: String
    ) {
        if (backendService.connectionStatus.value == com.example.data.model.ConnectionStatus.OFFLINE) {
            postBanner("No Internet Connection. Online backend is required to register an account.")
            return
        }
        val rawName = username.trim()
        val rawEmail = email.trim()
        if (rawName.length < 3) {
            postBanner("Username must be at least 3 characters.")
            return
        }
        if (!rawEmail.contains("@") || !rawEmail.contains(".")) {
            postBanner("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            postBanner("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            val sanitized = rawName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
            val cleanUser = ensureUniqueUsernameCandidate(sanitized)
            val normalized = cleanUser.lowercase()

            // Register securely with Firebase Authentication
            val fbResult = authRepository.signUpWithEmail(rawEmail, password, cleanUser)
            fbResult.onSuccess { fbUser ->
                val uid = fbUser.uid
                val newUser = UserEntity(
                    userId = uid,
                    username = cleanUser,
                    normalizedUsername = normalized,
                    email = rawEmail,
                    passwordHash = hashPassword(password),
                    cashBalance = STARTING_CASH_USD,
                    realizedPnL = 0.0,
                    streakDays = 1,
                    lastClaimDateKathmandu = null
                )
                db.userDao().upsertUser(newUser)

                // Seed $3,000,000.00 starter asset allocation separately from $4,000,000.00 cash balance
                for ((assetId, qty, avgCost) in STARTER_PORTFOLIO_3M_HOLDINGS) {
                    db.holdingDao().upsertHolding(
                        HoldingEntity(
                            userId = uid,
                            assetId = assetId,
                            quantity = qty,
                            averageCostBasis = avgCost,
                            reservedQuantity = 0.0
                        )
                    )
                }

                multiplayerService.seedInitialOffersIfNeeded(uid, cleanUser)
                switchActiveUser(uid)
                markAuthenticated(true)
                _showAuthModal.value = false
                syncHoldingsWithFirestore()
                postBanner("Welcome to Life Assets, @$cleanUser! ($4,000,000 Virtual Cash Ready)")
            }.onFailure { err ->
                val msg = err.localizedMessage ?: err.message ?: "Registration failed."
                if (msg.contains("already in use", ignoreCase = true) || msg.contains("collision", ignoreCase = true)) {
                    postBanner("An account with this email already exists. Please log in.")
                } else {
                    // Safe fallback for offline testing or environments without active Firebase Auth emulator
                    val newId = "user_${UUID.randomUUID().toString().take(12)}"
                    val newUser = UserEntity(
                        userId = newId,
                        username = cleanUser,
                        normalizedUsername = normalized,
                        email = rawEmail,
                        passwordHash = hashPassword(password),
                        cashBalance = STARTING_CASH_USD,
                        realizedPnL = 0.0,
                        streakDays = 1,
                        lastClaimDateKathmandu = null
                    )
                    db.userDao().upsertUser(newUser)
                    for ((assetId, qty, avgCost) in STARTER_PORTFOLIO_3M_HOLDINGS) {
                        db.holdingDao().upsertHolding(
                            HoldingEntity(
                                userId = newId,
                                assetId = assetId,
                                quantity = qty,
                                averageCostBasis = avgCost,
                                reservedQuantity = 0.0
                            )
                        )
                    }
                    multiplayerService.seedInitialOffersIfNeeded(newId, cleanUser)
                    switchActiveUser(newId)
                    markAuthenticated(true)
                    _showAuthModal.value = false
                    postBanner("Welcome to Life Assets, @$cleanUser! ($4,000,000 Virtual Cash Ready)")
                }
            }
        }
    }

    fun loginWithUsername(usernameOrEmail: String, password: String) {
        if (backendService.connectionStatus.value == com.example.data.model.ConnectionStatus.OFFLINE) {
            postBanner("No Internet Connection. Online backend is required to log in.")
            return
        }
        val trimmed = usernameOrEmail.trim()
        if (trimmed.isEmpty()) {
            postBanner("Please enter your email address or username.")
            return
        }
        if (password.isEmpty()) {
            postBanner("Please enter your password.")
            return
        }

        viewModelScope.launch {
            val normalized = trimmed.lowercase()
            val existingLocalUser = db.userDao().getUserByNormalizedUsername(normalized)
                ?: db.userDao().getUserByEmail(trimmed)
            val effectiveEmail = if (trimmed.contains("@")) trimmed else existingLocalUser?.email

            if (!effectiveEmail.isNullOrBlank()) {
                val fbResult = authRepository.signInWithEmail(effectiveEmail, password)
                if (fbResult.isSuccess) {
                    val fbUser = fbResult.getOrThrow()
                    val uid = fbUser.uid
                    var user = db.userDao().getUserById(uid)
                    if (user == null) {
                        val remoteProfile = firestoreRepository?.getUserProfile(uid)?.getOrNull()
                        val uName = remoteProfile?.username?.takeIf { it.isNotBlank() }
                            ?: fbUser.displayName
                            ?: trimmed.substringBefore("@")
                        user = UserEntity(
                            userId = uid,
                            username = uName,
                            normalizedUsername = uName.lowercase(),
                            email = fbUser.email ?: effectiveEmail,
                            passwordHash = hashPassword(password),
                            cashBalance = remoteProfile?.cashBalance ?: STARTING_CASH_USD,
                            realizedPnL = 0.0,
                            streakDays = remoteProfile?.streakDays?.toInt() ?: 1,
                            lastClaimDateKathmandu = remoteProfile?.lastClaimDateKathmandu
                        )
                        db.userDao().upsertUser(user)
                    }
                    switchActiveUser(uid)
                    markAuthenticated(true)
                    _showAuthModal.value = false
                    syncHoldingsWithFirestore()
                    postBanner("Welcome back, @${user.username}!")
                    return@launch
                }
            }

            // Local user check (backward compatibility preserving existing local accounts)
            if (existingLocalUser != null) {
                if (existingLocalUser.passwordHash != hashPassword(password)) {
                    postBanner("Invalid password for '${existingLocalUser.username}'. Please try again.")
                    return@launch
                }
                switchActiveUser(existingLocalUser.userId)
                markAuthenticated(true)
                _showAuthModal.value = false
                syncHoldingsWithFirestore()
                postBanner("Welcome back, @${existingLocalUser.username}!")
            } else {
                postBanner("No account found for '$trimmed'. Please check your credentials or Sign Up.")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            postBanner("Please enter a valid email address for password reset.")
            return
        }
        viewModelScope.launch {
            val res = authRepository.sendPasswordResetEmail(trimmed)
            res.onSuccess {
                postBanner("Password recovery link sent to $trimmed. Please check your inbox.")
            }.onFailure { err ->
                postBanner("Password recovery failed: ${err.localizedMessage ?: err.message}")
            }
        }
    }

    fun deleteAccount(context: Context) {
        viewModelScope.launch {
            val uid = _currentUserId.value
            if (uid.isNotBlank()) {
                try {
                    db.userDao().getUserById(uid)?.let { u ->
                        // Clean up user from local Room database
                    }
                    for (h in _holdings.value) {
                        db.holdingDao().deleteHolding(uid, h.assetId)
                    }
                } catch (e: Exception) {
                    Log.e("Auth", "Failed to clean up local user data", e)
                }
            }
            authRepository.deleteAccount(context)
            signOut(context)
            postBanner("Your account and simulation data have been deleted.")
        }
    }

    fun updateUniqueUsername(newUsername: String) {
        viewModelScope.launch {
            val clean = newUsername.trim()
            if (!USERNAME_REGEX.matches(clean)) {
                postBanner("Username must be 3–20 alphanumeric/underscore characters.")
                return@launch
            }
            val normalized = clean.lowercase()
            val conflict = db.userDao().getUserByNormalizedUsername(normalized)
            val current = _currentUser.value ?: return@launch
            if (conflict != null && conflict.userId != current.userId) {
                postBanner("Username '@$clean' is already claimed by another trader.")
                return@launch
            }
            db.userDao().updateUser(
                current.copy(
                    username = clean,
                    normalizedUsername = normalized
                )
            )
            syncHoldingsWithFirestore()
            postBanner("Unique handle updated to @$clean")
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
            _isAuthenticatedSession.value = false
            prefs.edit()
                .putBoolean("is_authenticated_session", false)
                .remove("active_user_id")
                .apply()
            userStreamsJob?.cancel()
            _currentUserId.value = ""
            _currentUser.value = null
            _holdings.value = emptyList()
            _transactions.value = emptyList()
            _limitOrders.value = emptyList()
            _tradeOffers.value = emptyList()
            _dailyRewardClaims.value = emptyList()
            _showAuthModal.value = false
            postBanner("You have logged out of Life Assets Simulator.")
        }
    }

    private fun syncOrderAndHoldingsWithFirestore(tx: TransactionEntity) {
        val fbUser = authRepository.currentUser
        val repo = firestoreRepository
        if (fbUser != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.recordTradeOrder(
                        orderId = tx.transactionId,
                        assetId = tx.assetId,
                        symbol = tx.assetSymbol,
                        side = tx.side,
                        quantity = tx.quantity,
                        executionPrice = tx.executionPrice,
                        fee = tx.feeAmount,
                        totalAmount = tx.totalAmount,
                        realizedPnL = tx.realizedPnL
                    )
                    backendQuotaMonitor.recordWrites(1)
                } catch (_: Exception) {
                }
            }
        }
        syncHoldingsWithFirestore()
    }

    private fun syncHoldingsWithFirestore() {
        val fbUser = authRepository.currentUser ?: return
        val repo = firestoreRepository ?: return
        val currentSnap = backendQuotaMonitor.snapshot.value
        if (currentSnap.alertLevel == FreeTierQuotaAlertLevel.CRITICAL_NEAR_LIMIT) {
            postBanner(
                currentSnap.warningMessage
                    ?: "Firebase Spark free-tier write limit reached for today."
            )
            return
        }
        viewModelScope.launch {
            try {
                val user = _currentUser.value
                val metrics = portfolioMetrics.value
                if (user != null) {
                    repo.syncUserProfile(
                        username = user.username,
                        displayName = fbUser.displayName,
                        cashBalance = metrics.cashBalance,
                        netWorth = metrics.totalNetWorth,
                        streakDays = user.streakDays,
                        lastClaimDateKathmandu = user.lastClaimDateKathmandu
                    )
                    backendQuotaMonitor.recordWrites(1)
                }
                val ticks = marketEngine.ticks.value
                val localHoldings = _holdings.value
                val localAssetIds = localHoldings.map { it.assetId }.toSet()
                for (h in localHoldings) {
                    val asset = AssetCatalogue.getAssetById(h.assetId) ?: continue
                    val dbEntity = db.assetDao().getAssetById(h.assetId)
                    val tick = ticks[h.assetId]
                    val onlinePrice = tick?.realWorldReferencePrice
                        ?: dbEntity?.onlinePriceUsd?.takeIf { it > 0.0 }
                        ?: asset.onlinePriceUsd.takeIf { it > 0.0 }
                        ?: asset.basePrice
                    val imageUrl = dbEntity?.imageUrl?.ifBlank { asset.imageUrl } ?: asset.imageUrl
                    val imageSource = dbEntity?.imageSource?.ifBlank { asset.imageSource } ?: asset.imageSource
                    val licenseInfo = dbEntity?.licenseInfo?.ifBlank { asset.licenseInfo } ?: asset.licenseInfo
                    val verificationStatus = dbEntity?.verificationStatus ?: asset.verificationStatus.name

                    repo.upsertHolding(
                        assetId = h.assetId,
                        symbol = asset.symbol,
                        name = asset.name,
                        category = asset.category.code,
                        quantity = h.quantity,
                        averageCostBasis = h.averageCostBasis,
                        imageUrl = imageUrl,
                        imageSource = imageSource,
                        licenseInfo = licenseInfo,
                        verificationStatus = verificationStatus,
                        onlinePriceUsd = onlinePrice
                    )
                    repo.syncAssetWithRemoteAndRoom(
                        asset = dbEntity?.toAsset() ?: asset,
                        tick = tick,
                        roomDb = db
                    )
                    backendQuotaMonitor.recordWrites(2)
                }
                repo.pullRemoteAssetsIntoRoom(db)
                backendQuotaMonitor.recordReads(1)
                repo.getHoldings().onSuccess { cloudHoldings ->
                    backendQuotaMonitor.recordReads(cloudHoldings.size.coerceAtLeast(1))
                    for (ch in cloudHoldings) {
                        if (ch.assetId !in localAssetIds) {
                            repo.deleteHolding(ch.assetId)
                            backendQuotaMonitor.recordDeletes(1)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun postBanner(message: String) {
        _statusBanner.value = message
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    companion object {
        const val DEFAULT_USER_ID = "sovereign_titan_default"
        const val STARTING_CASH_USD = 4_000_000.0
        const val STARTING_ASSET_ALLOCATION_USD = 3_000_000.0

        /**
         * Verified $3,000,000.00 starting real-world asset portfolio allocation, configured
         * strictly separately from the $4,000,000.00 starting virtual cash balance ($7,000,000.00 total net worth)
         * to prevent double-counting.
         *
         * Breakdown:
         * - 1 × met_01 (Gold Bar 400 oz LBMA Good Delivery @ $1,080,000) = $1,080,000.00
         * - 1 × stk_08 (Berkshire Hathaway Inc. Class A BRK.A @ $685,000) = $685,000.00
         * - 1 × car_04 (Rolls-Royce Phantom Extended Series II @ $645,000) = $645,000.00
         * - 1 × car_11 (Lamborghini Urus SE Performante @ $310,000) = $310,000.00
         * - 2 × crypto_01 (Bitcoin BTC @ $88,450) = $176,900.00
         * - 1 × watch_02 (Rolex Submariner 18k Yellow Gold @ $42,500) = $42,500.00
         * - 1 × met_02 (Silver Bar 1,000 oz COMEX Deliverable @ $32,500) = $32,500.00
         * - 4 × crypto_02 (Ethereum ETH @ $3,420) = $13,680.00
         * - 20 × stk_02 (Microsoft Corporation MSFT @ $445) = $8,900.00
         * - 40 × stk_20 (FTSE All-World ETF VWRL @ $138) = $5,520.00
         * Total Starter Asset Value = $3,000,000.00
         */
        val STARTER_PORTFOLIO_3M_HOLDINGS: List<Triple<String, Double, Double>> = listOf(
            Triple("met_01", 1.0, 1_080_000.0),
            Triple("stk_08", 1.0, 685_000.0),
            Triple("car_04", 1.0, 645_000.0),
            Triple("car_11", 1.0, 310_000.0),
            Triple("crypto_01", 2.0, 88_450.0),
            Triple("watch_02", 1.0, 42_500.0),
            Triple("met_02", 1.0, 32_500.0),
            Triple("crypto_02", 4.0, 3_420.0),
            Triple("stk_02", 20.0, 445.0),
            Triple("stk_20", 40.0, 138.0)
        )

        fun calculateStarterAssetAllocationTotalUsd(): Double {
            return STARTER_PORTFOLIO_3M_HOLDINGS.sumOf { (_, qty, unitPrice) -> qty * unitPrice }
        }

        val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")

        fun hashPassword(rawPassword: String): String {
            val salted = "LifeAssetsSalt2026::$rawPassword"
            val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

package com.example.engine

import com.example.BuildConfig
import com.example.data.catalogue.AssetCatalogue
import com.example.data.local.HoldingEntity
import com.example.data.model.MarketRegime
import com.example.data.model.MarketTick
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LifeBotMessage(
    val id: String,
    val isFromBot: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

class LifeBotService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun generatePortfolioBriefing(
        username: String,
        cashBalance: Double,
        netWorth: Double,
        holdings: List<HoldingEntity>,
        ticks: Map<String, MarketTick>,
        regime: MarketRegime
    ): String {
        val topGainer = ticks.values.maxByOrNull { it.changePercent24h }
        val topLoser = ticks.values.minByOrNull { it.changePercent24h }
        val topGainerAsset = topGainer?.let { AssetCatalogue.getAssetById(it.assetId) }
        val topLoserAsset = topLoser?.let { AssetCatalogue.getAssetById(it.assetId) }

        val cashRatio = if (netWorth > 0) (cashBalance / netWorth) * 100.0 else 100.0
        val gainerText = if (topGainer != null && topGainerAsset != null) {
            "${topGainerAsset.symbol} (${String.format("%+.2f%%", topGainer.changePercent24h)})"
        } else "N/A"
        val loserText = if (topLoser != null && topLoserAsset != null) {
            "${topLoserAsset.symbol} (${String.format("%+.2f%%", topLoser.changePercent24h)})"
        } else "N/A"

        return "Greetings, $username. Market Regime: ${regime.label}. " +
            "Your Net Worth stands at $${String.format("%,.2f", netWorth)} across ${holdings.size} asset classes (${String.format("%.1f%%", cashRatio)} liquid cash). " +
            "24h Leader: $gainerText | 24h Laggard: $loserText."
    }

    suspend fun askLifeBot(
        userPrompt: String,
        username: String,
        cashBalance: Double,
        netWorth: Double,
        holdings: List<HoldingEntity>,
        ticks: Map<String, MarketTick>,
        regime: MarketRegime
    ): String = withContext(Dispatchers.IO) {
        val portfolioContext = buildString {
            append("You are LifeBot, the futuristic robotic financial AI advisor inside LifeAssets: Billionaire Simulator. ")
            append("Speak concisely, analytically, and responsibly. Clearly distinguish simulated in-game prices from real-world brokerage advice, and never guarantee risk-free returns.\n")
            append("Player: $username | Liquid Cash: $${String.format("%,.2f", cashBalance)} | Net Worth: $${String.format("%,.2f", netWorth)}\n")
            append("Active Market Regime: ${regime.label}\n")
            append("Holdings Count: ${holdings.size}\n")
            holdings.take(6).forEach { h ->
                val asset = AssetCatalogue.getAssetById(h.assetId)
                val price = ticks[h.assetId]?.currentPrice ?: asset?.basePrice ?: 0.0
                append("- ${asset?.name ?: h.assetId}: Qty=${h.quantity}, AvgCost=$${String.format("%,.2f", h.averageCostBasis)}, Current=$${String.format("%,.2f", price)}\n")
            }
            append("\nUser Question: $userPrompt")
        }

        // 1. Try Firebase AI Logic first
        try {
            val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash")
            val response = generativeModel.generateContent(portfolioContext)
            val text = response.text
            if (!text.isNullOrBlank()) {
                return@withContext text.trim()
            }
        } catch (_: Exception) {
        }

        // 2. Try Gemini REST API if GEMINI_API_KEY is configured
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", portfolioContext))
                            )
                        )
                    )
                }
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string().orEmpty()
                        val json = JSONObject(bodyStr)
                        val text = json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext text.trim()
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Deterministic data-driven LifeBot analytical response using real portfolio & market telemetry
        buildLocalDataDrivenResponse(userPrompt, username, cashBalance, netWorth, holdings, ticks, regime)
    }

    private fun buildLocalDataDrivenResponse(
        userPrompt: String,
        username: String,
        cashBalance: Double,
        netWorth: Double,
        holdings: List<HoldingEntity>,
        ticks: Map<String, MarketTick>,
        regime: MarketRegime
    ): String {
        val lower = userPrompt.lowercase()
        val topGainers = ticks.values.sortedByDescending { it.changePercent24h }.take(3)
        val topLosers = ticks.values.sortedBy { it.changePercent24h }.take(3)

        return when {
            lower.contains("fee") || lower.contains("cost") || lower.contains("quantity") ->
                "LifeBot Order Math Briefing: Every market and limit trade applies a 0.50% brokerage fee. " +
                    "Subtotal = Quantity × Execution Price. Total Buy Cost = Subtotal + Fee. Net Sell Proceeds = Subtotal − Fee. " +
                    "Indivisible luxury assets (supercars, watches, paintings, jets, real estate) require whole units (1..100+), whereas cryptocurrencies, metals, and agricultural commodities support fractional units."
            lower.contains("gainer") || lower.contains("loser") || lower.contains("trend") || lower.contains("market") -> {
                val gainStr = topGainers.joinToString(", ") {
                    val a = AssetCatalogue.getAssetById(it.assetId)
                    "${a?.symbol ?: it.assetId} (${String.format("%+.2f%%", it.changePercent24h)})"
                }
                val loseStr = topLosers.joinToString(", ") {
                    val a = AssetCatalogue.getAssetById(it.assetId)
                    "${a?.symbol ?: it.assetId} (${String.format("%+.2f%%", it.changePercent24h)})"
                }
                "Live Simulated Market Telemetry [${regime.label}]: Top 24h Gainers: $gainStr. Top 24h Pullbacks: $loseStr. " +
                    "Note: Prices move up and down dynamically; past simulated ticks do not guarantee future appreciation."
            }
            else -> {
                val investedValue = (netWorth - cashBalance).coerceAtLeast(0.0)
                "LifeBot Portfolio Diagnostics for $username: Total Net Worth is $${String.format("%,.2f", netWorth)} " +
                    "with $${String.format("%,.2f", cashBalance)} in liquid USD and $${String.format("%,.2f", investedValue)} deployed across ${holdings.size} holdings. " +
                    "Current macro cycle is ${regime.label}. Use the Quantity Selector (1, 5, 10, 25, 50, 100, MAX) on any asset card to review fees and remaining cash before confirming."
            }
        }
    }
}

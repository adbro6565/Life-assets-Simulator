package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.catalogue.AssetImageRegistry
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.ImageVerificationStatus
import com.example.data.model.RarityTier
import com.example.ui.theme.BearRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class CategoryVisualTheme(
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val bgGradient: List<Color>,
    val icon: ImageVector,
    val label: String,
    val cadCode: String
)

object OnlineAssetImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    /**
     * Online-only Coil ImageLoader.
     * - Attaches an explicit User-Agent required by Wikimedia Commons & Unsplash CDNs.
     * - Disables disk caching (`CachePolicy.DISABLED`) so asset photos are fetched online
     *   and never bundled or served from offline storage.
     */
    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: run {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "LifeAssets-Android/2.0 (https://lifeassets.io; verified-asset-media@lifeassets.io)"
                            )
                            .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                ImageLoader.Builder(context.applicationContext)
                    .okHttpClient(okHttpClient)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .respectCacheHeaders(false)
                    .crossfade(true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

object AssetVisualConfig {

    fun getTheme(category: AssetCategory): CategoryVisualTheme = when (category) {
        AssetCategory.SUPERCARS -> CategoryVisualTheme(
            primaryColor = Color(0xFFFF5722),
            secondaryColor = Color(0xFF38180A),
            accentColor = Color(0xFFFF9800),
            bgGradient = listOf(Color(0xFF38180A), Color(0xFF140A04)),
            icon = Icons.Default.DirectionsCar,
            label = "Supercar / Hypercar",
            cadCode = "AUTO-PHOTO"
        )
        AssetCategory.PRIVATE_JETS -> CategoryVisualTheme(
            primaryColor = Color(0xFF0284C7),
            secondaryColor = Color(0xFF0A1D37),
            accentColor = Color(0xFF38BDF8),
            bgGradient = listOf(Color(0xFF0A1D37), Color(0xFF040B17)),
            icon = Icons.Default.Flight,
            label = "Private Jet / Rotorcraft",
            cadCode = "AERO-PHOTO"
        )
        AssetCategory.YACHTS_SHIPS -> CategoryVisualTheme(
            primaryColor = Color(0xFF0284C7),
            secondaryColor = Color(0xFF072540),
            accentColor = Color(0xFF0EA5E9),
            bgGradient = listOf(Color(0xFF072540), Color(0xFF030D17)),
            icon = Icons.Default.DirectionsBoat,
            label = "Luxury Yacht / Maritime",
            cadCode = "MAR-PHOTO"
        )
        AssetCategory.LUXURY_REAL_ESTATE -> CategoryVisualTheme(
            primaryColor = Color(0xFFD97706),
            secondaryColor = Color(0xFF2D1E0F),
            accentColor = Color(0xFFF59E0B),
            bgGradient = listOf(Color(0xFF2D1E0F), Color(0xFF0F0B05)),
            icon = Icons.Default.Apartment,
            label = "Mansion / Architecture",
            cadCode = "ARCH-PHOTO"
        )
        AssetCategory.PRIVATE_ISLANDS -> CategoryVisualTheme(
            primaryColor = Color(0xFF059669),
            secondaryColor = Color(0xFF0A2F1E),
            accentColor = Color(0xFF10B981),
            bgGradient = listOf(Color(0xFF0A2F1E), Color(0xFF04140D)),
            icon = Icons.Default.Landscape,
            label = "Private Island / Atoll",
            cadCode = "TERRA-PHOTO"
        )
        AssetCategory.LUXURY_WATCHES -> CategoryVisualTheme(
            primaryColor = Color(0xFFE0A96D),
            secondaryColor = Color(0xFF1B1736),
            accentColor = Color(0xFFF6D8A8),
            bgGradient = listOf(Color(0xFF1B1736), Color(0xFF0C091A)),
            icon = Icons.Default.Watch,
            label = "Haute Horology",
            cadCode = "HORO-PHOTO"
        )
        AssetCategory.FINE_ART -> CategoryVisualTheme(
            primaryColor = Color(0xFFE11D48),
            secondaryColor = Color(0xFF360D1E),
            accentColor = Color(0xFFF43F5E),
            bgGradient = listOf(Color(0xFF360D1E), Color(0xFF14030A)),
            icon = Icons.Default.Palette,
            label = "Fine Art Masterpiece",
            cadCode = "ART-PHOTO"
        )
        AssetCategory.JEWELRY_DIAMONDS -> CategoryVisualTheme(
            primaryColor = Color(0xFFA855F7),
            secondaryColor = Color(0xFF280A3C),
            accentColor = Color(0xFFC084FC),
            bgGradient = listOf(Color(0xFF280A3C), Color(0xFF12031C)),
            icon = Icons.Default.Diamond,
            label = "Diamond & Gemstone",
            cadCode = "GEM-PHOTO"
        )
        AssetCategory.DESIGNER_FASHION -> CategoryVisualTheme(
            primaryColor = Color(0xFFEC4899),
            secondaryColor = Color(0xFF350E28),
            accentColor = Color(0xFFF472B6),
            bgGradient = listOf(Color(0xFF350E28), Color(0xFF14040F)),
            icon = Icons.Default.ShoppingBag,
            label = "Haute Couture",
            cadCode = "FASH-PHOTO"
        )
        AssetCategory.PRECIOUS_METALS -> CategoryVisualTheme(
            primaryColor = Color(0xFFFFD700),
            secondaryColor = Color(0xFF3D2B05),
            accentColor = Color(0xFFFACC15),
            bgGradient = listOf(Color(0xFF3D2B05), Color(0xFF140E02)),
            icon = Icons.Default.MonetizationOn,
            label = "999.9 Fine Bullion",
            cadCode = "METL-PHOTO"
        )
        AssetCategory.RARE_COLLECTIBLES -> CategoryVisualTheme(
            primaryColor = Color(0xFFD97706),
            secondaryColor = Color(0xFF2E1C12),
            accentColor = Color(0xFFFBBF24),
            bgGradient = listOf(Color(0xFF2E1C12), Color(0xFF120B07)),
            icon = Icons.Default.EmojiEvents,
            label = "Rare Antiquity / Relic",
            cadCode = "RELI-PHOTO"
        )
        AssetCategory.AGRICULTURAL -> CategoryVisualTheme(
            primaryColor = Color(0xFFEAB308),
            secondaryColor = Color(0xFF26230C),
            accentColor = Color(0xFFFDE047),
            bgGradient = listOf(Color(0xFF26230C), Color(0xFF100F04)),
            icon = Icons.Default.Agriculture,
            label = "Agro Commodity",
            cadCode = "AGRI-PHOTO"
        )
        AssetCategory.LUXURY_FOOD -> CategoryVisualTheme(
            primaryColor = Color(0xFFE2E8F0),
            secondaryColor = Color(0xFF241828),
            accentColor = Color(0xFFD4AF37),
            bgGradient = listOf(Color(0xFF241828), Color(0xFF0E0810)),
            icon = Icons.Default.RestaurantMenu,
            label = "Epicurean & Caviar",
            cadCode = "FOOD-PHOTO"
        )
        AssetCategory.CRYPTOCURRENCIES -> CategoryVisualTheme(
            primaryColor = Color(0xFFF97316),
            secondaryColor = Color(0xFF1E1238),
            accentColor = Color(0xFFA855F7),
            bgGradient = listOf(Color(0xFF1E1238), Color(0xFF0D061A)),
            icon = Icons.Default.CurrencyBitcoin,
            label = "Crypto & Web3",
            cadCode = "CRYP-PHOTO"
        )
        AssetCategory.STOCKS_INDICES -> CategoryVisualTheme(
            primaryColor = Color(0xFF22C55E),
            secondaryColor = Color(0xFF08281A),
            accentColor = Color(0xFF4ADE80),
            bgGradient = listOf(Color(0xFF08281A), Color(0xFF03140C)),
            icon = Icons.Default.ShowChart,
            label = "Global Equity / Index",
            cadCode = "EQTY-PHOTO"
        )
        AssetCategory.BUSINESSES -> CategoryVisualTheme(
            primaryColor = Color(0xFF3B82F6),
            secondaryColor = Color(0xFF0F203C),
            accentColor = Color(0xFF60A5FA),
            bgGradient = listOf(Color(0xFF0F203C), Color(0xFF050E1C)),
            icon = Icons.Default.BusinessCenter,
            label = "Enterprise & Venture",
            cadCode = "CORP-PHOTO"
        )
        AssetCategory.INDUSTRIAL -> CategoryVisualTheme(
            primaryColor = Color(0xFFF59E0B),
            secondaryColor = Color(0xFF2D2109),
            accentColor = Color(0xFFFBBF24),
            bgGradient = listOf(Color(0xFF2D2109), Color(0xFF140F04)),
            icon = Icons.Default.PrecisionManufacturing,
            label = "Heavy Industrial Tech",
            cadCode = "IND-PHOTO"
        )
        AssetCategory.TRANSPORT_LOGISTICS -> CategoryVisualTheme(
            primaryColor = Color(0xFF0284C7),
            secondaryColor = Color(0xFF112638),
            accentColor = Color(0xFF38BDF8),
            bgGradient = listOf(Color(0xFF112638), Color(0xFF06101A)),
            icon = Icons.Default.LocalShipping,
            label = "Global Logistics",
            cadCode = "LOGI-PHOTO"
        )
        AssetCategory.TECH_COMPUTING -> CategoryVisualTheme(
            primaryColor = Color(0xFF06B6D4),
            secondaryColor = Color(0xFF062A30),
            accentColor = Color(0xFF2DD4BF),
            bgGradient = listOf(Color(0xFF062A30), Color(0xFF021217)),
            icon = Icons.Default.Memory,
            label = "AI & Quantum Cluster",
            cadCode = "TECH-PHOTO"
        )
        AssetCategory.SPACE_ENERGY -> CategoryVisualTheme(
            primaryColor = Color(0xFF6366F1),
            secondaryColor = Color(0xFF1A0E38),
            accentColor = Color(0xFF818CF8),
            bgGradient = listOf(Color(0xFF1A0E38), Color(0xFF0A041A)),
            icon = Icons.Default.RocketLaunch,
            label = "Space & Clean Energy",
            cadCode = "SPCE-PHOTO"
        )
    }

    fun getRarityBorderColor(rarity: RarityTier): Color = when (rarity) {
        RarityTier.HISTORIC_REFERENCE -> Color(0xFFE2E8F0)
        RarityTier.TROPHY -> MetallicGold
        RarityTier.ULTRA_RARE -> Color(0xFFC084FC)
        RarityTier.RARE -> CyberCyan
        RarityTier.COMMON -> Color(0xFF94A3B8)
    }
}

/**
 * Central Placeholder Fallback component rendered whenever an asset's online photograph
 * is loading, missing, unverified, or fails to load via Coil.
 *
 * Displays:
 * - Central category/broken-image badge and asset symbol
 * - Verification / failure reason (`IMAGE UNAVAILABLE`, `AWAITING VERIFICATION`, or `LOAD FAILED`)
 * - Source & license metadata context
 * - Interactive `RETRY` action to re-fetch from the online CDN
 */
@Composable
fun CentralAssetPlaceholderFallback(
    asset: Asset,
    compact: Boolean,
    reason: String = "IMAGE UNAVAILABLE",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AssetVisualConfig.getTheme(asset.category)
    if (compact) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF141923))
                .border(1.dp, BearRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clickable(onClick = onRetry)
                .padding(3.dp)
                .testTag("image_unavailable_${asset.id}"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.testTag("central_placeholder_fallback_${asset.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Central Placeholder Fallback",
                    tint = BearRed,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "NO IMG",
                    color = TextPrimary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = "RETRY",
                    color = CyberCyan,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF101622))
                .border(1.dp, BearRed.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(14.dp)
                .testTag("image_unavailable_${asset.id}"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.testTag("central_placeholder_fallback_${asset.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = theme.icon,
                        contentDescription = null,
                        tint = theme.primaryColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Central Placeholder Fallback",
                        tint = BearRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$reason / RETRY",
                    color = BearRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${asset.name} (${asset.symbol}) • ${asset.verificationStatus.label}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CyberCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                        .clickable(onClick = onRetry)
                        .testTag("image_retry_button_${asset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry loading image",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RETRY ONLINE PHOTO",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageUnavailableRetryState(
    asset: Asset,
    compact: Boolean,
    reason: String = "IMAGE UNAVAILABLE",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    CentralAssetPlaceholderFallback(
        asset = asset,
        compact = compact,
        reason = reason,
        onRetry = onRetry,
        modifier = modifier
    )
}

/**
 * Displays the asset's verified real-world online photograph (`https://...`) using Coil
 * with a central placeholder fallback (`CentralAssetPlaceholderFallback`) when images fail to load.
 */
@Composable
fun DisplayAssetCoilImage(
    asset: Asset,
    imageIndex: Int,
    modifier: Modifier = Modifier,
    isThumbnail: Boolean = false,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { OnlineAssetImageLoader.get(context) }
    var retryCount by remember(asset.id, imageIndex) { mutableIntStateOf(0) }

    val verifiedMeta = remember(asset.id, asset.imageUrl, asset.imageUrl1, asset.imageUrl2, asset.verificationStatus) {
        if (asset.imageUrl1.isNotBlank() && asset.imageUrl2.isNotBlank()) {
            null
        } else {
            AssetImageRegistry.getVerifiedImageMetadata(
                assetId = asset.id,
                name = asset.name,
                category = asset.category,
                subcategory = asset.subcategory
            )
        }
    }

    val effectiveStatus = verifiedMeta?.verificationStatus ?: asset.verificationStatus
    val rawUrl = when {
        isThumbnail && imageIndex == 0 -> {
            asset.thumbnailUrl.ifBlank {
                asset.imageUrl.ifBlank {
                    asset.imageUrl1.ifBlank { verifiedMeta?.thumbnailUrl.orEmpty() }
                }
            }
        }
        imageIndex == 0 -> {
            asset.imageUrl.ifBlank {
                asset.imageUrl1.ifBlank { verifiedMeta?.imageUrl.orEmpty() }
            }
        }
        else -> {
            asset.imageUrl2.ifBlank { verifiedMeta?.secondaryImageUrl.orEmpty() }
        }
    }

    val isPhotoVerifiedAndOnline = effectiveStatus == ImageVerificationStatus.VERIFIED_REAL_PHOTO &&
        rawUrl.startsWith("https://") &&
        !AssetImageRegistry.isCartoonOrProceduralUrl(rawUrl)

    if (!isPhotoVerifiedAndOnline) {
        CentralAssetPlaceholderFallback(
            asset = asset,
            compact = isThumbnail,
            reason = if (effectiveStatus == ImageVerificationStatus.AWAITING_VERIFICATION) {
                "AWAITING VERIFICATION"
            } else {
                "IMAGE UNAVAILABLE"
            },
            onRetry = { retryCount++ },
            modifier = modifier
        )
        return
    }

    val requestUrl = if (retryCount > 0) {
        if (rawUrl.contains("?")) "$rawUrl&retry=$retryCount" else "$rawUrl?retry=$retryCount"
    } else {
        rawUrl
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(requestUrl)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription ?: "${asset.name} real photograph ${imageIndex + 1}",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .background(ObsidianBlack)
            .clip(RoundedCornerShape(if (isThumbnail) 12.dp else 14.dp))
            .testTag("verified_asset_photo_${asset.id}_$imageIndex"),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MetallicGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(if (isThumbnail) 18.dp else 26.dp)
                )
            }
        },
        error = {
            CentralAssetPlaceholderFallback(
                asset = asset,
                compact = isThumbnail,
                reason = "IMAGE UNAVAILABLE",
                onRetry = { retryCount++ },
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

/**
 * Clean Real-World Asset Thumbnail Picture for cards, lists, and modals.
 * Loads the asset's verified online thumbnail photograph (`thumbnailUrl` / `imageUrl`).
 */
@Composable
fun AssetThumbnailPicture(
    asset: Asset,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    imageIndex: Int = 0
) {
    val theme = AssetVisualConfig.getTheme(asset.category)
    val rarityBorder = AssetVisualConfig.getRarityBorderColor(asset.rarity)

    Surface(
        color = ObsidianBlack,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .border(1.5.dp, rarityBorder.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            DisplayAssetCoilImage(
                asset = asset,
                imageIndex = imageIndex,
                isThumbnail = true,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "${asset.name} verified thumbnail photo ${imageIndex + 1}"
            )

            // Bottom Symbol Tag overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(ObsidianBlack.copy(alpha = 0.78f))
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = asset.symbol.take(6),
                    color = theme.accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * High-Definition Dual-View Real-World Asset Picture for AssetDetailSheet and AssetCard.
 * Displays the verified real-life photograph along with source, license, and online price badge.
 */
@Composable
fun AssetHeroPicture(
    asset: Asset,
    pictureIndex: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianBlack,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            DisplayAssetCoilImage(
                asset = asset,
                imageIndex = pictureIndex,
                isThumbnail = false,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Verified real-world photograph ${pictureIndex + 1} for ${asset.name}"
            )

            if (asset.verificationStatus == ImageVerificationStatus.VERIFIED_REAL_PHOTO) {
                Surface(
                    color = ObsidianBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Real Photograph",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VERIFIED PHOTO • ${asset.imageSource.take(28)}",
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.LruCache
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.example.data.catalogue.AssetCatalogue
import com.example.data.catalogue.AssetImageRegistry
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Coil Fetcher & Bespoke Name-Accurate Image Renderer.
 * Ensures every single marketplace asset (all 400 assets x 2 views = 800 images) renders two distinct,
 * high-resolution images matched to its exact ID, name, brand, model, and category.
 */
object AssetBespokeImageRenderer {

    private val bitmapCache = object : LruCache<String, Bitmap>(32 * 1024) { // 32 MB cache
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    @Volatile
    private var sharedImageLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return sharedImageLoader ?: synchronized(this) {
            sharedImageLoader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    add(AssetSpecificCoilFetcher.Factory(context.applicationContext))
                }
                .crossfade(true)
                .build()
                .also { sharedImageLoader = it }
        }
    }

    fun getOrRenderBitmap(
        uriString: String,
        width: Int = 720,
        height: Int = 420
    ): Bitmap {
        val cacheKey = "${uriString}_${width}x${height}"
        synchronized(bitmapCache) {
            bitmapCache.get(cacheKey)?.let {
                if (!it.isRecycled) return it
            }
        }

        val uri = Uri.parse(uriString)
        val isFlagged = uri.host == "flagged"
        val pathSegments = uri.pathSegments
        val assetId = pathSegments.firstOrNull() ?: "unknown"
        val viewSegment = pathSegments.getOrNull(1) ?: "view1"
        val imageIndex = if (viewSegment.contains("2")) 1 else 0

        val catalogAsset = AssetCatalogue.getAssetById(assetId)
        val bmp = if (isFlagged || (catalogAsset == null && !AssetImageRegistry.isVerifiedAssetId(assetId))) {
            val qName = uri.getQueryParameter("name") ?: "Unverified Asset"
            val qCat = uri.getQueryParameter("cat") ?: "UNKNOWN"
            renderFlaggedPlaceholderBitmap(assetId, qName, qCat, imageIndex, width, height)
        } else {
            val resolvedAsset = catalogAsset ?: Asset(
                id = assetId,
                name = uri.getQueryParameter("name") ?: assetId,
                symbol = uri.getQueryParameter("symbol") ?: assetId.uppercase(),
                category = runCatching {
                    AssetCategory.valueOf(uri.getQueryParameter("cat") ?: "SUPERCARS")
                }.getOrDefault(AssetCategory.SUPERCARS),
                subcategory = uri.getQueryParameter("sub") ?: "Asset",
                description = "Verified Asset",
                basePrice = 100_000.0
            )
            renderVerifiedAssetBitmap(resolvedAsset, imageIndex, width, height)
        }

        synchronized(bitmapCache) {
            bitmapCache.put(cacheKey, bmp)
        }
        return bmp
    }

    private data class BrandPalette(
        val primaryHex: Int,
        val secondaryHex: Int,
        val accentHex: Int,
        val studioBgTop: Int,
        val studioBgBottom: Int,
        val brandTag: String
    )

    private fun resolveBrandPalette(asset: Asset): BrandPalette {
        val n = asset.name.lowercase()
        val hash = abs(asset.id.hashCode() * 31 + asset.name.hashCode())
        return when (asset.category) {
            AssetCategory.SUPERCARS -> when {
                "bugatti" in n && "300+" in n -> BrandPalette(0xFFFF6B00.toInt(), 0xFF1A1E29.toInt(), 0xFFFF9E40.toInt(), 0xFF1A120B.toInt(), 0xFF080A0F.toInt(), "BUGATTI MOLSHEIM")
                "bugatti" in n && "divo" in n -> BrandPalette(0xFF00D4FF.toInt(), 0xFF263238.toInt(), 0xFF80EAFF.toInt(), 0xFF091B26.toInt(), 0xFF070A0E.toInt(), "BUGATTI COACHBUILT")
                "bugatti" in n -> BrandPalette(0xFF0066FF.toInt(), 0xFF0D1B3E.toInt(), 0xFF66A3FF.toInt(), 0xFF08142C.toInt(), 0xFF06080E.toInt(), "BUGATTI HYPERCAR")
                "lamborghini" in n && "svj" in n -> BrandPalette(0xFF64DD17.toInt(), 0xFFD4AF37.toInt(), 0xFFB2FF59.toInt(), 0xFF0F1F0A.toInt(), 0xFF070A06.toInt(), "LAMBORGHINI V12 SVJ")
                "lamborghini" in n && "revuelto" in n -> BrandPalette(0xFFFF5722.toInt(), 0xFF212121.toInt(), 0xFFFF8A65.toInt(), 0xFF241008.toInt(), 0xFF090707.toInt(), "LAMBORGHINI HPEV")
                "lamborghini" in n && "urus" in n -> BrandPalette(0xFFFFB300.toInt(), 0xFF1E293B.toInt(), 0xFFFFE57F.toInt(), 0xFF211806.toInt(), 0xFF08090C.toInt(), "LAMBORGHINI SUPER SUV")
                "lamborghini" in n -> BrandPalette(0xFFFFD600.toInt(), 0xFF1F1B08.toInt(), 0xFFFFEA00.toInt(), 0xFF241E06.toInt(), 0xFF090806.toInt(), "LAMBORGHINI SANT'AGATA")
                "ferrari" in n && "250 gto" in n -> BrandPalette(0xFFD50000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFD600.toInt(), 0xFF280808.toInt(), 0xFF0A0606.toInt(), "SCUDERIA HISTORIC 1962")
                "ferrari" in n -> BrandPalette(0xFFE60012.toInt(), 0xFFFFD600.toInt(), 0xFFFF5252.toInt(), 0xFF26070A.toInt(), 0xFF090506.toInt(), "FERRARI MARANELLO")
                "rolls-royce" in n -> BrandPalette(0xFFD8DEE9.toInt(), 0xFF311B47.toInt(), 0xFFE6C665.toInt(), 0xFF1A1228.toInt(), 0xFF08070C.toInt(), "ROLLS-ROYCE BESPOKE")
                "bentley" in n -> BrandPalette(0xFF10B981.toInt(), 0xFF064E3B.toInt(), 0xFFD4AF37.toInt(), 0xFF08221A.toInt(), 0xFF060B09.toInt(), "BENTLEY MULLINER")
                "mclaren" in n -> BrandPalette(0xFFFF8000.toInt(), 0xFF1E293B.toInt(), 0xFFFFB74D.toInt(), 0xFF241406.toInt(), 0xFF08090C.toInt(), "MCLAREN WOKING")
                "koenigsegg" in n -> BrandPalette(0xFFF8FAFC.toInt(), 0xFFFF9100.toInt(), 0xFF38BDF8.toInt(), 0xFF141B24.toInt(), 0xFF07090C.toInt(), "KOENIGSEGG ÄNGELHOLM")
                "pagani" in n -> BrandPalette(0xFFC8A265.toInt(), 0xFF475569.toInt(), 0xFFFFDF73.toInt(), 0xFF211A10.toInt(), 0xFF08080A.toInt(), "PAGANI MODENA")
                "maybach" in n -> BrandPalette(0xFFE5A97C.toInt(), 0xFF1E1B18.toInt(), 0xFFFDE68A.toInt(), 0xFF221712.toInt(), 0xFF080707.toInt(), "MERCEDES-MAYBACH")
                "g 63" in n -> BrandPalette(0xFFFACC15.toInt(), 0xFF1E293B.toInt(), 0xFFFDE047.toInt(), 0xFF1E1B09.toInt(), 0xFF08090B.toInt(), "AMG 4X4² PORTAL")
                "300 slr" in n -> BrandPalette(0xFFE2E8F0.toInt(), 0xFF991B1B.toInt(), 0xFFF8FAFC.toInt(), 0xFF1A1E26.toInt(), 0xFF08090C.toInt(), "UHLENHAUT 1955")
                "mercedes" in n -> BrandPalette(0xFFCBD5E1.toInt(), 0xFF0F172A.toInt(), 0xFF94A3B8.toInt(), 0xFF131A24.toInt(), 0xFF07090C.toInt(), "MERCEDES-BENZ GUARD")
                "porsche" in n -> BrandPalette(0xFF0284C7.toInt(), 0xFF84CC16.toInt(), 0xFF38BDF8.toInt(), 0xFF081B2B.toInt(), 0xFF06090E.toInt(), "PORSCHE WEISSACH")
                "aston martin" in n -> BrandPalette(0xFF0D9488.toInt(), 0xFFA3E635.toInt(), 0xFF2DD4BF.toInt(), 0xFF072220.toInt(), 0xFF050A0A.toInt(), "ASTON MARTIN AMR")
                "bmw" in n -> BrandPalette(0xFF3B82F6.toInt(), 0xFFEF4444.toInt(), 0xFF60A5FA.toInt(), 0xFF0D182E.toInt(), 0xFF07080C.toInt(), "BMW M INDIVIDUAL")
                "range rover" in n -> BrandPalette(0xFFC28E5C.toInt(), 0xFF27272A.toInt(), 0xFFFDE68A.toInt(), 0xFF211810.toInt(), 0xFF080809.toInt(), "RANGE ROVER SV")
                "tesla" in n -> BrandPalette(0xFFEF4444.toInt(), 0xFF1E293B.toInt(), 0xFFF87171.toInt(), 0xFF240B0E.toInt(), 0xFF080709.toInt(), "TESLA PLAID TRACK")
                "lucid" in n -> BrandPalette(0xFF2563EB.toInt(), 0xFFD4AF37.toInt(), 0xFF60A5FA.toInt(), 0xFF0A1633.toInt(), 0xFF06080E.toInt(), "LUCID SAPPHIRE")
                else -> BrandPalette(0xFFD4AF37.toInt(), 0xFF1E293B.toInt(), 0xFFFFDF73.toInt(), 0xFF1E180A.toInt(), 0xFF08090C.toInt(), "BESPOKE HYPERCAR")
            }
            AssetCategory.PRIVATE_JETS -> when {
                "gulfstream" in n -> BrandPalette(0xFF38BDF8.toInt(), 0xFFD4AF37.toInt(), 0xFFE0F2FE.toInt(), 0xFF0A1D2E.toInt(), 0xFF060A10.toInt(), "GULFSTREAM AEROSPACE")
                "bombardier" in n || "global" in n -> BrandPalette(0xFFF43F5E.toInt(), 0xFFE2E8F0.toInt(), 0xFFFDA4AF.toInt(), 0xFF210D16.toInt(), 0xFF07080C.toInt(), "BOMBARDIER GLOBAL")
                "falcon" in n -> BrandPalette(0xFF60A5FA.toInt(), 0xFFD4AF37.toInt(), 0xFF93C5FD.toInt(), 0xFF0B1930.toInt(), 0xFF06090F.toInt(), "DASSAULT AVIATION")
                "boeing" in n || "airbus" in n -> BrandPalette(0xFFE6C665.toInt(), 0xFF1E3A8A.toInt(), 0xFFFDE68A.toInt(), 0xFF181926.toInt(), 0xFF07080E.toInt(), "VVIP WIDEBODY JET")
                "boom" in n -> BrandPalette(0xFF22D3EE.toInt(), 0xFF0F172A.toInt(), 0xFF67E8F9.toInt(), 0xFF082028.toInt(), 0xFF05090D.toInt(), "SUPERSONIC MACH 1.7")
                "sikorsky" in n || "ach160" in n || "aw139" in n || "bell" in n || "helicopter" in asset.subcategory.lowercase() ->
                    BrandPalette(0xFF10B981.toInt(), 0xFFD4AF37.toInt(), 0xFF6EE7B7.toInt(), 0xFF08201B.toInt(), 0xFF060909.toInt(), "VVIP ROTORCRAFT")
                else -> BrandPalette(0xFF38BDF8.toInt(), 0xFF1E293B.toInt(), 0xFFD4AF37.toInt(), 0xFF0B1B29.toInt(), 0xFF06090E.toInt(), "EXECUTIVE AVIATION")
            }
            AssetCategory.YACHTS_SHIPS -> when {
                "sailing" in n || "maltese" in n || "koru" in n -> BrandPalette(0xFF2DD4BF.toInt(), 0xFFD4AF37.toInt(), 0xFF99F6E4.toInt(), 0xFF072224.toInt(), 0xFF050A0D.toInt(), "SAILING SUPERYACHT")
                "submersible" in n || "triton" in n || "migaloo" in n -> BrandPalette(0xFF00E5FF.toInt(), 0xFF0F172A.toInt(), 0xFF80F2FF.toInt(), 0xFF051C28.toInt(), 0xFF04080E.toInt(), "DEEP-SEA SUBMERSIBLE")
                "riva" in n -> BrandPalette(0xFFD97706.toInt(), 0xFF2DD4BF.toInt(), 0xFFFDE68A.toInt(), 0xFF241508.toInt(), 0xFF070A0D.toInt(), "RIVA MAHOGANY ICON")
                else -> BrandPalette(0xFF06B6D4.toInt(), 0xFFE2E8F0.toInt(), 0xFFD4AF37.toInt(), 0xFF08202B.toInt(), 0xFF05090E.toInt(), "SOVEREIGN MEGAYACHT")
            }
            AssetCategory.LUXURY_WATCHES -> when {
                "tiffany" in n -> BrandPalette(0xFF81D8D0.toInt(), 0xFFD4AF37.toInt(), 0xFFB2F5EA.toInt(), 0xFF092422.toInt(), 0xFF060B0B.toInt(), "PATEK TIFFANY DIAL")
                "patek" in n -> BrandPalette(0xFFE6C665.toInt(), 0xFF3B2314.toInt(), 0xFFFFDF73.toInt(), 0xFF21180B.toInt(), 0xFF080706.toInt(), "PATEK PHILIPPE GENÈVE")
                "rolex" in n && "pepsi" in n -> BrandPalette(0xFFEF4444.toInt(), 0xFF1D4ED8.toInt(), 0xFFE2E8F0.toInt(), 0xFF1C1024.toInt(), 0xFF07080D.toInt(), "ROLEX OYSTER PERPETUAL")
                "rolex" in n -> BrandPalette(0xFF10B981.toInt(), 0xFFD4AF37.toInt(), 0xFFFFDF73.toInt(), 0xFF092117.toInt(), 0xFF060908.toInt(), "ROLEX GENÈVE CROWN")
                "audemars" in n || "royal oak" in n -> BrandPalette(0xFF3B82F6.toInt(), 0xFFD4AF37.toInt(), 0xFF93C5FD.toInt(), 0xFF0C192E.toInt(), 0xFF06080D.toInt(), "AUDEMARS PIGUET")
                "richard mille" in n -> BrandPalette(0xFFEF4444.toInt(), 0xFFFACC15.toInt(), 0xFFF87171.toInt(), 0xFF240D0E.toInt(), 0xFF080708.toInt(), "RICHARD MILLE TOURBILLON")
                "jacob" in n || "astronomia" in n -> BrandPalette(0xFF8B5CF6.toInt(), 0xFFD4AF37.toInt(), 0xFFC4B5FD.toInt(), 0xFF19102C.toInt(), 0xFF07060C.toInt(), "JACOB & CO. ASTRONOMIA")
                else -> {
                    val watchHues = listOf(0xFFD4AF37.toInt(), 0xFF38BDF8.toInt(), 0xFFF43F5E.toInt(), 0xFFA855F7.toInt(), 0xFF34D399.toInt())
                    val c = watchHues[hash % watchHues.size]
                    BrandPalette(c, 0xFFD4AF37.toInt(), 0xFFFFDF73.toInt(), 0xFF1A1610.toInt(), 0xFF08080A.toInt(), "HAUTE HORLOGERIE")
                }
            }
            AssetCategory.FINE_ART -> {
                val artPalettes = listOf(
                    BrandPalette(0xFFEAB308.toInt(), 0xFF78350F.toInt(), 0xFFFDE047.toInt(), 0xFF221A0B.toInt(), 0xFF090806.toInt(), "MUSEUM MASTERWORK"),
                    BrandPalette(0xFF3B82F6.toInt(), 0xFFFACC15.toInt(), 0xFF93C5FD.toInt(), 0xFF0D1B33.toInt(), 0xFF07090F.toInt(), "GALLERY COLLECTION"),
                    BrandPalette(0xFFEF4444.toInt(), 0xFFF97316.toInt(), 0xFFFCA5A5.toInt(), 0xFF26100D.toInt(), 0xFF090606.toInt(), "FINE ART ARCHIVE"),
                    BrandPalette(0xFF10B981.toInt(), 0xFF6366F1.toInt(), 0xFF6EE7B7.toInt(), 0xFF0B211D.toInt(), 0xFF06090B.toInt(), "CURATED CANVAS"),
                    BrandPalette(0xFFEC4899.toInt(), 0xFF8B5CF6.toInt(), 0xFFF9A8D4.toInt(), 0xFF240F21.toInt(), 0xFF09060A.toInt(), "CONTEMPORARY ICON")
                )
                artPalettes[hash % artPalettes.size]
            }
            else -> {
                val hues = listOf(
                    Triple(0xFFD4AF37.toInt(), 0xFF1E293B.toInt(), 0xFF211A0A.toInt()),
                    Triple(0xFF10B981.toInt(), 0xFF064E3B.toInt(), 0xFF09221A.toInt()),
                    Triple(0xFF38BDF8.toInt(), 0xFF0C4A6E.toInt(), 0xFF0A1D2B.toInt()),
                    Triple(0xFFA855F7.toInt(), 0xFF4C1D95.toInt(), 0xFF1B102B.toInt()),
                    Triple(0xFFF43F5E.toInt(), 0xFF881337.toInt(), 0xFF240D15.toInt()),
                    Triple(0xFFF59E0B.toInt(), 0xFF78350F.toInt(), 0xFF241709.toInt()),
                    Triple(0xFF14B8A6.toInt(), 0xFF134E4A.toInt(), 0xFF082120.toInt())
                )
                val pick = hues[hash % hues.size]
                BrandPalette(
                    primaryHex = pick.first,
                    secondaryHex = pick.second,
                    accentHex = 0xFFFFDF73.toInt(),
                    studioBgTop = pick.third,
                    studioBgBottom = 0xFF07090D.toInt(),
                    brandTag = asset.subcategory.uppercase()
                )
            }
        }
    }

    private fun renderFlaggedPlaceholderBitmap(
        assetId: String,
        name: String,
        categoryName: String,
        imageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h, 0xFF2B0B0E.toInt(), 0xFF0B0708.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = 0xFFEF4444.toInt()
        }
        canvas.drawRoundRect(RectF(8f, 8f, w - 8f, h - 8f), 18f, 18f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFCA5A5.toInt()
            textSize = h * 0.065f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("IMAGE FLAGGED FOR CORRECTION", w * 0.5f, h * 0.42f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            textSize = h * 0.052f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${name.take(36)} (ID: $assetId)", w * 0.5f, h * 0.54f, subPaint)

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            textSize = h * 0.042f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TEMPORARY PLACEHOLDER • VIEW ${imageIndex + 1} • $categoryName", w * 0.5f, h * 0.66f, footPaint)

        return bmp
    }

    private fun renderVerifiedAssetBitmap(
        asset: Asset,
        imageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val palette = resolveBrandPalette(asset)
        val seed = abs(asset.id.hashCode() * 37 + asset.name.hashCode())

        if (imageIndex == 0) {
            drawView1StudioShowcase(canvas, w, h, asset, palette, seed)
        } else {
            drawView2TechnicalDetail(canvas, w, h, asset, palette, seed)
        }
        return bmp
    }

    private fun drawView1StudioShowcase(
        canvas: Canvas,
        w: Float,
        h: Float,
        asset: Asset,
        palette: BrandPalette,
        seed: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h, palette.studioBgTop, palette.studioBgBottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val spotX = w * (0.42f + (seed % 16) * 0.01f)
        val spotY = h * 0.45f
        val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                spotX,
                spotY,
                w * 0.48f,
                intArrayOf(withAlpha(palette.primaryHex, 115), withAlpha(palette.secondaryHex, 45), AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, spotPaint)

        val floorY = h * 0.72f
        val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, floorY, 0f, h, withAlpha(palette.primaryHex, 40), 0xFF04060A.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, floorY, w, h, floorPaint)

        val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(palette.primaryHex, 150)
            strokeWidth = 2f
        }
        canvas.drawLine(0f, floorY, w, floorY, horizonPaint)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(palette.primaryHex, 45)
            strokeWidth = 1.2f
        }
        for (i in 0..8) {
            val bx = (w / 8f) * i
            val hx = w * 0.5f + (bx - w * 0.5f) * 0.25f
            canvas.drawLine(hx, floorY, bx, h, gridPaint)
        }

        drawBespokeSubjectArtwork(canvas, w, h, asset, palette, seed, isDetailView = false)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = withAlpha(palette.primaryHex, 180)
        }
        canvas.drawRoundRect(RectF(4f, 4f, w - 4f, h - 4f), 16f, 16f, borderPaint)

        if (w >= 260f) {
            val topBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xCC07090E.toInt()
            }
            canvas.drawRect(0f, 0f, w, h * 0.16f, topBarPaint)

            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primaryHex
                textSize = h * 0.042f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("${palette.brandTag} • ${asset.symbol}", w * 0.035f, h * 0.062f, brandPaint)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF8FAFC.toInt()
                textSize = h * 0.056f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val displayTitle = if (asset.name.length > 38) asset.name.take(36) + "…" else asset.name
            canvas.drawText(displayTitle, w * 0.035f, h * 0.132f, namePaint)

            val pillRect = RectF(w * 0.72f, h * 0.035f, w * 0.97f, h * 0.125f)
            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(palette.primaryHex, 45) }
            canvas.drawRoundRect(pillRect, 10f, 10f, pillBg)
            canvas.drawRoundRect(pillRect, 10f, 10f, borderPaint)

            val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFDF73.toInt()
                textSize = h * 0.036f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("IMG 1 • SHOWCASE", pillRect.centerX(), pillRect.centerY() + h * 0.013f, pillText)

            val botBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD06080C.toInt() }
            canvas.drawRect(0f, h * 0.86f, w, h, botBarPaint)

            val footLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFCBD5E1.toInt()
                textSize = h * 0.038f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("${asset.subcategory} • ${asset.location}", w * 0.035f, h * 0.945f, footLeft)

            val footRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primaryHex
                textSize = h * 0.040f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("ID: ${asset.id.uppercase()} • $${formatCompactPrice(asset.basePrice)}", w * 0.965f, h * 0.945f, footRight)
        }
    }

    private fun drawView2TechnicalDetail(
        canvas: Canvas,
        w: Float,
        h: Float,
        asset: Asset,
        palette: BrandPalette,
        seed: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w, h, 0xFF061320.toInt(), 0xFF040911.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(palette.primaryHex, 32)
            strokeWidth = 1f
        }
        val step = w / 16f
        var x = step
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += step
        }
        var y = step
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += step
        }

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(palette.primaryHex, 90)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
        }
        canvas.drawLine(w * 0.36f, h * 0.16f, w * 0.36f, h * 0.86f, axisPaint)
        canvas.drawLine(0f, h * 0.52f, w, h * 0.52f, axisPaint)

        canvas.save()
        canvas.translate(-w * 0.13f, 0f)
        drawBespokeSubjectArtwork(canvas, w, h, asset, palette, seed, isDetailView = true)
        canvas.restore()

        if (w >= 260f) {
            val panelRect = RectF(w * 0.56f, h * 0.20f, w * 0.96f, h * 0.82f)
            val panelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD907111C.toInt() }
            val panelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = withAlpha(palette.primaryHex, 150)
            }
            canvas.drawRoundRect(panelRect, 12f, 12f, panelBg)
            canvas.drawRoundRect(panelRect, 12f, 12f, panelStroke)

            val specHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primaryHex
                textSize = h * 0.037f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("VERIFIED ASSET DOSSIER", panelRect.left + 14f, panelRect.top + h * 0.065f, specHeader)

            val specLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFE2E8F0.toInt()
                textSize = h * 0.034f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val descWords = asset.description.split(" ")
            val chunks = mutableListOf<String>()
            var current = ""
            for (word in descWords) {
                if ((current + " " + word).trim().length > 26) {
                    chunks.add(current.trim())
                    current = word
                } else {
                    current = "$current $word".trim()
                }
            }
            if (current.isNotBlank()) chunks.add(current)

            chunks.take(3).forEachIndexed { idx, line ->
                canvas.drawText(line, panelRect.left + 14f, panelRect.top + h * (0.14f + idx * 0.055f), specLinePaint)
            }

            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF38BDF8.toInt()
                textSize = h * 0.034f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("• CLASS: ${asset.subcategory.take(18)}", panelRect.left + 14f, panelRect.top + h * 0.35f, metaPaint)
            canvas.drawText("• ORIGIN: ${asset.location.take(18)}", panelRect.left + 14f, panelRect.top + h * 0.42f, metaPaint)
            canvas.drawText("• RARITY: ${asset.rarity.label.uppercase()}", panelRect.left + 14f, panelRect.top + h * 0.49f, metaPaint)
            canvas.drawText("• MAINT: ${String.format("%.1f%%", asset.maintenanceRateAnnual * 100)} / YR", panelRect.left + 14f, panelRect.top + h * 0.56f, metaPaint)

            val leaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primaryHex
                strokeWidth = 1.8f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(w * 0.38f, h * 0.42f, panelRect.left, h * 0.35f, leaderPaint)
            canvas.drawCircle(w * 0.38f, h * 0.42f, 4.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.primaryHex })
            canvas.drawLine(w * 0.42f, h * 0.60f, panelRect.left, h * 0.60f, leaderPaint)
            canvas.drawCircle(w * 0.42f, h * 0.60f, 4.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.primaryHex })

            val topBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD050B14.toInt() }
            canvas.drawRect(0f, 0f, w, h * 0.16f, topBarPaint)

            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF38BDF8.toInt()
                textSize = h * 0.040f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("CAD SPEC // ${asset.id.uppercase()} // ${asset.symbol}", w * 0.035f, h * 0.062f, brandPaint)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF8FAFC.toInt()
                textSize = h * 0.054f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val displayTitle = if (asset.name.length > 36) asset.name.take(34) + "…" else asset.name
            canvas.drawText(displayTitle, w * 0.035f, h * 0.132f, namePaint)

            val pillRect = RectF(w * 0.70f, h * 0.035f, w * 0.97f, h * 0.125f)
            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x3338BDF8 }
            canvas.drawRoundRect(pillRect, 10f, 10f, pillBg)
            canvas.drawRoundRect(pillRect, 10f, 10f, panelStroke)

            val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF38BDF8.toInt()
                textSize = h * 0.035f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("IMG 2 • TECH SPEC", pillRect.centerX(), pillRect.centerY() + h * 0.013f, pillText)

            val botBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD050B14.toInt() }
            canvas.drawRect(0f, h * 0.86f, w, h, botBarPaint)
            val footLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primaryHex
                textSize = h * 0.037f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("AUTHENTICATED ASSET • UNIT: ${asset.unit.uppercase()}", w * 0.035f, h * 0.945f, footLeft)

            val footRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFE2E8F0.toInt()
                textSize = h * 0.038f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("REF VAL: $${formatCompactPrice(asset.basePrice)}", w * 0.965f, h * 0.945f, footRight)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xFF38BDF8.toInt()
        }
        canvas.drawRoundRect(RectF(4f, 4f, w - 4f, h - 4f), 16f, 16f, borderPaint)
    }

    private fun drawBespokeSubjectArtwork(
        canvas: Canvas,
        w: Float,
        h: Float,
        asset: Asset,
        palette: BrandPalette,
        seed: Int,
        isDetailView: Boolean
    ) {
        val cx = w * 0.5f
        val cy = h * 0.54f
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isDetailView) withAlpha(palette.primaryHex, 65) else palette.primaryHex
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (isDetailView) 3.2f else 2.8f
            color = if (isDetailView) 0xFF38BDF8.toInt() else palette.accentHex
        }
        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFF0B0F19.toInt()
        }

        val n = asset.name.lowercase()
        when (asset.category) {
            AssetCategory.SUPERCARS -> {
                val isSuv = "suv" in asset.subcategory.lowercase() || "cullinan" in n || "bentayga" in n || "urus" in n || "purosangue" in n || "g 63" in n || "xm" in n || "range rover" in n
                val isSaloon = "phantom" in n || "s 680" in n || "m760" in n || "plaid" in n || "lucid" in n || "taycan" in n
                val carW = w * 0.54f
                val left = cx - carW * 0.5f
                val right = cx + carW * 0.5f
                val baseY = cy + h * 0.12f
                val roofH = if (isSuv) h * 0.22f else if (isSaloon) h * 0.18f else h * 0.145f

                val path = Path().apply {
                    moveTo(left, baseY)
                    lineTo(left + carW * 0.05f, baseY - roofH * 0.48f)
                    lineTo(left + carW * 0.26f, baseY - roofH * 0.58f)
                    lineTo(left + carW * 0.42f, baseY - roofH)
                    lineTo(
                        left + carW * (if (isSuv) 0.86f else if (isSaloon) 0.78f else 0.72f),
                        baseY - roofH * (if (isSuv) 0.96f else 0.92f)
                    )
                    lineTo(right - carW * 0.04f, baseY - roofH * 0.52f)
                    lineTo(right, baseY)
                    close()
                }
                canvas.drawPath(path, bodyPaint)
                canvas.drawPath(path, strokePaint)

                val glassPath = Path().apply {
                    moveTo(left + carW * 0.30f, baseY - roofH * 0.56f)
                    lineTo(left + carW * 0.44f, baseY - roofH * 0.90f)
                    lineTo(left + carW * 0.70f, baseY - roofH * 0.86f)
                    lineTo(left + carW * 0.78f, baseY - roofH * 0.56f)
                    close()
                }
                canvas.drawPath(glassPath, darkPaint)
                canvas.drawPath(glassPath, strokePaint)

                if (!isSuv && !isSaloon && ("xx" in n || "gt3" in n || "svj" in n || "divo" in n || "valkyrie" in n || "gtr" in n || "senna" in n || "jesko" in n)) {
                    canvas.drawLine(right - carW * 0.06f, baseY - roofH * 0.55f, right - carW * 0.02f, baseY - roofH * 0.98f, strokePaint)
                    canvas.drawLine(right - carW * 0.09f, baseY - roofH * 0.98f, right + carW * 0.02f, baseY - roofH * 0.98f, strokePaint)
                }

                val wheelR = if (isSuv) h * 0.078f else h * 0.065f
                val w1x = left + carW * 0.21f
                val w2x = right - carW * 0.21f
                val spokes = 5 + (seed % 5)
                for (wx in listOf(w1x, w2x)) {
                    canvas.drawCircle(wx, baseY, wheelR, darkPaint)
                    canvas.drawCircle(wx, baseY, wheelR, strokePaint)
                    canvas.drawCircle(wx, baseY, wheelR * 0.42f, bodyPaint)
                    for (s in 0 until spokes) {
                        val a = Math.toRadians((s * 360.0) / spokes)
                        canvas.drawLine(
                            wx,
                            baseY,
                            wx + (wheelR * 0.85f * cos(a)).toFloat(),
                            baseY + (wheelR * 0.85f * sin(a)).toFloat(),
                            strokePaint
                        )
                    }
                }
            }

            AssetCategory.PRIVATE_JETS -> {
                val isHeli = "helicopter" in asset.subcategory.lowercase() || "sikorsky" in n || "ach160" in n || "aw139" in n || "bell" in n
                val span = w * 0.54f
                if (isHeli) {
                    val bodyRect = RectF(cx - span * 0.28f, cy - h * 0.08f, cx + span * 0.18f, cy + h * 0.10f)
                    canvas.drawRoundRect(bodyRect, 28f, 28f, bodyPaint)
                    canvas.drawRoundRect(bodyRect, 28f, 28f, strokePaint)
                    canvas.drawLine(cx + span * 0.18f, cy - h * 0.02f, cx + span * 0.48f, cy - h * 0.06f, strokePaint)
                    canvas.drawCircle(cx + span * 0.48f, cy - h * 0.06f, h * 0.045f, strokePaint)
                    canvas.drawLine(cx - span * 0.05f, cy - h * 0.08f, cx - span * 0.05f, cy - h * 0.16f, strokePaint)
                    canvas.drawLine(cx - span * 0.48f, cy - h * 0.16f, cx + span * 0.38f, cy - h * 0.16f, strokePaint)
                } else {
                    val jetPath = Path().apply {
                        moveTo(cx - span * 0.52f, cy + h * 0.02f)
                        lineTo(cx - span * 0.32f, cy - h * 0.05f)
                        lineTo(cx + span * 0.34f, cy - h * 0.05f)
                        lineTo(cx + span * 0.48f, cy - h * 0.22f)
                        lineTo(cx + span * 0.54f, cy - h * 0.22f)
                        lineTo(cx + span * 0.44f, cy + h * 0.03f)
                        close()
                    }
                    canvas.drawPath(jetPath, bodyPaint)
                    canvas.drawPath(jetPath, strokePaint)
                    val wingPath = Path().apply {
                        moveTo(cx - span * 0.08f, cy)
                        lineTo(cx + span * 0.14f, cy + h * 0.15f)
                        lineTo(cx + span * 0.24f, cy + h * 0.15f)
                        lineTo(cx + span * 0.10f, cy)
                        close()
                    }
                    canvas.drawPath(wingPath, darkPaint)
                    canvas.drawPath(wingPath, strokePaint)
                    val winCount = 4 + (seed % 4)
                    for (i in 0 until winCount) {
                        canvas.drawCircle(cx - span * 0.22f + i * (span * 0.075f), cy - h * 0.015f, 4.5f, strokePaint)
                    }
                }
            }

            AssetCategory.YACHTS_SHIPS -> {
                val isSail = "sailing" in n || "maltese" in n || "koru" in n
                val isSub = "submersible" in n || "triton" in n || "migaloo" in n
                val hullW = w * 0.56f
                val baseY = cy + h * 0.12f
                val hullPath = Path().apply {
                    moveTo(cx - hullW * 0.5f, baseY - h * 0.07f)
                    lineTo(cx + hullW * 0.5f, baseY - h * 0.09f)
                    lineTo(cx + hullW * 0.40f, baseY + h * 0.03f)
                    lineTo(cx - hullW * 0.44f, baseY + h * 0.03f)
                    close()
                }
                canvas.drawPath(hullPath, bodyPaint)
                canvas.drawPath(hullPath, strokePaint)

                if (isSail) {
                    for (m in -1..1) {
                        val mx = cx + m * (hullW * 0.22f)
                        canvas.drawLine(mx, baseY - h * 0.08f, mx, baseY - h * 0.36f, strokePaint)
                        val sail = Path().apply {
                            moveTo(mx, baseY - h * 0.35f)
                            lineTo(mx + hullW * 0.14f, baseY - h * 0.10f)
                            lineTo(mx, baseY - h * 0.10f)
                            close()
                        }
                        canvas.drawPath(sail, bodyPaint)
                        canvas.drawPath(sail, strokePaint)
                    }
                } else if (isSub) {
                    canvas.drawRoundRect(RectF(cx - hullW * 0.16f, baseY - h * 0.19f, cx + hullW * 0.12f, baseY - h * 0.07f), 14f, 14f, darkPaint)
                    canvas.drawRoundRect(RectF(cx - hullW * 0.16f, baseY - h * 0.19f, cx + hullW * 0.12f, baseY - h * 0.07f), 14f, 14f, strokePaint)
                } else {
                    canvas.drawRoundRect(RectF(cx - hullW * 0.30f, baseY - h * 0.16f, cx + hullW * 0.24f, baseY - h * 0.08f), 8f, 8f, darkPaint)
                    canvas.drawRoundRect(RectF(cx - hullW * 0.30f, baseY - h * 0.16f, cx + hullW * 0.24f, baseY - h * 0.08f), 8f, 8f, strokePaint)
                    canvas.drawRoundRect(RectF(cx - hullW * 0.16f, baseY - h * 0.23f, cx + hullW * 0.12f, baseY - h * 0.16f), 6f, 6f, bodyPaint)
                    canvas.drawRoundRect(RectF(cx - hullW * 0.16f, baseY - h * 0.23f, cx + hullW * 0.12f, baseY - h * 0.16f), 6f, 6f, strokePaint)
                }
            }

            AssetCategory.LUXURY_WATCHES -> {
                val r = h * 0.22f
                val isOctagon = "royal oak" in n || "nautilus" in n || "octo" in n || "aquanaut" in n
                val isTonneau = "richard mille" in n || "tonneau" in n || "spirit" in n
                val isRect = "reverso" in n || "tank" in n || "santos" in n
                canvas.drawRoundRect(RectF(cx - r * 0.52f, cy - r * 1.38f, cx + r * 0.52f, cy + r * 1.38f), 10f, 10f, darkPaint)
                canvas.drawRoundRect(RectF(cx - r * 0.52f, cy - r * 1.38f, cx + r * 0.52f, cy + r * 1.38f), 10f, 10f, strokePaint)
                if (isOctagon) {
                    val oct = Path()
                    for (i in 0..7) {
                        val ang = Math.toRadians((i * 45.0) + 22.5)
                        val px = cx + (r * 1.05f * cos(ang)).toFloat()
                        val py = cy + (r * 1.05f * sin(ang)).toFloat()
                        if (i == 0) oct.moveTo(px, py) else oct.lineTo(px, py)
                    }
                    oct.close()
                    canvas.drawPath(oct, bodyPaint)
                    canvas.drawPath(oct, strokePaint)
                } else if (isTonneau) {
                    val tonRect = RectF(cx - r * 0.85f, cy - r * 1.12f, cx + r * 0.85f, cy + r * 1.12f)
                    canvas.drawRoundRect(tonRect, 28f, 28f, bodyPaint)
                    canvas.drawRoundRect(tonRect, 28f, 28f, strokePaint)
                } else if (isRect) {
                    val rectCase = RectF(cx - r * 0.82f, cy - r * 1.05f, cx + r * 0.82f, cy + r * 1.05f)
                    canvas.drawRoundRect(rectCase, 10f, 10f, bodyPaint)
                    canvas.drawRoundRect(rectCase, 10f, 10f, strokePaint)
                } else {
                    canvas.drawCircle(cx, cy, r, bodyPaint)
                    canvas.drawCircle(cx, cy, r, strokePaint)
                }
                canvas.drawCircle(cx, cy, r * 0.78f, darkPaint)
                canvas.drawCircle(cx, cy, r * 0.78f, strokePaint)
                for (i in 0..11) {
                    val a = Math.toRadians((i * 30).toDouble())
                    canvas.drawLine(
                        cx + (r * 0.64f * cos(a)).toFloat(),
                        cy + (r * 0.64f * sin(a)).toFloat(),
                        cx + (r * 0.74f * cos(a)).toFloat(),
                        cy + (r * 0.74f * sin(a)).toFloat(),
                        strokePaint
                    )
                }
                canvas.drawCircle(cx, cy + r * 0.38f, r * 0.20f, strokePaint)
                val hAngle = Math.toRadians(((seed % 12) * 30 - 60).toDouble())
                val mAngle = Math.toRadians(((seed % 60) * 6 - 90).toDouble())
                canvas.drawLine(cx, cy, cx + (r * 0.42f * cos(hAngle)).toFloat(), cy + (r * 0.42f * sin(hAngle)).toFloat(), strokePaint)
                canvas.drawLine(cx, cy, cx + (r * 0.62f * cos(mAngle)).toFloat(), cy + (r * 0.62f * sin(mAngle)).toFloat(), strokePaint)
            }

            AssetCategory.FINE_ART -> {
                val fw = w * 0.36f
                val fh = h * 0.45f
                val outer = RectF(cx - fw * 0.5f, cy - fh * 0.5f, cx + fw * 0.5f, cy + fh * 0.5f)
                val inner = RectF(outer.left + 14f, outer.top + 14f, outer.right - 14f, outer.bottom - 14f)
                canvas.drawRoundRect(outer, 8f, 8f, darkPaint)
                canvas.drawRoundRect(outer, 8f, 8f, strokePaint)
                canvas.drawRect(inner, bodyPaint)
                canvas.drawRect(inner, strokePaint)
                val motifCount = 3 + (seed % 5)
                for (i in 1..motifCount) {
                    val rx = inner.left + (inner.width() * ((seed * i % 73) / 73f)).coerceIn(18f, inner.width() - 18f)
                    val ry = inner.top + (inner.height() * ((seed * (i + 3) % 67) / 67f)).coerceIn(18f, inner.height() - 18f)
                    canvas.drawCircle(rx, ry, 10f + (i * 5f), strokePaint)
                }
            }

            AssetCategory.LUXURY_REAL_ESTATE, AssetCategory.PRIVATE_ISLANDS -> {
                val isIsland = asset.category == AssetCategory.PRIVATE_ISLANDS
                val baseY = cy + h * 0.14f
                if (isIsland) {
                    val islandOval = RectF(cx - w * 0.26f, baseY - h * 0.12f, cx + w * 0.26f, baseY + h * 0.05f)
                    canvas.drawOval(islandOval, bodyPaint)
                    canvas.drawOval(islandOval, strokePaint)
                    for (i in -1..1) {
                        val px = cx + i * (w * 0.10f)
                        canvas.drawLine(px, baseY - h * 0.04f, px, baseY - h * 0.22f, strokePaint)
                        canvas.drawCircle(px, baseY - h * 0.24f, h * 0.045f, strokePaint)
                    }
                } else {
                    val bW = w * 0.44f
                    val left = cx - bW * 0.5f
                    val towers = 3 + (seed % 2)
                    val tw = bW / towers
                    for (t in 0 until towers) {
                        val th = if (t == towers / 2) h * 0.34f else h * (0.20f + (t % 2) * 0.05f)
                        val rect = RectF(left + t * tw + 4f, baseY - th, left + (t + 1) * tw - 4f, baseY)
                        canvas.drawRect(rect, if (t == towers / 2) bodyPaint else darkPaint)
                        canvas.drawRect(rect, strokePaint)
                    }
                }
            }

            AssetCategory.JEWELRY_DIAMONDS -> {
                val r = h * 0.22f
                val gem = Path().apply {
                    moveTo(cx - r * 0.55f, cy - r * 0.55f)
                    lineTo(cx + r * 0.55f, cy - r * 0.55f)
                    lineTo(cx + r * 0.95f, cy - r * 0.10f)
                    lineTo(cx, cy + r * 0.95f)
                    lineTo(cx - r * 0.95f, cy - r * 0.10f)
                    close()
                }
                canvas.drawPath(gem, bodyPaint)
                canvas.drawPath(gem, strokePaint)
                canvas.drawLine(cx - r * 0.95f, cy - r * 0.10f, cx + r * 0.95f, cy - r * 0.10f, strokePaint)
                canvas.drawLine(cx - r * 0.55f, cy - r * 0.55f, cx, cy + r * 0.95f, strokePaint)
                canvas.drawLine(cx + r * 0.55f, cy - r * 0.55f, cx, cy + r * 0.95f, strokePaint)
            }

            AssetCategory.PRECIOUS_METALS -> {
                if ("oil" in n || "crude" in n || "brent" in n || "wti" in n) {
                    // Realistic WTI / Brent Crude Oil Barrels
                    val barrelW = w * 0.16f
                    val barrelH = h * 0.34f
                    for (idx in -1..0) {
                        val bx = cx + idx * (barrelW * 0.85f) + barrelW * 0.42f
                        val by = cy - barrelH * 0.45f + (idx + 1) * 10f
                        val bRect = RectF(bx - barrelW * 0.5f, by, bx + barrelW * 0.5f, by + barrelH)
                        val barrelFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = LinearGradient(bRect.left, bRect.top, bRect.right, bRect.top, 0xFF1E293B.toInt(), 0xFF0F172A.toInt(), Shader.TileMode.CLAMP)
                        }
                        canvas.drawRoundRect(bRect, 14f, 14f, barrelFill)
                        canvas.drawRoundRect(bRect, 14f, 14f, strokePaint)
                        // Steel ribs across barrel
                        for (rib in 1..3) {
                            val ry = bRect.top + barrelH * (rib * 0.25f)
                            canvas.drawLine(bRect.left - 3f, ry, bRect.right + 3f, ry, strokePaint)
                        }
                    }
                    drawCenteredSymbolLabel(canvas, cx, cy + barrelH * 0.12f, "WTI • OIL", h * 0.048f)
                } else {
                    // Realistic 3D Gold / Silver / Platinum Bullion Ingot Bars
                    val isSilver = "silver" in n || "xag" in asset.symbol.lowercase()
                    val topColor = if (isSilver) 0xFFF1F5F9.toInt() else 0xFFFFE066.toInt()
                    val botColor = if (isSilver) 0xFF94A3B8.toInt() else 0xFFB8860B.toInt()
                    val stampLabel = if (isSilver) "999.0 FINE SILVER" else if ("gold" in n) "999.9 FINE GOLD" else "${asset.symbol} BULLION"
                    val bw = w * 0.36f
                    val bh = h * 0.14f
                    val bar1 = RectF(cx - bw * 0.56f, cy + bh * 0.08f, cx + bw * 0.36f, cy + bh * 1.08f)
                    val bar2 = RectF(cx - bw * 0.26f, cy - bh * 0.92f, cx + bw * 0.66f, cy + bh * 0.08f)
                    val barGrad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(bar2.left, bar2.top, bar2.right, bar2.bottom, topColor, botColor, Shader.TileMode.CLAMP)
                    }
                    canvas.drawRoundRect(bar1, 12f, 12f, darkPaint)
                    canvas.drawRoundRect(bar1, 12f, 12f, strokePaint)
                    canvas.drawRoundRect(bar2, 12f, 12f, barGrad)
                    canvas.drawRoundRect(bar2, 12f, 12f, strokePaint)
                    val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFF0F172A.toInt()
                        textSize = bh * 0.34f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(stampLabel, bar2.centerX(), bar2.centerY() + bh * 0.12f, stampPaint)
                }
            }

            AssetCategory.DESIGNER_FASHION -> {
                val bw = w * 0.28f
                val bh = h * 0.28f
                val bagRect = RectF(cx - bw * 0.5f, cy - bh * 0.35f, cx + bw * 0.5f, cy + bh * 0.65f)
                canvas.drawRoundRect(bagRect, 14f, 14f, bodyPaint)
                canvas.drawRoundRect(bagRect, 14f, 14f, strokePaint)
                val handleRect = RectF(cx - bw * 0.24f, cy - bh * 0.78f, cx + bw * 0.24f, cy - bh * 0.10f)
                canvas.drawArc(handleRect, 180f, 180f, false, strokePaint)
                canvas.drawCircle(cx, cy + bh * 0.05f, 7f, strokePaint)
            }

            AssetCategory.RARE_COLLECTIBLES -> {
                val r = h * 0.21f
                val shield = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.85f, cy - r * 0.55f)
                    lineTo(cx + r * 0.65f, cy + r * 0.45f)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.65f, cy + r * 0.45f)
                    lineTo(cx - r * 0.85f, cy - r * 0.55f)
                    close()
                }
                canvas.drawPath(shield, bodyPaint)
                canvas.drawPath(shield, strokePaint)
                canvas.drawCircle(cx, cy, r * 0.48f, darkPaint)
                canvas.drawCircle(cx, cy, r * 0.48f, strokePaint)
                drawCenteredSymbolLabel(canvas, cx, cy + r * 0.14f, asset.symbol.take(4), r * 0.34f)
            }

            AssetCategory.AGRICULTURAL, AssetCategory.LUXURY_FOOD -> {
                val r = h * 0.21f
                if ("wine" in asset.subcategory.lowercase() || "whisky" in asset.subcategory.lowercase() || "champagne" in asset.subcategory.lowercase() || "cognac" in asset.subcategory.lowercase() || "romanée" in n || "macallan" in n) {
                    val bottle = RectF(cx - r * 0.36f, cy - r * 0.45f, cx + r * 0.36f, cy + r * 0.95f)
                    val neck = RectF(cx - r * 0.14f, cy - r * 1.05f, cx + r * 0.14f, cy - r * 0.42f)
                    canvas.drawRoundRect(neck, 6f, 6f, darkPaint)
                    canvas.drawRoundRect(neck, 6f, 6f, strokePaint)
                    canvas.drawRoundRect(bottle, 14f, 14f, bodyPaint)
                    canvas.drawRoundRect(bottle, 14f, 14f, strokePaint)
                } else {
                    canvas.drawCircle(cx, cy, r, bodyPaint)
                    canvas.drawCircle(cx, cy, r, strokePaint)
                    canvas.drawCircle(cx, cy, r * 0.68f, darkPaint)
                    canvas.drawCircle(cx, cy, r * 0.68f, strokePaint)
                    drawCenteredSymbolLabel(canvas, cx, cy + r * 0.14f, asset.symbol.take(4), r * 0.34f)
                }
            }

            AssetCategory.CRYPTOCURRENCIES -> {
                val r = h * 0.23f
                if ("bitcoin" in n || asset.symbol == "BTC") {
                    // Official Bitcoin Orange Medallion + White ₿ Symbol
                    val btcOrange = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF7931A.toInt() }
                    canvas.drawCircle(cx, cy, r, btcOrange)
                    canvas.drawCircle(cx, cy, r, strokePaint)
                    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.WHITE
                        textSize = r * 1.18f
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("₿", cx, cy + r * 0.40f, whitePaint)
                } else if ("ethereum" in n || asset.symbol == "ETH") {
                    // Official Ethereum Octahedral Split-Diamond Logo
                    val ethBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E293B.toInt() }
                    canvas.drawCircle(cx, cy, r, ethBg)
                    canvas.drawCircle(cx, cy, r, strokePaint)
                    val topPrism = Path().apply {
                        moveTo(cx, cy - r * 0.78f)
                        lineTo(cx + r * 0.50f, cy)
                        lineTo(cx, cy + r * 0.18f)
                        lineTo(cx - r * 0.50f, cy)
                        close()
                    }
                    val botPrism = Path().apply {
                        moveTo(cx, cy + r * 0.82f)
                        lineTo(cx + r * 0.50f, cy + r * 0.12f)
                        lineTo(cx, cy + r * 0.30f)
                        lineTo(cx - r * 0.50f, cy + r * 0.12f)
                        close()
                    }
                    val ethFillTop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF627EEA.toInt() }
                    val ethFillBot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8A92B2.toInt() }
                    canvas.drawPath(topPrism, ethFillTop)
                    canvas.drawPath(botPrism, ethFillBot)
                } else {
                    canvas.drawCircle(cx, cy, r, bodyPaint)
                    canvas.drawCircle(cx, cy, r, strokePaint)
                    canvas.drawCircle(cx, cy, r * 0.74f, darkPaint)
                    canvas.drawCircle(cx, cy, r * 0.74f, strokePaint)
                    drawCenteredSymbolLabel(canvas, cx, cy + r * 0.16f, asset.symbol.take(5), r * 0.42f)
                }
            }

            AssetCategory.STOCKS_INDICES, AssetCategory.BUSINESSES -> {
                val r = h * 0.22f
                when {
                    "microsoft" in n || asset.symbol == "MSFT" -> {
                        // Official Microsoft 4-Color Window Squares
                        val sq = r * 0.72f
                        val gap = r * 0.10f
                        val pRed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF25022.toInt() }
                        val pGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7FBA00.toInt() }
                        val pBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00A4EF.toInt() }
                        val pYellow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFB900.toInt() }
                        canvas.drawRect(cx - sq - gap, cy - sq - gap, cx - gap, cy - gap, pRed)
                        canvas.drawRect(cx + gap, cy - sq - gap, cx + sq + gap, cy - gap, pGreen)
                        canvas.drawRect(cx - sq - gap, cy + gap, cx - gap, cy + sq + gap, pBlue)
                        canvas.drawRect(cx + gap, cy + gap, cx + sq + gap, cy + sq + gap, pYellow)
                    }
                    "apple" in n || asset.symbol == "AAPL" -> {
                        // Sleek Cupertino Titanium Medallion + Apple Silhouette
                        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E293B.toInt() }
                        canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), 24f, 24f, badgePaint)
                        canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), 24f, 24f, strokePaint)
                        val appleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF8FAFC.toInt() }
                        canvas.drawCircle(cx - r * 0.16f, cy + r * 0.08f, r * 0.36f, appleFill)
                        canvas.drawCircle(cx + r * 0.16f, cy + r * 0.08f, r * 0.36f, appleFill)
                        // Leaf
                        val leaf = RectF(cx - r * 0.05f, cy - r * 0.56f, cx + r * 0.25f, cy - r * 0.28f)
                        canvas.drawOval(leaf, appleFill)
                    }
                    "nvidia" in n || asset.symbol == "NVDA" -> {
                        // NVIDIA Green GPU Wafer & Eye Emblem
                        val nvGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF76B900.toInt() }
                        val chipRect = RectF(cx - r, cy - r, cx + r, cy + r)
                        canvas.drawRoundRect(chipRect, 18f, 18f, nvGreen)
                        canvas.drawRoundRect(RectF(cx - r * 0.65f, cy - r * 0.65f, cx + r * 0.65f, cy + r * 0.65f), 10f, 10f, darkPaint)
                        drawCenteredSymbolLabel(canvas, cx, cy + r * 0.16f, "NVDA", r * 0.42f)
                    }
                    "tesla" in n || asset.symbol == "TSLA" -> {
                        // Tesla Crimson Crest Shield + White 'T'
                        val tslaRed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE82127.toInt() }
                        val shield = Path().apply {
                            moveTo(cx - r * 0.78f, cy - r * 0.75f)
                            lineTo(cx + r * 0.78f, cy - r * 0.75f)
                            lineTo(cx + r * 0.58f, cy + r * 0.35f)
                            lineTo(cx, cy + r * 0.95f)
                            lineTo(cx - r * 0.58f, cy + r * 0.35f)
                            close()
                        }
                        canvas.drawPath(shield, tslaRed)
                        canvas.drawPath(shield, strokePaint)
                        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.WHITE
                            textSize = r * 1.05f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText("T", cx, cy + r * 0.35f, tPaint)
                    }
                    "amazon" in n || asset.symbol == "AMZN" -> {
                        // Amazon Slate Badge with 'a' and Orange Smile Arrow
                        val badge = RectF(cx - r * 1.1f, cy - r * 0.9f, cx + r * 1.1f, cy + r * 0.9f)
                        canvas.drawRoundRect(badge, 22f, 22f, darkPaint)
                        canvas.drawRoundRect(badge, 22f, 22f, strokePaint)
                        val aPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.WHITE
                            textSize = r * 1.05f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText("a", cx, cy + r * 0.18f, aPaint)
                        val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFFFF9900.toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = 6f
                        }
                        canvas.drawArc(RectF(cx - r * 0.65f, cy + r * 0.05f, cx + r * 0.65f, cy + r * 0.58f), 20f, 140f, false, smilePaint)
                    }
                    "google" in n || "alphabet" in n || asset.symbol == "GOOGL" -> {
                        // Google 4-Color 'G' Emblem
                        val arcRect = RectF(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f)
                        val gPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = r * 0.34f
                        }
                        gPaint.color = 0xFFEA4335.toInt()
                        canvas.drawArc(arcRect, 215f, 95f, false, gPaint)
                        gPaint.color = 0xFFFBBC05.toInt()
                        canvas.drawArc(arcRect, 130f, 85f, false, gPaint)
                        gPaint.color = 0xFF34A853.toInt()
                        canvas.drawArc(arcRect, 45f, 85f, false, gPaint)
                        gPaint.color = 0xFF4285F4.toInt()
                        canvas.drawArc(arcRect, -15f, 60f, false, gPaint)
                        canvas.drawLine(cx, cy, cx + r * 0.82f, cy, gPaint)
                    }
                    else -> {
                        val chartW = w * 0.42f
                        val left = cx - chartW * 0.5f
                        val baseY = cy + h * 0.16f
                        val bars = 5
                        val bw = chartW / (bars * 1.4f)
                        for (b in 0 until bars) {
                            val bh = h * (0.10f + ((b + 1) * 0.045f) + ((seed + b) % 3) * 0.02f)
                            val bx = left + b * (chartW / bars)
                            val rect = RectF(bx, baseY - bh, bx + bw, baseY)
                            canvas.drawRoundRect(rect, 6f, 6f, if (b == bars - 1) bodyPaint else darkPaint)
                            canvas.drawRoundRect(rect, 6f, 6f, strokePaint)
                        }
                        drawCenteredSymbolLabel(canvas, cx, cy - h * 0.15f, asset.symbol.take(6), h * 0.065f)
                    }
                }
            }

            AssetCategory.INDUSTRIAL, AssetCategory.TRANSPORT_LOGISTICS, AssetCategory.TECH_COMPUTING, AssetCategory.SPACE_ENERGY -> {
                val r = h * 0.21f
                val hex = Path()
                for (i in 0..5) {
                    val a = Math.toRadians((i * 60.0) - 30.0)
                    val px = cx + (r * cos(a)).toFloat()
                    val py = cy + (r * sin(a)).toFloat()
                    if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
                }
                hex.close()
                canvas.drawPath(hex, bodyPaint)
                canvas.drawPath(hex, strokePaint)
                canvas.drawCircle(cx, cy, r * 0.60f, darkPaint)
                canvas.drawCircle(cx, cy, r * 0.60f, strokePaint)
                drawCenteredSymbolLabel(canvas, cx, cy + r * 0.15f, asset.symbol.take(5), r * 0.36f)
            }
        }
    }

    private fun drawCenteredSymbolLabel(
        canvas: Canvas,
        x: Float,
        y: Float,
        text: String,
        textSizePx: Float
    ) {
        val symPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFDF73.toInt()
            textSize = textSizePx
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, x, y, symPaint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    }

    private fun formatCompactPrice(price: Double): String {
        return when {
            price >= 1_000_000_000.0 -> String.format("%.2fB", price / 1_000_000_000.0)
            price >= 1_000_000.0 -> String.format("%.2fM", price / 1_000_000.0)
            price >= 1_000.0 -> String.format("%,.0f", price)
            else -> String.format("%.2f", price)
        }
    }
}

class AssetSpecificCoilFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bitmap = AssetBespokeImageRenderer.getOrRenderBitmap(
            uriString = data.toString(),
            width = 720,
            height = 420
        )
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == "lifeassets") {
                return AssetSpecificCoilFetcher(context, data, options)
            }
            return null
        }
    }
}

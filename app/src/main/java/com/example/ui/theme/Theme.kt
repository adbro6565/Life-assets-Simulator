package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeAssetsDarkColorScheme = darkColorScheme(
    primary = MetallicGold,
    onPrimary = Color(0xFF0A0C10),
    primaryContainer = Color(0xFF2B230B),
    onPrimaryContainer = BrightGold,
    secondary = CyberCyan,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = Color(0xFF00363F),
    onSecondaryContainer = CyberCyan,
    tertiary = BullGreen,
    onTertiary = Color(0xFF00210E),
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedPanel,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = BearRed,
    onError = Color.White
)

@Composable
fun LifeAssetsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LifeAssetsDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    LifeAssetsTheme(content = content)
}

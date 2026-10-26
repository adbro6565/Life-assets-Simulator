package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val showInBottomBar: Boolean = true
) {
    DASHBOARD("Home", Icons.Default.Home, "nav_dashboard", true),
    MARKETS("Markets", Icons.Default.ShowChart, "nav_markets", true),
    PORTFOLIO("Portfolio", Icons.Default.AccountBalanceWallet, "nav_portfolio", true),
    REWARDS("Rewards", Icons.Default.CardGiftcard, "nav_rewards", true),
    PROFILE("Profile", Icons.Default.Person, "nav_profile", true),
    MARKETPLACE("400 Assets", Icons.Default.Storefront, "nav_marketplace", false),
    MULTIPLAYER("Arena", Icons.Default.Groups, "nav_multiplayer", false),
    LIFEBOT("LifeBot", Icons.Default.SmartToy, "nav_lifebot", false)
}

@Composable
fun BottomNavigationBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0A111D),
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        AppDestination.entries.filter { it.showInBottomBar }.forEach { dest ->
            val selected = dest == currentDestination
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag(dest.testTag),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldAccent,
                    selectedTextColor = EmeraldAccent,
                    indicatorColor = EmeraldAccent.copy(alpha = 0.16f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

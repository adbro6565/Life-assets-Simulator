package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalogue.AssetCatalogue
import com.example.data.model.Asset
import com.example.data.model.AssetCategory
import com.example.data.model.MarketTick
import com.example.ui.components.AssetCard
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssetSortOption

@Composable
fun MarketplaceScreen(
    assets: List<Asset>,
    ticks: Map<String, MarketTick>,
    selectedCategory: AssetCategory?,
    searchQuery: String,
    sortOption: AssetSortOption,
    getOwnedQuantity: (String) -> Double,
    onSelectCategory: (AssetCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSort: (AssetSortOption) -> Unit,
    onAssetCardClick: (Asset) -> Unit,
    onBuyClick: (Asset) -> Unit,
    onSellClick: (Asset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GLOBAL LUXURY, COMMODITY & SOVEREIGN CATALOGUE",
                    color = MetallicGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Showing ${assets.size} of ${AssetCatalogue.totalCount} Assets",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("marketplace_count_header")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search assets (e.g., Rolex, Ferrari, Bitcoin, Island, Jet)...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MetallicGold) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("marketplace_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MetallicGold,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = CarbonSurface,
                unfocusedContainerColor = CarbonSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips (All 20 Categories)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                CategoryChip(
                    label = "All (${AssetCatalogue.totalCount})",
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) }
                )
            }
            items(AssetCategory.entries, key = { it.name }) { category ->
                val count = AssetCatalogue.getAssetsByCategory(category).size
                CategoryChip(
                    label = "${category.displayName} ($count)",
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sort Selector Strip
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AssetSortOption.entries, key = { it.name }) { opt ->
                val selected = sortOption == opt
                Surface(
                    color = if (selected) CyberCyan.copy(alpha = 0.2f) else CarbonSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) CyberCyan else GlassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSort(opt) }
                ) {
                    Text(
                        text = opt.label,
                        color = if (selected) CyberCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Asset List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(assets, key = { it.id }) { asset ->
                AssetCard(
                    asset = asset,
                    tick = ticks[asset.id],
                    ownedQuantity = getOwnedQuantity(asset.id),
                    onCardClick = { onAssetCardClick(asset) },
                    onBuyClick = { onBuyClick(asset) },
                    onSellClick = { onSellClick(asset) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MetallicGold else CarbonSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .border(
                1.dp,
                if (selected) MetallicGold else GlassBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) ObsidianBlack else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

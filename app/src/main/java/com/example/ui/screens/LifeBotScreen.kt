package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketRegime
import com.example.engine.LifeBotMessage
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LifeBotScreen(
    messages: List<LifeBotMessage>,
    isThinking: Boolean,
    regime: MarketRegime,
    onSendPrompt: (String) -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "Analyze my portfolio allocation & cash ratio",
        "Show top 24h gainers & market regime risks",
        "Explain 0.5% fee & 1–100 quantity order math",
        "Compare fractional Crypto vs Luxury Real Estate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Surface(
            color = CarbonSurface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ElevatedPanel, CircleShape)
                        .border(1.5.dp, CyberCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "LifeBot AI",
                        tint = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "LIFEBOT • ROBOTIC AI WEALTH STRATEGIST",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Active Macro Cycle: ${regime.label}",
                        color = MetallicGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Powered by Gemini AI + Real-Time Portfolio Telemetry",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Prompt Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { prompt ->
                Surface(
                    color = ElevatedPanel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { onSendPrompt(prompt) }
                ) {
                    Text(
                        text = prompt,
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isFromBot) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (msg.isFromBot) CarbonSurface else MetallicGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .border(
                                1.dp,
                                if (msg.isFromBot) CyberCyan.copy(alpha = 0.35f) else MetallicGold.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (msg.isFromBot) "LIFEBOT AI" else "YOU",
                                color = if (msg.isFromBot) CyberCyan else MetallicGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Text(
                        text = "LifeBot is analyzing 400+ market ticks and portfolio risk...",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row
        OutlinedTextField(
            value = inputPrompt,
            onValueChange = { inputPrompt = it },
            placeholder = { Text("Ask LifeBot about any asset, fee math, or portfolio strategy...", fontSize = 12.sp) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            onSendPrompt(inputPrompt)
                            inputPrompt = ""
                        }
                    },
                    modifier = Modifier.testTag("lifebot_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to LifeBot",
                        tint = CyberCyan
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lifebot_input_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = CarbonSurface,
                unfocusedContainerColor = CarbonSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }
}

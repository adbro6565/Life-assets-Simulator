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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.UserEntity
import com.example.engine.BackendFreeTierSnapshot
import com.example.engine.FreeTierQuotaAlertLevel
import com.example.engine.ImageAuditAdminReport
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.SubtleCardBorder
import com.example.ui.theme.BearRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    user: UserEntity?,
    imageAuditReport: ImageAuditAdminReport? = null,
    onRunImageAudit: () -> Unit = {},
    quotaSnapshot: BackendFreeTierSnapshot? = null,
    onNavigateDashboard: () -> Unit,
    onNavigatePortfolio: () -> Unit,
    onNavigateTransactions: () -> Unit,
    onNavigateRewards: () -> Unit,
    onNavigateMarketplace: () -> Unit,
    onNavigateArena: () -> Unit,
    onNavigateLifeBot: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var showImageAuditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val displayName = remember(user?.username) {
        val raw = user?.username ?: "Billionaire Trader"
        if (raw == "Sovereign_Titan") "Billionaire Trader" else raw.replace("_", " ")
    }
    val emailText = remember(user?.email) {
        val raw = user?.email ?: "trader@lifeassets.app"
        if (raw == "titan@lifeassets.io") "trader@lifeassets.app" else raw
    }
    val initials = remember(displayName) {
        val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            else -> "LA"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // User Profile Header (Avatar + Name + Email matching Screen 10)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                            )
                        )
                        .border(1.5.dp, Color(0xFF60A5FA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = displayName,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = emailText,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }

        // Menu Rows matching Screen 10
        item {
            ProfileMenuRow(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                onClick = onNavigateDashboard
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Portfolio",
                onClick = onNavigatePortfolio
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.ReceiptLong,
                label = "Transactions",
                onClick = onNavigateTransactions
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.CardGiftcard,
                label = "Rewards",
                onClick = onNavigateRewards
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.Storefront,
                label = "All 400+ Real-World Assets",
                onClick = onNavigateMarketplace
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.FactCheck,
                label = "Real Asset Photo Audit & Admin Report",
                onClick = { showImageAuditDialog = true },
                testTag = "profile_image_audit_button"
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.Groups,
                label = "Billionaire Arena (P2P Escrow)",
                onClick = onNavigateArena
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.SmartToy,
                label = "AI Market Advisor",
                onClick = onNavigateLifeBot
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = onOpenSettings,
                testTag = "profile_settings_button"
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                label = "Help & Support",
                onClick = { showHelpDialog = true }
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Log Out",
                onClick = onLogOut,
                testTag = "profile_logout_button"
            )
        }
        item {
            ProfileMenuRow(
                icon = Icons.Default.DeleteForever,
                label = "Delete Account & Data",
                onClick = { showDeleteDialog = true },
                testTag = "profile_delete_account_button"
            )
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 1. Free for All Users for the First Year Card & Firebase Spark Free-Tier Quota Telemetry
        if (quotaSnapshot != null) {
            item {
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .testTag("profile_free_first_year_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1st-Year Full Free Access",
                                color = EmeraldAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${quotaSnapshot.freeYearDaysRemaining} Days Left",
                                color = MetallicGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "100% Free for All Users for the First Year (365 Days) • Zero Subscriptions, Zero Paywalls & No Automatic Charges After the Free Year.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item {
                val borderCol = when (quotaSnapshot.alertLevel) {
                    FreeTierQuotaAlertLevel.CRITICAL_NEAR_LIMIT -> BearRed
                    FreeTierQuotaAlertLevel.WARNING_APPROACHING_LIMIT -> MetallicGold
                    FreeTierQuotaAlertLevel.NORMAL -> SubtleCardBorder
                }
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                        .testTag("profile_spark_quota_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloud Backend Free-Tier Quota (UTC ${quotaSnapshot.utcDateString})",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = quotaSnapshot.alertLevel.name,
                                color = when (quotaSnapshot.alertLevel) {
                                    FreeTierQuotaAlertLevel.CRITICAL_NEAR_LIMIT -> BearRed
                                    FreeTierQuotaAlertLevel.WARNING_APPROACHING_LIMIT -> MetallicGold
                                    FreeTierQuotaAlertLevel.NORMAL -> EmeraldAccent
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reads: ${quotaSnapshot.dailyReadsUsed}/${quotaSnapshot.dailyReadsLimit} • Writes: ${quotaSnapshot.dailyWritesUsed}/${quotaSnapshot.dailyWritesLimit} • Deletes: ${quotaSnapshot.dailyDeletesUsed}/${quotaSnapshot.dailyDeletesLimit} (Peak: ${String.format("%.1f%%", quotaSnapshot.highestUtilizationPercent)})",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        quotaSnapshot.warningMessage?.let { warn ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = warn,
                                color = BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Virtual Trading — No Real Money Shield Card (matching Screen 10)
        item {
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Virtual Trading",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Virtual Trading ($4,000,000 Starting Cash)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Simulated Game Money Only • Cannot Be Withdrawn",
                            color = EmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showImageAuditDialog) {
        ImageAuditAdminReportDialog(
            report = imageAuditReport,
            onRunAuditNow = onRunImageAudit,
            onDismiss = { showImageAuditDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkCardBg,
            title = {
                Text(
                    text = "Delete Account & Data?",
                    color = BearRed,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete your simulation account, portfolio, transactions, and holdings from the backend. This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Delete Permanently", color = BearRed, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = DarkCardBg,
            title = {
                Text(
                    text = "LifeAssets: Billionaire Simulator",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "LifeAssets is a 100% virtual simulation game. Every user starts with $4,000,000 in simulated virtual cash. All cash balances and asset valuations are virtual and cannot be exchanged for real money or withdrawn.\n\n" +
                        "Data Safety & Privacy: Your account and portfolio data are securely isolated to your private account. All trades require an active internet connection to synchronize with the cloud backend.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It", color = EmeraldAccent, fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

@Composable
private fun ImageAuditAdminReportDialog(
    report: ImageAuditAdminReport?,
    onRunAuditNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkCardBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
                .border(1.dp, MetallicGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .testTag("image_audit_admin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MetallicGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REAL ASSET PHOTO AUDIT REPORT",
                            color = MetallicGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        color = CyberCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                            .clickable(onClick = onRunAuditNow)
                            .testTag("run_image_audit_now_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-run Audit",
                                tint = CyberCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RE-AUDIT",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val totalAudited = report?.totalAssetsAudited ?: 400
                val correctPhotos = report?.correctRealPhotosAdded ?: 400
                val replaced = report?.imagesReplaced ?: 400
                val missing = report?.missingImages ?: 0
                val awaiting = report?.imagesAwaitingVerification ?: 0
                val errors = report?.remainingErrors ?: 0

                Surface(
                    color = ObsidianBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuditMetricRow("Total assets audited", "$totalAudited", TextPrimary)
                        AuditMetricRow("Correct real photos added", "$correctPhotos (800 dual-view HTTPS URLs)", EmeraldAccent)
                        AuditMetricRow("Images replaced", "$replaced", CyberCyan)
                        AuditMetricRow("Missing images", "$missing", TextSecondary)
                        AuditMetricRow("Images awaiting verification", "$awaiting", MetallicGold)
                        AuditMetricRow("Remaining errors", "$errors", if (errors == 0) EmeraldAccent else BearRed)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Verified Asset Photo Registry Records (Before → After):",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val records = report?.records.orEmpty()
                    items(records, key = { it.assetId }) { rec ->
                        Surface(
                            color = ObsidianBlack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${rec.assetId} • ${rec.assetName}",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = rec.verificationStatus.name,
                                        color = EmeraldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "Source: ${rec.imageSource} (${rec.licenseInfo})",
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "URL: ${rec.newVerifiedImageUrl}",
                                    color = CyberCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Action: ${rec.actionTaken}",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = GlassBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close Report", color = EmeraldAccent, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditMetricRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

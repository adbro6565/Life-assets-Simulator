package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.components.DarkCardBg
import com.example.ui.components.DarkNavyBg
import com.example.ui.components.EmeraldAccent
import com.example.ui.components.LifeAssetsBrandHeader
import com.example.ui.components.SubtleCardBorder
import com.example.ui.theme.BullGreen
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
fun OnboardingAuthFlowScreen(
    statusBanner: String?,
    onClearBanner: () -> Unit,
    onSignInWithGoogle: () -> Unit,
    onRegisterAccount: (username: String, email: String, password: String) -> Unit,
    onLoginWithEmailOrUsername: (emailOrUsername: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit
) {
    // 0 = Splash Screen, 1 = Log In Screen, 2 = Sign Up (Register) Screen
    var authStage by rememberSaveable { mutableIntStateOf(0) }

    if (authStage != 0) {
        BackHandler {
            authStage = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF060B14),
                        DarkNavyBg,
                        Color(0xFF091322)
                    )
                )
            )
    ) {
        when (authStage) {
            0 -> {
                SplashLandingContent(
                    onSignUpClick = { authStage = 2 },
                    onLogInClick = { authStage = 1 },
                    onSignInWithGoogle = onSignInWithGoogle
                )
            }

            1, 2 -> {
                LoginSignupContent(
                    isSignUpMode = authStage == 2,
                    statusBanner = statusBanner,
                    onClearBanner = onClearBanner,
                    onBackToSplash = { authStage = 0 },
                    onSwitchMode = { isSignUp -> authStage = if (isSignUp) 2 else 1 },
                    onSignInWithGoogle = onSignInWithGoogle,
                    onRegisterAccount = onRegisterAccount,
                    onLoginWithEmailOrUsername = onLoginWithEmailOrUsername,
                    onForgotPassword = onForgotPassword
                )
            }
        }
    }
}

@Composable
private fun SplashLandingContent(
    onSignUpClick: () -> Unit,
    onLogInClick: () -> Unit,
    onSignInWithGoogle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Branding & Tagline
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LifeAssetsBrandHeader(compact = false)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Learn   •   Trade   •   Grow",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Explore real-world assets, track live prices, and build your virtual portfolio.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Center City Skyline + Glowing Green Market Chart Graphic (matching Screen 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Ambient city horizon glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            EmeraldAccent.copy(alpha = 0.20f),
                            Color(0xFF2979FF).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.55f, h * 0.55f),
                        radius = (w * 0.55f).coerceAtLeast(1f)
                    )
                )

                // City silhouette & illuminated financial towers
                val buildingWidths = listOf(0.08f, 0.10f, 0.09f, 0.12f, 0.11f, 0.10f, 0.12f, 0.09f, 0.10f)
                val buildingHeights = listOf(0.32f, 0.48f, 0.38f, 0.64f, 0.52f, 0.70f, 0.58f, 0.44f, 0.36f)
                var curX = w * 0.04f
                buildingWidths.forEachIndexed { idx, bwRatio ->
                    val bw = w * bwRatio
                    val bh = h * buildingHeights[idx]
                    val topY = h - bh
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A2B45).copy(alpha = 0.85f),
                                Color(0xFF0B1320).copy(alpha = 0.95f)
                            ),
                            startY = topY,
                            endY = h
                        ),
                        topLeft = Offset(curX, topY),
                        size = Size(bw * 0.86f, bh),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    // Golden city lights on towers
                    val rows = (bh / 18f).toInt().coerceAtMost(8)
                    for (r in 1..rows) {
                        val wy = topY + r * 14f
                        if (wy < h - 10f) {
                            drawRect(
                                color = if ((idx + r) % 2 == 0) MetallicGold.copy(alpha = 0.38f) else CyberCyan.copy(alpha = 0.25f),
                                topLeft = Offset(curX + bw * 0.2f, wy),
                                size = Size(bw * 0.45f, 3.dp.toPx())
                            )
                        }
                    }
                    curX += bw
                }

                // Glowing emerald volume bars
                val barCount = 10
                val barSpacing = w / (barCount + 2)
                for (i in 0 until barCount) {
                    val bx = barSpacing * (i + 1)
                    val barH = h * (0.14f + (i * 0.048f) + (if (i % 2 == 0) 0.04f else -0.02f))
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                EmeraldAccent.copy(alpha = 0.65f),
                                EmeraldAccent.copy(alpha = 0.12f)
                            ),
                            startY = h - barH,
                            endY = h
                        ),
                        topLeft = Offset(bx, h - barH),
                        size = Size(barSpacing * 0.52f, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }

                // Upward surging green trend arrow
                val trendPath = Path().apply {
                    moveTo(w * 0.08f, h * 0.82f)
                    lineTo(w * 0.26f, h * 0.65f)
                    lineTo(w * 0.38f, h * 0.72f)
                    lineTo(w * 0.56f, h * 0.46f)
                    lineTo(w * 0.68f, h * 0.53f)
                    lineTo(w * 0.88f, h * 0.16f)
                }
                drawPath(
                    path = trendPath,
                    color = EmeraldAccent.copy(alpha = 0.28f),
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = trendPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF00C853), EmeraldAccent, Color(0xFF69F0AE))
                    ),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val arrowHead = Path().apply {
                    moveTo(w * 0.77f, h * 0.15f)
                    lineTo(w * 0.91f, h * 0.13f)
                    lineTo(w * 0.90f, h * 0.28f)
                    close()
                }
                drawPath(arrowHead, EmeraldAccent)
            }
        }

        // Bottom Primary Actions: Sign Up & Log In (matching Screen 1)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("splash_sign_up_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = ObsidianBlack
                )
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            OutlinedButton(
                onClick = onLogInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("splash_log_in_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, EmeraldAccent.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DarkCardBg.copy(alpha = 0.7f),
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = "Log In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onSignInWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("splash_google_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SubtleCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DarkCardBg,
                    contentColor = TextPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Text(
                text = "100% Virtual Simulation Game • No Real Money or Delivery",
                color = TextSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun LoginSignupContent(
    isSignUpMode: Boolean,
    statusBanner: String?,
    onClearBanner: () -> Unit,
    onBackToSplash: () -> Unit,
    onSwitchMode: (Boolean) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onRegisterAccount: (username: String, email: String, password: String) -> Unit,
    onLoginWithEmailOrUsername: (emailOrUsername: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit
) {
    var fullNameInput by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }
    var recoveryEmailInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top back button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToSplash,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }

        // Brand Logo Header
        LifeAssetsBrandHeader(compact = false)

        Spacer(modifier = Modifier.height(22.dp))

        // Feedback Banner if any validation message is active
        statusBanner?.let { bannerMsg ->
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, EmeraldAccent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .clickable { onClearBanner() }
            ) {
                Text(
                    text = bannerMsg,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Welcome Header (left-aligned matching Screen 2)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isSignUpMode) "Create Account" else "Welcome Back!",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSignUpMode) {
                    "Register to start your virtual trading journey"
                } else {
                    "Sign in to continue your journey"
                },
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (isSignUpMode) {
            OutlinedTextField(
                value = fullNameInput,
                onValueChange = { fullNameInput = it },
                placeholder = { Text("Full Name or Username", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Username",
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCardBg,
                    unfocusedContainerColor = DarkCardBg,
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = SubtleCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            placeholder = {
                Text(
                    text = if (isSignUpMode) "Email address" else "Email address",
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isSignUpMode) Icons.Default.Email else Icons.Default.Person,
                    contentDescription = "Email address",
                    tint = TextSecondary
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(if (isSignUpMode) "register_email_input" else "login_username_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkCardBg,
                unfocusedContainerColor = DarkCardBg,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = SubtleCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            placeholder = { Text("Password", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = TextSecondary
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(if (isSignUpMode) "register_password_input" else "login_password_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkCardBg,
                unfocusedContainerColor = DarkCardBg,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = SubtleCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Remember me & Forgot password row (matching Screen 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldAccent,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = ObsidianBlack
                    )
                )
                Text(
                    text = "Remember me",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (!isSignUpMode) {
                Text(
                    text = "Forgot password?",
                    color = EmeraldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            recoveryEmailInput = emailInput.trim()
                            showForgotDialog = true
                        }
                        .padding(vertical = 6.dp)
                )
            }
        }

        if (showForgotDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                containerColor = DarkCardBg,
                title = {
                    Text(
                        text = "Reset Password",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your registered email address to receive a secure password recovery link:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = recoveryEmailInput,
                            onValueChange = { recoveryEmailInput = it },
                            placeholder = { Text("Email address", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = SubtleCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showForgotDialog = false
                            onForgotPassword(recoveryEmailInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = ObsidianBlack)
                    ) {
                        Text("Send Link", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showForgotDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Primary Submit Button: Log In or Sign Up
        Button(
            onClick = {
                if (isSignUpMode) {
                    onRegisterAccount(fullNameInput, emailInput, passwordInput)
                } else {
                    onLoginWithEmailOrUsername(emailInput, passwordInput)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(if (isSignUpMode) "register_submit_button" else "login_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldAccent,
                contentColor = ObsidianBlack
            )
        ) {
            Text(
                text = if (isSignUpMode) "Sign Up" else "Log In",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Divider: — or —
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = SubtleCardBorder
            )
            Text(
                text = "  or  ",
                color = TextSecondary,
                fontSize = 13.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = SubtleCardBorder
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Continue with Google Button
        OutlinedButton(
            onClick = onSignInWithGoogle,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SubtleCardBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkCardBg,
                contentColor = TextPrimary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSignUpMode) "Sign Up with Google" else "Sign In with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Switch Prompt: "Don't have an account? Sign Up" / "Already have an account? Log In"
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = if (isSignUpMode) "Log In" else "Sign Up",
                color = EmeraldAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clickable { onSwitchMode(!isSignUpMode) }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .testTag(if (isSignUpMode) "switch_to_login_button" else "switch_to_signup_button")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthModalSheet(
    currentUser: UserEntity?,
    isGoogleSignedIn: Boolean,
    onSignInWithGoogle: () -> Unit,
    onRegisterAccount: (username: String, email: String, password: String) -> Unit,
    onLoginWithUsername: (username: String, password: String) -> Unit,
    onUpdateUsername: (newUsername: String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabIndex by remember { mutableStateOf(0) } // 0 = Profile/Handle, 1 = Register, 2 = Login
    var usernameInput by remember(currentUser?.username) { mutableStateOf(currentUser?.username ?: "") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "LIFE ASSETS SIMULATOR • ACCOUNT SETTINGS",
                color = EmeraldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Account & Profile Management",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Current Active Account Summary
            Surface(
                color = ElevatedPanel,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentUser?.username?.replace("_", " ") ?: "Trader Profile",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "trader@lifeassets.app",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            color = if (isGoogleSignedIn) BullGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isGoogleSignedIn) "CLOUD SYNCED" else "ACTIVE SESSION",
                                color = if (isGoogleSignedIn) BullGreen else CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSignInWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("modal_google_sign_in_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = ObsidianBlack
                )
            ) {
                Text(
                    text = if (isGoogleSignedIn) "RE-SYNC GOOGLE ACCOUNT" else "CONTINUE WITH GOOGLE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBlack, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Edit Profile", "Sign Up", "Log In").forEachIndexed { idx, title ->
                    val selected = tabIndex == idx
                    Surface(
                        color = if (selected) ElevatedPanel else ObsidianBlack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        onClick = { tabIndex = idx }
                    ) {
                        Text(
                            text = title,
                            color = if (selected) EmeraldAccent else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (tabIndex) {
                0 -> {
                    Text(
                        text = "Update Display Username (3–20 characters):",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onUpdateUsername(usernameInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_username_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("SAVE PROFILE HANDLE", fontWeight = FontWeight.ExtraBold)
                    }
                }

                1 -> {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onRegisterAccount(usernameInput, emailInput, passwordInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("CREATE ACCOUNT", fontWeight = FontWeight.ExtraBold)
                    }
                }

                2 -> {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Email or Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onLoginWithUsername(usernameInput, passwordInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("LOG IN", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("modal_logout_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("LOG OUT OF ACCOUNT", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

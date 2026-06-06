package com.infinity.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.R
import com.infinity.ai.ui.theme.BluePrimary
import com.infinity.ai.ui.theme.BlueGradientEnd
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val description: String,
    val highlights: List<String>,
    val illustration: @Composable (isDark: Boolean) -> Unit
)

@Composable
fun OnboardingScreen(
    isDarkTheme: Boolean,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "Your AI Assistant, Anywhere",
                description = "Access powerful AI tools, voice assistance, image understanding, and intelligent conversations directly from your device.",
                highlights = listOf("Offline-first AI", "Fast responses", "Privacy focused"),
                illustration = { isDark -> OnboardingImageIllustration(R.drawable.onboarding_assistant, "AI Assistant", isDark) }
            ),
            OnboardingPageData(
                title = "Understand Anything Instantly",
                description = "Upload images, files, PDFs, screenshots, or notes and get explanations, summaries, answers, and insights within seconds.",
                highlights = listOf("File Analyzer", "OCR Scanner", "Image Understanding", "Smart Summaries"),
                illustration = { isDark -> OnboardingImageIllustration(R.drawable.onboarding_file_analyzer, "File Analyzer", isDark) }
            ),
            OnboardingPageData(
                title = "Circle. Learn. Explore.",
                description = "Circle anything on your screen to instantly explain, summarize, translate, generate notes, quizzes, flashcards, and more.",
                highlights = listOf("Circle Learn", "Explain Concepts", "Generate Notes", "Flashcards & Quizzes"),
                illustration = { isDark -> OnboardingImageIllustration(R.drawable.onboarding_circle_learn, "Circle Learn", isDark) }
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Theme values
    val bgColor = if (isDarkTheme) Color(0xFF0B0B0F) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkTheme) Color(0xFF17171C) else Color(0xFFFFFFFF)
    val borderColor = if (isDarkTheme) Color(0xFF2E2E3A) else Color(0xFFE2E8F0)
    val primaryColor = BluePrimary // Brand Gold
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    // Animated entrance for the screen content
    val entranceAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, tween(800, easing = EaseOutQuad))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .alpha(entranceAlpha.value)
    ) {
        // Skip Button in top-right
        if (pagerState.currentPage < pages.size - 1) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
            ) {
                Text(
                    text = "Skip",
                    color = textSecondaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Horizontal pager with page content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Illustration Section (upper 45%)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f),
                    contentAlignment = Alignment.Center
                ) {
                    page.illustration(isDarkTheme)
                }

                // Text Content Section (lower 55%)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 18.dp else 7.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dotWidth"
                            )
                            Box(
                                modifier = Modifier
                                    .size(height = 7.dp, width = dotWidth)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) primaryColor else textSecondaryColor.copy(
                                            alpha = 0.3f
                                        )
                                    )
                            )
                        }
                    }

                    // Title
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Description
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp
                        ),
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Highlights List (card-based pills)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        page.highlights.forEach { highlight ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(0.8.dp, borderColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = highlight,
                                    color = primaryColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Action Row
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
        ) {
            val isLastPage = pagerState.currentPage == pages.size - 1

            if (isLastPage) {
                // Large startup-grade CTA Button
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // Skip on bottom-left, Next on bottom-right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = "Skip",
                            color = textSecondaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ── Image Onboarding Illustration helper
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OnboardingImageIllustration(
    imageResId: Int,
    contentDescription: String,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "illustrationPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val borderColor = if (isDark) Color(0xFF2E2E3A) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .size(260.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial background glow
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(Color(0x1ADB9F0B), Color.Transparent)
                        } else {
                            listOf(Color(0x0FDB9F0B), Color.Transparent)
                        }
                    )
                )
        )

        // The image with rounded corners and border
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(width = 240.dp, height = 240.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF17171C) else Color.White),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ── Page 1 Illustration: Device Chat
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun DeviceChatIllustration(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "deviceChat")
    
    // Wave animation for lines
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chatLines"
    )

    // Pulsing circle scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val backdropColor = if (isDark) Color(0x15DD9F0B) else Color(0x0DDD9F0B)
    val frameColor = if (isDark) Color(0xFF2E2E3A) else Color(0xFFCBD5E1)
    val goldAccent = BluePrimary
    val goldAccentLight = BlueGradientEnd

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .scale(pulseScale)
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension * 0.42f

        // Draw dynamic backdrop radial circle
        drawCircle(
            color = backdropColor,
            radius = radius,
            center = Offset(cx, cy)
        )

        // Draw Laptop Monitor (Center-left)
        val laptopW = 120.dp.toPx()
        val laptopH = 76.dp.toPx()
        val laptopX = cx - laptopW * 0.65f
        val laptopY = cy - laptopH * 0.45f

        // Monitor outline
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(laptopX, laptopY),
            size = Size(laptopW, laptopH),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
        // Monitor stand
        val standW = 32.dp.toPx()
        val standH = 14.dp.toPx()
        drawRect(
            color = frameColor,
            topLeft = Offset(laptopX + laptopW / 2 - standW / 2, laptopY + laptopH),
            size = Size(standW, standH)
        )
        // Laptop base
        val baseW = 144.dp.toPx()
        val baseH = 5.dp.toPx()
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(laptopX + laptopW / 2 - baseW / 2, laptopY + laptopH + standH),
            size = Size(baseW, baseH),
            cornerRadius = CornerRadius(2.5.dp.toPx())
        )

        // Draw Mobile Phone (Center-right, overlapping laptop)
        val phoneW = 48.dp.toPx()
        val phoneH = 92.dp.toPx()
        val phoneX = cx + laptopW * 0.18f
        val phoneY = cy - phoneH * 0.35f

        // Phone backdrop shadow/fill to cover overlapping lines
        drawRoundRect(
            color = if (isDark) Color(0xFF0B0B0F) else Color(0xFFF8FAFC),
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        // Phone frame
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
        // Home bar or status dot
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(phoneX + phoneW / 2 - 8.dp.toPx(), phoneY + 4.dp.toPx()),
            size = Size(16.dp.toPx(), 3.dp.toPx()),
            cornerRadius = CornerRadius(1.5.dp.toPx())
        )

        // Connection waves / speech bubbles
        val path = Path().apply {
            moveTo(laptopX + laptopW * 0.7f, laptopY + laptopH * 0.3f)
            quadraticTo(
                (laptopX + laptopW * 0.7f + phoneX) / 2f,
                (laptopY + laptopH * 0.3f + phoneY + phoneH * 0.4f) / 2f - 20.dp.toPx(),
                phoneX + phoneW * 0.2f,
                phoneY + phoneH * 0.4f
            )
        }

        // Draw animated connection path
        drawPath(
            path = path,
            color = goldAccent.copy(alpha = 0.5f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(20f, 15f),
                    phase = animatedOffset * 100f
                ),
                cap = StrokeCap.Round
            )
        )

        // Draw speech bubble representations
        val bubbleW = 44.dp.toPx()
        val bubbleH = 26.dp.toPx()

        // Laptop bubble
        val b1x = laptopX + 16.dp.toPx()
        val b1y = laptopY + 16.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(listOf(goldAccent, goldAccentLight)),
            topLeft = Offset(b1x, b1y),
            size = Size(bubbleW, bubbleH),
            cornerRadius = CornerRadius(5.dp.toPx())
        )
        // Chat text lines in bubble
        drawLine(Color.White, Offset(b1x + 6.dp.toPx(), b1y + 8.dp.toPx()), Offset(b1x + bubbleW - 6.dp.toPx(), b1y + 8.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, Offset(b1x + 6.dp.toPx(), b1y + 14.dp.toPx()), Offset(b1x + bubbleW - 14.dp.toPx(), b1y + 14.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)

        // Phone bubble
        val b2x = phoneX + 8.dp.toPx()
        val b2y = phoneY + 28.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(listOf(goldAccent, goldAccentLight)),
            topLeft = Offset(b2x, b2y),
            size = Size(28.dp.toPx(), 18.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawLine(Color.White, Offset(b2x + 4.dp.toPx(), b2y + 6.dp.toPx()), Offset(b2x + 24.dp.toPx(), b2y + 6.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, Offset(b2x + 4.dp.toPx(), b2y + 11.dp.toPx()), Offset(b2x + 16.dp.toPx(), b2y + 11.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ── Page 2 Illustration: Document Analyze & Scan
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun DocumentAnalyzeIllustration(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "docAnalyze")
    
    // Scanner bar animation
    val scannerProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner"
    )

    // Pulse animation
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val backdropColor = if (isDark) Color(0x15DD9F0B) else Color(0x0DDD9F0B)
    val cardColor = if (isDark) Color(0xFF1E1E28) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF323242) else Color(0xFFE2E8F0)
    val textLineColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    val goldAccent = BluePrimary
    val goldAccentLight = BlueGradientEnd

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .scale(scalePulse)
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension * 0.42f

        // Draw circular background
        drawCircle(
            color = backdropColor,
            radius = radius,
            center = Offset(cx, cy)
        )

        // Draw Document Card (Background card, tilted left)
        drawCard(
            x = cx - 55.dp.toPx(),
            y = cy - 60.dp.toPx(),
            width = 80.dp.toPx(),
            height = 110.dp.toPx(),
            rotation = -8f,
            cardColor = cardColor,
            borderColor = cardBorder,
            textLineColor = textLineColor
        )

        // Draw Image Card (Foreground card, tilted right)
        drawPictureCard(
            x = cx - 10.dp.toPx(),
            y = cy - 40.dp.toPx(),
            width = 85.dp.toPx(),
            height = 95.dp.toPx(),
            rotation = 6f,
            cardColor = cardColor,
            borderColor = cardBorder,
            goldAccent = goldAccent,
            goldAccentLight = goldAccentLight
        )

        // Draw Glowing Scanner Line moving vertically across the image card
        val scanMinY = cy - 35.dp.toPx()
        val scanMaxY = cy + 50.dp.toPx()
        val scanY = scanMinY + (scanMaxY - scanMinY) * scannerProgress

        // Line glow brush
        val scannerBrush = Brush.horizontalGradient(
            colors = listOf(
                goldAccent.copy(alpha = 0.05f),
                goldAccent,
                goldAccent,
                goldAccent.copy(alpha = 0.05f)
            )
        )

        // Draw line shadow/glow
        drawRect(
            color = goldAccent.copy(alpha = 0.15f),
            topLeft = Offset(cx - 65.dp.toPx(), scanY - 3.dp.toPx()),
            size = Size(135.dp.toPx(), 6.dp.toPx())
        )

        // Draw line
        drawLine(
            brush = scannerBrush,
            start = Offset(cx - 65.dp.toPx(), scanY),
            end = Offset(cx + 70.dp.toPx(), scanY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCard(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    rotation: Float,
    cardColor: Color,
    borderColor: Color,
    textLineColor: Color
) {
    val center = Offset(x + width / 2, y + height / 2)
    
    rotate(degrees = rotation, pivot = center) {
        // Card Body
        drawRoundRect(
            color = cardColor,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        // Card Border
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Draw text mock lines
        val lineMargin = 10.dp.toPx()
        val lineSpacing = 8.dp.toPx()
        val startY = y + 16.dp.toPx()

        // Title line
        drawRoundRect(
            color = textLineColor.copy(alpha = 1.2f),
            topLeft = Offset(x + lineMargin, startY),
            size = Size(width * 0.5f, 6.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Content lines
        repeat(5) { i ->
            val curY = startY + 16.dp.toPx() + i * lineSpacing
            val fractionalWidth = when (i) {
                4 -> 0.4f
                2 -> 0.75f
                else -> 0.82f
            }
            drawRoundRect(
                color = textLineColor,
                topLeft = Offset(x + lineMargin, curY),
                size = Size(width * fractionalWidth, 3.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawPictureCard(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    rotation: Float,
    cardColor: Color,
    borderColor: Color,
    goldAccent: Color,
    goldAccentLight: Color
) {
    val center = Offset(x + width / 2, y + height / 2)
    
    rotate(degrees = rotation, pivot = center) {
        // Card Body
        drawRoundRect(
            color = cardColor,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        // Card Border
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Top image window box
        val imgMargin = 8.dp.toPx()
        val imgH = height * 0.5f
        val imgW = width - imgMargin * 2
        drawRoundRect(
            color = goldAccent.copy(alpha = 0.08f),
            topLeft = Offset(x + imgMargin, y + imgMargin),
            size = Size(imgW, imgH),
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // Draw simple mountains outline in image box
        val peakY = y + imgMargin + imgH * 0.4f
        val bottomY = y + imgMargin + imgH
        val p1 = Path().apply {
            moveTo(x + imgMargin + 4.dp.toPx(), bottomY)
            lineTo(x + imgMargin + imgW * 0.4f, peakY)
            lineTo(x + imgMargin + imgW * 0.7f, bottomY - 6.dp.toPx())
            lineTo(x + imgMargin + imgW * 0.85f, peakY + 8.dp.toPx())
            lineTo(x + imgMargin + imgW, bottomY)
            close()
        }
        drawPath(
            p1,
            brush = Brush.linearGradient(listOf(goldAccent.copy(0.2f), goldAccentLight.copy(0.05f)))
        )
        drawPath(
            p1,
            color = goldAccent.copy(0.4f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw a miniature sun
        drawCircle(
            color = goldAccentLight.copy(alpha = 0.5f),
            radius = 5.dp.toPx(),
            center = Offset(x + imgMargin + imgW * 0.75f, y + imgMargin + imgH * 0.35f)
        )

        // Draw description text lines below the photo
        val textStartY = y + imgMargin + imgH + 10.dp.toPx()
        drawRoundRect(
            color = goldAccent.copy(alpha = 0.6f),
            topLeft = Offset(x + imgMargin, textStartY),
            size = Size(width * 0.5f, 4.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        drawRoundRect(
            color = borderColor.copy(alpha = 1.5f),
            topLeft = Offset(x + imgMargin, textStartY + 8.dp.toPx()),
            size = Size(width * 0.72f, 3.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ── Page 3 Illustration: Circle Learn Floating Bubble
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun CircleLearnOnboardingIllustration(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "circleLearn")
    
    // Bubble float animation
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubbleFloat"
    )

    // Pulse animation
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val backdropColor = if (isDark) Color(0x15DD9F0B) else Color(0x0DDD9F0B)
    val frameColor = if (isDark) Color(0xFF2E2E3A) else Color(0xFFCBD5E1)
    val elementColor = if (isDark) Color(0xFF1E1E28) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color(0xFF323242) else Color(0xFFE2E8F0)
    val goldAccent = BluePrimary
    val goldAccentLight = BlueGradientEnd

    Canvas(
        modifier = Modifier.size(240.dp)
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension * 0.42f

        // Draw backdrop
        drawCircle(
            color = backdropColor,
            radius = radius,
            center = Offset(cx, cy)
        )

        // Draw Mock Mobile Screen Frame (Vertical card, center)
        val phoneW = 110.dp.toPx()
        val phoneH = 150.dp.toPx()
        val phoneX = cx - phoneW / 2
        val phoneY = cy - phoneH * 0.55f

        drawRoundRect(
            color = elementColor,
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = borderCol,
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw fake items inside mobile screen
        val boxW = phoneW - 16.dp.toPx()
        val boxH = 34.dp.toPx()
        val boxX = phoneX + 8.dp.toPx()

        repeat(3) { i ->
            val boxY = phoneY + 16.dp.toPx() + i * (boxH + 8.dp.toPx())
            drawRoundRect(
                color = borderCol.copy(alpha = 0.5f),
                topLeft = Offset(boxX, boxY),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            // Mini item icons inside fake list
            drawCircle(
                color = goldAccent.copy(alpha = 0.15f),
                radius = 6.dp.toPx(),
                center = Offset(boxX + 12.dp.toPx(), boxY + boxH / 2)
            )
            drawRoundRect(
                color = borderCol,
                topLeft = Offset(boxX + 24.dp.toPx(), boxY + boxH / 2 - 2.dp.toPx()),
                size = Size(boxW - 36.dp.toPx(), 4.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }

        // Dotted gesture circle around the second item
        val circleGestureCx = boxX + boxW / 2
        val circleGestureCy = phoneY + 16.dp.toPx() + boxH + 8.dp.toPx() + boxH / 2
        val circleGestureR = 34.dp.toPx()

        drawCircle(
            color = goldAccent,
            radius = circleGestureR,
            center = Offset(circleGestureCx, circleGestureCy),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                cap = StrokeCap.Round
            )
        )

        // Glowing selection backing highlight
        drawCircle(
            color = goldAccent.copy(alpha = 0.07f * pulseAlpha),
            radius = circleGestureR,
            center = Offset(circleGestureCx, circleGestureCy)
        )

        // Draw floating gold Infinity Bubble interacting on the edge
        val bubbleCx = circleGestureCx + circleGestureR * 0.72f
        val bubbleCy = circleGestureCy - circleGestureR * 0.72f + floatOffsetDp.dp.toPx()
        val bubbleR = 21.dp.toPx()

        // 1. Shadow glow
        drawCircle(
            color = goldAccent.copy(alpha = 0.28f),
            radius = bubbleR + 6.dp.toPx(),
            center = Offset(bubbleCx, bubbleCy)
        )

        // 2. White bubble body with gold gradient border
        drawCircle(
            color = Color.White,
            radius = bubbleR,
            center = Offset(bubbleCx, bubbleCy)
        )
        drawCircle(
            brush = Brush.verticalGradient(listOf(goldAccent, goldAccentLight)),
            radius = bubbleR,
            center = Offset(bubbleCx, bubbleCy),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 3. Draw Infinity Symbol Loop in the center of the bubble
        val hw = bubbleR * 0.45f
        val path = Path().apply {
            val steps = 48
            for (i in 0..steps) {
                val t = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
                // Lemniscate of Bernoulli equations
                val scale = hw / (1f + Math.sin(t.toDouble()).toFloat() * Math.sin(t.toDouble()).toFloat())
                val x = scale * Math.cos(t.toDouble()).toFloat()
                val y = scale * Math.sin(t.toDouble()).toFloat() * Math.cos(t.toDouble()).toFloat()
                if (i == 0) {
                    moveTo(bubbleCx + x, bubbleCy + y)
                } else {
                    lineTo(bubbleCx + x, bubbleCy + y)
                }
            }
            close()
        }

        drawPath(
            path = path,
            color = goldAccent,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Tiny floating magical spark dots
        drawCircle(goldAccentLight, 3.dp.toPx(), Offset(bubbleCx - 24.dp.toPx(), bubbleCy - 12.dp.toPx()))
        drawCircle(goldAccent, 2.dp.toPx(), Offset(bubbleCx + 14.dp.toPx(), bubbleCy - 22.dp.toPx()))
        drawCircle(goldAccentLight, 2.5.dp.toPx(), Offset(bubbleCx + 4.dp.toPx(), bubbleCy + 22.dp.toPx()))
    }
}

package com.infinity.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.infinity.ai.R
import com.infinity.ai.ui.components.*
import com.infinity.ai.ui.theme.*

@Composable
fun DashboardScreen(
    isDarkTheme            : Boolean,
    orbState               : OrbState,
    bottomPadding          : Dp,
    onNavigateToChat       : () -> Unit,
    onNavigateToVoice      : () -> Unit,
    onOrbTap               : () -> Unit,
    onNavigateToCircle     : () -> Unit = onOrbTap,
    onNavigateToOcr        : () -> Unit = onNavigateToChat,
    onNavigateToPdf        : () -> Unit = onNavigateToChat,
    onNavigateToQuiz       : () -> Unit = onNavigateToChat,
    onNavigateToScreenshot : () -> Unit = onNavigateToChat
) {
    val scroll = rememberScrollState()

    GradientBackground(darkTheme = isDarkTheme, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(bottom = bottomPadding + 96.dp) // Scroll above the bottom navbar
        ) {
            // ── 1. Header (Infinity Logo & Name, Notifications) ──────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 24.dp, top = 6.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Logo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Infinity Logo",
                        modifier = Modifier.size(width = 120.dp, height = 65.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Right: Notification Icon with 3D shadow
                Box(
                    modifier = Modifier
                        .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0xFF64748B).copy(alpha = 0.15f), spotColor = Color(0xFF1E293B).copy(alpha = 0.12f))
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color(0xFF1E293B) else CardWhite)
                        .border(0.8.dp, if (isDarkTheme) BluePrimary.copy(alpha = 0.25f) else BorderLight, CircleShape)
                        .clickable { }
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = if (isDarkTheme) Color.White else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── 2. Premium Plan Banner - Slider Pager ─────────────────────────
            val slides = remember {
                listOf(
                    SlideItem("Premium Plan", "Unlock all premium features", R.drawable.bot, "Chat Now"),
                    SlideItem("Smart Learning", "Learn faster with AI", R.drawable.student, "Learn Now"),
                    SlideItem("Always Offline", "Chat offline anywhere", R.drawable.offline, "Start Now")
                )
            }
            val slideGradients = remember {
                listOf(
                    listOf(Color(0xFFDD9F0B), Color(0xFF0F172A)), // gold to black
                    listOf(Color(0xFFDD9F0B), Color(0xFFB07F08)), // gold to dark amber
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))  // black to slate
                )
            }

            val pagerState = rememberPagerState(pageCount = { 3 })

            // Auto-slide LaunchedEffect
            LaunchedEffect(pagerState) {
                while (true) {
                    kotlinx.coroutines.delay(3500)
                    val nextPage = (pagerState.currentPage + 1) % 3
                    pagerState.animateScrollToPage(nextPage)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp), // Height to fit the 200.dp outer Box + shadows
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp
            ) { pageIndex ->
                val slide = slides[pageIndex]
                val title = slide.title
                val subtitle = slide.subtitle
                val imageResId = slide.imageResId
                val buttonText = slide.buttonText
                val currentGradient = slideGradients[pageIndex]

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Outer Box container
                ) {
                    // Card background with text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp) // Card height adjusted to fit content elegantly
                            .align(Alignment.BottomCenter) // Sits at the bottom of outer Box
                            .shadow(
                                elevation = 12.dp, // High elevation gradient glow shadow
                                shape = RoundedCornerShape(24.dp),
                                clip = false,
                                ambientColor = currentGradient.first().copy(alpha = 0.35f),
                                spotColor = currentGradient.last().copy(alpha = 0.25f)
                            )
                            .shadow(
                                elevation = 4.dp, // Ground contact shadow
                                shape = RoundedCornerShape(24.dp),
                                clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.1f),
                                spotColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.horizontalGradient(colors = currentGradient))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(0.60f) // Keep content from overlapping image
                        ) {
                            Text(
                                title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                subtitle,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardWhite)
                                    .clickable {
                                        when (pageIndex) {
                                            0 -> onNavigateToChat()
                                            1 -> onNavigateToCircle()
                                            2 -> onNavigateToChat()
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp) // Compact button padding
                            ) {
                                Text(
                                    buttonText,
                                    fontSize = 13.sp, // Visible and compact font size
                                    fontWeight = FontWeight.ExtraBold, // ExtraBold visibility
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // 3D image popped out of the card boundaries, shifted right
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = title,
                        modifier = Modifier
                            .align(Alignment.TopEnd) // Sits on top right
                            .size(160.dp) // Sized appropriately to prevent overlap (increased from 145.dp)
                            .offset(x = 10.dp, y = 2.dp) // Shifted right and pops out off the top edge (within container)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Slide Indicator Dots
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val color = if (pagerState.currentPage == index) BluePrimary else Color(0xFFE2E8F0)
                    val width = if (pagerState.currentPage == index) 16.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .height(8.dp)
                            .width(width)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 3. Quick Access Title ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Quick Access",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDarkTheme) Color.White else TextPrimary
                )
                Text(
                    "See All",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = BluePrimary,
                    modifier = Modifier.clickable { onNavigateToCircle() }
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── 4. Bento Grid (Asymmetric 5-card layout) ─────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top row: Voice (Left, 60%) and Stack of Image & Chat (Right, 40%)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left side: Voice Assistant (60%)
                    BentoQuickCard(
                        drawableResId = R.drawable.voice,
                        title = "Voice Assistant",
                        subtitle = "Try Voice Recognition",
                        onClick = onNavigateToVoice,
                        isDarkTheme = isDarkTheme,
                        layoutType = BentoCardType.Tall,
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    )

                    // Right side: Image Analysis (Top) & Chat Assistant (Bottom)
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BentoQuickCard(
                            drawableResId = R.drawable.image,
                            title = "Image Analysis",
                            subtitle = "Search by image",
                            onClick = onNavigateToOcr,
                            isDarkTheme = isDarkTheme,
                            layoutType = BentoCardType.WideStacked,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        BentoQuickCard(
                            drawableResId = R.drawable.chat,
                            title = "Chat Assistant",
                            subtitle = "Recently chat",
                            onClick = onNavigateToChat,
                            isDarkTheme = isDarkTheme,
                            layoutType = BentoCardType.WideStacked,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }

                // Bottom row: Learn and File Analyzer (Full width, decreased height)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoQuickCard(
                        drawableResId = R.drawable.learn,
                        title = "Learn",
                        subtitle = "Circle to explain",
                        onClick = onNavigateToCircle,
                        isDarkTheme = isDarkTheme,
                        layoutType = BentoCardType.BottomRow,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    BentoQuickCard(
                        drawableResId = R.drawable.file,
                        title = "File Analyzer",
                        subtitle = "Analyze docs & PDF",
                        onClick = onNavigateToPdf,
                        isDarkTheme = isDarkTheme,
                        layoutType = BentoCardType.BottomRow,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 5. Start New Chat Button ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isDarkTheme) Color(0xFF1E293B) else CardWhite)
                    .border(1.dp, if (isDarkTheme) BluePrimary.copy(alpha = 0.2f) else BorderLight, RoundedCornerShape(32.dp))
                    .clickable(onClick = onNavigateToChat)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left circular chevron
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center Text
                    Text(
                        "Start New Chat",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) Color.White else TextPrimary
                    )

                    // Right Indicators
                    Text(
                        ">>>",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }

            // ── 6. Previous Search Section (Recently Visited) ────────────────
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Previous search",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color.White else TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 0.dp else 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = Color(0xFF64748B).copy(alpha = 0.14f),
                        spotColor = Color(0xFF1E293B).copy(alpha = 0.12f)
                    )
                    .shadow(
                        elevation = if (isDarkTheme) 0.dp else 3.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = Color(0xFF94A3B8).copy(alpha = 0.08f),
                        spotColor = Color(0xFF475569).copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.8f) else CardWhite)
                    .border(1.dp, if (isDarkTheme) BluePrimary.copy(alpha = 0.15f) else BorderLight, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recently visited",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDarkTheme) Color.White else TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) Color(0xFF1E293B) else CardWhite)
                                .border(1.dp, if (isDarkTheme) BluePrimary.copy(alpha = 0.2f) else BorderLight, CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                null,
                                tint = if (isDarkTheme) Color.White else TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Separator below title
                    HorizontalDivider(color = if (isDarkTheme) BluePrimary.copy(alpha = 0.15f) else BorderLight, thickness = 0.8.dp)

                    // Item 1 (Chat search)
                    PreviousSearchItem(
                        iconResId = R.drawable.chat,
                        title = "Write a Python function to check prime",
                        subtitle = "AI Chat",
                        timeText = "Changed an hour ago",
                        isStarred = true,
                        isDarkTheme = isDarkTheme,
                        onClick = onNavigateToChat
                    )

                    // Separator between items
                    HorizontalDivider(color = if (isDarkTheme) BluePrimary.copy(alpha = 0.15f) else BorderLight, thickness = 0.8.dp)

                    // Item 2 (Workspace learn)
                    PreviousSearchItem(
                        iconResId = R.drawable.learn,
                        title = "Social media schedule",
                        subtitle = "Main workspace",
                        timeText = "Changed an hour ago",
                        isStarred = false,
                        isDarkTheme = isDarkTheme,
                        onClick = onNavigateToCircle
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoQuickCard(
    drawableResId: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    layoutType: BentoCardType,
    modifier: Modifier = Modifier
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "quickCardScale"
    )

    // Determine colors based on Theme
    val cardBg = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White
    val borderColor = if (isDarkTheme) BluePrimary.copy(alpha = 0.15f) else BorderLight
    val textPrimaryColor = if (isDarkTheme) Color.White else TextPrimary
    val textSecondaryColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else TextSecondary
    
    // Soft shadow for light mode, no shadow or very subtle glow for dark mode
    val cardModifier = modifier
        .scale(scale)
        .shadow(
            elevation = if (isDarkTheme) 0.dp else 10.dp,
            shape = RoundedCornerShape(24.dp),
            clip = false,
            ambientColor = Color(0xFF64748B).copy(alpha = 0.14f),
            spotColor = Color(0xFF1E293B).copy(alpha = 0.12f)
        )
        .shadow(
            elevation = if (isDarkTheme) 0.dp else 3.dp,
            shape = RoundedCornerShape(24.dp),
            clip = false,
            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.08f),
            spotColor = Color(0xFF475569).copy(alpha = 0.06f)
        )
        .clip(RoundedCornerShape(24.dp))
        .background(cardBg)
        .border(1.dp, borderColor, RoundedCornerShape(24.dp))
        .clickable(
            interactionSource = src,
            indication = LocalIndication.current,
            onClick = onClick
        )

    Box(modifier = cardModifier) {
        when (layoutType) {
            BentoCardType.Tall -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            color = textPrimaryColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = textSecondaryColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Image(
                            painter = painterResource(id = drawableResId),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxHeight(1.0f)
                                .fillMaxWidth(0.9f)
                        )
                    }
                }
            }

            BentoCardType.WideStacked -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 17.sp
                            ),
                            color = textPrimaryColor,
                            maxLines = 2
                        )
                    }

                    Image(
                        painter = painterResource(id = drawableResId),
                        contentDescription = title,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            BentoCardType.BottomRow -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = painterResource(id = drawableResId),
                        contentDescription = title,
                        modifier = Modifier.size(48.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = textPrimaryColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = textSecondaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviousSearchItem(
    iconResId: Int,
    title: String,
    subtitle: String,
    timeText: String,
    isStarred: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color.White else TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else TextSecondary
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else TextSecondary.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Starred status",
            tint = if (isStarred) Color(0xFFFFB020) else (if (isDarkTheme) Color.White.copy(0.3f) else Color(0xFFC4CDD5)),
            modifier = Modifier.size(22.dp)
        )
    }
}

private enum class BentoCardType {
    Tall,
    WideStacked,
    BottomRow
}

private data class SlideItem(
    val title: String,
    val subtitle: String,
    val imageResId: Int,
    val buttonText: String
)

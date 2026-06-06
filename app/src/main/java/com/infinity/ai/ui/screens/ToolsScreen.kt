package com.infinity.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.theme.*

@Composable
fun ToolsScreen(
    isDarkTheme            : Boolean,
    bottomPadding          : Dp,
    onNavigateToPdf        : () -> Unit = {},
    onNavigateToOcr        : () -> Unit = {},
    onNavigateToScreenshot : () -> Unit = {},
    onNavigateToQuiz       : () -> Unit = {},
    onNavigateToCircle     : () -> Unit = {},
    onSendPrompt           : (String) -> Unit = {}
) {
    val scroll = rememberScrollState()

    GradientBackground(darkTheme = false, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomPadding + 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── 1. Top Navigation Row ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back arrow
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable { onNavigateToCircle() }, // default back/dashboard
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Title
                Text(
                    "Tools",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    color = TextPrimary
                )

                // Ellipsis button (to keep space balanced)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        "Options",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Bento list area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(28.dp))

                // ── 2. Greeting Header ───────────────────────────────────────────
                Text(
                    "Hello, Jenny Wilson!",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "What are we going to\ndo Today?",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 38.sp),
                    color = TextPrimary
                )

                Spacer(Modifier.height(28.dp))

                // ── 3. 2x2 Bento grid of Tools ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // File Analyzer
                    ToolBentoCard(
                        icon = Icons.Outlined.FolderOpen,
                        iconBg = Color(0xFFFFFBEB), // light yellow
                        iconColor = Color(0xFFDD9F0B),
                        title = "File Analyzer",
                        description = "Upload and analyze PDF files to get instant summaries, extract key insights, and find quick answers.",
                        onClick = onNavigateToPdf,
                        modifier = Modifier.weight(1f).height(190.dp)
                    )

                    // OCR Scanner
                    ToolBentoCard(
                        icon = Icons.Outlined.CropFree,
                        iconBg = Color(0xFFFEF3C7), // light orange
                        iconColor = Color(0xFFD97706),
                        title = "OCR Scanner",
                        description = "Scan and convert printed or handwritten text from any image into clear, editable copy instantly.",
                        onClick = onNavigateToOcr,
                        modifier = Modifier.weight(1f).height(190.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Screenshot
                    ToolBentoCard(
                        icon = Icons.Outlined.Laptop,
                        iconBg = Color(0xFFECFDF5), // light green
                        iconColor = Color(0xFF059669),
                        title = "Screenshot",
                        description = "Capture or upload screenshots of code/errors to get immediate explanations and solutions.",
                        onClick = onNavigateToScreenshot,
                        modifier = Modifier.weight(1f).height(190.dp)
                    )

                    // Quiz Gen
                    ToolBentoCard(
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        iconBg = Color(0xFFFEE2E2), // light red
                        iconColor = Color(0xFFDC2626),
                        title = "Quiz Gen",
                        description = "Generate customized multiple-choice tests from study materials to practice and learn.",
                        onClick = onNavigateToQuiz,
                        modifier = Modifier.weight(1f).height(190.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ToolBentoCard(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "toolBentoScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

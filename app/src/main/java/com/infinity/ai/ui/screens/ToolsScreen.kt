package com.infinity.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.theme.*

// ── Exact same signature — all navigation callbacks preserved ─────────────────
@Composable
fun ToolsScreen(
    isDarkTheme            : Boolean,
    bottomPadding          : Dp,
    onNavigateToPdf        : () -> Unit = {},
    onNavigateToOcr        : () -> Unit = {},
    onNavigateToScreenshot : () -> Unit = {},
    onNavigateToQuiz       : () -> Unit = {},
    onNavigateToCircle     : () -> Unit = {}
) {
    val dark = isDarkTheme
    val bg   = if (dark) DarkBg       else LightBg
    val surf = if (dark) DarkSurface  else LightSurface
    val bord = if (dark) DarkBorder   else LightBorder

    GradientBackground(darkTheme = dark, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(
                "Tools",
                style      = MaterialTheme.typography.headlineMedium,
                color      = if (dark) TextPrimary else TextPrimaryLight,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AI-powered capabilities",
                style = MaterialTheme.typography.bodyMedium,
                color = if (dark) TextSecondary else TextSecondaryLight
            )

            Spacer(Modifier.height(28.dp))

            // ════════════════════════════════════════════════════════════════
            // ROW 1 — Circle Learn: full-width hero card
            // ════════════════════════════════════════════════════════════════
            BentoHeroCard(
                icon        = Icons.Default.RadioButtonChecked,
                title       = "Circle Learn",
                description = "Circle anything on your screen to instantly explain, summarize, translate, or quiz yourself — powered by on-device AI.",
                badge       = "Flagship",
                isDarkTheme = dark,
                onClick     = onNavigateToCircle,
                modifier    = Modifier.fillMaxWidth().height(172.dp)
            )

            Spacer(Modifier.height(12.dp))

            // ════════════════════════════════════════════════════════════════
            // ROW 2 — File Analyzer (tall) | OCR + Screenshot (stacked)
            // ════════════════════════════════════════════════════════════════
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left — tall card
                BentoLargeCard(
                    icon        = Icons.Default.FolderOpen,
                    title       = "File Analyzer",
                    description = "Summarize PDFs and documents with AI",
                    isDarkTheme = dark,
                    onClick     = onNavigateToPdf,
                    modifier    = Modifier.weight(1f).height(280.dp)
                )

                // Right — two stacked medium cards
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoMediumCard(
                        icon        = Icons.Default.DocumentScanner,
                        title       = "OCR Scanner",
                        description = "Extract text from images",
                        isDarkTheme = dark,
                        onClick     = onNavigateToOcr,
                        modifier    = Modifier.fillMaxWidth().height(134.dp)
                    )
                    BentoMediumCard(
                        icon        = Icons.Default.ScreenSearchDesktop,
                        title       = "Screenshot",
                        description = "Explain errors & code",
                        isDarkTheme = dark,
                        onClick     = onNavigateToScreenshot,
                        modifier    = Modifier.fillMaxWidth().height(134.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ════════════════════════════════════════════════════════════════
            // ROW 3 — Quiz Generator | Smart Notes (equal halves)
            // ════════════════════════════════════════════════════════════════
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoMediumCard(
                    icon        = Icons.Default.Quiz,
                    title       = "Quiz Generator",
                    description = "Generate MCQs from any content",
                    isDarkTheme = dark,
                    onClick     = onNavigateToQuiz,
                    modifier    = Modifier.weight(1f).height(148.dp)
                )
                BentoMediumCard(
                    icon        = Icons.Default.EditNote,
                    title       = "Smart Notes",
                    description = "AI-powered note taking",
                    isDarkTheme = dark,
                    onClick     = {},        // no dedicated route yet — same as before
                    modifier    = Modifier.weight(1f).height(148.dp)
                )
            }

            Spacer(Modifier.height(bottomPadding + 24.dp))
        }
    }
}

// ── Hero card — full-width, gradient accent strip ─────────────────────────────

@Composable
private fun BentoHeroCard(
    icon:        ImageVector,
    title:       String,
    description: String,
    badge:       String,
    isDarkTheme: Boolean,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed           by interactionSource.collectIsPressedAsState()
    val scale             by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "heroScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) DarkSurface else LightSurface)
            .border(1.dp, if (isDarkTheme) DarkBorder else LightBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        // Subtle blue gradient strip on the left edge
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(listOf(Blue500, Blue600)),
                    RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(44.dp).background(Blue50, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Blue500, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style      = MaterialTheme.typography.titleMedium,
                        color      = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    BadgeChip(badge, isDarkTheme)
                }
                Icon(
                    Icons.Default.ArrowForward, null,
                    tint     = Blue500,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                description,
                style      = MaterialTheme.typography.bodySmall,
                color      = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Large card — tall, left column ────────────────────────────────────────────

@Composable
private fun BentoLargeCard(
    icon:        ImageVector,
    title:       String,
    description: String,
    isDarkTheme: Boolean,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed           by interactionSource.collectIsPressedAsState()
    val scale             by animateFloatAsState(
        targetValue   = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "largeScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) DarkSurface else LightSurface)
            .border(1.dp, if (isDarkTheme) DarkBorder else LightBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon at top
        Box(
            modifier         = Modifier.size(48.dp).background(Blue50, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(24.dp))
        }

        // Text at bottom
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style      = MaterialTheme.typography.titleSmall,
                color      = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                style      = MaterialTheme.typography.bodySmall,
                color      = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Open",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Blue500,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Default.ArrowForward, null,
                    tint = Blue500, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Medium card — used in stacked right column and bottom row ─────────────────

@Composable
private fun BentoMediumCard(
    icon:        ImageVector,
    title:       String,
    description: String,
    isDarkTheme: Boolean,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed           by interactionSource.collectIsPressedAsState()
    val scale             by animateFloatAsState(
        targetValue   = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "medScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDarkTheme) DarkSurface else LightSurface)
            .border(1.dp, if (isDarkTheme) DarkBorder else LightBorder, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier         = Modifier.size(38.dp).background(Blue50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(19.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style      = MaterialTheme.typography.labelLarge,
                color      = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                fontWeight = FontWeight.Bold,
                maxLines   = 1
            )
            Text(
                description,
                style    = MaterialTheme.typography.bodySmall,
                color    = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Badge chip ────────────────────────────────────────────────────────────────

@Composable
private fun BadgeChip(label: String, isDarkTheme: Boolean) {
    Box(
        modifier = Modifier
            .background(Blue50, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = Blue500,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.infinity.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.ui.components.*
import com.infinity.ai.ui.theme.*

// ── Exact same signature as before — navigation untouched ─────────────────────
@Composable
fun DashboardScreen(
    isDarkTheme: Boolean,
    orbState: OrbState,
    bottomPadding: Dp,
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onOrbTap: () -> Unit
) {
    val scroll = rememberScrollState()

    GradientBackground(darkTheme = isDarkTheme, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp)
                            .background(Blue50, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("∞", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Blue500)
                    }
                    Text("Infinity", style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                        fontWeight = FontWeight.SemiBold)
                }
                DashHeaderIcon(Icons.Default.Notifications, isDarkTheme) {}
            }

            Spacer(Modifier.height(24.dp))

            // ── Greeting ──────────────────────────────────────────────────────
            val greeting = remember {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when { hour < 12 -> "Good morning" ; hour < 17 -> "Good afternoon" ; else -> "Good evening" }
            }
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(greeting, style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
                Spacer(Modifier.height(2.dp))
                Text("Welcome back", style = MaterialTheme.typography.headlineMedium,
                    color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                    fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            // ── Search bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkTheme) DarkSurface else LightSurface)
                    .border(1.dp,
                        if (isDarkTheme) DarkBorder else LightBorder,
                        RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateToChat)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Search, null,
                    tint = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                    modifier = Modifier.size(18.dp))
                Text("Ask Infinity anything...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                    modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Blue500, CircleShape)
                        .clickable(onClick = onNavigateToVoice)
                        .semantics { contentDescription = "Voice input" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── AI Orb ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                AiBodyOrb(
                    orbState    = orbState,
                    isDarkTheme = isDarkTheme,
                    size        = 200.dp,
                    modifier    = Modifier.clickable(onClick = onOrbTap)
                )
                AnimatedContent(
                    targetState = orbState,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "stateLabel",
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) { state ->
                    Text(
                        text = when (state) {
                            OrbState.Idle       -> "Tap to chat"
                            OrbState.Loading    -> "Loading model..."
                            OrbState.Thinking   -> "Thinking..."
                            OrbState.Responding -> "Responding..."
                            OrbState.Error      -> "Something went wrong"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (state) {
                            OrbState.Error -> ErrorRed
                            OrbState.Idle  -> if (isDarkTheme) TextSecondary else TextSecondaryLight
                            else           -> Blue500
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Quick Actions 2×4 grid ────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                DashSectionLabel("Quick Actions", isDarkTheme)
                Spacer(Modifier.height(12.dp))

                // Use a fixed-height grid (4 rows × ~72dp = 308dp + spacing)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(quickActions) { qa ->
                        QuickActionCell(
                            icon       = qa.icon,
                            label      = qa.label,
                            onClick    = when (qa.label) {
                                "Chat"        -> onNavigateToChat
                                "Voice"       -> onNavigateToVoice
                                else          -> onNavigateToChat
                            },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Suggestions (same content as before) ──────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DashSectionLabel("Suggestions", isDarkTheme)
                Spacer(Modifier.height(4.dp))
                AITaskCard(Icons.Default.Chat,     "Start a conversation", "Ask Infinity anything",       Blue500,           onNavigateToChat,  darkTheme = isDarkTheme)
                AITaskCard(Icons.Default.Mic,      "Voice command",        "Speak to activate Infinity",  Blue500,           onNavigateToVoice, darkTheme = isDarkTheme)
                AITaskCard(Icons.Default.EditNote, "Smart Notes",          "AI-powered note taking",      Blue500,           onNavigateToChat,  darkTheme = isDarkTheme)
                AITaskCard(Icons.Default.Terminal, "Run command",          "Execute AI commands",         Blue500,           onNavigateToChat,  darkTheme = isDarkTheme)
            }

            Spacer(Modifier.height(bottomPadding + 20.dp))
        }
    }
}

// ── Quick action data ─────────────────────────────────────────────────────────

private data class QuickAction(val icon: ImageVector, val label: String)

private val quickActions = listOf(
    QuickAction(Icons.Default.Chat,             "Chat"),
    QuickAction(Icons.Default.DocumentScanner,  "OCR"),
    QuickAction(Icons.Default.FolderOpen,       "PDF"),
    QuickAction(Icons.Default.EditNote,         "Notes"),
    QuickAction(Icons.Default.Style,            "Flashcards"),
    QuickAction(Icons.Default.Quiz,             "Quiz"),
    QuickAction(Icons.Default.RecordVoiceOver,  "Viva"),
    QuickAction(Icons.Default.RadioButtonChecked, "Circle"),
)

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun DashHeaderIcon(icon: ImageVector, isDarkTheme: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isDarkTheme) DarkSurface else LightSurface,
                CircleShape
            )
            .border(1.dp,
                if (isDarkTheme) DarkBorder else LightBorder,
                CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null,
            tint = if (isDarkTheme) TextSecondary else TextSecondaryLight,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DashSectionLabel(text: String, isDarkTheme: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (isDarkTheme) TextSecondary else TextSecondaryLight,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp
    )
}

@Composable
private fun QuickActionCell(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDarkTheme) DarkSurface else LightSurface)
            .border(1.dp,
                if (isDarkTheme) DarkBorder else LightBorder,
                RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(Blue50, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(17.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

package com.infinity.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
            Spacer(Modifier.height(16.dp))

            // ── Header ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("∞", fontSize = 28.sp, fontWeight = FontWeight.Light, color = Blue500)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderIconButton(Icons.Default.Notifications, isDarkTheme) {}
                    HeaderIconButton(Icons.Default.GridView, isDarkTheme) {}
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Greeting ──────────────────────────────────────────────
            val greeting = remember {
                // Use java.util.Calendar instead of java.time.LocalTime —
                // java.time requires API 26 but minSdk is 24.
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when {
                    hour < 12 -> "Good morning,"
                    hour < 17 -> "Good afternoon,"
                    else      -> "Good evening,"
                }
            }
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(greeting, style = MaterialTheme.typography.bodyLarge,
                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
                Text("Infinity", style = MaterialTheme.typography.headlineLarge,
                    color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                    fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            // ── Search bar ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkTheme) DarkGlass else LightGlass)
                    .border(0.5.dp,
                        if (isDarkTheme) Color.White.copy(0.1f) else Color.White.copy(0.7f),
                        RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToChat)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

            // ── AI Body Orb ───────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                AiBodyOrb(
                    orbState    = orbState,
                    isDarkTheme = isDarkTheme,
                    size        = 220.dp,
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

            Spacer(Modifier.height(28.dp))

            // ── Quick Actions ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(Icons.Default.Chat,   "Chat",   Blue500,             isDarkTheme, onNavigateToChat,  Modifier.weight(1f))
                ActionButton(Icons.Default.Mic,    "Voice",  Color(0xFF8B5CF6),   isDarkTheme, onNavigateToVoice, Modifier.weight(1f))
                ActionButton(Icons.Default.Search, "Search", Color(0xFF06B6D4),   isDarkTheme, onNavigateToChat,  Modifier.weight(1f))
                ActionButton(Icons.Default.Create, "Write",  Color(0xFFF59E0B),   isDarkTheme, onNavigateToChat,  Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))

            // ── Suggestions ───────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SUGGESTIONS", style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                    letterSpacing = 1.5.sp)
                Spacer(Modifier.height(4.dp))
                AITaskCard(Icons.Default.Chat,     "Start a conversation", "Ask Infinity anything",       Blue500,           onNavigateToChat,  darkTheme = isDarkTheme)
                AITaskCard(Icons.Default.Mic,      "Voice command",        "Speak to activate Infinity",  Color(0xFF8B5CF6), onNavigateToVoice, darkTheme = isDarkTheme)
                AITaskCard(Icons.Default.EditNote, "Smart Notes",          "AI-powered note taking",      Color(0xFF10B981), onNavigateToChat,  darkTheme = isDarkTheme)
                AITaskCard(Icons.Default.Terminal, "Run command",          "Execute AI commands",         Color(0xFFF59E0B), onNavigateToChat,  darkTheme = isDarkTheme)
            }

            Spacer(Modifier.height(bottomPadding + 16.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, isDarkTheme: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (isDarkTheme) DarkGlass else LightGlass, CircleShape)
            .border(0.5.dp, if (isDarkTheme) Color.White.copy(0.08f) else Color.White.copy(0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (isDarkTheme) TextPrimary else TextPrimaryLight, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector, label: String, color: Color,
    isDarkTheme: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkTheme) DarkGlass else LightGlass)
            .border(0.5.dp, if (isDarkTheme) Color.White.copy(0.08f) else Color.White.copy(0.7f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
    }
}

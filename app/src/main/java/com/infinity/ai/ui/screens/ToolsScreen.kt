package com.infinity.ai.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.components.GlassCard
import com.infinity.ai.ui.theme.*
import kotlinx.coroutines.launch

private data class Tool(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val available: Boolean = true
)

@Composable
fun ToolsScreen(isDarkTheme: Boolean, bottomPadding: Dp) {
    val tools = listOf(
        Tool("Voice Assistant", "Natural language voice commands", Icons.Default.Mic, Blue500),
        Tool("File Analyzer", "Scan and extract insights from documents", Icons.Default.FolderOpen, Color(0xFF10B981)),
        Tool("Smart Notes", "AI-powered note taking and summarization", Icons.Default.EditNote, Color(0xFFF59E0B)),
        Tool("Smart Commands", "Execute intelligent AI commands", Icons.Default.Terminal, Color(0xFF8B5CF6)),
        Tool("Local AI Model", "Offline LLM inference engine", Icons.Default.Memory, Color(0xFF06B6D4), false),
        Tool("Image Vision", "Analyze and describe images", Icons.Default.Image, Color(0xFFEC4899), false),
    )

    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        GradientBackground(darkTheme = isDarkTheme, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(scroll)) {
            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Tools", style = MaterialTheme.typography.headlineMedium,
                    color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("AI-powered capabilities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("4 Active", Blue500, isDarkTheme, Modifier.weight(1f))
                StatPill("2 Soon", if (isDarkTheme) TextSecondary else TextSecondaryLight, isDarkTheme, Modifier.weight(1f))
                StatPill("∞ Scale", Color(0xFF8B5CF6), isDarkTheme, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Available tools
            Column(modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Available", isDarkTheme)
                tools.filter { it.available }.forEach { tool ->
                    ToolCard(tool, isDarkTheme) {
                        scope.launch {
                            snackbarHostState.showSnackbar("${tool.name} — coming soon with full AI integration")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SectionLabel("Coming Soon", isDarkTheme)
                tools.filter { !it.available }.forEach { tool ->
                    ToolCard(tool, isDarkTheme, onClick = null)
                }
            }

            Spacer(Modifier.height(bottomPadding + 16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, isDarkTheme: Boolean) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall,
        color = if (isDarkTheme) TextSecondary else TextSecondaryLight,
        fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
}

@Composable
private fun StatPill(text: String, color: Color, isDarkTheme: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) DarkGlass else LightGlass)
            .border(0.5.dp,
                if (isDarkTheme) Color.White.copy(0.08f) else Color.White.copy(0.7f),
                RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToolCard(tool: Tool, isDarkTheme: Boolean, onClick: (() -> Unit)? = null) {
    val bg = if (isDarkTheme) DarkGlass else LightGlass
    val border = if (isDarkTheme) Color.White.copy(0.08f) else Color.White.copy(0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(16.dp))
            .then(
                if (onClick != null && tool.available)
                    Modifier.clickable(onClick = onClick)
                else
                    Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp)
                .background(
                    tool.color.copy(if (tool.available) 0.15f else 0.07f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(tool.icon, null,
                tint = tool.color.copy(if (tool.available) 1f else 0.4f),
                modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(tool.name, style = MaterialTheme.typography.bodyLarge,
                color = if (tool.available)
                    if (isDarkTheme) TextPrimary else TextPrimaryLight
                else
                    if (isDarkTheme) TextDisabled else TextSecondaryLight.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(tool.description, style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
        }
        if (!tool.available) {
            Box(
                modifier = Modifier
                    .background(if (isDarkTheme) DarkSurfaceElevated else LightSurfaceElevated,
                        RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Soon", style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) TextDisabled else TextSecondaryLight)
            }
        } else {
            Icon(Icons.Default.ChevronRight, null,
                tint = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                modifier = Modifier.size(18.dp))
        }
    }
}

package com.infinity.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.model.ChatMessage
import com.infinity.ai.ui.components.AiBodyOrb
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.components.AITaskCard
import com.infinity.ai.ui.components.OrbState
import com.infinity.ai.ui.components.toOrbState
import com.infinity.ai.ui.theme.*
import com.infinity.ai.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    isDarkTheme: Boolean,
    bottomPadding: Dp,
    onNavigateToVoice: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages       by chatViewModel.messages.collectAsState()
    val input          by chatViewModel.input.collectAsState()
    val showSuggestions by chatViewModel.showSuggestions.collectAsState()
    val aiState        by chatViewModel.aiState.collectAsState()
    val isExtracting   by chatViewModel.isExtracting.collectAsState()
    val extractProgress by chatViewModel.extractionProgress.collectAsState()
    val listState      = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val isGenerating = aiState is AIInferenceState.Thinking ||
                       aiState is AIInferenceState.Responding

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, isGenerating) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    GradientBackground(darkTheme = isDarkTheme, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomPadding)
                .imePadding()
        ) {
            ChatHeader(
                isDarkTheme  = isDarkTheme,
                aiState      = aiState,
                onClearChat  = { chatViewModel.clearChat() }
            )

            // ── First-launch extraction progress ──────────────────────────────
            AnimatedVisibility(visible = isExtracting) {
                ExtractionProgressBar(progress = extractProgress, isDarkTheme = isDarkTheme)
            }

            // ── Body ──────────────────────────────────────────────────────────
            if (showSuggestions && messages.isEmpty()) {
                EmptyState(
                    modifier    = Modifier.weight(1f),
                    isDarkTheme = isDarkTheme,
                    aiState     = aiState,
                    onSuggestionClick = { chatViewModel.startFromSuggestion(it) }
                )
            } else {
                LazyColumn(
                    state           = listState,
                    modifier        = Modifier.weight(1f),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(msg, isDarkTheme, isStreaming = isGenerating && msg == messages.last() && !msg.isUser)
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            ChatInputBar(
                input        = input,
                isDarkTheme  = isDarkTheme,
                isGenerating = isGenerating,
                onInputChange = { chatViewModel.onInputChange(it) },
                onSend = {
                    chatViewModel.sendMessage()
                    keyboardController?.hide()
                },
                onStop  = { chatViewModel.stopGeneration() },
                onVoice = onNavigateToVoice
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(isDarkTheme: Boolean, aiState: AIInferenceState, onClearChat: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(if (isDarkTheme) DarkGlass else LightGlass)
                .border(0.5.dp,
                    if (isDarkTheme) Color.White.copy(0.1f) else Color.White.copy(0.6f),
                    RoundedCornerShape(50.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live status dot
            val dotColor = when (aiState) {
                is AIInferenceState.Idle      -> Color(0xFF22C55E)
                is AIInferenceState.Loading   -> Color(0xFFF59E0B)
                is AIInferenceState.Thinking  -> Blue500
                is AIInferenceState.Responding -> Blue500
                is AIInferenceState.Error     -> Color(0xFFEF4444)
            }
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
            Text("Infinity AI", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
            Text(
                text = when (aiState) {
                    is AIInferenceState.Idle      -> "Ready"
                    is AIInferenceState.Loading   -> "Loading..."
                    is AIInferenceState.Thinking  -> "Thinking..."
                    is AIInferenceState.Responding -> "Responding..."
                    is AIInferenceState.Error     -> "Error"
                },
                style = MaterialTheme.typography.labelSmall,
                color = dotColor
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = onClearChat,
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Default.Add, "New chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Extraction progress bar (first launch only) ───────────────────────────────

@Composable
private fun ExtractionProgressBar(progress: Float, isDarkTheme: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Setting up AI model...", style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                color = Blue500, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = Blue500,
            trackColor = Blue500.copy(alpha = 0.15f)
        )
        Spacer(Modifier.height(4.dp))
        Text("This only happens once on first launch",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDarkTheme) TextDisabled else TextSecondaryLight.copy(0.6f))
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    aiState: AIInferenceState,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AiBodyOrb(orbState = aiState.toOrbState(), isDarkTheme = isDarkTheme, size = 120.dp)
        }

        Spacer(Modifier.height(20.dp))

        Text("Hey, there!", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light)
        Row {
            Text("How can I ", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light)
            Text("help you?", style = MaterialTheme.typography.headlineMedium,
                color = Blue500, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))

        Text("THINGS YOU CAN DO", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple(Icons.Default.Chat,     "Ask me anything",    "Start a conversation"),
                Triple(Icons.Default.Code,     "Help me write code", "Generate or review code"),
                Triple(Icons.Default.EditNote, "Summarize my notes", "AI-powered summaries"),
                Triple(Icons.Default.Language, "Translate text",     "Any language supported")
            ).forEach { (icon, title, sub) ->
                AITaskCard(icon = icon, title = title, subtitle = sub, iconBg = Blue500,
                    onClick = { onSuggestionClick(title) }, darkTheme = isDarkTheme)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    input: String,
    isDarkTheme: Boolean,
    isGenerating: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit
) {
    val canSend = input.isNotBlank() && !isGenerating

    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Blue500),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text(
                                if (isGenerating) "Generating..." else "Message Infinity...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    maxLines = 5
                )
            }

            // Send / Stop / Voice button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        when {
                            isGenerating -> Color(0xFFEF4444)
                            canSend      -> Blue500
                            else         -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape
                    )
                    .clickable {
                        when {
                            isGenerating -> onStop()
                            canSend      -> onSend()
                            else         -> onVoice()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isGenerating -> Icons.Default.Stop
                        canSend      -> Icons.Default.ArrowUpward
                        else         -> Icons.Default.Mic
                    },
                    contentDescription = when {
                        isGenerating -> "Stop"
                        canSend      -> "Send"
                        else         -> "Voice"
                    },
                    tint = if (canSend || isGenerating) Color.White
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage, isDarkTheme: Boolean, isStreaming: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (!message.isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            ) {
                Box(modifier = Modifier.size(22.dp).background(Blue500.copy(0.15f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("∞", style = MaterialTheme.typography.labelSmall, color = Blue500)
                }
                Text("Infinity", style = MaterialTheme.typography.labelSmall,
                    color = Blue500, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 4.dp,
                    bottomEnd   = if (message.isUser) 4.dp  else 18.dp
                ))
                .background(
                    if (message.isUser) Blue500
                    else if (isDarkTheme) DarkSurfaceElevated else LightSurface
                )
                .then(if (!message.isUser) Modifier.border(
                    0.5.dp,
                    if (isDarkTheme) Color.White.copy(0.08f) else LightBorder,
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                ) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Show streaming cursor when this is the active streaming message
            val displayText = if (isStreaming && message.text.isNotEmpty())
                message.text + "▍" else message.text

            if (message.text.isEmpty() && isStreaming) {
                // Show typing dots while waiting for first token
                TypingDots(isDarkTheme)
            } else {
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// ── Typing dots ───────────────────────────────────────────────────────────────

@Composable
private fun TypingDots(isDarkTheme: Boolean) {
    val inf = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val scale by inf.animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(400, delayMillis = i * 130, easing = EaseInOut), RepeatMode.Reverse
                ), label = "dot$i"
            )
            Box(modifier = Modifier.size((6 * scale).dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), CircleShape))
        }
    }
}

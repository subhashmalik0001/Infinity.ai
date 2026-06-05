package com.infinity.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.data.library.EntryType
import com.infinity.ai.data.library.LibraryRepository
import com.infinity.ai.model.ChatMessage
import com.infinity.ai.ui.components.AiBodyOrb
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.components.AITaskCard
import com.infinity.ai.ui.components.toOrbState
import com.infinity.ai.ui.theme.*
import com.infinity.ai.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    isDarkTheme: Boolean,
    bottomPadding: Dp,
    onNavigateToVoice: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages        by chatViewModel.messages.collectAsState()
    val input           by chatViewModel.input.collectAsState()
    val showSuggestions by chatViewModel.showSuggestions.collectAsState()
    val aiState         by chatViewModel.aiState.collectAsState()
    val isExtracting    by chatViewModel.isExtracting.collectAsState()
    val extractProgress by chatViewModel.extractionProgress.collectAsState()
    val listState       = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context         = LocalContext.current
    val scope           = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isGenerating = aiState is AIInferenceState.Thinking ||
                       aiState is AIInferenceState.Responding

    LaunchedEffect(messages.size, isGenerating) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    // ── Action handlers (UI only — no ViewModel changes) ──────────────────────

    fun copyToClipboard(text: String, label: String = "AI Response") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
    }

    fun shareText(prompt: String, response: String) {
        val body = if (prompt.isNotBlank()) "Prompt:\n$prompt\n\nResponse:\n$response" else response
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun saveToVault(context: Context, prompt: String, response: String) {
        scope.launch(Dispatchers.IO) {
            val repo = LibraryRepository.getInstance(context)
            repo.save(
                type       = EntryType.NOTE,
                content    = if (prompt.isNotBlank()) "Q: $prompt\n\nA: $response" else response,
                title      = prompt.take(60).ifBlank { "Chat Response" },
                sourceInfo = "Infinity Chat"
            )
            scope.launch { snackbarHostState.showSnackbar("Saved to Knowledge Vault") }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = if (isDarkTheme) DarkSurfaceElevated else LightSurface,
                    contentColor    = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                    shape           = RoundedCornerShape(12.dp),
                    modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        GradientBackground(darkTheme = isDarkTheme, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = maxOf(bottomPadding, innerPadding.calculateBottomPadding()))
                    .imePadding()
            ) {
                ChatHeader(
                    isDarkTheme = isDarkTheme,
                    aiState     = aiState,
                    onClearChat = { chatViewModel.clearChat() }
                )

                AnimatedVisibility(visible = isExtracting) {
                    ExtractionProgressBar(progress = extractProgress, isDarkTheme = isDarkTheme)
                }

                if (showSuggestions && messages.isEmpty()) {
                    EmptyState(
                        modifier          = Modifier.weight(1f),
                        isDarkTheme       = isDarkTheme,
                        aiState           = aiState,
                        onSuggestionClick = { chatViewModel.startFromSuggestion(it) }
                    )
                } else {
                    // Find the prompt paired with each AI message (the user message just before it)
                    val promptMap = remember(messages) {
                        buildMap {
                            messages.forEachIndexed { i, msg ->
                                if (!msg.isUser) {
                                    val prev = messages.getOrNull(i - 1)
                                    put(msg.id, prev?.text ?: "")
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state           = listState,
                        modifier        = Modifier.weight(1f),
                        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val streaming = isGenerating && msg == messages.last() && !msg.isUser
                            ChatBubble(
                                message     = msg,
                                isDarkTheme = isDarkTheme,
                                isStreaming = streaming,
                                prompt      = promptMap[msg.id] ?: "",
                                onCopy      = { copyToClipboard(msg.text) },
                                onShare     = { shareText(promptMap[msg.id] ?: "", msg.text) },
                                onSave      = { saveToVault(context, promptMap[msg.id] ?: "", msg.text) },
                                onRegenerate = {
                                    val p = promptMap[msg.id] ?: ""
                                    if (p.isNotBlank()) chatViewModel.startFromSuggestion(p)
                                }
                            )
                        }
                    }
                }

                ChatInputBar(
                    input         = input,
                    isDarkTheme   = isDarkTheme,
                    isGenerating  = isGenerating,
                    onInputChange = { chatViewModel.onInputChange(it) },
                    onSend        = { chatViewModel.sendMessage(); keyboardController?.hide() },
                    onStop        = { chatViewModel.stopGeneration() },
                    onVoice       = onNavigateToVoice
                )
            }
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
            val dotColor = when (aiState) {
                is AIInferenceState.Idle       -> Color(0xFF22C55E)
                is AIInferenceState.Loading    -> Color(0xFFF59E0B)
                is AIInferenceState.Thinking   -> Blue500
                is AIInferenceState.Responding -> Blue500
                is AIInferenceState.Error      -> Color(0xFFEF4444)
            }
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
            Text("Infinity AI", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
            Text(
                text = when (aiState) {
                    is AIInferenceState.Idle       -> "Ready"
                    is AIInferenceState.Loading    -> "Loading..."
                    is AIInferenceState.Thinking   -> "Thinking..."
                    is AIInferenceState.Responding -> "Responding..."
                    is AIInferenceState.Error      -> "Error"
                },
                style = MaterialTheme.typography.labelSmall,
                color = dotColor
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick  = onClearChat,
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Default.Add, "New chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Extraction progress bar ───────────────────────────────────────────────────

@Composable
private fun ExtractionProgressBar(progress: Float, isDarkTheme: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Setting up AI model...", style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                color = Blue500, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress        = { progress },
            modifier        = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color           = Blue500,
            trackColor      = Blue500.copy(alpha = 0.15f)
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
                    value         = input,
                    onValueChange = onInputChange,
                    modifier      = Modifier.weight(1f),
                    enabled       = !isGenerating,
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp
                    ),
                    cursorBrush   = SolidColor(Blue500),
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
                        imeAction       = ImeAction.Send,
                        capitalization  = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    maxLines = 5
                )
            }
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
                    tint     = if (canSend || isGenerating) Color.White
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message:      ChatMessage,
    isDarkTheme:  Boolean,
    isStreaming:  Boolean,
    prompt:       String,
    onCopy:       () -> Unit,
    onShare:      () -> Unit,
    onSave:       () -> Unit,
    onRegenerate: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        // ── AI label row ──────────────────────────────────────────────────────
        if (!message.isUser) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.padding(bottom = 4.dp, start = 2.dp)
            ) {
                Box(
                    modifier           = Modifier.size(22.dp).background(Blue500.copy(0.15f), CircleShape),
                    contentAlignment   = Alignment.Center
                ) { Text("∞", style = MaterialTheme.typography.labelSmall, color = Blue500) }
                Text("Infinity", style = MaterialTheme.typography.labelSmall,
                    color = Blue500, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Bubble + action row ───────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier              = Modifier.fillMaxWidth()
        ) {
            // For AI messages: bubble first, actions after
            if (!message.isUser) {
                AiBubbleContent(
                    message     = message,
                    isDarkTheme = isDarkTheme,
                    isStreaming = isStreaming,
                    modifier    = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 300.dp)
                        .combinedClickable(
                            onClick      = {},
                            onLongClick  = { if (!isStreaming && message.text.isNotBlank()) showSheet = true }
                        )
                )

                // Inline action icons — only shown after streaming is done
                AnimatedVisibility(
                    visible = !isStreaming && message.text.isNotBlank(),
                    enter   = fadeIn(tween(200)) + slideInHorizontally { it / 2 },
                    exit    = fadeOut(tween(150))
                ) {
                    Row(
                        modifier              = Modifier.padding(start = 6.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BubbleActionIcon(Icons.Default.ContentCopy, "Copy",    onCopy)
                        BubbleActionIcon(Icons.Default.Share,       "Share",   onShare)
                        BubbleActionIcon(Icons.Default.BookmarkAdd, "Save",    onSave)
                    }
                }
            } else {
                // User bubble — no actions
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                        .background(Blue500)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(message.text,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = Color.White,
                        lineHeight = 22.sp)
                }
            }
        }
    }

    // ── Long-press bottom sheet ───────────────────────────────────────────────
    if (showSheet) {
        MessageActionsSheet(
            onDismiss    = { showSheet = false },
            onCopy       = { showSheet = false; onCopy() },
            onShare      = { showSheet = false; onShare() },
            onSave       = { showSheet = false; onSave() },
            onRegenerate = { showSheet = false; onRegenerate() }
        )
    }
}

// ── AI bubble content (text selection + code blocks) ─────────────────────────

@Composable
private fun AiBubbleContent(
    message:     ChatMessage,
    isDarkTheme: Boolean,
    isStreaming: Boolean,
    modifier:    Modifier = Modifier
) {
    val bubbleBg = if (isDarkTheme) DarkSurfaceElevated else LightSurface
    val border   = if (isDarkTheme) Color.White.copy(0.08f) else LightBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
            .background(bubbleBg)
            .border(0.5.dp, border, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (message.text.isEmpty() && isStreaming) {
            TypingDots(isDarkTheme)
        } else {
            val segments = remember(message.text) { parseMessageSegments(message.text) }
            val displayText = if (isStreaming && message.text.isNotEmpty()) message.text + "▍"
                              else message.text

            if (segments.size == 1 && segments[0] is MessageSegment.PlainText) {
                // Fast path: no code blocks — use SelectionContainer directly
                SelectionContainer {
                    Text(
                        text      = if (isStreaming && message.text.isNotEmpty())
                                        buildAnnotatedString { append(displayText) }
                                    else buildAnnotatedString { append(message.text) },
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            } else {
                // Has code blocks
                val context = LocalContext.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    segments.forEach { seg ->
                        when (seg) {
                            is MessageSegment.PlainText -> {
                                if (seg.text.isNotBlank()) {
                                    SelectionContainer {
                                        Text(
                                            text       = seg.text,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            color      = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                            is MessageSegment.CodeBlock -> {
                                CodeBlockView(
                                    code        = seg.code,
                                    language    = seg.language,
                                    isDarkTheme = isDarkTheme,
                                    onCopyCode  = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Code", seg.code))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Code block view ───────────────────────────────────────────────────────────

@Composable
private fun CodeBlockView(
    code:       String,
    language:   String,
    isDarkTheme: Boolean,
    onCopyCode: () -> Unit
) {
    val codeBg = if (isDarkTheme) Color(0xFF1A1D23) else Color(0xFFF3F4F6)
    var copied by remember { mutableStateOf(false) }
    val scope  = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(codeBg)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) Color(0xFF252830) else Color(0xFFE5E7EB))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                language.ifBlank { "code" },
                style     = MaterialTheme.typography.labelSmall,
                color     = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
            TextButton(
                onClick      = {
                    onCopyCode()
                    copied = true
                    scope.launch { delay(2000); copied = false }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    null,
                    tint     = if (copied) Color(0xFF22C55E) else Blue500,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (copied) "Copied!" else "Copy code",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) Color(0xFF22C55E) else Blue500
                )
            }
        }
        // Code body
        SelectionContainer {
            Text(
                text      = code,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState()),
                style     = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color     = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1F2937),
                lineHeight = 20.sp
            )
        }
    }
}

// ── Inline action icon button ─────────────────────────────────────────────────

@Composable
private fun BubbleActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick  = onClick,
        modifier = Modifier.size(30.dp)
    ) {
        Icon(
            icon, label,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(15.dp)
        )
    }
}

// ── Long-press bottom sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    onDismiss:    () -> Unit,
    onCopy:       () -> Unit,
    onShare:      () -> Unit,
    onSave:       () -> Unit,
    onRegenerate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = MaterialTheme.colorScheme.surface,
        dragHandle        = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                "Message Actions",
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            Spacer(Modifier.height(4.dp))

            SheetAction(Icons.Default.ContentCopy, "Copy",                   "Copy full response",            onCopy)
            SheetAction(Icons.Default.Share,       "Share",                  "Send via WhatsApp, Gmail, etc.", onShare)
            SheetAction(Icons.Default.BookmarkAdd, "Save to Knowledge Vault","Store for offline access",       onSave)
            SheetAction(Icons.Default.Refresh,     "Regenerate",             "Generate a new response",        onRegenerate)
            SheetAction(Icons.Default.TextFields,  "Select Text",            "Long-press text to select",      onDismiss)
        }
    }
}

@Composable
private fun SheetAction(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .background(Blue500.copy(0.10f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ── Markdown code block parser ────────────────────────────────────────────────

private sealed interface MessageSegment {
    data class PlainText(val text: String) : MessageSegment
    data class CodeBlock(val language: String, val code: String) : MessageSegment
}

/**
 * Splits message text into plain text and fenced code block segments.
 * Handles ``` with optional language tag. No external library needed.
 */
private fun parseMessageSegments(text: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val regex    = Regex("```(\\w*)\\n?([\\s\\S]*?)```", RegexOption.MULTILINE)
    var cursor   = 0

    for (match in regex.findAll(text)) {
        if (match.range.first > cursor) {
            segments += MessageSegment.PlainText(text.substring(cursor, match.range.first))
        }
        val lang = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd('\n')
        segments += MessageSegment.CodeBlock(lang, code)
        cursor = match.range.last + 1
    }

    if (cursor < text.length) {
        segments += MessageSegment.PlainText(text.substring(cursor))
    }

    return if (segments.isEmpty()) listOf(MessageSegment.PlainText(text)) else segments
}

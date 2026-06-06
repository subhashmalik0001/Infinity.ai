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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.data.library.EntryType
import com.infinity.ai.data.library.LibraryRepository
import com.infinity.ai.R
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
    onNavigateBack: () -> Unit,
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
    var isThinkingActive by remember { mutableStateOf(false) }
    var isDeepResearchActive by remember { mutableStateOf(false) }
    var selectedImageName by remember { mutableStateOf<String?>(null) }
    var selectedDocName by remember { mutableStateOf<String?>(null) }

    val isGenerating = aiState is AIInferenceState.Thinking ||
                       aiState is AIInferenceState.Responding

    LaunchedEffect(messages.size, isGenerating, messages.lastOrNull()?.text?.length) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) {
            if (isGenerating) {
                listState.scrollToItem(count - 1) // Snap scroll for immediate updates during streaming
            } else {
                listState.animateScrollToItem(count - 1) // Smooth scroll when new messages are added or generation stops
            }
        }
    }

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
                type       = EntryType.CHAT,
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
                    containerColor  = TextPrimary,
                    contentColor    = LightSurface,
                    shape           = RoundedCornerShape(12.dp),
                    modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        val density = LocalDensity.current
        val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
        val calculatedBottomPadding = if (isKeyboardOpen) 0.dp else maxOf(bottomPadding, innerPadding.calculateBottomPadding())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .statusBarsPadding()
                .padding(bottom = calculatedBottomPadding)
                .imePadding()
        ) {
            // ── Global Header Row (Back on left, 3-dots option on right) ──────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3-dots Menu Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable { chatViewModel.clearChat() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        "Options",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExtracting) {
                ExtractionProgressBar(progress = extractProgress, isDarkTheme = false)
            }

            if (messages.isEmpty()) {
                CenteredEmptyState(
                    modifier = Modifier.weight(1f),
                    input = input,
                    onInputChange = { chatViewModel.onInputChange(it) },
                    onSend = { chatViewModel.sendMessage(); keyboardController?.hide() },
                    onVoice = onNavigateToVoice,
                    onSuggestionClick = { chatViewModel.startFromSuggestion(it) },
                    isDarkTheme = false,
                    isThinkingActive = isThinkingActive,
                    onThinkingToggle = { isThinkingActive = it },
                    isDeepResearchActive = isDeepResearchActive,
                    onDeepResearchToggle = { isDeepResearchActive = it },
                    selectedImageName = selectedImageName,
                    onImageSelect = { selectedImageName = it },
                    selectedDocName = selectedDocName,
                    onDocSelect = { selectedDocName = it }
                )
            } else {
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
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Optional Date Header
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardWhite)
                                    .border(0.8.dp, BorderLight, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Today, 09:00 PM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        val streaming = isGenerating && msg == messages.last() && !msg.isUser
                        ChatBubble(
                            message      = msg,
                            isDarkTheme  = false,
                            isStreaming  = streaming,
                            prompt       = promptMap[msg.id] ?: "",
                            onCopy       = { copyToClipboard(msg.text) },
                            onShare      = { shareText(promptMap[msg.id] ?: "", msg.text) },
                            onSave       = { saveToVault(context, promptMap[msg.id] ?: "", msg.text) },
                            onRegenerate = {
                                val p = promptMap[msg.id] ?: ""
                                if (p.isNotBlank()) chatViewModel.startFromSuggestion(p)
                            }
                        )
                    }
                }

                // ChatInputBar
                ChatInputBar(
                    input         = input,
                    isDarkTheme   = false,
                    isGenerating  = isGenerating,
                    onInputChange = { chatViewModel.onInputChange(it) },
                    onSend        = { chatViewModel.sendMessage(); keyboardController?.hide() },
                    onStop        = { chatViewModel.stopGeneration() },
                    onVoice       = onNavigateToVoice,
                    isThinkingActive = isThinkingActive,
                    onThinkingToggle = { isThinkingActive = it },
                    isDeepResearchActive = isDeepResearchActive,
                    onDeepResearchToggle = { isDeepResearchActive = it },
                    selectedImageName = selectedImageName,
                    onImageSelect = { selectedImageName = it },
                    selectedDocName = selectedDocName,
                    onDocSelect = { selectedDocName = it }
                )
            }
        }
    }
}

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
            Text(
                "Setting up AI model…",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = BluePrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress        = { progress },
            modifier        = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color           = BluePrimary,
            trackColor      = Blue50
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "This only happens once on first launch",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    aiState: AIInferenceState,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // App logo (large infinity symbol)
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFFDD9F0B), Color(0xFFFBBF24))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Infinity Logo",
                modifier = Modifier.size(width = 80.dp, height = 44.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "How can I help you today?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // Model selector pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "AI Chat",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Quick action chips
        val chips = listOf(
            Pair(Icons.Default.Edit, "Write"),
            Pair(Icons.Default.School, "Learn"),
            Pair(Icons.Default.Code, "Code"),
            Pair(Icons.Default.Folder, "File Analyze")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            chips.forEach { (icon, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardWhite)
                        .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                        .clickable { onSuggestionClick(label) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .clickable { onSuggestionClick("Create Image") }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Create Image",
                    tint = Color(0xFFE040FB),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Create Image",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // View History link
        Text(
            "View History",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = BluePrimary,
            modifier = Modifier.clickable { }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    isDarkTheme: Boolean,
    isGenerating: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit,
    isThinkingActive: Boolean,
    onThinkingToggle: (Boolean) -> Unit,
    isDeepResearchActive: Boolean,
    onDeepResearchToggle: (Boolean) -> Unit,
    selectedImageName: String?,
    onImageSelect: (String?) -> Unit,
    selectedDocName: String?,
    onDocSelect: (String?) -> Unit
) {
    val canSend = input.isNotBlank() && !isGenerating

    // Main input card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Active attachments preview list
            if (selectedImageName != null || selectedDocName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedImageName?.let { img ->
                        AttachmentPill(
                            name = img,
                            icon = Icons.Default.Image,
                            onClose = { onImageSelect(null) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                    selectedDocName?.let { doc ->
                        AttachmentPill(
                            name = doc,
                            icon = Icons.Default.Description,
                            onClose = { onDocSelect(null) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            // Text input area
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp, max = 120.dp),
                enabled = !isGenerating,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(BluePrimary),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text(
                            if (isGenerating) "Generating…" else "How can I help you today?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary.copy(alpha = 0.55f)
                        )
                    }
                    inner()
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() })
            )

            Spacer(Modifier.height(12.dp))

            // Bottom row: Plus | Model label | Voice | Send
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Plus button
                var showMenu by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(if (isDarkTheme) Color(0xFF1E1E24) else Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Image", color = if (isDarkTheme) Color.White else Color(0xFF0F172A)) },
                            leadingIcon = { Icon(Icons.Default.Image, "Image", tint = BluePrimary) },
                            onClick = {
                                showMenu = false
                                onImageSelect("photo_attachment.jpg")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Document", color = if (isDarkTheme) Color.White else Color(0xFF0F172A)) },
                            leadingIcon = { Icon(Icons.Default.Description, "Document", tint = BluePrimary) },
                            onClick = {
                                showMenu = false
                                onDocSelect("document_attachment.pdf")
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Thinking Mode", color = if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                    Spacer(Modifier.width(16.dp))
                                    Switch(
                                        checked = isThinkingActive,
                                        onCheckedChange = { onThinkingToggle(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = BluePrimary)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Lightbulb, "Thinking", tint = BluePrimary) },
                            onClick = {
                                showMenu = false
                                onThinkingToggle(!isThinkingActive)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Deep Research", color = if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                    Spacer(Modifier.width(16.dp))
                                    Switch(
                                        checked = isDeepResearchActive,
                                        onCheckedChange = { onDeepResearchToggle(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = BluePrimary)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Search, "Research", tint = BluePrimary) },
                            onClick = {
                                showMenu = false
                                onDeepResearchToggle(!isDeepResearchActive)
                            }
                        )
                    }
                }

                // Model label in center
                val modelText = remember(isThinkingActive, isDeepResearchActive) {
                    when {
                        isThinkingActive && isDeepResearchActive -> "AI Chat · Deep + Thinking"
                        isThinkingActive -> "AI Chat · Thinking"
                        isDeepResearchActive -> "AI Chat · Deep Research"
                        else -> "AI Chat"
                    }
                }
                Text(
                    modelText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary.copy(alpha = 0.7f)
                )

                // Right action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Voice / Stop / Send
                    if (isGenerating) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                "Stop",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (canSend) {
                        IconButton(
                            onClick = onSend,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onVoice,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                "Voice",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message     : ChatMessage,
    isDarkTheme : Boolean,
    isStreaming : Boolean,
    prompt      : String,
    onCopy      : () -> Unit,
    onShare     : () -> Unit,
    onSave      : () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Blue50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("∞", style = MaterialTheme.typography.labelSmall, color = BluePrimary, fontSize = 10.sp)
                }
                Text(
                    "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = BluePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUser) {
                // User Bubble
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = 16.dp, bottomEnd = 2.dp
                        ))
                        .background(BluePrimary)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                }
            } else {
                // Assistant Bubble Card (with divider and inner action row)
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(
                            topStart = 2.dp, topEnd = 16.dp,
                            bottomStart = 16.dp, bottomEnd = 16.dp
                        ))
                        .background(CardWhite)
                        .border(
                            0.8.dp,
                            BorderLight,
                            RoundedCornerShape(2.dp, 16.dp, 16.dp, 16.dp)
                        )
                ) {
                    Column {
                        // Message text / Code blocks
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            if (message.text.isEmpty() && isStreaming) {
                                TypingDots(isDarkTheme)
                            } else {
                                val segments = remember(message.text) { parseMessageSegments(message.text) }
                                if (segments.size == 1 && segments[0] is MessageSegment.PlainText) {
                                    SelectionContainer {
                                        Text(
                                            text = message.text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            lineHeight = 22.sp
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        segments.forEach { seg ->
                                            when (seg) {
                                                is MessageSegment.PlainText -> {
                                                    if (seg.text.isNotBlank()) {
                                                        SelectionContainer {
                                                            Text(
                                                                text = seg.text,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = TextPrimary,
                                                                lineHeight = 22.sp
                                                            )
                                                        }
                                                    }
                                                }
                                                is MessageSegment.CodeBlock -> {
                                                    CodeBlockView(
                                                        code = seg.code,
                                                        language = seg.language,
                                                        isDarkTheme = isDarkTheme,
                                                        onCopyCode = onCopy
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Divider and Inline Action Icons
                        if (!isStreaming && message.text.isNotEmpty()) {
                            HorizontalDivider(color = BorderLight, thickness = 0.8.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.ContentCopy, "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Bookmark, "Save", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { /* Liked */ }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.ThumbUp, "Like", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { /* Speak */ }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.VolumeUp, "Speak", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Refresh, "Regenerate", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(
    code        : String,
    language    : String,
    isDarkTheme : Boolean,
    onCopyCode  : () -> Unit
) {
    val codeBg   = Color(0xFFF6F8FA)
    val headerBg = Color(0xFFEAECF0)
    var copied   by remember { mutableStateOf(false) }
    val scope    = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD8DEE4), RoundedCornerShape(12.dp))
            .background(codeBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(7.dp).background(Color(0xFFFF5F56), CircleShape))
                Box(modifier = Modifier.size(7.dp).background(Color(0xFFFFBD2E), CircleShape))
                Box(modifier = Modifier.size(7.dp).background(Color(0xFF27C93F), CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(
                    language.ifBlank { "code" },
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color(0xFF57606A),
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(
                onClick        = {
                    onCopyCode()
                    copied = true
                    scope.launch { delay(2000); copied = false }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    null,
                    tint     = if (copied) SuccessGreen else BluePrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (copied) "Copied!" else "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) SuccessGreen else BluePrimary
                )
            }
        }
        SelectionContainer {
            Text(
                text      = code,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .horizontalScroll(rememberScrollState()),
                style     = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color     = Color(0xFF1F2328),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TypingDots(isDarkTheme: Boolean) {
    val inf = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val scale by inf.animateFloat(
                initialValue = 0.5f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(380, delayMillis = i * 120, easing = EaseInOut),
                    RepeatMode.Reverse
                ), label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size((5 * scale).dp)
                    .background(
                        TextSecondary.copy(0.4f),
                        CircleShape
                    )
            )
        }
    }
}

private sealed interface MessageSegment {
    data class PlainText(val text: String) : MessageSegment
    data class CodeBlock(val language: String, val code: String) : MessageSegment
}

private fun parseMessageSegments(text: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val regex    = Regex("```(\\w*)\\n?([\\s\\S]*?)```", RegexOption.MULTILINE)
    var cursor   = 0

    for (match in regex.findAll(text)) {
        if (match.range.first > cursor) {
            segments += MessageSegment.PlainText(text.substring(cursor, match.range.first))
        }
        segments += MessageSegment.CodeBlock(
            match.groupValues[1].trim(),
            match.groupValues[2].trimEnd('\n')
        )
        cursor = match.range.last + 1
    }

    if (cursor < text.length) segments += MessageSegment.PlainText(text.substring(cursor))
    return if (segments.isEmpty()) listOf(MessageSegment.PlainText(text)) else segments
}

@Composable
private fun CenteredEmptyState(
    modifier: Modifier = Modifier,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    isDarkTheme: Boolean,
    isThinkingActive: Boolean,
    onThinkingToggle: (Boolean) -> Unit,
    isDeepResearchActive: Boolean,
    onDeepResearchToggle: (Boolean) -> Unit,
    selectedImageName: String?,
    onImageSelect: (String?) -> Unit,
    selectedDocName: String?,
    onDocSelect: (String?) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // 1. Gradient infinity loop logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Infinity Logo",
            modifier = Modifier.size(width = 140.dp, height = 76.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(10.dp))

        // 2. Dropdown selector pill
        Box(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "AI Chat - Offline",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF475569)
                )
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Recently Search Section (Shifted to above the Chatbox)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0xFF94A3B8).copy(alpha = 0.10f), spotColor = Color(0xFF1E293B).copy(alpha = 0.08f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recently visited",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(0.8.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                // Item 1
                RecentSearchItem(
                    iconResId = R.drawable.chat,
                    title = "Write a Python function to check prime",
                    subtitle = "AI Chat",
                    timeText = "Changed an hour ago",
                    isStarred = true
                )

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                // Item 2
                RecentSearchItem(
                    iconResId = R.drawable.learn,
                    title = "Social media schedule",
                    subtitle = "Main workspace",
                    timeText = "Changed an hour ago",
                    isStarred = false
                )
            }
        }

        // Spacer weight to push the remaining elements (chatbox input, options, history) to the bottom
        Spacer(Modifier.weight(1f))

        // 4. Compact text input card with 3D shadow effect (height decreased slightly)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0xFF64748B).copy(alpha = 0.12f), spotColor = Color(0xFF1E293B).copy(alpha = 0.10f))
                .shadow(4.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0xFF94A3B8).copy(alpha = 0.08f), spotColor = Color(0xFF475569).copy(alpha = 0.06f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Active attachments preview list
                if (selectedImageName != null || selectedDocName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        selectedImageName?.let { img ->
                            AttachmentPill(
                                name = img,
                                icon = Icons.Default.Image,
                                onClose = { onImageSelect(null) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                        selectedDocName?.let { doc ->
                            AttachmentPill(
                                name = doc,
                                icon = Icons.Default.Description,
                                onClose = { onDocSelect(null) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }

                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp, max = 90.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF1E293B),
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(BluePrimary),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text(
                                "How can I help you today?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        inner()
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (input.isNotBlank()) onSend() })
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Plus icon
                    var showMenu by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "Add", tint = Color(0xFF475569), modifier = Modifier.size(18.dp))

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(if (isDarkTheme) Color(0xFF1E1E24) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Image", color = if (isDarkTheme) Color.White else Color(0xFF0F172A)) },
                                leadingIcon = { Icon(Icons.Default.Image, "Image", tint = BluePrimary) },
                                onClick = {
                                    showMenu = false
                                    onImageSelect("photo_attachment.jpg")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Document", color = if (isDarkTheme) Color.White else Color(0xFF0F172A)) },
                                leadingIcon = { Icon(Icons.Default.Description, "Document", tint = BluePrimary) },
                                onClick = {
                                    showMenu = false
                                    onDocSelect("document_attachment.pdf")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Thinking Mode", color = if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                        Spacer(Modifier.width(16.dp))
                                        Switch(
                                            checked = isThinkingActive,
                                            onCheckedChange = { onThinkingToggle(it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = BluePrimary)
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Lightbulb, "Thinking", tint = BluePrimary) },
                                onClick = {
                                    showMenu = false
                                    onThinkingToggle(!isThinkingActive)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Deep Research", color = if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                        Spacer(Modifier.width(16.dp))
                                        Switch(
                                            checked = isDeepResearchActive,
                                            onCheckedChange = { onDeepResearchToggle(it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = BluePrimary)
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Search, "Research", tint = BluePrimary) },
                                onClick = {
                                    showMenu = false
                                    onDeepResearchToggle(!isDeepResearchActive)
                                }
                            )
                        }
                    }

                    // Center label
                    val modelText = remember(isThinkingActive, isDeepResearchActive) {
                        when {
                            isThinkingActive && isDeepResearchActive -> "AI Chat · Deep + Thinking"
                            isThinkingActive -> "AI Chat · Thinking"
                            isDeepResearchActive -> "AI Chat · Deep Research"
                            else -> "AI Chat"
                        }
                    }
                    Text(
                        modelText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF94A3B8)
                    )

                    // Right: Mic + wave or Send
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (input.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary)
                                    .clickable { onSend() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Icon(Icons.Default.Mic, "Voice", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp).clickable { onVoice() })
                            // Wave bars
                            Row(
                                modifier = Modifier.height(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(7.dp, 12.dp, 9.dp, 14.dp, 10.dp, 5.dp).forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(h)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color(0xFF94A3B8))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 5. Action chips - Row 1 (Write, Learn, Code)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            GlassyChip(icon = Icons.Default.Edit, label = "Write") { onSuggestionClick("Write") }
            GlassyChipWithImage(drawableRes = R.drawable.learn, label = "Learn") { onSuggestionClick("Learn") }
            GlassyChip(icon = Icons.Default.Code, label = "</> Code") { onSuggestionClick("Code") }
        }

        Spacer(Modifier.height(8.dp))

        // Row 2 (File Analyzer + Create Image)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            GlassyChipWithImage(drawableRes = R.drawable.file, label = "File Analyzer") { onSuggestionClick("File Analyzer") }
            GlassyChipWithImage(drawableRes = R.drawable.image, label = "Create Image") { onSuggestionClick("Create Image") }
        }

        Spacer(Modifier.height(16.dp))

        // 6. View History
        Text(
            "View History",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = BluePrimary,
            modifier = Modifier.clickable { }
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ── Glassy chip with vector icon ─────────────────────────────────────────────
@Composable
private fun GlassyChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color(0xFF94A3B8).copy(alpha = 0.15f), spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, label, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1E293B)
            )
        }
    }
}

// ── Glassy chip with drawable image icon ─────────────────────────────────────
@Composable
private fun GlassyChipWithImage(
    drawableRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color(0xFF94A3B8).copy(alpha = 0.15f), spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun RecentSearchItem(
    iconResId: Int,
    title: String,
    subtitle: String,
    timeText: String,
    isStarred: Boolean,
    onClick: () -> Unit = {}
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
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }

        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Starred status",
            tint = if (isStarred) Color(0xFFFFB020) else Color(0xFFC4CDD5),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AttachmentPill(
    name: String,
    icon: ImageVector,
    onClose: () -> Unit,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) Color(0xFF2E2E3A) else Color(0xFFF1F5F9))
            .border(0.8.dp, if (isDarkTheme) Color(0xFF47475A) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                maxLines = 1
            )
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClose() }
            )
        }
    }
}

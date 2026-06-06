package com.infinity.ai.circle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.ui.res.painterResource
import com.infinity.ai.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Blue500    = Color(0xFFDD9F0B)
private val Purple500  = Color(0xFFFBBF24)
private val Green500   = Color(0xFF10B981)
private val Amber500   = Color(0xFFF59E0B)
private val Red500     = Color(0xFFEF4444)

/**
 * CircleLearnBottomSheetHost
 *
 * Root composable for CircleLearnActivity after OCR completes.
 * Reacts to CircleUiState — shows the right panel for each state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleLearnBottomSheetHost(
    vm          : CircleLearnViewModel,
    onDismiss   : () -> Unit,
    onOpenInApp : ((String?) -> Unit)? = null   // null when called from Activity (already in app)
) {
    val uiState     by vm.uiState.collectAsState()
    val ocrText     by vm.ocrText.collectAsState()
    val resultText  by vm.resultText.collectAsState()
    val detection   by vm.detection.collectAsState()
    val savedBanner by vm.savedToVault.collectAsState()

    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xD9121214) else Color(0xD9FFFFFF)
    val borderStrokeColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(indication = null, interactionSource = remember {
                MutableInteractionSource()
            }) { /* absorb touches outside sheet */ }
    ) {
        // Bottom sheet panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            borderStrokeColor,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .navigationBarsPadding()
        ) {
            // Drag handle
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDarkTheme) Color.White.copy(0.2f) else Color.Black.copy(0.1f)))
            }

            when (val s = uiState) {
                is CircleUiState.Idle, is CircleUiState.Processing ->
                    ProcessingPanel(isDarkTheme)

                is CircleUiState.OcrDone ->
                    ActionPanel(
                        ocrText   = ocrText,
                        detection = detection,
                        onAction  = { vm.runAction(it) },
                        onDismiss = onDismiss,
                        isDarkTheme = isDarkTheme
                    )

                is CircleUiState.Generating ->
                    ResultPanel(
                        resultText  = resultText,
                        actionLabel = s.action.label,
                        isStreaming = true,
                        onStop      = { vm.stop() },
                        onSave      = { vm.saveToVault() },
                        onDismiss   = onDismiss,
                        isDarkTheme = isDarkTheme
                    )

                is CircleUiState.Done ->
                    ResultPanel(
                        resultText  = resultText,
                        actionLabel = s.action.label,
                        isStreaming = false,
                        onStop      = {},
                        onSave      = { vm.saveToVault() },
                        onDismiss   = onDismiss,
                        onOpenInApp = onOpenInApp,
                        onRunAnother = { vm.reset() },
                        isDarkTheme = isDarkTheme
                    )

                is CircleUiState.Error ->
                    ErrorPanel(message = s.message, onDismiss = onDismiss, onRetry = { vm.reset() }, isDarkTheme = isDarkTheme)
            }
        }

        // "Saved to Vault" banner
        AnimatedVisibility(
            visible = savedBanner,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Green500)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Saved to Library", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

// ── Processing panel ──────────────────────────────────────────────────────────

@Composable
private fun ProcessingPanel(isDarkTheme: Boolean) {
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp)
                .background(
                    Brush.sweepGradient(listOf(Blue500, Purple500, Blue500)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Infinity Logo",
                modifier = Modifier.size(width = 56.dp, height = 31.dp)
            )
        }
        Text("Reading your selection…", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimaryColor, textAlign = TextAlign.Center)
        Text("OCR in progress", style = MaterialTheme.typography.bodySmall,
            color = textSecondaryColor)
        Spacer(Modifier.height(8.dp))
    }
}

// ── Action panel ──────────────────────────────────────────────────────────────

@Composable
private fun ActionPanel(
    ocrText  : String,
    detection: ContentTypeDetector.DetectionResult?,
    onAction : (CircleAction) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val borderStrokeColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF17171C) else Color(0xFFFFFFFF)

    val primaryActions = detection?.primaryActions
        ?: listOf(
            CircleAction.EXPLAIN,
            CircleAction.SUMMARIZE,
            CircleAction.NOTES,
            CircleAction.TRANSLATE,
            CircleAction.QUIZ,
            CircleAction.FLASHCARDS
        )

    var selectedAction by remember { mutableStateOf(primaryActions.firstOrNull() ?: CircleAction.EXPLAIN) }
    var isAutoSaveEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Header Row (matching mockup header "Create a room" layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left circular button (Edit/Pen icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color.White.copy(0.08f) else Color(0xFFF1F5F9))
                        .clickable { /* edit/reset action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit selection",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center Title Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Circle Learn",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = textPrimaryColor
                    )
                    Text(
                        text = "Choose how to learn",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = textSecondaryColor
                    )
                }

                // Right circular button (Trash icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color.White.copy(0.08f) else Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Dismiss",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Selection Details Card (matches "Room name" and "Add description" card layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 0.dp else 4.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                        spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBackgroundColor)
                    .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    // Row 1: Selection text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* open full text view or copy */ }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFFDD9F0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selection",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = if (ocrText.isNotBlank()) {
                                    if (ocrText.length > 50) ocrText.take(50) + "…" else ocrText
                                } else "No selection captured",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = textSecondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = borderStrokeColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))

                    // Row 2: Target Language
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* change language action */ }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color(0xFFDD9F0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target Language",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = "English (Auto-detect)",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = textSecondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3. Suggested Action Avatars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First avatar item: "+ More"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { /* action list or settings */ }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(cardBackgroundColor)
                            .border(1.dp, borderStrokeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "More features",
                            tint = textPrimaryColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = textPrimaryColor
                        )
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = textSecondaryColor
                        )
                    }
                }

                // Avatars representing actions
                primaryActions.forEach { action ->
                    val isSelected = selectedAction == action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { selectedAction = action }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFDD9F0B).copy(alpha = 0.15f) else (if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFDD9F0B) else borderStrokeColor,
                                    shape = CircleShape
                                )
                                .padding(if (isSelected) 3.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(action.emoji, fontSize = 28.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = action.label.take(12),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isSelected) "Active" else "Primary",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = if (isSelected) Color(0xFFDD9F0B) else textSecondaryColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Settings Section Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = textPrimaryColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Settings horizontal scrollable cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Schedule Study
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 0.dp else 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                            spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                        .clickable { /* schedule study action */ }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFFDD9F0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Schedule\nStudy",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp
                            ),
                            color = textPrimaryColor
                        )
                    }
                }

                // Card 2: Auto Save (Switch toggle)
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(140.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 0.dp else 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                            spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                        .clickable { isAutoSaveEnabled = !isAutoSaveEnabled }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Auto Save",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = "Save to vault",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = textSecondaryColor
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAutoSaveEnabled) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isAutoSaveEnabled) Color(0xFFDD9F0B) else textSecondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            // Custom Switch resembling the mockup
                            Switch(
                                checked = isAutoSaveEnabled,
                                onCheckedChange = { isAutoSaveEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFDD9F0B),
                                    uncheckedThumbColor = textSecondaryColor,
                                    uncheckedTrackColor = borderStrokeColor
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                    }
                }

                // Card 3: Generate Quiz
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 0.dp else 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                            spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                        .clickable { selectedAction = CircleAction.QUIZ }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = Color(0xFFDD9F0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Generate\nQuiz",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp
                            ),
                            color = textPrimaryColor
                        )
                    }
                }

                // Card 4: Create Flashcards
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 0.dp else 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                            spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                        .clickable { selectedAction = CircleAction.FLASHCARDS }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkTheme) Color.White.copy(0.06f) else Color(0xFFFFFBEB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = Color(0xFFDD9F0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Create\nFlashcards",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp
                            ),
                            color = textPrimaryColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Sticky Bottom CTA Pill Button ("Let's start!")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) Color(0xFF0F0F12) else Color(0xFFF8FAFC))
                .border(
                    width = 1.dp,
                    color = borderStrokeColor,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { onAction(selectedAction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0xFFDD9F0B).copy(alpha = 0.25f),
                        spotColor = Color(0xFFDD9F0B).copy(alpha = 0.35f)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDD9F0B))
            ) {
                Text(
                    text = "Let's start!",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

// ── Result panel ──────────────────────────────────────────────────────────────

@Composable
private fun ResultPanel(
    resultText   : String,
    actionLabel  : String,
    isStreaming  : Boolean,
    onStop       : () -> Unit,
    onSave       : () -> Unit,
    onDismiss    : () -> Unit,
    onOpenInApp  : ((String?) -> Unit)? = null,
    onRunAnother : (() -> Unit)? = null,
    isDarkTheme  : Boolean
) {
    val scroll = rememberScrollState()
    LaunchedEffect(resultText.length) {
        if (isStreaming) scroll.animateScrollTo(scroll.maxValue)
    }

    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val borderStrokeColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
        // Result header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isStreaming) {
                val inf = rememberInfiniteTransition(label = "dot")
                val a by inf.animateFloat(0.3f, 1f,
                    infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a")
                Box(modifier = Modifier.size(8.dp).background(Blue500.copy(a), CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(actionLabel, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textPrimaryColor,
                modifier = Modifier.weight(1f))

            if (isStreaming) {
                TextButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelMedium)
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDarkTheme) Color.White.copy(0.08f) else Color.Black.copy(0.04f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Close", tint = textPrimaryColor, modifier = Modifier.size(16.dp))
            }
        }

        // Result content
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDarkTheme) Color.White.copy(0.04f) else Color.White)
                .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
                if (resultText.isEmpty() && isStreaming) {
                    // Typing dots
                    val inf = rememberInfiniteTransition(label = "typing")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { i ->
                            val sc by inf.animateFloat(0.6f, 1f,
                                infiniteRepeatable(tween(400, delayMillis = i * 130,
                                    easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d$i")
                            Box(modifier = Modifier.size((6 * sc).dp)
                                .background(textPrimaryColor.copy(0.5f), CircleShape))
                        }
                    }
                } else {
                    Text(
                        text = if (isStreaming && resultText.isNotEmpty()) "$resultText▍" else resultText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textPrimaryColor.copy(0.9f), lineHeight = 24.sp
                    )
                }
            }
        }

        // Bottom actions
        if (!isStreaming) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, null, tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", color = Color(0xFF10B981))
                }
                if (onRunAnother != null) {
                    Button(
                        onClick = onRunAnother,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDD9F0B))
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Action", color = Color.White)
                    }
                }
            }
            // "Open Full Screen"
            if (onOpenInApp != null) {
                TextButton(
                    onClick = { onOpenInApp("library") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.OpenInFull, null,
                        tint = textSecondaryColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View in Library", color = textSecondaryColor.copy(0.7f),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Error panel ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorPanel(message: String, onDismiss: () -> Unit, onRetry: () -> Unit, isDarkTheme: Boolean) {
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = textPrimaryColor.copy(0.8f), textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss,
                border = BorderStroke(1.dp, textPrimaryColor.copy(0.3f)),
                shape = RoundedCornerShape(12.dp)) {
                Text("Close", color = textPrimaryColor.copy(0.7f))
            }
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDD9F0B))) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}

// ── Standalone error screen (called before bottom sheet) ─────────────────────

@Composable
fun CircleErrorScreen(message: String, onDismiss: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF0B0B0F).copy(0.7f) else Color(0xFFFFFFFF).copy(0.7f)
    Box(
        modifier = Modifier.fillMaxSize().background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        ErrorPanel(message = message, onDismiss = onDismiss, onRetry = onDismiss, isDarkTheme = isDarkTheme)
    }
}

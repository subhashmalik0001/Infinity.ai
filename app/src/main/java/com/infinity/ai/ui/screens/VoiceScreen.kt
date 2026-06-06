package com.infinity.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.ui.components.*
import com.infinity.ai.ui.theme.*

@Composable
fun VoiceScreen(
    isDarkTheme: Boolean,
    orbState: OrbState,
    aiState: AIInferenceState,
    onSetListening: () -> Unit,
    onSetIdle: () -> Unit,
    onDismiss: () -> Unit
) {
    val isListening = aiState is AIInferenceState.Thinking || aiState is AIInferenceState.Responding
    val transcript  = if (aiState is AIInferenceState.Responding) aiState.partialText else ""

    GradientBackground(darkTheme = false, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── 1. Header Row (Back, Voice, Options) ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    "Voice",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

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
                        "More Options",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.2f))

            // ── 2. Immersive Centered Orb ─────────────────────────────────────
            AiBodyOrb(orbState = orbState, isDarkTheme = false, size = 260.dp)

            Spacer(Modifier.height(48.dp))

            // ── 3. Transcript Text directly on Background ─────────────────────
            val textToShow = if (transcript.isNotEmpty()) transcript else if (isListening) "Listening..." else "Tap microphone to speak"
            Text(
                text = textToShow,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    lineHeight = 32.sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.weight(0.35f))

            // ── 4. Control Buttons ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Keyboard Button (dismiss voice screen to text input)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Keyboard,
                        "Keyboard Input",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center animated Microphone Button with glowing ring ripples
                Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    if (isListening) {
                        val inf = rememberInfiniteTransition(label = "ripple")
                        val rippleScale by inf.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart),
                            label = "scale"
                        )
                        val rippleAlpha by inf.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(rippleScale)
                                .background(BluePrimary.copy(alpha = rippleAlpha), CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(colors = listOf(BlueGradientStart, BlueGradientEnd)))
                            .clickable { if (isListening) onSetIdle() else onSetListening() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Pause else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Right Close "X" Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight, CircleShape)
                        .clickable(onClick = onSetIdle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Cancel",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

package com.infinity.ai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.infinity.ai.R
import com.infinity.ai.circle.InfinityOverlayService
import com.infinity.ai.circle.OverlayPermissionHelper
import com.infinity.ai.ui.components.GlassCard
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.theme.*

@Composable
fun CircleLearnEntryScreen(
    isDarkTheme   : Boolean,
    bottomPadding : Dp,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var serviceRunning  by remember { mutableStateOf(false) }
    var overlayGranted  by remember { mutableStateOf(OverlayPermissionHelper.hasOverlayPermission(context)) }
    var projectionData  by remember { mutableStateOf<Intent?>(null) }
    var showOnboarding  by remember { mutableStateOf(true) }

    // Check overlay permission on resume
    LaunchedEffect(Unit) {
        overlayGranted = OverlayPermissionHelper.hasOverlayPermission(context)
    }

    // MediaProjection permission launcher
    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as MediaProjectionManager
    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            projectionData = result.data
            startOverlayService(context, result.resultCode, result.data!!)
            serviceRunning = true
        }
    }

    // Notification permission launcher (API 33+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless */ }

    fun startService() {
        if (!overlayGranted) {
            OverlayPermissionHelper.requestOverlayPermission(context as Activity)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !OverlayPermissionHelper.hasNotificationPermission(context)) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    fun stopService() {
        val intent = Intent(context, InfinityOverlayService::class.java).apply {
            action = InfinityOverlayService.ACTION_STOP
        }
        context.stopService(intent)
        serviceRunning = false
    }

    val scrollState = rememberScrollState()

    // Determine colors based on active theme
    val backgroundColor = if (isDarkTheme) Color(0xFF0B0B0F) else Color(0xFFF8F9FA)
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF17171C) else Color(0xFFFFFFFF)
    val borderStrokeColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button on left
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // "Circle Learn" Title
                Text(
                    text = "Circle Learn",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    color = textPrimaryColor
                )

                // Help/info icon on right
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, CircleShape)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Help",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp)
            ) {
                // 1. Hero Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circle Learn Logo (Orb)
                    CircleLearnOrb(isActive = serviceRunning)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        text = if (serviceRunning) "Circle Learn Active" else "Circle Learn Inactive",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = textPrimaryColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "Circle anything on your screen and instantly understand, summarize, translate, explain, or learn from it.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        ),
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 2. Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 0.dp else 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFF64748B).copy(alpha = 0.15f),
                            spotColor = Color(0xFF1E293B).copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBackgroundColor)
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (serviceRunning) Color(0xFF10B981).copy(alpha = 0.15f)
                                    else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (serviceRunning) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (serviceRunning) "Ready to Learn" else "Circle Learn Inactive",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (serviceRunning) "Tap and drag the Circle Learn bubble over any content on your screen."
                                else "Start the service to get a floating bubble to circle and learn from anything on your screen.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                color = textSecondaryColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 3. Quick Actions Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Core Actions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = textPrimaryColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircleQuickActionCard(
                            title = "Explain",
                            description = "Understand any concept instantly",
                            icon = Icons.Default.AutoAwesome,
                            iconBg = Color(0xFFFFFBEB),
                            iconColor = Color(0xFFDD9F0B),
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                        CircleQuickActionCard(
                            title = "Summarize",
                            description = "Get concise key points",
                            icon = Icons.Default.Description,
                            iconBg = Color(0xFFFEF3C7),
                            iconColor = Color(0xFFD97706),
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircleQuickActionCard(
                            title = "Translate",
                            description = "Convert text into any language",
                            icon = Icons.Default.Translate,
                            iconBg = Color(0xFFECFDF5),
                            iconColor = Color(0xFF059669),
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                        CircleQuickActionCard(
                            title = "Notes",
                            description = "Create structured study notes",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            iconBg = Color(0xFFFEE2E2),
                            iconColor = Color(0xFFDC2626),
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 4. Secondary Features horizontal scroll section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "More Features",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = textPrimaryColor,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val features = listOf(
                            Pair("Flashcards", Icons.Default.Style),
                            Pair("Quiz", Icons.Default.Quiz),
                            Pair("Viva Questions", Icons.Default.QuestionAnswer),
                            Pair("Explain Code", Icons.Default.Code),
                            Pair("Find Bugs", Icons.Default.BugReport)
                        )
                        
                        features.forEach { (name, icon) ->
                            Box(
                                modifier = Modifier
                                    .shadow(
                                        elevation = if (isDarkTheme) 0.dp else 2.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                                        spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBackgroundColor)
                                    .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
                                    .clickable { }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimaryColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp)) // Extra space to scroll above the sticky button
            }

            // Bottom CTA Button (Sticky)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { if (serviceRunning) stopService() else startService() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = BluePrimary.copy(alpha = 0.4f),
                            spotColor = BluePrimary.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (serviceRunning) Color(0xFFEF4444) else BluePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (serviceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (serviceRunning) "Stop Circle Learn" else "Let's start!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleLearnOrb(isActive: Boolean) {
    val inf = rememberInfiniteTransition(label = "orb")
    val pulse by inf.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )

    Box(
        modifier = Modifier.size((110 * pulse).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.sweepGradient(
                        listOf(
                            BluePrimary.copy(0.4f),
                            Color(0xFFFBBF24).copy(0.4f),
                            BluePrimary.copy(0.1f),
                            BluePrimary.copy(0.4f)
                        )
                    ), CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.linearGradient(listOf(BluePrimary, Color(0xFFFACC15))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Infinity Logo",
                modifier = Modifier.size(width = 66.dp, height = 36.dp)
            )
        }
    }
}

@Composable
private fun CircleQuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isDarkTheme) 0.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF64748B).copy(alpha = 0.08f),
                spotColor = Color(0xFF1E293B).copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) Color(0xFF17171C) else CardWhite)
            .border(1.dp, if (isDarkTheme) Color.White.copy(0.08f) else BorderLight, RoundedCornerShape(20.dp))
            .clickable(interactionSource = src, indication = null, onClick = { /* visual only */ })
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = if (isDarkTheme) Color.White else TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    ),
                    color = if (isDarkTheme) Color.White.copy(0.5f) else TextSecondary
                )
            }
        }
    }
}

private fun startOverlayService(context: Context, resultCode: Int, data: Intent) {
    val intent = Intent(context, InfinityOverlayService::class.java).apply {
        action = InfinityOverlayService.ACTION_START
        putExtra(InfinityOverlayService.EXTRA_RESULT_CODE, resultCode)
        putExtra(InfinityOverlayService.EXTRA_RESULT_DATA, data)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        context.startForegroundService(intent)
    else
        context.startService(intent)
}

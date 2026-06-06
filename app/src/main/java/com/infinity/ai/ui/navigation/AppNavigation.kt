package com.infinity.ai.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import com.infinity.ai.ui.components.toOrbState
import com.infinity.ai.ui.screens.*
import com.infinity.ai.ui.theme.*
import com.infinity.ai.viewmodel.ChatViewModel
import com.infinity.ai.viewmodel.LibraryViewModel

sealed class Screen(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Home",     Icons.Filled.Home, Icons.Outlined.Home)
    object Chat      : Screen("chat",      "Chat",     Icons.Filled.Chat, Icons.Outlined.Chat)
    object Tools     : Screen("tools",     "Tools",    Icons.Filled.Build, Icons.Outlined.Build)
    object Library   : Screen("library",   "Library",  Icons.Filled.AutoStories, Icons.Outlined.AutoStories)
    object Settings  : Screen("settings",  "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val navItems = listOf(
    Screen.Dashboard,
    Screen.Tools,
    Screen.Chat,
    Screen.Library,
    Screen.Settings
)

@Composable
fun AppNavigation(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route

    val context = LocalContext.current
    val onboardingPref = remember { com.infinity.ai.data.OnboardingPreference(context) }
    val isOnboardingCompleted by onboardingPref.isOnboardingCompleted.collectAsState(initial = null)
    val chatViewModel: ChatViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()
    val aiState by chatViewModel.aiState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val orbState = aiState.toOrbState()

    val showNav       = currentRoute in navItems.map { it.route } && (currentRoute != Screen.Chat.route || messages.isEmpty())

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context))
            SpeechRecognizer.createSpeechRecognizer(context)
        else null
    }
    DisposableEffect(Unit) { onDispose { speechRecognizer?.destroy() } }

    val startListening: () -> Unit = {
        val hasMic = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            speechRecognizer?.let { sr ->
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                sr.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(p: android.os.Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(v: Float) {}
                    override fun onBufferReceived(b: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(code: Int) {}
                    override fun onResults(results: android.os.Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                        chatViewModel.startFromSuggestion(text)
                    }
                    override fun onPartialResults(partial: android.os.Bundle?) {}
                    override fun onEvent(type: Int, params: android.os.Bundle?) {}
                })
                sr.startListening(intent)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNav) {
                GoldBottomNavBar(
                    currentRoute = currentRoute,
                    isDarkTheme = isDarkTheme,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "splash",
            enterTransition  = { fadeIn(tween(220)) },
            exitTransition   = { fadeOut(tween(220)) }
        ) {
            composable("splash") {
                SplashScreen {
                    val dest = if (isOnboardingCompleted == false) "onboarding" else Screen.Dashboard.route
                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            composable("onboarding") {
                val coroutineScope = rememberCoroutineScope()
                OnboardingScreen(isDarkTheme = isDarkTheme) {
                    coroutineScope.launch {
                        onboardingPref.setOnboardingCompleted(true)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    isDarkTheme       = isDarkTheme,
                    orbState          = orbState,
                    bottomPadding     = innerPadding.calculateBottomPadding(),
                    onNavigateToChat  = { navController.navigate(Screen.Chat.route) },
                    onNavigateToVoice = { navController.navigate("voice") },
                    onOrbTap          = { navController.navigate(Screen.Chat.route) },
                    onNavigateToCircle = { navController.navigate("circle_learn") },
                    onNavigateToOcr   = { navController.navigate("ocr") },
                    onNavigateToPdf   = { navController.navigate("pdf_summary") },
                    onNavigateToQuiz  = { navController.navigate("quiz") },
                    onNavigateToScreenshot = { navController.navigate("screenshot") }
                )
            }
            composable(Screen.Chat.route) {
                ChatScreen(
                    isDarkTheme       = isDarkTheme,
                    bottomPadding     = innerPadding.calculateBottomPadding(),
                    onNavigateToVoice = { navController.navigate("voice") },
                    onNavigateBack    = { navController.popBackStack() },
                    chatViewModel     = chatViewModel
                )
            }
            composable(Screen.Tools.route) {
                ToolsScreen(
                    isDarkTheme         = isDarkTheme,
                    bottomPadding       = innerPadding.calculateBottomPadding(),
                    onNavigateToPdf     = { navController.navigate("pdf_summary") },
                    onNavigateToOcr     = { navController.navigate("ocr") },
                    onNavigateToScreenshot = { navController.navigate("screenshot") },
                    onNavigateToQuiz    = { navController.navigate("quiz") },
                    onNavigateToCircle  = { navController.navigate("circle_learn") },
                    onSendPrompt        = { prompt ->
                        chatViewModel.startFromSuggestion(prompt)
                        navController.navigate(Screen.Chat.route)
                    }
                )
            }
            composable("pdf_summary") {
                PdfSummaryScreen(
                    isDarkTheme    = isDarkTheme,
                    bottomPadding  = innerPadding.calculateBottomPadding(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("ocr") {
                OcrScreen(
                    isDarkTheme    = isDarkTheme,
                    bottomPadding  = innerPadding.calculateBottomPadding(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("screenshot") {
                ScreenshotExplainerScreen(
                    isDarkTheme    = isDarkTheme,
                    bottomPadding  = innerPadding.calculateBottomPadding(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("quiz") {
                QuizScreen(
                    isDarkTheme    = isDarkTheme,
                    bottomPadding  = innerPadding.calculateBottomPadding(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("circle_learn") {
                CircleLearnEntryScreen(
                    isDarkTheme    = isDarkTheme,
                    bottomPadding  = innerPadding.calculateBottomPadding(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme   = isDarkTheme,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    onToggleTheme = onToggleTheme
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    isDarkTheme   = isDarkTheme,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    onOpenEntry   = { },
                    vm            = libraryViewModel
                )
            }
            composable("voice") {
                VoiceScreen(
                    isDarkTheme    = isDarkTheme,
                    orbState       = orbState,
                    aiState        = aiState,
                    onSetListening = startListening,
                    onSetIdle      = {
                        speechRecognizer?.stopListening()
                        chatViewModel.stopGeneration()
                    },
                    onDismiss      = { navController.popBackStack() }
                )
            }
        }
    }
}

class BottomBarCutoutShape(
    private val cutoutRadius: Dp,
    private val cornerRadius: Dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val densityVal = density.density
        val rCutout = cutoutRadius.value * densityVal
        val rCorner = cornerRadius.value * densityVal
        
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            
            val outerCornerPx = 28.dp.value * densityVal
            val startCutoutX = centerX - rCutout - rCorner
            val endCutoutX = centerX + rCutout + rCorner
            
            moveTo(outerCornerPx, 0f)
            lineTo(startCutoutX, 0f)
            
            // 1. Left Convex Curve
            cubicTo(
                centerX - rCutout - rCorner / 2f, 0f,
                centerX - rCutout, rCorner / 2f,
                centerX - rCutout, rCorner
            )
            // 2. Left Concave Curve
            cubicTo(
                centerX - rCutout, rCorner + (rCutout - rCorner) / 2f,
                centerX - rCutout / 2f, rCutout,
                centerX, rCutout
            )
            // 3. Right Concave Curve
            cubicTo(
                centerX + rCutout / 2f, rCutout,
                centerX + rCutout, rCorner + (rCutout - rCorner) / 2f,
                centerX + rCutout, rCorner
            )
            // 4. Right Convex Curve
            cubicTo(
                centerX + rCutout, rCorner / 2f,
                centerX + rCutout + rCorner / 2f, 0f,
                centerX + rCutout + rCorner, 0f
            )
            
            lineTo(width - outerCornerPx, 0f)
            arcTo(Rect(width - outerCornerPx * 2f, 0f, width, outerCornerPx * 2f), -90f, 90f, false)
            lineTo(width, height)
            lineTo(0f, height)
            lineTo(0f, outerCornerPx)
            arcTo(Rect(0f, 0f, outerCornerPx * 2f, outerCornerPx * 2f), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun GoldBottomNavBar(
    currentRoute: String?,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Solid background depending on light/dark mode (glassy effect removed)
    val bgColor = if (isDarkTheme) Color(0xFF1E2022) else Color.White
    val borderColor = if (isDarkTheme) Color(0xFFFFFFFF).copy(alpha = 0.08f) else Color(0x0F000000)
    val activeColor = BluePrimary
    val inactiveColor = if (isDarkTheme) Color.White.copy(alpha = 0.45f) else TextSecondary.copy(alpha = 0.6f)
    
    Box(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val customShape = remember { BottomBarCutoutShape(cutoutRadius = 36.dp, cornerRadius = 16.dp) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .shadow(elevation = 16.dp, shape = customShape, clip = false, spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.4f else 0.08f))
                .background(bgColor, customShape)
                .border(width = 1.dp, color = borderColor, shape = customShape)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, screen ->
                    val selected = currentRoute == screen.route
                    if (index == 2) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigate(screen.route) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = screen.label, style = MaterialTheme.typography.labelSmall, color = if (selected) activeColor else inactiveColor)
                        }
                    } else {
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.1f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.7f),
                            label = "tabScale"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!selected) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                    onNavigate(screen.route)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) screen.activeIcon else screen.inactiveIcon,
                                contentDescription = screen.label,
                                tint = if (selected) activeColor else inactiveColor,
                                modifier = Modifier.size(24.dp).scale(scale)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = screen.label, style = MaterialTheme.typography.labelSmall, color = if (selected) activeColor else inactiveColor)
                        }
                    }
                }
            }
        }
        val chatSelected = currentRoute == Screen.Chat.route
        val fabInteractionSource = remember { MutableInteractionSource() }
        val isFabPressed by fabInteractionSource.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (isFabPressed) 0.9f else if (chatSelected) 1.15f else 1.05f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "fabScale"
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-46).dp)
                .size(60.dp)
                .scale(fabScale)
                .shadow(elevation = 12.dp, shape = CircleShape, clip = false, ambientColor = activeColor.copy(alpha = 0.4f), spotColor = activeColor)
                .background(activeColor, CircleShape)
                .clip(CircleShape)
                .clickable(interactionSource = fabInteractionSource, indication = LocalIndication.current) {
                    if (!chatSelected) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    onNavigate(Screen.Chat.route)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Screen.Chat.activeIcon, contentDescription = Screen.Chat.label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

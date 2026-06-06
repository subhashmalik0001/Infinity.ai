package com.infinity.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinity.ai.ui.components.GradientBackground
import kotlinx.coroutines.delay

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.infinity.ai.R

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(900, easing = EaseOut))
        delay(1400)
        onNavigate()
    }

    GradientBackground(darkTheme = true, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Infinity Logo",
                modifier = Modifier
                    .width(260.dp)
                    .height(142.dp)
                    .alpha(alpha.value)
            )
        }
    }
}

package com.infinity.ai.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Yellow (Gold) ─────────────────────────────────────────────────────
val BluePrimary      = Color(0xFFDD9F0B)
val BlueDark         = Color(0xFFB07F08)
val BlueGradientStart = Color(0xFFDD9F0B)
val BlueGradientEnd   = Color(0xFFFBBF24)

// ── Backgrounds ──────────────────────────────────────────────────────────────
val LightBlueBgStart  = Color(0xFFF8F9FA)
val LightBlueBgEnd    = Color(0xFFF8F9FA)
val CardWhite         = Color(0xFFFFFFFF)
val BorderLight       = Color(0xFFE2E8F0)

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary       = Color(0xFF0F172A)   // Slate 900
val TextSecondary     = Color(0xFF64748B)   // Slate 500
val TextMuted         = Color(0xFF94A3B8)   // Slate 400
val TextOnPrimary     = Color(0xFFFFFFFF)

// ── Status ───────────────────────────────────────────────────────────────────
val SuccessGreen      = Color(0xFF22C55E)
val ErrorRed          = Color(0xFFEF4444)
val WarnAmber         = Color(0xFFF59E0B)

// ── Theme Aliases & Compatibility Layers ─────────────────────────────────────
val GoldPrimary       = BluePrimary
val GoldDark          = BlueDark
val GoldSubtle        = Color(0x1ADD9F0B)   // 10% alpha of yellow
val GoldGlow          = Color(0x2BDD9F0B)   // 17% alpha
val GoldBorderActive  = Color(0x40DD9F0B)   // 25% alpha
val TextOnGold        = TextOnPrimary

val BgDark            = LightBlueBgEnd
val SurfaceDark       = CardWhite
val CardSurface       = CardWhite
val ElevatedCard      = Color(0xFFF8FAFC)
val CardGlass         = Color(0xFFFFFFFF)
val GradWarmMid       = Color(0xFFF1F5F9)
val BorderSubtle      = BorderLight

val Blue500           = BluePrimary
val Blue400           = BlueDark
val BlueAlpha12       = GoldSubtle
val DarkBg            = LightBlueBgEnd
val DarkSurface       = CardWhite
val DarkSurfaceElevated = Color(0xFFF8FAFC)
val DarkBorder        = BorderLight
val DarkGlass         = Color(0x0A000000)
val LightBg           = LightBlueBgEnd
val LightSurface      = CardWhite
val LightSurfaceElevated = Color(0xFFF8FAFC)
val LightBorder       = BorderLight
val LightGlass        = CardWhite
val TextPrimaryLight  = TextPrimary
val TextSecondaryLight = TextSecondary
val TextDisabled      = TextMuted
val TextTertiary      = TextMuted

// Extra Shorthands
val Blue50            = Color(0xFFFFFBEB)
val Blue600           = Color(0xFFD97706)
val GradStart         = Color(0xFFF8FAFC)
val GradMid           = Color(0xFFF1F5F9)
val GradEnd           = Color(0xFFFEF3C7) // warm light yellow/amber instead of blue-grey
val OrbColor1         = Color(0xFFFDE047)
val OrbColor2         = Color(0xFFFACC15)
val OrbColor3         = Color(0xFFFEF08A)

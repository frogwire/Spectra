package com.duck.privacy.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Spectra Brand Colors & Design System
val SpectraBgDark = Color(0xFF0F0E15)
val SpectraSurface = Color(0xFF17151F)
val SpectraSurface2 = Color(0xFF201D2B)
val SpectraSurface3 = Color(0xFF2A2738)

val TextMain = Color(0xFFECEAF4)
val TextMuted = Color(0xFF8E8A9F)

// Accents & Brand Gradients
val SpectraViolet = Color(0xFF6D4AFF)
val SpectraCyan = Color(0xFF5BC0EB)
val IncognitoViolet = Color(0xFF6D4AFF)

// Prism Glow Gradient: Violet -> Cyan
val PrismGlowBrush = Brush.horizontalGradient(
    colors = listOf(SpectraViolet, SpectraCyan)
)

// State Colors
val ShieldTeal = Color(0xFF2EC4A6)     // Protected
val ShieldYellow = Color(0xFFF2C14E)   // Partial protection / Warning
val ShieldRed = Color(0xFFFF6B6B)      // Connection warning / Insecure

val HairlineBorder = Color(0x26FFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = SpectraViolet,
    secondary = SpectraCyan,
    background = SpectraBgDark,
    surface = SpectraSurface,
    onPrimary = Color.White,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun PrivacyBrowserTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

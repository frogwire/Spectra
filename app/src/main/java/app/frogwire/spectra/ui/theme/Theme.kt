package app.frogwire.spectra.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

// Lighter violet for text and icons on dark surfaces: #6D4AFF alone is under 4.5:1 there
val VioletText = Color(0xFFA48FFF)

// Minimum touch target (spec: at least 44 px; Android guidance is 48dp)
val MinTouch = 48.dp

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

// ---- Typography -------------------------------------------------------------------------
// Space Grotesk for the wordmark and headings, Inter for interface text.
// Fonts are bundled (no Google font provider, nothing fetched at runtime). Drop the static
// .ttf files into app/src/main/assets/fonts/; any that are missing fall back to the system
// sans-serif, so the app builds and runs without them.

data class SpectraFontSet(
    val display: FontFamily = FontFamily.SansSerif,
    val body: FontFamily = FontFamily.SansSerif
)

val LocalSpectraFonts = staticCompositionLocalOf { SpectraFontSet() }

/** Space Grotesk (or fallback): wordmark and headings. */
val SpectraDisplay: FontFamily
    @Composable get() = LocalSpectraFonts.current.display

/** Inter (or fallback): interface text. */
val SpectraBody: FontFamily
    @Composable get() = LocalSpectraFonts.current.body

private val DisplayFontFiles = listOf(
    "SpaceGrotesk-Medium.ttf" to FontWeight.Medium,
    "SpaceGrotesk-SemiBold.ttf" to FontWeight.SemiBold,
    "SpaceGrotesk-Bold.ttf" to FontWeight.Bold
)

private val BodyFontFiles = listOf(
    "Inter-Regular.ttf" to FontWeight.Normal,
    "Inter-Medium.ttf" to FontWeight.Medium,
    "Inter-SemiBold.ttf" to FontWeight.SemiBold,
    "Inter-Bold.ttf" to FontWeight.Bold
)

private fun loadFamily(assets: AssetManager, files: List<Pair<String, FontWeight>>): FontFamily {
    val present = runCatching { assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
    val fonts = files
        .filter { (name, _) -> name in present }
        .map { (name, weight) -> Font("fonts/$name", assets, weight) }
    return if (fonts.isEmpty()) FontFamily.SansSerif else FontFamily(fonts)
}

/**
 * Inter ships as one variable font (assets/fonts/Inter-Variable.ttf) holding every weight.
 * Each weight is a named instance of that file; falls back to static Inter-*.ttf files.
 */
private fun loadBodyFamily(assets: AssetManager): FontFamily {
    val present = runCatching { assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
    if (INTER_VARIABLE !in present) return loadFamily(assets, BodyFontFiles)
    val fonts = BodyFontFiles.map { (_, weight) ->
        Font(
            path = "fonts/$INTER_VARIABLE",
            assetManager = assets,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
        )
    }
    return FontFamily(fonts)
}

private const val INTER_VARIABLE = "Inter-Variable.ttf"

private fun spectraTypography(fonts: SpectraFontSet): Typography {
    val base = Typography()
    fun TextStyle.display() = copy(fontFamily = fonts.display, letterSpacing = (-0.25).sp)
    // Inter is designed for tight UI text: drop Material's default 0.1-0.5sp letter spacing
    fun TextStyle.body() = copy(fontFamily = fonts.body, letterSpacing = 0.sp)
    return base.copy(
        displayLarge = base.displayLarge.display(),
        displayMedium = base.displayMedium.display(),
        displaySmall = base.displaySmall.display(),
        headlineLarge = base.headlineLarge.display(),
        headlineMedium = base.headlineMedium.display(),
        headlineSmall = base.headlineSmall.display(),
        titleLarge = base.titleLarge.display(),
        titleMedium = base.titleMedium.body(),
        titleSmall = base.titleSmall.body(),
        // Generous line-height for interface text
        bodyLarge = base.bodyLarge.body().copy(lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.body().copy(lineHeight = 21.sp),
        bodySmall = base.bodySmall.body().copy(lineHeight = 18.sp),
        labelLarge = base.labelLarge.body(),
        labelMedium = base.labelMedium.body(),
        labelSmall = base.labelSmall.body()
    )
}

@Composable
fun PrivacyBrowserTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val fonts = remember {
        SpectraFontSet(
            display = loadFamily(context.assets, DisplayFontFiles),
            body = loadBodyFamily(context.assets)
        )
    }
    CompositionLocalProvider(LocalSpectraFonts provides fonts) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = remember(fonts) { spectraTypography(fonts) },
            content = content
        )
    }
}

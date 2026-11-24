package app.frogwire.spectra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.ui.theme.*

/**
 * New-tab page: the crystal elk far off in the mist, the wordmark, and shortcuts.
 * Searching happens in the address bar at the top.
 */
@Composable
fun HomeView(
    isIncognito: Boolean,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpectraBgDark)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .semantics { contentDescription = "A crystal elk standing in the mist" }
        ) {
            ElkInMist(
                modifier = Modifier.fillMaxSize(),
                glow = if (isIncognito) Color(0xFF8A5CFF) else SpectraViolet
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isIncognito) "Incognito+" else "Spectra",
                    fontSize = 36.sp,
                    fontFamily = SpectraDisplay,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = TextMain
                )
                Text(
                    text = if (isIncognito) "Nothing from this session is kept when you close it."
                    else "Browse freely, leave no fingerprint.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Shortcuts (become your top bookmarks once bookmarks exist)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ShortcutTile("W", TextMain, "Wikipedia") { onNavigate("wikipedia.org") }
            ShortcutTile("Y", ShieldYellow, "Hacker News") { onNavigate("news.ycombinator.com") }
            ShortcutTile("G", VioletText, "GitHub") { onNavigate("github.com") }
            ShortcutTile("!", SpectraCyan, "!bangs") { onNavigate("https://duckduckgo.com/bangs") }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "No account · No sync · No telemetry",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

/** A tile with its label centered underneath; both share one fixed width so they line up. */
@Composable
private fun ShortcutTile(letter: String, letterColor: Color, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpectraSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
        ) {
            Text(text = letter, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = letterColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

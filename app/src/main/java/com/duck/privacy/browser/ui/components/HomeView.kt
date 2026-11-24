package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.privacy.SiteAllowlist
import com.duck.privacy.browser.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeView(
    isIncognito: Boolean,
    totalBlocked: Long,
    privacyState: SiteAllowlist.State,
    pageShieldsSupported: Boolean = true,
    onNavigate: (String) -> Unit
) {
    var homeQuery by remember { mutableStateOf("") }
    val accent = if (isIncognito) VioletText else SpectraCyan

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to (if (isIncognito) Color(0xFF1A1530) else Color(0xFF14121D)),
                    0.55f to SpectraBgDark
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Crystal elk mascot
            CrystalElk(
                modifier = Modifier
                    .size(120.dp)
                    .semantics { contentDescription = "Spectra crystal elk" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isIncognito) "Incognito+" else "Spectra",
                fontSize = 34.sp,
                fontFamily = SpectraDisplay,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isIncognito)
                    "Nothing from this session is kept when you close it."
                else
                    "Browse freely, leave no fingerprint.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Search capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(CircleShape)
                    .background(SpectraSurface)
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = homeQuery,
                    onValueChange = { homeQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextMain, fontSize = 16.sp, fontFamily = SpectraBody),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        autoCorrect = !isIncognito
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (homeQuery.isNotBlank()) onNavigate(homeQuery)
                    }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (homeQuery.isEmpty()) {
                            Text(
                                text = "Search privately with DuckDuckGo",
                                color = TextMuted,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live protection status (reflects the actual settings)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusBadge(
                    text = when {
                        !privacyState.blockTrackers -> "Tracker blocking off"
                        totalBlocked > 0 -> "${formatCount(totalBlocked)} trackers blocked"
                        else -> "Tracker blocking on"
                    },
                    on = privacyState.blockTrackers
                )
                val fingerprintOn = pageShieldsSupported && privacyState.fingerprintShield
                val webRtcOn = pageShieldsSupported && privacyState.webRtcLeakGuard
                StatusBadge(
                    text = if (fingerprintOn) "Fingerprint shield on" else "Fingerprint shield off",
                    on = fingerprintOn
                )
                StatusBadge(
                    text = if (webRtcOn) "WebRTC guard on" else "WebRTC guard off",
                    on = webRtcOn
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickDialItem(Modifier.weight(1f), "W", TextMain, "Wikipedia") { onNavigate("wikipedia.org") }
                QuickDialItem(Modifier.weight(1f), "Y", ShieldYellow, "Hacker News") { onNavigate("news.ycombinator.com") }
                QuickDialItem(Modifier.weight(1f), "G", VioletText, "GitHub") { onNavigate("github.com") }
                QuickDialItem(Modifier.weight(1f), "!", SpectraCyan, "!bangs") {
                    onNavigate("https://duckduckgo.com/bangs")
                }
            }

            Text(
                text = "No account · No sync · No telemetry",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
            )
        }
    }
}

private fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 10_000 -> "${n / 1000}k"
    else -> "%,d".format(n)
}

@Composable
private fun StatusBadge(text: String, on: Boolean) {
    val color = if (on) ShieldTeal else ShieldYellow
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SpectraSurface)
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filled dot when on, hollow ring when off: readable without color
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .then(
                    if (on) Modifier.background(color)
                    else Modifier.border(1.5.dp, color, CircleShape)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextMain, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickDialItem(
    modifier: Modifier = Modifier,
    letter: String,
    letterColor: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpectraSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
        ) {
            Text(text = letter, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = letterColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = TextMuted, maxLines = 1)
    }
}

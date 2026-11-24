package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.ui.theme.*

@Composable
fun HomeView(
    isIncognito: Boolean,
    totalBlocked: Long,
    onNavigate: (String) -> Unit
) {
    var homeQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpectraBgDark)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Center Hero Section (Crystal Elk Mascot & Title)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mascot Emblem Box with Violet-Cyan Prism Glow
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isIncognito) IncognitoViolet.copy(alpha = 0.25f)
                        else SpectraSurface
                    )
                    .border(
                        1.5.dp,
                        PrismGlowBrush,
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isIncognito) "🕶️" else "🦌",
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isIncognito) "Spectra Incognito+" else "Spectra",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isIncognito)
                    "Ephemeral session. Leaves no trace on close."
                else
                    "Browse freely, leave no fingerprint.",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(SpectraSurface)
                    .border(1.dp, HairlineBorder, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔍", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(10.dp))

                BasicTextField(
                    value = homeQuery,
                    onValueChange = { homeQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextMain,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(SpectraCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (homeQuery.isNotBlank()) onNavigate(homeQuery)
                    }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (homeQuery.isEmpty()) {
                            Text(
                                text = "Search privately with DuckDuckGo",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Badges (Teal Indicators)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(text = if (totalBlocked > 0) "$totalBlocked Trackers blocked" else "Trackers blocked")
                StatusBadge(text = "Fingerprint shield on")
                StatusBadge(text = "WebRTC leak guard on")
            }
        }

        // Quick Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickDialItem(
                modifier = Modifier.weight(1f),
                icon = "W",
                label = "Wikipedia",
                onClick = { onNavigate("wikipedia.org") }
            )
            QuickDialItem(
                modifier = Modifier.weight(1f),
                icon = "Y",
                iconColor = ShieldYellow,
                label = "HackerNews",
                onClick = { onNavigate("news.ycombinator.com") }
            )
            QuickDialItem(
                modifier = Modifier.weight(1f),
                icon = "🐙",
                label = "GitHub",
                onClick = { onNavigate("github.com") }
            )
            QuickDialItem(
                modifier = Modifier.weight(1f),
                icon = "!bang",
                iconColor = SpectraCyan,
                label = "DDG !bang",
                onClick = { onNavigate("https://duckduckgo.com/?q=!w+privacy") }
            )
        }

        // Footer
        Text(
            text = "No account · No sync · No telemetry",
            fontSize = 11.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun StatusBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ShieldTeal)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            color = TextMain,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QuickDialItem(
    modifier: Modifier = Modifier,
    icon: String,
    iconColor: Color = TextMain,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iconColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 9.sp, color = TextMuted)
    }
}

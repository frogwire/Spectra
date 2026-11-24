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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.ui.theme.*

@Composable
fun Omnibar(
    urlText: String,
    isIncognito: Boolean,
    blockedCount: Int,
    protectionEnabled: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onPurge: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenShield: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleIncognito: () -> Unit
) {
    var textValue by remember(urlText) { mutableStateOf(urlText) }

    // Shield 3-state logic:
    // Protected: Teal (protection enabled, HTTPS)
    // Partial: Yellow (protection off for site)
    // Warning: Red (HTTP cleartext or unsafe scheme)
    val isHttp = urlText.startsWith("http://", ignoreCase = true)
    val (shieldColor, shieldIcon, shieldStatusText) = when {
        isHttp -> Triple(ShieldRed, "❌", "Connection warning")
        !protectionEnabled -> Triple(ShieldYellow, "⚠️", "Partial protection")
        else -> Triple(ShieldTeal, "🛡️", "Protected")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder)
    ) {
        // Spectra Thin Prism Glow Indicator Line (Violet -> Cyan gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PrismGlowBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Search Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(SpectraBgDark)
                    .border(
                        1.dp,
                        if (isIncognito) IncognitoViolet.copy(alpha = 0.5f) else HairlineBorder,
                        CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shield Button (3-state security shield)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(shieldColor.copy(alpha = 0.18f))
                        .border(1.dp, shieldColor.copy(alpha = 0.5f), CircleShape)
                        .clickable(role = Role.Button) { onOpenShield() }
                        .semantics {
                            contentDescription = "$shieldStatusText, $blockedCount trackers blocked. Open privacy panel"
                        }
                ) {
                    Text(text = shieldIcon, fontSize = 14.sp)
                    val badge = when {
                        !protectionEnabled -> "OFF"
                        blockedCount > 0 -> "$blockedCount"
                        else -> null
                    }
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(shieldColor)
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Text(text = if (isHttp) "⚠️" else "🔒", fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.width(6.dp))

                // Search/URL Input
                BasicTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    textStyle = TextStyle(
                        color = TextMain,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(if (isIncognito) IncognitoViolet else SpectraCyan),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        autoCorrect = !isIncognito
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { onSearch(textValue) }
                    ),
                    decorationBox = { innerTextField ->
                        if (textValue.isEmpty()) {
                            Text(
                                text = if (isIncognito) "Search DuckDuckGo (Incognito+)" else "Search DuckDuckGo or enter URL",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Incognito+ Pill Button Indicator
                if (isIncognito) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IncognitoViolet)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Incognito+",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (textValue.isNotEmpty()) {
                    Text(
                        text = "✕",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { textValue = "" }
                            .padding(start = 6.dp, end = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back
                Text(
                    text = "‹",
                    fontSize = 22.sp,
                    color = if (canGoBack) TextMain else TextMuted.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = canGoBack) { onBack() }
                        .padding(4.dp)
                )

                // Forward
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = if (canGoForward) TextMain else TextMuted.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = canGoForward) { onForward() }
                        .padding(4.dp)
                )

                // 🔥 Purge Session Button
                Box(
                    modifier = Modifier
                        .clickable { onPurge() }
                        .padding(4.dp)
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 18.sp
                    )
                }

                // Tabs Counter Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isIncognito) IncognitoViolet else TextMuted,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onOpenTabs() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$tabCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isIncognito) IncognitoViolet else TextMain
                    )
                }

                // Incognito+ Toggle Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isIncognito) IncognitoViolet.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable(role = Role.Button) { onToggleIncognito() }
                        .semantics { contentDescription = if (isIncognito) "Leave Incognito+" else "Go Incognito+" }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🕶️",
                        fontSize = 16.sp
                    )
                }

                // Settings Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onOpenSettings() }
                        .semantics { contentDescription = "Settings" }
                        .padding(4.dp)
                ) {
                    Text(text = "⚙️", fontSize = 16.sp)
                }
            }
        }
    }
}

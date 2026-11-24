package com.duck.privacy.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.ui.theme.*

/** The three address-bar shield states. Each has its own icon *and* color, never color alone. */
enum class ShieldState(val color: Color, val icon: ImageVector, val label: String) {
    Protected(ShieldTeal, Icons.Rounded.GppGood, "Protected"),
    Partial(ShieldYellow, Icons.Rounded.GppMaybe, "Partial protection"),
    Warning(ShieldRed, Icons.Rounded.GppBad, "Connection warning")
}

fun shieldStateFor(url: String, protectionEnabled: Boolean): ShieldState = when {
    url.startsWith("http://", ignoreCase = true) -> ShieldState.Warning
    !protectionEnabled -> ShieldState.Partial
    else -> ShieldState.Protected
}

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
    val shield = shieldStateFor(urlText, protectionEnabled)
    val shieldColor by animateColorAsState(shield.color, label = "shieldColor")
    val accent = if (isIncognito) VioletText else SpectraCyan

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isIncognito) Color(0xFF1A1530) else SpectraSurface)
    ) {
        // Prism glow: shown only on secure pages; a plain hairline otherwise
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (shield == ShieldState.Warning) 1.dp else 2.dp)
                .then(
                    if (shield == ShieldState.Warning) Modifier.background(HairlineBorder)
                    else Modifier.background(PrismGlowBrush)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 2.dp)
        ) {
            // Address capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(CircleShape)
                    .background(SpectraBgDark)
                    .border(
                        1.dp,
                        if (isIncognito) IncognitoViolet.copy(alpha = 0.6f) else HairlineBorder,
                        CircleShape
                    )
                    .padding(start = 4.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Security shield: opens the privacy panel
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(MinTouch)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onOpenShield() }
                        .semantics {
                            contentDescription =
                                "${shield.label}, $blockedCount trackers blocked. Open privacy panel"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(shieldColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = shield.icon,
                            contentDescription = null,
                            tint = shieldColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    val badge = when {
                        !protectionEnabled -> "OFF"
                        blockedCount > 99 -> "99+"
                        blockedCount > 0 -> "$blockedCount"
                        else -> null
                    }
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 1.dp)
                                .clip(CircleShape)
                                .background(shieldColor)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpectraBgDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                BasicTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    textStyle = TextStyle(color = TextMain, fontSize = 15.sp, fontFamily = SpectraBody),
                    singleLine = true,
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        autoCorrect = !isIncognito
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSearch(textValue) }),
                    decorationBox = { innerTextField ->
                        if (textValue.isEmpty()) {
                            Text(
                                text = if (isIncognito) "Search privately" else "Search or enter address",
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )

                if (isIncognito) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IncognitoViolet)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Incognito+",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (textValue.isNotEmpty()) {
                    ToolbarIcon(
                        icon = Icons.Rounded.Close,
                        description = "Clear address",
                        tint = TextMuted,
                        iconSize = 18.dp,
                        onClick = { textValue = "" }
                    )
                }
            }

            // Action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", enabled = canGoBack, onClick = onBack)
                ToolbarIcon(Icons.AutoMirrored.Rounded.ArrowForward, "Forward", enabled = canGoForward, onClick = onForward)
                ToolbarIcon(
                    Icons.Rounded.LocalFireDepartment,
                    "Burn session: close all tabs and wipe data",
                    tint = Color(0xFFFF9F6B),
                    onClick = onPurge
                )

                // Tab counter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(MinTouch)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onOpenTabs() }
                        .semantics { contentDescription = "$tabCount open tabs" }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .border(
                                1.5.dp,
                                if (isIncognito) VioletText else TextMain,
                                RoundedCornerShape(7.dp)
                            )
                    ) {
                        Text(
                            text = if (tabCount > 99) ":D" else "$tabCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncognito) VioletText else TextMain
                        )
                    }
                }

                ToolbarIcon(
                    Icons.Rounded.VisibilityOff,
                    if (isIncognito) "Leave Incognito+" else "Go Incognito+",
                    tint = if (isIncognito) Color.White else TextMain,
                    background = if (isIncognito) IncognitoViolet else Color.Transparent,
                    onClick = onToggleIncognito
                )
                ToolbarIcon(Icons.Rounded.Settings, "Settings", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
internal fun ToolbarIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    tint: Color = TextMain,
    background: Color = Color.Transparent,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MinTouch)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(background)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else TextMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

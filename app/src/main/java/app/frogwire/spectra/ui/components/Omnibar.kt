package app.frogwire.spectra.ui.components

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
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.ui.theme.*

/** The three security states. Each has its own icon *and* color, never color alone. */
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

/**
 * Top bar: one field for both searching and addresses (DuckDuckGo search unless it looks like
 * an address), with the shield button that opens the privacy panel.
 */
@Composable
fun AddressBar(
    displayText: String,
    fullUrl: String,
    isIncognito: Boolean,
    protectionEnabled: Boolean,
    onSubmit: (String) -> Unit,
    onOpenShield: () -> Unit
) {
    val shield = shieldStateFor(fullUrl, protectionEnabled)
    val shieldColor by animateColorAsState(shield.color, label = "shieldColor")
    val accent = if (isIncognito) VioletText else SpectraCyan
    val focusManager = LocalFocusManager.current

    var field by remember(displayText) { mutableStateOf(TextFieldValue(displayText)) }
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isIncognito) Color(0xFF1A1530) else SpectraSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .heightIn(min = 52.dp)
                .clip(CircleShape)
                .background(SpectraBgDark)
                .border(
                    1.dp,
                    when {
                        focused -> accent.copy(alpha = 0.6f)
                        isIncognito -> IncognitoViolet.copy(alpha = 0.6f)
                        else -> HairlineBorder
                    },
                    CircleShape
                )
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The one privacy button: state icon, opens the privacy panel
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(MinTouch)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onOpenShield() }
                    .semantics { contentDescription = "${shield.label}. Open privacy panel" }
            ) {
                Icon(shield.icon, contentDescription = null, tint = shieldColor, modifier = Modifier.size(22.dp))
            }

            BasicTextField(
                value = field,
                onValueChange = { field = it },
                textStyle = TextStyle(color = TextMain, fontSize = 16.sp, fontFamily = SpectraBody),
                singleLine = true,
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = !isIncognito
                ),
                keyboardActions = KeyboardActions(onGo = {
                    if (field.text.isNotBlank()) {
                        onSubmit(field.text)
                        focusManager.clearFocus()
                    }
                }),
                decorationBox = { inner ->
                    if (field.text.isEmpty()) {
                        Text(
                            text = if (isIncognito) "Search privately or enter address" else "Search or enter address",
                            color = TextMuted,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        if (state.isFocused && !focused) {
                            // Editing shows the full address, selected so typing replaces it
                            val text = if (fullUrl.isBlank() || fullUrl == "about:blank") "" else fullUrl
                            field = TextFieldValue(text, TextRange(0, text.length))
                        } else if (!state.isFocused && focused) {
                            field = TextFieldValue(displayText)
                        }
                        focused = state.isFocused
                    }
            )

            if (isIncognito && !focused) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IncognitoViolet)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Incognito+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (focused && field.text.isNotEmpty()) {
                ToolbarIcon(Icons.Rounded.Close, "Clear", tint = TextMuted, iconSize = 18.dp) {
                    field = TextFieldValue("")
                }
            }
        }

        // Prism glow under the bar on secure pages; a plain hairline otherwise
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (shield == ShieldState.Warning) 1.dp else 2.dp)
                .then(
                    if (shield == ShieldState.Warning) Modifier.background(HairlineBorder)
                    else Modifier.background(PrismGlowBrush)
                )
        )
    }
}

/** Bottom bar, thumb-reachable: Back, Forward, Burn, Tabs, Menu. */
@Composable
fun BottomBar(
    isIncognito: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onPurge: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isIncognito) Color(0xFF1A1530) else SpectraSurface)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HairlineBorder))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", enabled = canGoBack, onClick = onBack)
            ToolbarIcon(Icons.AutoMirrored.Rounded.ArrowForward, "Forward", enabled = canGoForward, onClick = onForward)
            ToolbarIcon(
                Icons.Rounded.LocalFireDepartment,
                "Burn session: close all tabs and wipe data",
                tint = Color(0xFFFF9F6B),
                iconSize = 26.dp,
                onClick = onPurge
            )
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
                        .border(1.5.dp, if (isIncognito) VioletText else TextMain, RoundedCornerShape(7.dp))
                ) {
                    Text(
                        text = if (tabCount > 99) ":D" else "$tabCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncognito) VioletText else TextMain
                    )
                }
            }
            ToolbarIcon(Icons.Rounded.Menu, "Menu", onClick = onOpenMenu)
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
    iconSize: Dp = 24.dp,
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
                .size(40.dp)
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

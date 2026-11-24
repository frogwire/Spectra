package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.privacy.SiteAllowlist
import com.duck.privacy.browser.ui.theme.*

/**
 * Spectra Privacy Panel (Bottom Sheet).
 * Matches the popover panel from Spectra-Browser-Mockup.pdf.
 */
@Composable
fun PrivacyShieldSheet(
    domain: String,
    siteKey: String?,
    isIncognito: Boolean,
    pageShieldsSupported: Boolean,
    privacyState: SiteAllowlist.State,
    onProtectionChange: (Boolean) -> Unit,
    onToggleBlockTrackers: (Boolean) -> Unit,
    onToggleFingerprintShield: (Boolean) -> Unit,
    onToggleBlockThirdPartyCookies: (Boolean) -> Unit,
    onToggleWebRtcLeakGuard: (Boolean) -> Unit,
    onToggleWipeCookiesOnClose: (Boolean) -> Unit,
    blockedTrackers: List<String>,
    onOpenDashboard: () -> Unit,
    onDismiss: () -> Unit
) {
    val isSiteProtected = siteKey == null || privacyState.isSiteProtected(siteKey, isIncognito)
    val siteTrackersOn = privacyState.blockTrackers && (siteKey == null || !privacyState.isOff(siteKey, isIncognito))
    val wipeOn = privacyState.wipeCookiesOnClose || isIncognito
    val cookiesOn = privacyState.blockThirdPartyCookies || isIncognito
    val fingerprintOn = pageShieldsSupported && privacyState.fingerprintShield
    val webRtcOn = pageShieldsSupported && privacyState.webRtcLeakGuard
    val protectionsOn = listOf(siteTrackersOn, fingerprintOn, cookiesOn, webRtcOn, wipeOn).count { it }

    val shield = if (isSiteProtected) ShieldState.Protected else ShieldState.Partial
    val headerSubtitle = when {
        siteKey == null -> "Protections apply to every site you visit."
        isSiteProtected -> "Protections are on for $siteKey."
        else -> "Protections are off for $siteKey."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextMuted.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Header: elk grows one crystal per protection that is on
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CrystalElk(modifier = Modifier.size(64.dp), crystals = protectionsOn)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = shield.icon,
                        contentDescription = null,
                        tint = shield.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shield.label,
                        fontSize = 19.sp,
                        fontFamily = SpectraDisplay,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = headerSubtitle, fontSize = 13.sp, lineHeight = 18.sp, color = TextMuted)
                Text(
                    text = "$protectionsOn of 5 protections on",
                    fontSize = 12.sp,
                    color = if (protectionsOn == 5) ShieldTeal else ShieldYellow,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5 Core Privacy Toggles (Spectra Privacy Panel Specs)
        ShieldToggleRow(
            title = "Block trackers",
            subtitle = "Third-party trackers and ads",
            checked = siteTrackersOn,
            onCheckedChange = { enabled ->
                if (siteKey != null) onProtectionChange(enabled)
                else onToggleBlockTrackers(enabled)
            }
        )

        ShieldToggleRow(
            title = "Fingerprint shield",
            subtitle = if (pageShieldsSupported) "Randomizes canvas, audio and fonts" else UPDATE_WEBVIEW,
            checked = fingerprintOn,
            enabled = pageShieldsSupported,
            onCheckedChange = onToggleFingerprintShield
        )

        ShieldToggleRow(
            title = "Block third-party cookies",
            subtitle = if (isIncognito) "Always on in Incognito+" else "First-party cookies still work",
            checked = cookiesOn,
            enabled = !isIncognito,
            onCheckedChange = onToggleBlockThirdPartyCookies
        )

        ShieldToggleRow(
            title = "WebRTC leak guard",
            subtitle = if (pageShieldsSupported) "Hides your local IP; calls still work" else UPDATE_WEBVIEW,
            checked = webRtcOn,
            enabled = pageShieldsSupported,
            onCheckedChange = onToggleWebRtcLeakGuard
        )

        ShieldToggleRow(
            title = "Wipe cookies on close",
            subtitle = if (isIncognito) "Always on in Incognito+" else "Incognito+ behaviour for every window",
            checked = wipeOn,
            enabled = !isIncognito,
            onCheckedChange = onToggleWipeCookiesOnClose
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Open Privacy Dashboard Action Button
        OutlinedButton(
            onClick = {
                onDismiss()
                onOpenDashboard()
            },
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpectraViolet.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextMain
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouch)
        ) {
            Text(
                text = "Open privacy dashboard",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Trackers blocked on this page
        if (blockedTrackers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Blocked on this page · ${blockedTrackers.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(blockedTrackers) { tracker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpectraBgDark)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = "Blocked",
                            tint = ShieldTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tracker,
                            fontSize = 13.sp,
                            color = TextMain,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

internal const val UPDATE_WEBVIEW = "Unavailable: update Android System WebView"

@Composable
internal fun ShieldToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ShieldRowContainer(title, subtitle) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SpectraViolet,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SpectraBgDark,
                // Locked-on rows (e.g. in Incognito+) still read as on
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.85f),
                disabledCheckedTrackColor = SpectraViolet.copy(alpha = 0.45f),
                disabledUncheckedThumbColor = TextMuted.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = SpectraBgDark
            ),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
internal fun ShieldRowContainer(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SpectraBgDark)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(text = subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = TextMuted)
        }
        trailing()
    }
    Spacer(modifier = Modifier.height(6.dp))
}

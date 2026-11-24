package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
    val headerColor = if (isSiteProtected) ShieldTeal else ShieldYellow
    val headerIcon = if (isSiteProtected) "🛡️" else "⚠️"
    val headerTitle = if (isSiteProtected) "Protected" else "Partial protection"
    val headerSubtitle = if (isSiteProtected) {
        "Your protections are on for every site."
    } else {
        "Protections are off for $siteKey."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextMuted.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Popover Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SpectraBgDark)
                .border(1.dp, headerColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(headerColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = headerIcon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = headerTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = headerSubtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5 Core Privacy Toggles (Spectra Privacy Panel Specs)
        ShieldToggleRow(
            title = "Block trackers",
            subtitle = "Third-party trackers and ads",
            checked = privacyState.blockTrackers && (siteKey == null || !privacyState.isOff(siteKey, isIncognito)),
            onCheckedChange = { enabled ->
                if (siteKey != null) onProtectionChange(enabled)
                else onToggleBlockTrackers(enabled)
            }
        )

        ShieldToggleRow(
            title = "Fingerprint shield",
            subtitle = "Randomizes canvas, audio and fonts",
            checked = privacyState.fingerprintShield,
            onCheckedChange = onToggleFingerprintShield
        )

        ShieldToggleRow(
            title = "Block third-party cookies",
            subtitle = "First-party cookies still work",
            checked = privacyState.blockThirdPartyCookies,
            onCheckedChange = onToggleBlockThirdPartyCookies
        )

        ShieldToggleRow(
            title = "WebRTC leak guard",
            subtitle = "Hides your local IP; calls still work",
            checked = privacyState.webRtcLeakGuard,
            onCheckedChange = onToggleWebRtcLeakGuard
        )

        ShieldToggleRow(
            title = "Wipe cookies on close",
            subtitle = "Incognito+ behaviour for every window",
            checked = privacyState.wipeCookiesOnClose || isIncognito,
            onCheckedChange = onToggleWipeCookiesOnClose
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Open Privacy Dashboard Action Button
        OutlinedButton(
            onClick = {
                onDismiss()
                onOpenDashboard()
            },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpectraViolet.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextMain
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Open privacy dashboard",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Live Blocked Trackers Log (if on a page)
        if (blockedTrackers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "TRACKERS DROPPED ON THIS PAGE (${blockedTrackers.size})",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(blockedTrackers) { tracker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E1218))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tracker,
                            fontSize = 10.sp,
                            color = Color(0xFFF87171),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DROPPED ✕",
                            fontSize = 8.sp,
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShieldToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ShieldRowContainer(title, subtitle) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SpectraViolet,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SpectraBgDark
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
            .clip(RoundedCornerShape(10.dp))
            .background(SpectraBgDark)
            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
        }
        trailing()
    }
    Spacer(modifier = Modifier.height(6.dp))
}

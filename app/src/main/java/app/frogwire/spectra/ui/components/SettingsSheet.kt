package app.frogwire.spectra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.privacy.SiteAllowlist
import app.frogwire.spectra.ui.theme.*

/**
 * Spectra settings: this site, the five protections, always-on protections,
 * and the sites where protection is off. Links to the full privacy dashboard.
 */
@Composable
fun SettingsSheet(
    currentSiteKey: String?,
    isIncognito: Boolean,
    incognitoSupported: Boolean,
    pageShieldsSupported: Boolean,
    currentSiteProtected: Boolean,
    onCurrentSiteProtectionChange: (Boolean) -> Unit,
    privacyState: SiteAllowlist.State,
    onToggleBlockTrackers: (Boolean) -> Unit,
    onToggleFingerprintShield: (Boolean) -> Unit,
    onToggleBlockThirdPartyCookies: (Boolean) -> Unit,
    onToggleWebRtcLeakGuard: (Boolean) -> Unit,
    onToggleWipeCookiesOnClose: (Boolean) -> Unit,
    onToggleSafeBrowsing: (Boolean) -> Unit,
    totalBlocked: Long,
    onOpenDashboard: () -> Unit,
    downloadCount: Int,
    onOpenDownloads: () -> Unit,
    sitesWithProtectionOff: List<String>,
    onTurnProtectionBackOn: (String) -> Unit,
    onTurnAllBackOn: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .sheetSurface()
            .fillMaxHeight(0.88f)
            .padding(horizontal = 20.dp)
    ) {
        SheetHandle()
        SheetHeader(title = "Settings", onDone = onDismiss)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Privacy dashboard entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpectraBgDark)
                    .border(1.dp, SpectraViolet.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onOpenDashboard)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Insights,
                    contentDescription = null,
                    tint = SpectraCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Privacy dashboard", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Text(
                        text = if (totalBlocked > 0) "%,d trackers blocked so far".format(totalBlocked)
                        else "Tracker stats appear as you browse",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            SettingsEntryRow(
                icon = Icons.Rounded.Download,
                title = "Downloads",
                subtitle = if (downloadCount > 0) "$downloadCount in the list" else "No downloads yet",
                onClick = onOpenDownloads
            )

            // --- This site ---
            SheetSectionLabel("This site")
            if (currentSiteKey != null) {
                val scope = if (isIncognito) " Lasts until Incognito+ closes." else ""
                ShieldToggleRow(
                    title = "Protection for $currentSiteKey",
                    subtitle = if (currentSiteProtected) {
                        "Turn off if this site isn't working properly.$scope"
                    } else {
                        "Off: trackers on this site are not blocked.$scope"
                    },
                    checked = currentSiteProtected,
                    onCheckedChange = onCurrentSiteProtectionChange
                )
            } else {
                SheetInfoBox("Open a website to turn its protection on or off.")
            }

            // --- Protections (the same five switches as the privacy panel) ---
            SheetSectionLabel("Protections")
            ShieldToggleRow(
                title = "Block trackers",
                subtitle = "Third-party trackers and ads, on every site",
                checked = privacyState.blockTrackers,
                onCheckedChange = onToggleBlockTrackers
            )
            ShieldToggleRow(
                title = "Fingerprint shield",
                subtitle = if (pageShieldsSupported) "Randomizes canvas, audio and fonts" else UPDATE_WEBVIEW,
                checked = pageShieldsSupported && privacyState.fingerprintShield,
                enabled = pageShieldsSupported,
                onCheckedChange = onToggleFingerprintShield
            )
            ShieldToggleRow(
                title = "Block third-party cookies",
                subtitle = "First-party cookies still work. Always blocked in Incognito+",
                checked = privacyState.blockThirdPartyCookies,
                onCheckedChange = onToggleBlockThirdPartyCookies
            )
            ShieldToggleRow(
                title = "WebRTC leak guard",
                subtitle = if (pageShieldsSupported) "Hides your local IP; calls still work" else UPDATE_WEBVIEW,
                checked = pageShieldsSupported && privacyState.webRtcLeakGuard,
                enabled = pageShieldsSupported,
                onCheckedChange = onToggleWebRtcLeakGuard
            )
            ShieldToggleRow(
                title = "Wipe cookies on close",
                subtitle = "Incognito+ behaviour for every window",
                checked = privacyState.wipeCookiesOnClose,
                onCheckedChange = onToggleWipeCookiesOnClose
            )

            // --- Security ---
            SheetSectionLabel("Security")
            ShieldToggleRow(
                title = "Safe Browsing",
                subtitle = "Warns before phishing and malware sites. Checks some addresses " +
                    "with Google as partial hashes, not full web addresses.",
                checked = privacyState.safeBrowsing,
                onCheckedChange = onToggleSafeBrowsing
            )

            // --- Always on ---
            SheetSectionLabel("Always on")
            StatusRow("HTTPS only", "Insecure connections are upgraded or blocked", on = true)
            StatusRow("Tracking links cleaned", "utm_*, fbclid, gclid and similar are removed", on = true)
            StatusRow(
                "Incognito+ isolation",
                if (incognitoSupported) "Separate storage, wiped when Incognito+ closes"
                else "Unavailable: update Android System WebView",
                on = incognitoSupported
            )

            // --- Sites with protection off ---
            SheetSectionLabel("Sites with protection off · ${sitesWithProtectionOff.size}")
            SitesWithProtectionOff(sitesWithProtectionOff, onTurnProtectionBackOn, onTurnAllBackOn)

            Text(
                text = "No account · No sync · No telemetry",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun SettingsEntryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SpectraBgDark)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SpectraCyan, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}

/** Shared by settings and the dashboard. */
@Composable
internal fun SitesWithProtectionOff(
    sites: List<String>,
    onTurnProtectionBackOn: (String) -> Unit,
    onTurnAllBackOn: () -> Unit
) {
    if (sites.isEmpty()) {
        SheetInfoBox("None. Protection is on for every site.")
        return
    }
    sites.forEach { site ->
        ShieldRowContainer(title = site, subtitle = "Trackers not blocked") {
            SheetTextAction("Turn on", ShieldTeal) { onTurnProtectionBackOn(site) }
        }
    }
    if (sites.size > 1) {
        SheetTextAction("Turn protection back on for all sites", VioletText, onTurnAllBackOn)
    }
}

@Composable
private fun StatusRow(title: String, subtitle: String, on: Boolean) {
    ShieldRowContainer(title, subtitle) {
        val color = if (on) ShieldTeal else ShieldYellow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (on) "On" else "Off",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

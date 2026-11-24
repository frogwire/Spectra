package com.duck.privacy.browser.ui.components

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.ui.theme.*

/**
 * Spectra App Settings Dashboard.
 */
@Composable
fun SettingsSheet(
    currentSiteKey: String?,
    isIncognito: Boolean,
    incognitoSupported: Boolean,
    currentSiteProtected: Boolean,
    onCurrentSiteProtectionChange: (Boolean) -> Unit,
    sitesWithProtectionOff: List<String>,
    onTurnProtectionBackOn: (String) -> Unit,
    onTurnAllBackOn: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .clip(sheetShape)
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, sheetShape)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextMuted.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Privacy Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(
                text = "Done",
                color = SpectraCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // --- This site ---
            SectionLabel("THIS SITE")
            if (currentSiteKey != null) {
                val scope = if (isIncognito) " · changes last until incognito closes" else ""
                ShieldToggleRow(
                    title = "Protection for $currentSiteKey",
                    subtitle = if (currentSiteProtected) {
                        "Turn off if this site isn't working properly$scope"
                    } else {
                        "Off: trackers on this site are not blocked$scope"
                    },
                    checked = currentSiteProtected,
                    onCheckedChange = onCurrentSiteProtectionChange
                )
            } else {
                InfoBox("Open a website to turn its protection on or off.")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Saved list ---
            SectionLabel("SITES WITH PROTECTION OFF (${sitesWithProtectionOff.size})")
            if (sitesWithProtectionOff.isEmpty()) {
                InfoBox("None. Protection is on for every site.")
            } else {
                sitesWithProtectionOff.forEach { site ->
                    ShieldRowContainer(title = site, subtitle = "Trackers not blocked") {
                        Text(
                            text = "Turn on",
                            color = ShieldTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { onTurnProtectionBackOn(site) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }
                }
                Text(
                    text = "Turn protection back on for all sites",
                    color = SpectraViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onTurnAllBackOn)
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Always on ---
            SectionLabel("SPECTRA CORE PROTECTIONS")
            ShieldStatusRow("Fingerprint Shield", "Canvas, audio & font randomization", on = true)
            ShieldStatusRow("WebRTC Leak Guard", "Hides local IP while calls function", on = true)
            ShieldStatusRow("HTTPS Only", "Insecure connections are upgraded or blocked", on = true)
            ShieldStatusRow("Third-party cookies", "Always blocked", on = true)
            ShieldStatusRow("Tracking links cleaned", "utm_*, fbclid, gclid… stripped automatically", on = true)
            ShieldStatusRow(
                "Incognito+ Isolation",
                if (incognitoSupported) "Isolated profile & storage wiped on close"
                else "Unavailable: update Android System WebView",
                on = incognitoSupported
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun InfoBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpectraBgDark)
            .padding(10.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun ShieldStatusRow(title: String, subtitle: String, on: Boolean) {
    ShieldRowContainer(title, subtitle) {
        val color = if (on) ShieldTeal else TextMuted
        Text(
            text = if (on) "ON" else "OFF",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
        )
    }
}
